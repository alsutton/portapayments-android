package com.portapayment.android.paypal;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

public class PayPalHelper {

	/**
	 * The endpoint URL for paypal requests.
	 */
	
	private static final String PAYPAL_URL = "https://svcs.sandbox.paypal.com/AdaptivePayments/Pay";
	
	/**
	 * Method to make a payment between a sender and receiver
	 * 
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 */
	
	
	public static void makePayment(final String sender, final String recipient, final String currency,
			final String amount ) throws ClientProtocolException, IOException {
		
		HttpPost message = createRequest();
		
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);  
        nameValuePairs.add(new BasicNameValuePair("actionType", "PAY"));  
        nameValuePairs.add(new BasicNameValuePair("senderEmail", sender));  
        nameValuePairs.add(new BasicNameValuePair("receiverList.receiver(0).email", recipient));
        nameValuePairs.add(new BasicNameValuePair("receiverList.receiver(0).amount", amount));
        nameValuePairs.add(new BasicNameValuePair("currencyCode", currency));
        nameValuePairs.add(new BasicNameValuePair("feesPayer", "EACHRECEIVER"));
        nameValuePairs.add(new BasicNameValuePair("memo", "Paid using PortaPayments."));
        message.setEntity(new UrlEncodedFormEntity(nameValuePairs));
        
        HttpClient client = new DefaultHttpClient();
        HttpResponse response = client.execute(message);
        int statusCode = response.getStatusLine().getStatusCode();
        if(statusCode != HttpStatus.SC_OK) {
        	throw new IOException("Error on PayPal Response "+statusCode);
        }
     
        Map<String,String> results = getResults(response);
        String payKey = results.get("payKey");
        if(payKey == null) {
        	throw new IOException("No PayKey provided by PayPal.");
        }
        
	}
	
	/**
	 * Create a Map of the results from a post
	 * @throws IOException 
	 * @throws IllegalStateException 
	 */
	
	private static Map<String,String> getResults(final HttpResponse response) 
		throws IllegalStateException, IOException {
        Map<String,String> results = new HashMap<String,String>();
        InputStream responseStream = response.getEntity().getContent();
        try {
        	int character;
        	String key = "X";
        	StringBuilder builder = new StringBuilder();
        	while((character = responseStream.read()) != -1) {
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
        } finally {
        	responseStream.close();
        }
        return results;
	}
	
	/**
	 * Create a request.
	 */
	
	private static HttpPost createRequest() {
		HttpPost message = new HttpPost(PAYPAL_URL);
		message.setHeader("X-PAYPAL-SECURITY-USERID", "payments_api1.funkyandroid.com"); 
		message.setHeader("X-PAYPAL-SECURITY-PASSWORD","8PHXACWXATXPW9QA"); 
		message.setHeader("X-PAYPAL-SECURITY-SIGNATURE","A3F8ibcD.y4vlg9hgBrTNX-nZaVPAF0lgltCEaVuHALO5vzjj6fhxS8I");
		message.setHeader("X-PAYPAL-REQUEST-DATA-FORMAT", "NV"); 
		message.setHeader("X-PAYPAL-RESPONSE-DATA-FORMAT", "NV");  
		message.setHeader("X-PAYPAL-APPLICATION-ID", "APP-80W284485P519543T");
		return message;
	}
}
