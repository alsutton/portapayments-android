package com.portapayments.android;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.portapayments.android.database.PaymentsProvider;
import com.portapayments.android.paypal.PayPalHelper;
import com.portapayments.android.paypal.PayPalHelper.PayPalException;
import com.portapayments.android.paypal.PayPalHelper.PayPalExceptionWithErrorCode;
import com.portapayments.android.util.DataDecoder;

public final class ProcessPayment extends Activity {
	
	/**
	 * The URL for completed payments.
	 */
	
	private final static String PAYMENT_OK_URL = "http://postpay.portapayments.mobi/ppm/PayOK.jsp";
	
	/**
	 * The URL stub for passing the payment authentication to PayPal
	 */

	private final static String PAY_URL_STUB = "https://www.paypal.com/webscr?cmd=_ap-payment&paykey=";
	private final static int PAY_URL_STUB_LENGTH = PAY_URL_STUB.length();
	
	/**
	 * The intent extra holding the text to encode.
	 */
	
	public static final String PAYMENT_DATA_EXTRA = "QR_DATA";
	
	/**
	 * A map of PayPal error codes to their Android String ID
	 */
	
	private static final Map<String,Integer> payPalErrorMap = new HashMap<String,Integer>();
	
	static {
		payPalErrorMap.put("500000",R.string.paypal_error_500000);
		payPalErrorMap.put("520002",R.string.paypal_error_520002);
		payPalErrorMap.put("520003",R.string.paypal_error_520003);
		payPalErrorMap.put("520005",R.string.paypal_error_520005);
		payPalErrorMap.put("520006",R.string.paypal_error_520006);
		payPalErrorMap.put("529038",R.string.paypal_error_529038);
		payPalErrorMap.put("539012",R.string.paypal_error_539012);
		payPalErrorMap.put("539041",R.string.paypal_error_539041);
		payPalErrorMap.put("539043",R.string.paypal_error_539043);
		payPalErrorMap.put("540031",R.string.paypal_error_540031);
		payPalErrorMap.put("559044",R.string.paypal_error_559044);
		payPalErrorMap.put("560027",R.string.paypal_error_560027);
		payPalErrorMap.put("569000",R.string.paypal_error_569000);
		payPalErrorMap.put("569013",R.string.paypal_error_569013);
		payPalErrorMap.put("569016",R.string.paypal_error_569016);
		payPalErrorMap.put("569017",R.string.paypal_error_569017);
		payPalErrorMap.put("569018",R.string.paypal_error_569018);
		payPalErrorMap.put("569019",R.string.paypal_error_569019);
		payPalErrorMap.put("569042",R.string.paypal_error_569042);
		payPalErrorMap.put("579007",R.string.paypal_error_579007);
		payPalErrorMap.put("579010",R.string.paypal_error_579010);
		payPalErrorMap.put("579014",R.string.paypal_error_579014);
		payPalErrorMap.put("579017",R.string.paypal_error_579017);
		payPalErrorMap.put("579024",R.string.paypal_error_579024);
		payPalErrorMap.put("579025",R.string.paypal_error_579025);
		payPalErrorMap.put("579026",R.string.paypal_error_579026);
		payPalErrorMap.put("579027",R.string.paypal_error_579027);
		payPalErrorMap.put("579028",R.string.paypal_error_579028);
		payPalErrorMap.put("579030",R.string.paypal_error_579030);
		payPalErrorMap.put("579031",R.string.paypal_error_579031);
		payPalErrorMap.put("579033",R.string.paypal_error_579033);
		payPalErrorMap.put("579040",R.string.paypal_error_579040);
		payPalErrorMap.put("579042",R.string.paypal_error_579042);
		payPalErrorMap.put("579045",R.string.paypal_error_579045);
		payPalErrorMap.put("579047",R.string.paypal_error_579047);
		payPalErrorMap.put("579048",R.string.paypal_error_579048);
		payPalErrorMap.put("580001",R.string.paypal_error_580001);
		payPalErrorMap.put("580023",R.string.paypal_error_580023);
		payPalErrorMap.put("580027",R.string.paypal_error_580027);
		payPalErrorMap.put("580028",R.string.paypal_error_580028);
		payPalErrorMap.put("580029",R.string.paypal_error_580029);
		payPalErrorMap.put("589009",R.string.paypal_error_589009);
		payPalErrorMap.put("589023",R.string.paypal_error_589023);
		payPalErrorMap.put("589039",R.string.paypal_error_589039);
	}
	/**
	 * The WebView used to communicate with PayPal
	 */
	
	private WebView webView;
	
	/**
	 * The handler used to populate the encoded image area
	 */
	
