package com.portapayment.android;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.ByteMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.Button;
import android.widget.ImageView;

public class DisplayQRCode extends Activity {
	
	/**
	 * The intent extra holding the text to encode.
	 */
	
	public static final String ENCODE_DATA_EXTRA = "QR_DATA";
	
	/**
	 * The current text on display as a QR Code.
	 */
	
	private String currentQRCodeText = null;
	
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
    		new Thread(new MyEncodingThread(codeView, currentQRCodeText)).start();
    		
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
        
        ((Button)findViewById(R.id.done_button)).setOnClickListener(
        		new OnClickListener() {
					public void onClick(View v) {
						finish();
					}
        		}
        	);
    }

    @Override
    public void onResume() {
    	super.onResume();
    	
    	String codeText = getIntent().getStringExtra(ENCODE_DATA_EXTRA);
    	if( codeText == null ) {
    		finish();
    		return;
    	}
    	
    	findViewById(R.id.qr_code_layout).
    		getViewTreeObserver().
    		addOnGlobalLayoutListener(layoutListener);
        firstLayout = true;

        currentQRCodeText = codeText;
    }

    /**
     * Class to encoding the data into a QR code then display it.
     */
    
    private class MyEncodingThread implements Runnable {
    	private final ImageView imageView;
    	private final String text;
    	
    	MyEncodingThread(final ImageView imageView, final String text) {
    		this.imageView = imageView;
    		this.text = text;
    		
    	}
    	
    	public void run () {
    		try {
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
    			// TODO: Handle Exception
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
    
}
