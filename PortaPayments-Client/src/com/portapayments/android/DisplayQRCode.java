package com.portapayments.android;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.ByteMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.Button;
import android.widget.ImageView;

public class DisplayQRCode extends Activity {
	
	/**
	 * The intent extra parameters.
	 */
	
	public static final String RECIPIENT = "recipient",
								AMOUNT = "amount",
								CURRENCY = "currency";
	
	/**
	 * The recipient from the code
	 */
	
	private String recipient;
	
	/**
	 * The amount for the code
	 */
	
	private String amount;
	
	/**
	 * The currency for the code.
	 */
	
	private String currency;
	
	/**
	 * Flag to determine the first pass of the layout engine which allows
	 * the dimensions of the image view to be determined.
	 */
	private boolean firstLayout;

	/**
	 * The size of the QR Code area.
	 */
	
	private int size;
	
	  /**
	   * This needs to be delayed until after the first layout so that the view dimensions will be
	   * available.
	   */
	  private final OnGlobalLayoutListener layoutListener = new OnGlobalLayoutListener() {
	    public void onGlobalLayout() {
	      if (firstLayout) {
	        View layout = findViewById(R.id.qr_code);
	        int width = layout.getWidth();
	        int height = layout.getHeight();
	        int smallerDimension = width < height ? width : height;
	        size = smallerDimension * 7 / 8;
	       
    		ImageView codeView = (ImageView) findViewById(R.id.qr_code);
    		new Thread(new MyEncodingThread(codeView)).start();
    		
	        firstLayout = false;
	      }
	    }
	  };

	 /**
	 * The handler used to populate the encoded image area
	 */
	
	private final Handler handler = new Handler();
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.show_qr_code);
        
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
    	recipient	= intent.getStringExtra(DisplayQRCode.RECIPIENT);
    	amount 		= intent.getStringExtra(DisplayQRCode.AMOUNT);
    	currency	= intent.getStringExtra(DisplayQRCode.CURRENCY);
    	
    	findViewById(R.id.qr_code_layout).
    		getViewTreeObserver().
    		addOnGlobalLayoutListener(layoutListener);
        firstLayout = true;
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
    
    private class MyEncodingThread implements Runnable {
    	private final ImageView imageView;
    	
    	MyEncodingThread(final ImageView imageView) {
    		this.imageView = imageView;
    	}
    	
    	public void run () {
    		try {
    			StringBuilder code = new StringBuilder(recipient.length()
    					+ amount.length() + 6);
    			code.append("r_");
    			code.append(amount);
    			code.append('_');
    			code.append(currency);
    			code.append('_');
    			code.append(recipient);
    			String text = code.toString();
    			
	            ByteMatrix result = new QRCodeWriter().encode
	            		(text, BarcodeFormat.QR_CODE, DisplayQRCode.this.size,  DisplayQRCode.this.size);
	            int width = result.getWidth();
	            int height = result.getHeight();
	            byte[][] array = result.getArray();
	            int[] pixels = new int[width * height];
	            for (int y = 0; y < height; y++) {
	              for (int x = 0; x < width; x++) {
	                int grey = array[y][x] & 0xff;
	                // pixels[y * width + x] = (0xff << 24) | (grey << 16) | (grey << 8) | grey;
	                pixels[y * width + x] = 0xff000000 | (0x00010101 * grey);
	              }
	            }
	
	            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
	            bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
	            
	            handler.post(new MyImageViewPopulator(imageView, bitmap));
    		} catch(Exception ex) {
    			Log.e("PortaPayments", "Error during code generation.", ex);
    			raiseError(R.string.error_codegen);
    		}
    	}
    	
    }
    
	private static class MyImageViewPopulator implements Runnable {
		private ImageView imageView;
		private Bitmap bitmap;
		
		MyImageViewPopulator(final ImageView imageView, final Bitmap bitmap) {
			this.imageView = imageView;
			this.bitmap = bitmap;
		}
		
		public void run() {
			imageView.setImageBitmap(bitmap);
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
        	new AlertDialog.Builder(DisplayQRCode.this)
    		.setIcon(android.R.drawable.ic_dialog_alert)
    		.setTitle(R.string.dlg_error_title)
    		.setMessage(errorMessageId)
    		.setPositiveButton(R.string.dialog_ok, new OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					DisplayQRCode.this.finish();
				}
    		})
    		.setOnCancelListener(new OnCancelListener() {
				public void onCancel(DialogInterface dialog) {
					DisplayQRCode.this.finish();					
				}    			
    		})
    		.show();
    	}
    }
}