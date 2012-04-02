package com.portapayments.android;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.Button;
import android.widget.ImageView;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.MenuItem;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.ByteMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

public final class DisplayQRCode extends SherlockActivity {
	
	/**
	 * The intent extra parameters.
	 */
	
	public static final String RECIPIENT = "recipient",
								AMOUNT = "amount",
								CURRENCY = "currency",
								MEMO = "memo";
	
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
        setContentView(R.layout.show_qr_code);
        super.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        
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
    	    	
    	findViewById(R.id.qr_code_layout).
    		getViewTreeObserver().
    		addOnGlobalLayoutListener(layoutListener);
        firstLayout = true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
      switch (item.getItemId()) {
	      case android.R.id.home:
              finish();
              return true;
	  }
      return super.onOptionsItemSelected(item);
    }
    
    /**
     * Raise an error.
     */

    public void raiseError(final int errorMessageId) {
    	handler.post(ErrorPoster.getClosingError(this, errorMessageId));
    }

    /**
     * Class to encoding the data into a QR code then display it.
     */
    
    private final class MyEncodingThread implements Runnable {
    	private final ImageView imageView;
    	
    	MyEncodingThread(final ImageView imageView) {
    		this.imageView = imageView;
    	}
    	
    	public void run () {
    		try {
    	    	Intent intent = DisplayQRCode.this.getIntent();

    	    	final String currency = intent.getStringExtra(DisplayQRCode.CURRENCY);
    	    	final String memo = intent.getStringExtra(DisplayQRCode.MEMO);

    	    	StringBuilder code = new StringBuilder(128);
    			code.append("r\n");
    			code.append(memo);
    			code.append('\n');
    			code.append(currency);
    			
    			for(int i = 0 ; i < 6 ; i++) {	
    				final StringBuilder recipientParam = new StringBuilder(RECIPIENT);
    				recipientParam.append('_');
    				recipientParam.append(i);
    				final String recipient = intent.getStringExtra(recipientParam.toString());
    				if(recipient == null || recipient.length() == 0) {
    					continue;
    				}
    				
    				StringBuilder amountParam = new StringBuilder(AMOUNT);
    				amountParam.append('_');
    				amountParam.append(i);
    				final String amount = intent.getStringExtra(amountParam.toString());
    				if(amount == null || amount.length() == 0) {
    					continue;
    				}
    				try {
    					double parsedDouble = Double.parseDouble(amount);
    					if(parsedDouble < 0 ) {
    						continue;
    					}
    				} catch( Exception ex ) {
    					continue;
    				}
        			
        			code.append('\n');
        			code.append(amount);
        			code.append('_');
        			code.append(recipient);
    			}
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
    
	private static final class MyImageViewPopulator implements Runnable {
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
}
