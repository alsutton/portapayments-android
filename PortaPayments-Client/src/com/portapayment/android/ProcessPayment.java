package com.portapayment.android;

import java.io.IOException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Bitmap;
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

import com.portapayment.android.paypal.PayPalHelper;
import com.portapayment.android.paypal.PayPalHelper.PayPalException;
import com.portapayment.android.paypal.PayPalHelper.PayPalExceptionWithErrorCode;

public class ProcessPayment extends Activity {
	
	/**
	 * The URL for completed payments.
	 */
	
	private final static String PAYMENT_OK_URL = "http://postpay.portapayments.mobi/ppm/PayOK.jsp";
	
	/**
	 * The URL stub for passing the payment authentication to PayPal
	 */

	private final static String PAY_URL_STUB = "https://www.sandbox.paypal.com/webscr?cmd=_ap-payment&paykey=";
	private final static int PAY_URL_STUB_LENGTH = PAY_URL_STUB.length();
	
	/**
	 * The intent extra holding the text to encode.
	 */
	
	public static final String PAYMENT_DATA_EXTRA = "QR_DATA";
	
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
     */

    public void raiseError(final int errorMessageId) {
    	handler.post(new MyErrorPoster(errorMessageId));
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
    		if(!qrData.startsWith("r_")) {
    			raiseError(R.string.error_bad_format);
    		}
    		
    		int firstUnderscore = qrData.indexOf('_', 2);
    		if(firstUnderscore == -1) {
    			raiseError(R.string.error_bad_format);
    		}
    		
    		int secondUnderscore = qrData.indexOf('_', firstUnderscore+1);
    		if(secondUnderscore == -1) {
    			raiseError(R.string.error_bad_format);
    		}
    		
    		final String amount =  qrData.substring(2, firstUnderscore);
    		String currency = qrData.substring(firstUnderscore+1, secondUnderscore);
    		String recipient = qrData.substring(secondUnderscore+1);
    		
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ProcessPayment.this);
            String sender = prefs.getString(Preferences.PAYPAL_USERNAME, "");
    		
			try {
				final String payKey = PayPalHelper.startPayment(ProcessPayment.this, 
						sender, recipient, currency, amount);
	    		if(payKey == null) {
	    			handler.post(new MyHTMLRedirectHandler(ProcessPayment.PAYMENT_OK_URL));
	    			return;
	    		}
	    		
	    		final StringBuilder paypalAuthURLBuilder = new StringBuilder(PAY_URL_STUB_LENGTH+payKey.length());
	    		paypalAuthURLBuilder.append(PAY_URL_STUB);
	    		paypalAuthURLBuilder.append(payKey);
	    		paypalAuthURLBuilder.append("&login_email=");
	    		paypalAuthURLBuilder.append(sender);
	    		
				handler.post(new MyHTMLRedirectHandler(paypalAuthURLBuilder.toString()));
			} catch (PayPalExceptionWithErrorCode e) {
				Log.e("PortaPayments", "Error returned by PayPal : "+e.getErrorCode(), e);
				raiseError(R.string.error_paypal_process);
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
    
    private class MyHTMLRedirectHandler implements Runnable {
    	private String url;
    	
    	MyHTMLRedirectHandler(final String url) {
    		this.url = url;
    	}
    	
    	public void run() {
    		webView.loadUrl(url);
    	}
    }
    
    /**
     * Runnable to hand to the handler to handle the bouncing to the web page (handle handle handle :)).
     */
    
    private class MyErrorPoster implements Runnable {
    	private int errorMessageId;
    	
    	MyErrorPoster(final int errorMessageId) {
    		this.errorMessageId = errorMessageId;
    	}
    	
    	public void run() {
        	new AlertDialog.Builder(ProcessPayment.this)
    		.setIcon(android.R.drawable.ic_dialog_alert)
    		.setTitle(R.string.dlg_error_title)
    		.setMessage(errorMessageId)
    		.setPositiveButton(R.string.dialog_ok, new OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					ProcessPayment.this.finish();
				}
    		})
    		.setOnCancelListener(new OnCancelListener() {
				public void onCancel(DialogInterface dialog) {
					ProcessPayment.this.finish();					
				}    			
    		})
    		.show();
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
