package com.portapayment.android;

import com.google.zxing.client.android.CaptureActivity;
import com.google.zxing.client.android.Intents;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public final class Startup extends Activity {
	/**
	 * The request code for scanning a payment
	 */

	private static final int SCAN_PAYMENT_CODE = 0;
	
	/**
	 * The currency button.
	 */
	
	private Button currencyButton;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.main);
        
        ((Button)findViewById(R.id.show_qr_button)).setOnClickListener(
        		new OnClickListener() {
					public void onClick(View v) {
				    	final String recipient = getPayPalUsername();
				        if(recipient == null) {
				        	return;
				        }

						generateCode();
					}
        		}
        	);
        
        ((Button)findViewById(R.id.read_qr_button)).setOnClickListener(
        		new OnClickListener() {
					public void onClick(View v) {
				    	final String recipient = getPayPalUsername();
				        if(recipient == null) {
				        	return;
				        }

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

        currencyButton = (Button)findViewById(R.id.currency);
        currencyButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				selectCurrency();
			}        	
        });

        if(currencyButton.getText() == null || currencyButton.getText().length() == 0) {
	        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
	        currencyButton.setText(prefs.getString(Preferences.DEFAULT_CURRENCY, "USD"));
        }
        
        getPayPalUsername();
    }

    /**
     * Handles the response from the scanner.
     */
    
    protected void onActivityResult(int requestCode, int resultCode,
            Intent data) {
        if (requestCode == SCAN_PAYMENT_CODE) {
            if (resultCode == RESULT_OK) {
            	Intent startIntent = new Intent(this, ProcessPayment.class);
            	startIntent.putExtra(ProcessPayment.PAYMENT_DATA_EXTRA, data.getStringExtra(Intents.Scan.RESULT));
            	startActivity(startIntent);
            }
        }
    }
    
    /**
     * Work out the QR code to use and start the QR code display activity.
     */
    
    private void generateCode() {
    	final String recipient = getPayPalUsername();
        if(recipient == null) {
        	return;
        }
        
        final String amount = ((EditText) findViewById(R.id.amount)).getText().toString();
    	final String currency = currencyButton.getText().toString();
    	
    	final StringBuilder data = new StringBuilder(amount.length()+currency.length()+recipient.length()+2);
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
    
    private String getPayPalUsername() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String paypalUsername = prefs.getString(Preferences.PAYPAL_USERNAME, "");
        if(paypalUsername == null || paypalUsername.length() == 0) {
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
	    	
	    	return null;
        }
        return paypalUsername;
    }
    
    
    protected void selectCurrency() {
    	Builder builder = new AlertDialog.Builder(this);
    	
        String[] currencyValues = this.getResources().getStringArray(R.array.preferences_default_currency_values);
        
        String currentCurrency = currencyButton.getText().toString();
        int idx;
        for(idx = 0; idx < currencyValues.length; idx++) {
        	if(currentCurrency.equals(currencyValues[idx])) {
        		break;
        	}
        }
        if(idx == currencyValues.length) {
        	idx --;
        }
        
        builder.setItems(
        		R.array.preferences_default_currency_entries, 
        		new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
				        String[] currencyValues = 
				        	Startup.this.getResources().getStringArray(R.array.preferences_default_currency_values);
						currencyButton.setText(currencyValues[which]);
						dialog.dismiss();
					}
				});
				
		builder.setPositiveButton(null, null);
        builder.show();
    }
}