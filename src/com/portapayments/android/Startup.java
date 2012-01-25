package com.portapayments.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.Button;
import android.widget.EditText;

import com.flurry.android.FlurryAgent;
import com.portapayments.android.zxing.CaptureActivity;
import com.portapayments.android.zxing.Intents;

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
                
        Button readQrButton = (Button)findViewById(R.id.read_qr_button);
        readQrButton.setOnClickListener(
        		new OnClickListener() {
					public void onClick(View v) {
				    	final String recipient = getPayPalUsername();
				        if(recipient == null) {
				        	return;
				        }

				        checkAutofocusAndStartScanner();
					}
        		}
        	);

        ((Button)findViewById(R.id.create_qr_quick_button)).setOnClickListener(
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

        ((Button)findViewById(R.id.create_qr_button)).setOnClickListener(
        		new OnClickListener() {
					public void onClick(View v) {
				    	final String recipient = getPayPalUsername();
				        if(recipient == null) {
				        	return;
				        }
				        
						Intent startIntent = new Intent(Startup.this, CreateRequestActivity.class);
						Startup.this.startActivity(startIntent);
					}
        		}
        	);
        
        
		((EditText)findViewById(R.id.amount)).
			setOnKeyListener(new OnKeyListener(){
				public boolean onKey(View v, int keyCode, KeyEvent event) {
					Editable text = ((EditText)v).getText();
					int length = text.length();
					int dotIdx = -1;
					for(int i = 0 ; i < length ; i++) {
						if(text.charAt(i) == '.') {
							dotIdx = i;
							break;
						}
					}
					if( dotIdx != -1 && dotIdx + 3 < length ) {
						text.delete(dotIdx+3, length);
					}
					
					return false;
				}    				
			});

        currencyButton = (Button)findViewById(R.id.currency);
        currencyButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				selectCurrency();
			}        	
        });

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        if(currencyButton.getText() == null || currencyButton.getText().length() == 0) {
	        currencyButton.setText(prefs.getString(Preferences.DEFAULT_CURRENCY, "USD"));
        }
        
        boolean aboutShown = prefs.getBoolean("about_shown", false);
        if(!aboutShown) {
        	try {
	        	showAbout();
	        	Editor editor = prefs.edit();
	        	editor.putBoolean("about_shown", true);
	        	editor.commit();
        	} catch(Exception ex) {
        		Log.e("PortaPayments", "Error blocking about screen.", ex);
        	}
        }
        
        getPayPalUsername();
        readQrButton.requestFocus();
    }

    /**
     * Start the flurry session
     */
    
    @Override
    public void onStart() {
    	super.onStart();
    	FlurryAgent.onStartSession(this, "F6XKDGEXRCNXKZVIMBID");
    }
    
    /**
     * Stop the flurry session
     */
    
    @Override
    public void onStop() {
    	FlurryAgent.onEndSession(this);
    	super.onStop();
    }
    
    /**
     * Handles the response from the scanner.
     */
    
    protected void onActivityResult(int requestCode, int resultCode,
            Intent data) {
        if (requestCode == SCAN_PAYMENT_CODE
        &&	resultCode == RESULT_OK) {
    		FlurryAgent.onEvent("Barcode Scanner returned a result");
           	parseScannedData( data.getStringExtra(Intents.Scan.RESULT) );
        }
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
	    	  showAbout();
	          break;
	      case SETTINGS_MENU_OPTION:
			Intent startIntent = new Intent(Startup.this, Preferences.class);
			Startup.this.startActivity(startIntent);
			break;
	  }
      return super.onOptionsItemSelected(item);
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
	    	} else if (data.startsWith("http://")) {
		    	Intent startIntent = new Intent();
		    	startIntent.setAction(Intent.ACTION_VIEW);
		    	startIntent.addCategory(Intent.CATEGORY_BROWSABLE);
		    	startIntent.setData(Uri.parse(data));
		    	startActivity(startIntent);
		    	return;
	    	}  else {
				FlurryAgent.onEvent("Barcode Scanner returned an unknown code");
	    	}
    	} else {
			FlurryAgent.onEvent("Barcode Scanner returned an too-short code");    		
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
			startIntent.putExtra(DisplayQRCode.RECIPIENT+"_0", recipient);
			startIntent.putExtra(DisplayQRCode.AMOUNT+"_0", amount);
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

    /**
     * Checks the camera has an autofocus option and handles warning the user
     * or starting the scanner.
     */
    
    private void checkAutofocusAndStartScanner() {
    	Camera camera = Camera.open();
    	try {
    		startScanner();
    	} finally {
    		camera.release();
    	}
    }
    
    /**
     * Show the about screen.
     */
    
    private void showAbout() {
  	  LayoutInflater inflater = 
		  (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
      AlertDialog.Builder builder = new AlertDialog.Builder(this);
      builder.setView(inflater.inflate(R.layout.about, (ViewGroup) findViewById(R.id.about_root)));
      builder.setPositiveButton(R.string.dialog_ok, null);
      builder.show();
    }
    
    /**
     * Warn the user about their device not having autofocus
     */
    
/*    private void warnAboutAutofocus() {
    	new AlertDialog.Builder(this)
		.setIcon(android.R.drawable.ic_dialog_info)
		.setTitle(R.string.dlg_non_autofocus_title)
		.setMessage(R.string.dlg_non_autofocus)
		.setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				startScanner();
			}
		})
		.show();    	
    }
*/    
    /**
     * Starts the barcode scanner
     */
    
    private void startScanner() {
		Intent startIntent = new Intent(Startup.this, CaptureActivity.class);
		startIntent.setAction(Intents.Scan.ACTION);
		startIntent.putExtra(Intents.Scan.MODE, Intents.Scan.QR_CODE_MODE);
		FlurryAgent.onEvent("Barcode Scanner Started");
		Startup.this.startActivityForResult(startIntent, SCAN_PAYMENT_CODE);
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