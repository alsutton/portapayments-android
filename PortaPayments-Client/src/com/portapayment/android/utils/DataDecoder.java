package com.portapayment.android.utils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

public final class DataDecoder {
	/**
	 * The URL of the decoder
	 */
	
	private static final String DECODER_URL = "http://generator.portapayments.com/dec/Decode?d=";
	
	public static String decode(final String data) {
		StringBuilder url = new StringBuilder(DECODER_URL.length()+data.length());
		url.append(DECODER_URL);
		for(int i = 0; i < data.length() ; i++) {
			char c = data.charAt(i);
			if(!Character.isWhitespace(c)) {
				url.append(c);
			}
		}
		
		int retries = 3;
		while( retries > 0) {
			HttpClient client = new DefaultHttpClient();
			HttpGet get = new HttpGet(url.toString());
			try {
				HttpResponse response = client.execute(get);
				int statusCode = response.getStatusLine().getStatusCode();
				if( statusCode != HttpStatus.SC_OK ) {
					throw new IOException("Non-OK response from server : "+statusCode);
				}
				
				LineNumberReader lnr = 
					new LineNumberReader(
						new InputStreamReader(response.getEntity().getContent())
					);
				try {
					String responseData = lnr.readLine();
					if(responseData == null) {
						throw new IOException("No response data provided.");
					}
					return responseData;
				} finally {
					lnr.close();
				}
			} catch(Exception e) {
				retries--;
				try {
					Thread.sleep(1000L);
				} catch (InterruptedException ex) {
					; // Interrupted sleep is not a problem.
				}
			}
		}
		
		return null;
	}
}
