package com.portapayments.android;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.Window;
import android.widget.Button;

import com.portapayments.android.database.PaymentsProvider;

public final class PaymentDetailsActivity extends Activity {
	
	/**
	 * The intent extra holding the payment ID
	 */
	
	public static final String PAYMENT_ID_EXTRA = "paymentId";
	
	/**
	 * The result columns
	 */
	
	public static final String[] RESULT_COLS = {
		"_id", "timestamp", "scannedData"
	};
	
	/**
	 * The handler used to populate the encoded image area
	 */
	
	private final Handler handler = new Handler();
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.payment_details);
        
        ((Button)findViewById(R.id.done_button)).setOnClickListener(
        		new View.OnClickListener() {
					public void onClick(View v) {
						finish();
					}
        		}
        	);
    }

    @Override
    public void onResume() {
    	super.onResume();
    	
    	Intent intent = getIntent();
    	final long id = intent.getLongExtra(PaymentDetailsActivity.PAYMENT_ID_EXTRA, -1);
    	String[] selectionArgs = { Long.toString(id) };
    	Cursor cursor = getContentResolver().query(
    			PaymentsProvider.CONTENT_URI, 
    			RESULT_COLS, 
    			"_id = ?", selectionArgs, 
    			null);
    	try {
    		
    	} finally {
    		cursor.close();
    	}
    	
    }
    
    /**
     * Raise an error.
     */

    public void raiseError(final int errorMessageId) {
    	handler.post(ErrorPoster.getClosingError(this, errorMessageId));
    }

}
