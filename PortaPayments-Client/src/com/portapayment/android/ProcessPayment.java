package com.portapayment.android;

import java.io.IOException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Window;

import com.portapayment.android.paypal.PayPalHelper;
import com.portapayment.android.paypal.PayPalHelper.PayPalException;


public class ProcessPayment extends Activity {
	
	/**
	 * The URL for completed payments.
	 */
	
	private final static String PAYMENT_OK_URL = "http://portapayments.com/pp/PayOK.jsp";
	
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
	 * The handler used to populate the encoded image area
	 */
	
	private final Handler handler = new Handler();
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.processing_payment);
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
    		int firstUnderscore = qrData.indexOf('_');
    		if(firstUnderscore == -1) {
    			raiseError(R.string.error_bad_format);
    		}
    		
    		int secondUnderscore = qrData.indexOf('_', firstUnderscore+1);
    		if(secondUnderscore == -1) {
    			raiseError(R.string.error_bad_format);
    		}
    		
    		final String amount =  qrData.substring(0, firstUnderscore);
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
			} catch (PayPalException ppe) {
				Log.e("PortaPayments", "Error returned by PayPal", ppe);
				raiseError(R.string.error_paypal_process);
			} catch (IOException e) {
				Log.e("PortaPayments", "Error talking to PayPal.", e );
				raiseError(R.string.error_io);
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
    		Intent webIntent = new Intent(Intent.ACTION_VIEW);
    		webIntent.setData(Uri.parse(url));
    		ProcessPayment.this.startActivity(webIntent);
    		ProcessPayment.this.finish();
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
    		}).show();
    	}
    }
}
