package com.portapayment.android.paypal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.http.client.ClientProtocolException;

import android.content.Context;
import android.telephony.TelephonyManager;
import android.util.Log;

public final class PayPalHelper {

	/**
	 * The endpoint URL for paypal requests.
	 */
	
	private static final String PAYPAL_URL = "https://svcs.sandbox.paypal.com/AdaptivePayments/Pay/";

	/**
	 * Method to make a payment between a sender and receiver
	 * 
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 */
	
	
	public static String startPayment(final Context context, final String sender, 
			final String recipient, final String currency,
			final String amount ) throws ClientProtocolException, IOException {
	
		Properties headers = new Properties();
		
		String devID = null;
		try {
			final TelephonyManager tMgr = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
			if(tMgr != null) {
				devID = tMgr.getDeviceId();
				if(devID != null) {
					headers.put("X-PAYPAL-DEVICE_ID", devID); 				
				}
			}
		} catch(Exception ex) {
			; // Do nothing, device ID is optional.
		}
		headers.put("X-PAYPAL-SECURITY-USERID", "paypal_1223368472_biz_api1.alsutton.com"); 
		headers.put("X-PAYPAL-SECURITY-PASSWORD","1223368483"); 
		headers.put("X-PAYPAL-SECURITY-SIGNATURE","AGkOsAbzHqAoa5KajZ8KTeHzbH6-AD9Ogr36rPgfyiyskE63m2KemnVG");
		headers.put("X-PAYPAL-REQUEST-DATA-FORMAT", "NV"); 
		headers.put("X-PAYPAL-RESPONSE-DATA-FORMAT", "NV");  
		headers.put("X-PAYPAL-APPLICATION-ID", "APP-80W284485P519543T");
		

		StringBuilder requestBody = new StringBuilder();
        requestBody.append("senderEmail=");
        requestBody.append(sender);  
		requestBody.append("&actionType=PAY&currencyCode=");
		requestBody.append(currency);
		requestBody.append("&feesPayer=EACHRECEIVER");
		requestBody.append("&receiverList.receiver(0).email=");
		requestBody.append(recipient);
		requestBody.append("&receiverList.receiver(0).amount=");
		requestBody.append(amount);
		requestBody.append("&returnUrl=http://portapayments.com/pp/PayOK.jsp");
		requestBody.append("&cancelUrl=http://portapayments.com/pp/PayCancel.jsp");
		requestBody.append("&requestEnvelope.errorLanguage=en_US");
		requestBody.append("&clientDetails.ipAddress=127.0.0.1");
		requestBody.append("&clientDetails.deviceId=");
		requestBody.append("&memo=Paid via PortaPayments");
		if(devID != null && devID.length() > 0) {
			requestBody.append(devID);
		} else {
			requestBody.append("AndroidDevice");
		}
		requestBody.append("&clientDetails.applicationId=PortaPayments");
        
        Map<String,String> results = postData(headers, requestBody.toString());
        if(results == null) {
        	throw new PayPalException("PayPal was unable to start the transaction.");
        }
        
        final String ack = results.get("responseEnvelope.ack");
        if(ack != null && "Failure".equals(ack)) {
        	throw new PayPalExceptionWithErrorCode("PayPal generated an error ", results.get("error(0).errorId"));
        }
        
        return results.get("payKey");
	}
	
	public static Map<String,String> postData(final Properties headers, final String data) {
		HttpURLConnection connection = 
			setupConnection(PayPalHelper.PAYPAL_URL, headers, null);

		return sendHttpPost(connection, data);

	}

	public static  Map<String,String> sendHttpPost(final HttpURLConnection connection, final String data) {
		BufferedReader reader = null;
		
		try {

			OutputStream os = connection.getOutputStream();
			os.write(data.toString().getBytes("UTF-8"));
			os.close();
			int status = connection.getResponseCode();
			if (status != 200) {
				Log.e("PortaPayments", "HTTP Error code " + status
						+ " received, transaction not submitted");
				reader = new BufferedReader(new InputStreamReader(connection
						.getErrorStream()));
			} else {
				reader = new BufferedReader(new InputStreamReader(connection
						.getInputStream()));
			}

			return getResults(reader);
		} catch (Exception e) {
			System.out.println(e);
		} finally {

			try {
				if (reader != null)
					reader.close();
				if (connection != null)
					connection.disconnect();
			} catch (Exception e) {
				; // Do nothing
			}

		}
		
		return null;
	}

	private static HttpURLConnection setupConnection(String endpoint,
			Properties headers, Properties connectionProps) {
		HttpURLConnection connection = null;

		try {
			URL url = new URL(endpoint);
			connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("POST");
			connection.setDoOutput(true);

			Object[] keys = headers.keySet().toArray();
			for (int i = 0; i < keys.length; i++) {
				connection.setRequestProperty((String) keys[i],
						(String) headers.get(keys[i]));
			}
		} catch (Exception e) {
			Log.e("PortaPayments", "Failed setting up HTTP Connection", e);
		}
		return connection;

	}
	
	/**
	 * Create a Map of the results from a post
	 * @throws IOException 
	 * @throws IllegalStateException 
	 */
	
	private static Map<String,String> getResults(final Reader reader) 
		throws IllegalStateException, IOException {
        Map<String,String> results = new HashMap<String,String>();

    	int character;
    	String key = "X";
    	StringBuilder builder = new StringBuilder();
    	while((character = reader.read()) != -1) {
    		char c = (char) character;
    		if(c == '=') {
    			key = builder.toString();
    			builder.delete(0, builder.length());
    		} else if (c == '&') {
    			results.put(key, builder.toString());
    			builder.delete(0, builder.length());
    		} else {
    			builder.append(c);
    		}
    	}

        return results;
	}
	
	/**
	 * Exception thrown if something is goes wrong in the app/PayPal communication
	 */
	
	public static class PayPalException extends RuntimeException {
		//569057 - Recipient invalid
		/**
		 * Generated serial ID
		 */
		private static final long serialVersionUID = -1416134527103317337L;

		PayPalException(final String message) {
			super(message);
		}
	}
	
	/**
	 * Exception thrown if PayPal reports an error that contains an error code
	 */
	
	public static final class PayPalExceptionWithErrorCode extends PayPalException {
		private String errorCode;
		
		PayPalExceptionWithErrorCode(final String message, final String errorCode) {
			super(message);
			this.errorCode = errorCode;
		}

		public String getErrorCode() {
			return errorCode;
		}
	}
}
