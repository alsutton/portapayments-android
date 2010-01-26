package com.portapayments.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;

/**
 * Class to be used with a handler to present an error message.
 *
 * @author Al Sutton
 */
public final class ErrorPoster implements Runnable {
	private Context context;
	private int errorMessageId;
	private OnClickListener positiveButtonHandler;
	private OnCancelListener cancelListener;
	
	/**
	 * Constructor. Stores objects relevant to the error.
	 * 
	 * @param context
	 * @param errorMessageId
	 * @param positiveButtonHandler
	 * @param cancelListener
	 */
	ErrorPoster(final Context context, final int errorMessageId,
				final OnClickListener positiveButtonHandler,
				final OnCancelListener cancelListener) {
		this.context = context;
		this.errorMessageId = errorMessageId;
		this.positiveButtonHandler = positiveButtonHandler;
		this.cancelListener = cancelListener;
	}
	
	/**
	 * Run method used by Handler to show the dialog
	 */
	public void run() {
    	new AlertDialog.Builder(context)
		.setIcon(android.R.drawable.ic_dialog_alert)
		.setTitle(R.string.dlg_error_title)
		.setMessage(errorMessageId)
		.setPositiveButton(R.string.dialog_ok, positiveButtonHandler)
		.setOnCancelListener(cancelListener)
		.show();
	}

	/**
	 * Utility method to present an error dialog that closes the 
	 * activity when the dialog is OKed or cancelled.
	 */
	
	public static ErrorPoster getClosingError(final Activity activity, 
			final int errorMessageId) {
		return new ErrorPoster(
					activity,
					errorMessageId,
					new EndAppOnClickListener(activity),
					new EndAppOnCancelListener(activity)
				);
	}
	
	
	/**
	 * Class to close an activity when the OK button is selected.
	 */
	public static final class EndAppOnClickListener implements OnClickListener {
		private Activity activity;
		
		public EndAppOnClickListener(final Activity activity) {
			this.activity = activity;
		}
		
		public void onClick(DialogInterface dialog, int which) {
			activity.finish();
		}
	}

	/**
	 * Close an activity when a dialog is cancelled.
	 */
	public static final class EndAppOnCancelListener implements OnCancelListener {
		private Activity activity;
		
		public EndAppOnCancelListener(final Activity activity) {
			this.activity = activity;
		}
		
		public void onCancel(DialogInterface dialog) {
			activity.finish();
		}
	}
}
