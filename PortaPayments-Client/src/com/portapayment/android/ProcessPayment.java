package com.portapayment.android;

import java.io.IOException;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.webkit.WebView;

import com.portapayment.android.paypal.PayPalHelper;


public class ProcessPayment extends Activity {
	
	/**
	 * The URL for completed payments.
	 */
	
	private final static String PAYMENT_OK_URL = "http://portapayments.com/pp/PayOK.jsp";
	
	/**
	 * The URL stub for passing the payment authentication to PayPal
	 */

	private final static String PAY_URL_STUB = "https://www.paypal.com/webscr?vmd=_ap-payment&paykey=";
	private final static int PAY_URL_STUB_LENGTH = PAY_URL_STUB.length();
	
	/**
	 * The intent extra holding the text to encode.
	 */
	
	public static final String PAYMENT_DATA_EXTRA = "QR_DATA";
	
	 /**
	 * The handler used to populate the encoded image area
	 */
	
	private final Handler handler = new Handler();
	
	/**
	 * The WebView on display
	 */
	
	private WebView webView;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.processing_payment);
        webView = (WebView) findViewById(R.id.webview);
        webView.getSettings().setJavaScriptEnabled(true);
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
    		int firstUnderscore = qrData.indexOf('_');
    		if(firstUnderscore == -1) {
    			raiseError(R.string.error_bad_format);
    		}
    		
    		int secondUnderscore = qrData.indexOf('_', firstUnderscore+1);
    		if(secondUnderscore == -1) {
    			raiseError(R.string.error_bad_format);
    		}
    		
    		int thirdUnderscore = qrData.indexOf('_', secondUnderscore+1);
    		if(thirdUnderscore == -1) {
    			raiseError(R.string.error_bad_format);
    		}
    		
    		StringBuilder amountBuilder = new StringBuilder(secondUnderscore);
    		amountBuilder.append(qrData.substring(0,firstUnderscore));
    		amountBuilder.append('.');
    		amountBuilder.append(qrData.substring(firstUnderscore+1,secondUnderscore));
    		final String amount = amountBuilder.toString();
    		String currency = qrData.substring(secondUnderscore+1, thirdUnderscore);
    		String recipient = qrData.substring(thirdUnderscore+1);
    		
    		Log.e("PP", amount+":"+currency+":"+recipient);
/*            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ProcessPayment.this);
            String sender = prefs.getString(Preferences.PAYPAL_USERNAME, "");
    		
			try {
				final String payKey = PayPalHelper.startPayment(sender, recipient, currency, amount);
	    		if(payKey == null) {
	    			handler.post(new MyHTMLRedirectHandler(ProcessPayment.PAYMENT_OK_URL));
	    			return;
	    		}
	    		
	    		final StringBuilder paypalAuthURLBuilder = new StringBuilder(PAY_URL_STUB_LENGTH+payKey.length());
	    		paypalAuthURLBuilder.append(PAY_URL_STUB);
	    		paypalAuthURLBuilder.append(payKey);
	    		
				handler.post(new MyHTMLRedirectHandler(paypalAuthURLBuilder.toString()));
			} catch (IOException e) {
				Log.e("PortaPayments", "Error talking to PayPal.", e );
				raiseError(R.string.error_io);
			}
*/    	}
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
}
