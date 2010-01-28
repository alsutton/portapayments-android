package com.portapayments.android;

import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public final class CreateRequestActivity extends Activity {
	/**
	 * The IDs of the about menu options
	 */
	
	private static final int	ABOUT_MENU_OPTION = 0,
								SETTINGS_MENU_OPTION = 1;
	
	/**
	 * The array of sections containing payments.
	 */
	
	private static final int	PAYMENT_SECTION_IDS[] = {
		R.id.payment_1, R.id.payment_2, R.id.payment_3,
		R.id.payment_4, R.id.payment_5, R.id.payment_6
	};
	
	/**
	 * Handler for manipulating the UI outside of the UI thread.
	 */
	
	private final Handler handler = new Handler();

	/**
	 * The currency symbol in use
	 */
	
	private String currencySymbol;
	
	/**
	 * The currency in use (3 letter ISO code).
	 */
	
	private String currencyCode;
	
	/**
	 * The memo field in use.
	 */
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.create_request);
        
    	((EditText)(findViewById(R.id.payment_1).findViewById(R.id.recipient))).
        	setText(getPayPalUsername());
    	
    	for(int i = 0 ; i < CreateRequestActivity.PAYMENT_SECTION_IDS.length ; i++) {
    		final View paymentView = findViewById(PAYMENT_SECTION_IDS[i]);
    		((EditText)paymentView.findViewById(R.id.amount)).
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
						
						updateTotal();
						return false;
					}    				
    			});
    	}
    	
    	
    	((Button)findViewById(R.id.create_button)).
    		setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					Intent startIntent = new Intent(CreateRequestActivity.this, DisplayQRCode.class);
					startIntent.putExtra(DisplayQRCode.CURRENCY, currencyCode);
					startIntent.putExtra(
							DisplayQRCode.MEMO, 
							((EditText)findViewById(R.id.note)).getText().toString()
						);

			    	for(int i = 0 ; i < CreateRequestActivity.PAYMENT_SECTION_IDS.length ; i++) {
			    		final View paymentView = findViewById(PAYMENT_SECTION_IDS[i]);
			    		try {
			    			String amount = 
			    				((EditText)paymentView.
			    						findViewById(R.id.amount))
			    							.getText().toString();
			    			if(amount == null || amount.length() == 0) {
			    				continue;
			    			}
			    			String recipient = 
			    				((EditText)paymentView.
			    						findViewById(R.id.recipient))
			    							.getText().toString();
			    			if(recipient == null || recipient.length() == 0) {
			    				continue;
			    			}
			    			
			    			
		    				StringBuilder amountParam = new StringBuilder(DisplayQRCode.AMOUNT);
		    				amountParam.append('_');
		    				amountParam.append(i);

		    				final StringBuilder recipientParam = new StringBuilder(DisplayQRCode.RECIPIENT);
		    				recipientParam.append('_');
		    				recipientParam.append(i);
				    				
							startIntent.putExtra(recipientParam.toString(), recipient);
							startIntent.putExtra(amountParam.toString(), amount);
			    		} catch(Exception ex) {
			    			Log.e("PortaPayments", "Exception preparing code", ex);
			    		}
			    	}
					CreateRequestActivity.this.startActivity(startIntent);					
				}    			
    		});
    }

    /**
     * On resume updates the total.
     */
    
    @Override
    public void onResume() {
    	super.onResume();
    	updateCurrency();
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
			Intent startIntent = new Intent(CreateRequestActivity.this, Preferences.class);
			CreateRequestActivity.this.startActivity(startIntent);
			break;
	  }
      return super.onOptionsItemSelected(item);
    }
    
    /**
     * Update the total shown.
     */
    
    private void updateTotal() {
    	double total = 0.00;
    	for(int i = 0 ; i < CreateRequestActivity.PAYMENT_SECTION_IDS.length ; i++) {
    		final View paymentView = findViewById(PAYMENT_SECTION_IDS[i]);
    		EditText amount = (EditText) paymentView.findViewById(R.id.amount);
    		if(amount.getText() == null) {
    			continue;
    		}
    		String value = amount.getText().toString();
    		if(value == null || value.length() == 0) {
    			continue;
    		}
    		try {
    			total += Double.parseDouble(value);
    		} catch(Exception ex) {
    			; // Invalid amounts just get ignored.
    		}
    	}
    	
    	NumberFormat nf = NumberFormat.getInstance();
    	nf.setMinimumFractionDigits(2);
    	nf.setMaximumFractionDigits(2);
    	StringBuilder totalBuilder = new StringBuilder(32);
    	totalBuilder.append(' ');
    	totalBuilder.append(currencySymbol);
    	totalBuilder.append(' ');
    	totalBuilder.append(nf.format(total));
    	((TextView)findViewById(R.id.total)).setText(totalBuilder.toString());
    }

    /**
     * Update the currency shown.
     */
    
    private void updateCurrency() {
    	currencySymbol = getCurrencySymbol();
    	for(int i = 0 ; i < CreateRequestActivity.PAYMENT_SECTION_IDS.length ; i++) {
    		final View paymentView = findViewById(PAYMENT_SECTION_IDS[i]);
    		((TextView)paymentView.findViewById(R.id.currency)).setText(currencySymbol);
    	}
    	updateTotal();    	
    }
    
    /**
     * Gets the currency symbol
     */
    
    private String getCurrencySymbol() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String defaultCurrency;
        try {
        	defaultCurrency = Currency.getInstance(Locale.getDefault()).getCurrencyCode();
        } catch(Exception ex) {
        	defaultCurrency = "USD";
        }
         
        currencyCode = prefs.getString(Preferences.DEFAULT_CURRENCY, defaultCurrency);    	

    	Currency currency = Currency.getInstance(currencyCode);
    	if(currency == null) {
    		return currencyCode;
    	} 

    	return currency.getSymbol();
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
						Intent startIntent = new Intent(CreateRequestActivity.this, Preferences.class);
						CreateRequestActivity.this.startActivity(startIntent);
	    			}
	    		}).
	    		show();
	    	
	    	return null;
        }
        return paypalUsername;
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