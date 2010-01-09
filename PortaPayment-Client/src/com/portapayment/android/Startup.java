package com.portapayment.android;

import com.google.zxing.client.android.CaptureActivity;
import com.google.zxing.client.android.Intents;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class Startup extends Activity {
	/**
	 * The request code for scanning a payment
	 */

	private static final int SCAN_PAYMENT_CODE = 0;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.main);
        
        ((Button)findViewById(R.id.show_qr_button)).setOnClickListener(
        		new OnClickListener() {
					public void onClick(View v) {
						generateCode();
					}
        		}
        	);
        
        ((Button)findViewById(R.id.read_qr_button)).setOnClickListener(
        		new OnClickListener() {
					public void onClick(View v) {
						Intent startIntent = new Intent(Startup.this, CaptureActivity.class);
						startIntent.setAction(Intents.Scan.ACTION);
						startIntent.putExtra(Intents.Scan.MODE, Intents.Scan.QR_CODE_MODE);
						Startup.this.startActivityForResult(startIntent, SCAN_PAYMENT_CODE);
					}
        		}
        	);
        
        ((Button)findViewById(R.id.preferences_button)).setOnClickListener(
        		new OnClickListener() {
					public void onClick(View v) {
						Intent startIntent = new Intent(Startup.this, Preferences.class);
						Startup.this.startActivity(startIntent);
					}
        		}
        	);
        
        EditText amountBox = (EditText)findViewById(R.id.amount);
        amountBox.requestFocus();
        amountBox.setSelection(0, amountBox.getText().length());
        
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String paypalUsername = prefs.getString(Preferences.PAYPAL_USERNAME, "");
        if(paypalUsername == null || paypalUsername.length() == 0) {
        	handleNoPayPalUsername();
        }
    }

    /**
     * Handles the response from the scanner.
     */
    
    protected void onActivityResult(int requestCode, int resultCode,
            Intent data) {
        if (requestCode == SCAN_PAYMENT_CODE) {
            if (resultCode == RESULT_OK) {
            	Log.e("PORTAPAY", "Got : "+data.getStringExtra(Intents.Scan.RESULT));
            }
        }
    }
    
    /**
     * Work out the QR code to use and start the QR code display activity.
     */
    
    private void generateCode() {
    	String amount = ((EditText) findViewById(R.id.amount)).getText().toString();
    	String currency = "USD";
    	String recipient = "al@foo.bar";
    	
    	StringBuilder data = new StringBuilder(amount.length()+currency.length()+recipient.length()+2);
    	data.append(amount);
    	data.append('_');
    	data.append(currency);
    	data.append('_');
    	data.append(recipient);
    	
		Intent startIntent = new Intent(Startup.this, DisplayQRCode.class);
		startIntent.putExtra(DisplayQRCode.ENCODE_DATA_EXTRA, data.toString());
		Startup.this.startActivity(startIntent);    	
    }
    
    /**
     * Handle the case where no PayPal username has been entered.
     */
    
    private void handleNoPayPalUsername() {
    	new AlertDialog.Builder(this).
    		setTitle(R.string.no_paypalid_dialog_title).
    		setIcon(android.R.drawable.ic_dialog_alert).
    		setMessage(R.string.no_paypalid_dialog_message).
    		setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {				
				public void onClick(DialogInterface dialog, int which) {
					Intent startIntent = new Intent(Startup.this, Preferences.class);
					Startup.this.startActivity(startIntent);
    			}
    		}).
    		show();
    	
    }
 }