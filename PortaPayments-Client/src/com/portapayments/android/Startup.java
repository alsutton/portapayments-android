package com.portapayments.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

import com.google.zxing.client.android.CaptureActivity;
import com.google.zxing.client.android.Intents;

public final class Startup extends Activity {
	/**
	 * The request code for scanning a payment
	 */

	private static final int SCAN_PAYMENT_CODE = 0;
	
	/**
	 * The IDs of the about menu options
	 */
	
	private static final int	ABOUT_MENU_OPTION = 0,
								SETTINGS_MENU_OPTION = 1;
	
	/**
	 * The currency button.
	 */
	
	private Button currencyButton;
	
	/**
	 * Handler for manipulating the UI outside of the UI thread.
	 */
	
	private final Handler handler = new Handler();
	
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
        
        Button readQrButton = (Button)findViewById(R.id.read_qr_button);
        readQrButton.setOnClickListener(
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
        
        ((Button)findViewById(R.id.view_history_button)).setOnClickListener(
        		new OnClickListener() {
					public void onClick(View v) {
						Intent startIntent = new Intent(Startup.this, History.class);
						Startup.this.startActivity(startIntent);
					}
        		}
        	);
        
//        EditText amountBox = (EditText)findViewById(R.id.amount);
//        amountBox.requestFocus();
//        amountBox.setSelection(0, amountBox.getText().length());

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
        readQrButton.requestFocus();
    }

    /**
     * Handles the response from the scanner.
     */
    
    protected void onActivityResult(int requestCode, int resultCode,
            Intent data) {
        if (requestCode == SCAN_PAYMENT_CODE
        &&	resultCode == RESULT_OK) {
           	parseScannedData( data.getStringExtra(Intents.Scan.RESULT) );
        }
    }
    
    /**
     * Handle some scanned data
     */
    
    private void parseScannedData(final String data) {
    	if(data != null && data.length() > 3) {
			if(data.startsWith("r\n")) {
		    	Intent startIntent = new Intent(this, ProcessPayment.class);
		    	startIntent.putExtra(ProcessPayment.PAYMENT_DATA_EXTRA, data);
		    	startActivity(startIntent);
		    	return;
	    	} else if (data.startsWith("s\n")) {
	    		return;
	    	}
    	}
    	
		raiseError(R.string.error_bad_format);
    }
    
    /**
     * Work out the QR code to use and start the QR code display activity.
     */
    
    private void generateCode() {
    	final String recipient = getPayPalUsername();
        if(recipient == null) {
        	return;
        }
        
        final String memo = ((EditText) findViewById(R.id.note)).getText().toString();
        final String amount = ((EditText) findViewById(R.id.amount)).getText().toString();
    	final String currency = currencyButton.getText().toString();
    	
		try {
			Intent startIntent = new Intent(Startup.this, DisplayQRCode.class);
			startIntent.putExtra(DisplayQRCode.RECIPIENT, recipient);
			startIntent.putExtra(DisplayQRCode.AMOUNT, amount);
			startIntent.putExtra(DisplayQRCode.CURRENCY, currency);
			startIntent.putExtra(DisplayQRCode.MEMO, memo);
			Startup.this.startActivity(startIntent);    	
		} catch (Exception e) {
			e.printStackTrace();
		}
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
    
    /**
     * Starts the currency selector.
     * 
     */
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


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
      super.onCreateOptionsMenu(menu);
      menu.add(0, ABOUT_MENU_OPTION, 0, R.string.menu_about)
      	.setIcon(android.R.drawable.ic_menu_info_details);
      menu.add(0, SETTINGS_MENU_OPTION, 0, R.string.menu_settings)
          .setIcon(android.R.drawable.ic_menu_preferences);
      return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
      switch (item.getItemId()) {
	      case ABOUT_MENU_OPTION:
	          AlertDialog.Builder builder = new AlertDialog.Builder(this);
	          builder.setTitle(getString(R.string.about_title));
	          builder.setMessage(getString(R.string.about_message));
	          builder.setIcon(R.drawable.icon);
	          builder.setPositiveButton(R.string.dialog_ok, null);
	          builder.show();
	          break;
	      case SETTINGS_MENU_OPTION:
			Intent startIntent = new Intent(Startup.this, Preferences.class);
			Startup.this.startActivity(startIntent);
			break;
	  }
      return super.onOptionsItemSelected(item);
    }
    
    /**
     * Raise an error.
     * 
     * @param errorMessageId The resource ID for the error message.
     */

    public void raiseError(final int errorMessageId) {
    	handler.post(new ErrorPoster(this, errorMessageId, null, null));
    }
}