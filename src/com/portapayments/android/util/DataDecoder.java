package com.portapayments.android.util;

import java.util.ArrayList;
import java.util.List;

public final class DataDecoder {

	/**
	 * Private constructor to prevent instantiation
	 */
	
	private DataDecoder() {
		super();
	}
	
	/**
	 * Decode a payment request into a list of seperate payments
	 */
	
	public static RequestDetails parseRequest(final String data) 
		throws FormatException {
		if(!data.startsWith("r\n")) {
			throw new FormatException();
		}
		
		RequestDetails request = new RequestDetails();
		int memoEnd = data.indexOf('\n', 2);
		if(memoEnd == -1) {
			throw new FormatException();
		}
		request.memo = data.substring(2, memoEnd);
		
		int currencyEnd = data.indexOf('\n', memoEnd+1);
		if(currencyEnd == -1) {
			throw new FormatException();
		}
		request.currency = data.substring(memoEnd+1, currencyEnd);
		
		int currentIdx = currencyEnd+1;
		while(currentIdx < data.length()) {
			currentIdx = parsePaymentsLine(data, request, currentIdx);
		}		
		
		return request;
	}

    /**
     * Parse a payments line.
     */
    
    private static int parsePaymentsLine(final String data,
    		final RequestDetails request, final int startIdx) {
    	int lineEnd = data.indexOf('\n', startIdx);
    	if(lineEnd == -1) {
    		lineEnd = data.length();
    	}
    	
    	char c;
    	int pos = startIdx;
    	StringBuilder dataBuilder = new StringBuilder(lineEnd - startIdx);
    	while(pos < lineEnd && (c = data.charAt(pos)) != '_') {
        	dataBuilder.append(c);
        	pos++;
    	}       
    	
    	if(pos > lineEnd-2 ) {
			throw new FormatException();
    	}

    	final PaymentDetails payment = new PaymentDetails();
    	payment.amount = dataBuilder.toString();        	
    	payment.recipient = data.substring(pos+1, lineEnd);
    	request.payments.add(payment);
    	
		return lineEnd+1;
    }
        
    /**
     * Exception thrown if the data format is incorrect
     */
    
    public final static class FormatException extends RuntimeException {
    	/**
		 * Generated Serial Number
		 */
		private static final long serialVersionUID = -1787478441287761462L;

		FormatException() {
    		super();
    	}
    }
    
    /**
     * Details of a request
     */
    
    public final static class RequestDetails {
    	public String currency;
    	public String memo;
    	public List<PaymentDetails> payments = new ArrayList<PaymentDetails>();
    }
    
    /**
     * Class holding the details of a payment to make
     */
    
    public final static class PaymentDetails {
    	public String recipient;
    	public String amount;
    }
}
