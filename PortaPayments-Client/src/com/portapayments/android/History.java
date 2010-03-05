package com.portapayments.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

import com.flurry.android.FlurryAgent;
import com.portapayments.android.ErrorPoster.EndAppOnCancelListener;
import com.portapayments.android.ErrorPoster.EndAppOnClickListener;
import com.portapayments.android.database.PaymentsProvider;
import com.portapayments.android.util.DataDecoder;
import com.portapayments.android.util.DataDecoder.PaymentDetails;

public final class History extends Activity {
	/**
	 * The result columns
	 */
	
	public static final String[] RESULT_COLS = {
		"_id", "timestamp", "scannedData"
	};
	
	/**
	 * The IDs of the about menu options
	 */
	
	private static final int	ABOUT_MENU_OPTION = 0,
								SETTINGS_MENU_OPTION = 1;
	
	/**
	 * The list view.
	 */
	
	private ListView list;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.history);
        
        list = (ListView) findViewById(R.id.list);
        Cursor cursor = managedQuery(
        		PaymentsProvider.CONTENT_URI, 
        		RESULT_COLS, 
        		null, 
        		null, 
        		"timestamp DESC");
        list.setAdapter(new MyListAdapter(cursor));
        list.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(final AdapterView<?> parent, 
					final View view, final int position, 
					final long id) {
		    	Intent startIntent = new Intent(History.this, PaymentDetails.class);
		    	startIntent.putExtra(PaymentDetailsActivity.PAYMENT_ID_EXTRA, id);
		    	startActivity(startIntent);
			}	
        });
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
			Intent startIntent = new Intent(History.this, Preferences.class);
			History.this.startActivity(startIntent);
			break;
	  }
      return super.onOptionsItemSelected(item);
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	
    	if( list.getAdapter().isEmpty() ) {
        	new AlertDialog.Builder(this)
    		.setMessage(R.string.error_history_empty)
    		.setPositiveButton(R.string.dialog_ok, new EndAppOnClickListener(this))
    		.setOnCancelListener(new EndAppOnCancelListener(this))
    		.show();
    	}
    }
    
     /**
     * The list adapter for payments
     */
    
    final class MyListAdapter extends ResourceCursorAdapter {
    	/**
    	 * The IDs of the entries in a payment history.
    	 */
    	private int recipientEntries[] = {
    			R.id.payment_1, R.id.payment_2, R.id.payment_3,
    			R.id.payment_4, R.id.payment_5, R.id.payment_6
    	};
    	
    	
    	MyListAdapter(final Cursor cursor) {
    		super(History.this, R.layout.history_entry, cursor);
    	}

    	@Override
    	public void bindView(final View view, final Context context, final Cursor cursor) {
    		((TextView)view.findViewById(R.id.date)).setText(cursor.getString(1));
    		
    		DataDecoder.RequestDetails request = DataDecoder.parseRequest(cursor.getString(2));
    		float total = 0;
    		int idx = 0;
    		for( PaymentDetails details : request.payments ) {
    			View entryView = view.findViewById(recipientEntries[idx++]);
    			entryView.setVisibility(View.VISIBLE);
    			
        		((TextView)entryView.
        			findViewById(R.id.amount)).
        				setText(createAmountRepresentation(details.amount, request.currency));
        		
        		((TextView)entryView.findViewById(R.id.recipient)).setText(details.recipient);
        		
        		try {
        			total += Float.parseFloat(details.amount);
        		} catch(Exception ex) {
        			Log.e("PortaPayments", "Error parsing amount", ex);
        		}
    		}
    		
    		for(;idx < recipientEntries.length; idx++) {
    			view.findViewById(recipientEntries[idx]).setVisibility(View.GONE);
    		}

    		StringBuilder totalAmount = new StringBuilder();
    		totalAmount.append(total);
    		if((total*100)%100 < 10) {
    			totalAmount.append('0');
    		}
    		((TextView)view.
    			findViewById(R.id.total)).
    				setText(createAmountRepresentation(totalAmount.toString(), request.currency));
    	}
    	
    	private String createAmountRepresentation(final String amount, final String currency) {
			StringBuilder builder = new StringBuilder();
			builder.append(amount);			
			builder.append(' ');
			builder.append(currency);
    		return builder.toString();
    	}
    }
}