	private final Handler handler = new Handler();
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.requestWindowFeature(Window.FEATURE_PROGRESS);
        setContentView(R.layout.processing_payment);
        webView = (WebView) findViewById(R.id.webview);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setSupportZoom(false);
        webView.getSettings().setSaveFormData(false);
        webView.getSettings().setSavePassword(false);
        webView.setWebViewClient(new WebViewClient() {
        	@Override
        	public void onPageStarted(final WebView view, final String url, final Bitmap favicon) {
        		ProcessPayment.this.setProgress(0);
        	}
        	@Override
        	public void onPageFinished(final WebView view, final String url) {
        		ProcessPayment.this.findViewById(R.id.webview_wait_message).setVisibility(View.GONE);
        		view.setVisibility(View.VISIBLE);
        		view.bringToFront();
        		view.requestFocus();
        	}
        });
        webView.setWebChromeClient(new WebChromeClient() {
        	@Override
        	public void onProgressChanged(final WebView view, final int progress) {
        		ProcessPayment.this.setProgress(progress*100);
        	}
            @Override         
            public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
            	Log.d("PortaPayments", message);
            	result.confirm();
            	return true;         
            }
        });
        webView.addJavascriptInterface(new PortaPaymentsJavascriptInterface(), "PortaPayments");
        webView.requestFocus();
    }

    @Override
    public void onResume() {
    	super.onResume();
    	
    	String codeText = getIntent().getStringExtra(PAYMENT_DATA_EXTRA);
    	if( codeText == null ) {
    		finish();
    		return;
    	}
    	
    	new Thread( new MyPayPalCommunicationThread(codeText) ).start();
    }
   
    /**
     * Raise an error.
     * 
     * @param errorMessageId The resource ID for the error message.
     */

    public void raiseError(final int errorMessageId) {
    	handler.post(ErrorPoster.getClosingError(this, errorMessageId));
    }
    
    /**
     * Class to encoding the data into a QR code then display it.
     */
    
    private class MyPayPalCommunicationThread implements Runnable {
    	
    	private final String qrData;
    	
    	MyPayPalCommunicationThread( final String qrData ) {
    		this.qrData = qrData;    		
    	}
    	
    	public void run () {
    		DataDecoder.RequestDetails request = null;
    		try {
    			request = DataDecoder.parseRequest(qrData);
    		} catch(DataDecoder.FormatException ex) {
    			Log.e("PortaPayments", "Error in request format :"+qrData, ex);
				raiseError(R.string.error_bad_format);
    		}
    		
    		Uri entryUri = null;
    		try {
    			ContentValues values = new ContentValues();
	    		values.put("scannedData", qrData);
				values.put("timestamp", System.currentTimeMillis());
				entryUri = ProcessPayment.this.
							getContentResolver().
								insert(PaymentsProvider.CONTENT_URI, values);
    		} catch(Exception ex) {
    			Log.e("PortaPayments", "Error recording history", ex);
    		}

    		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ProcessPayment.this);
            String sender = prefs.getString(Preferences.PAYPAL_USERNAME, "");
    		
			try {
				final String payKey = 
					PayPalHelper.startPayment(ProcessPayment.this, sender, request);
	    		if(payKey == null) {
	    			handler.post(new MyHTMLRedirectHandler(ProcessPayment.PAYMENT_OK_URL));
	    			return;
	    		}	    		
	
	    		if(entryUri != null) {
		    		try {
		    			ContentValues values = new ContentValues();
		    			values.put("payKey", payKey);
						ProcessPayment.this.
							getContentResolver().
								update(entryUri, values, null, null);
		    		} catch(Exception ex) {
		    			Log.e("PortaPayments", "Error updating history", ex);
		    		}
	    		}
	    		
	    		final StringBuilder paypalAuthURLBuilder = new StringBuilder(PAY_URL_STUB_LENGTH+payKey.length());
	    		paypalAuthURLBuilder.append(PAY_URL_STUB);
	    		paypalAuthURLBuilder.append(payKey);
	    		paypalAuthURLBuilder.append("&login_email=");
	    		paypalAuthURLBuilder.append(sender);
	    		
				handler.post(new MyHTMLRedirectHandler(paypalAuthURLBuilder.toString()));
			} catch (PayPalExceptionWithErrorCode e) {
				Log.e("PortaPayments", "Error returned by PayPal : "+e.getErrorCode(), e);
				int errorCode;
				if( e.getErrorCode() == null ) {
					errorCode = R.string.error_paypal_process;
				} else {
					Integer errorCodeObject = ProcessPayment.payPalErrorMap.get(e.getErrorCode());
					if(errorCodeObject == null) {
						errorCode = R.string.error_paypal_process;
					} else {
						errorCode = errorCodeObject;
					}
				}
				raiseError(errorCode);
			} catch (PayPalException e) {
				Log.e("PortaPayments", "Error returned by PayPal", e);
				raiseError(R.string.error_paypal_process);
			} catch (IOException e) {
				Log.e("PortaPayments", "Error talking to PayPal.", e );
				raiseError(R.string.error_io);
			} catch (Exception e) {
				Log.e("PortaPayments", "Non-specific error", e);
				raiseError(R.string.error_general);
			}
    	}
    }
    
    /**
     * Runnable to hand to the handler to handle the bouncing to the web page (handle handle handle :)).
     */
    
    private final class MyHTMLRedirectHandler implements Runnable {
    	private String url;
    	
    	MyHTMLRedirectHandler(final String url) {
    		this.url = url;
    	}
    	
    	public void run() {
    		webView.loadUrl(url);
    	}
    }
    
    /**
     * Javascript interface allowing the web page to finish this activity
     */
    
    final class PortaPaymentsJavascriptInterface {
    	PortaPaymentsJavascriptInterface() {
    		super();
    	}
    	
    	/**
    	 * End this activity
    	 */
    	
    	public void finish() {
    		handler.post(new Runnable() {
				public void run() {
					ProcessPayment.this.finish();
				}
    		});
    	}
    }
}
