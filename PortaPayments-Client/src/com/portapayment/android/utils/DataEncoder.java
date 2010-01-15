package com.portapayment.android.utils;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;

public class DataEncoder {

	/**
	 * Base64 Mapping table
	 */
	private static char[] map1 = new char[64];
	static {
		int i = 0;
		for (char c = 'A'; c <= 'Z'; c++) {
			map1[i++] = c;
		}
		for (char c = 'a'; c <= 'z'; c++) {
			map1[i++] = c;
		}
		for (char c = '0'; c <= '9'; c++) {
			map1[i++] = c;
		}
		map1[i++] = '+';
		map1[i++] = '/';
	}
	private static byte[] map2 = new byte[128];
	static {
		for (int i = 0; i < map2.length; i++) {
			map2[i] = -1;
		}
		for (int i = 0; i < 64; i++) {
			map2[map1[i]] = (byte) i;
		}
	}

	/**
	 * The public key for encryption
	 */

	private static PublicKey key;

	/**
	 * The encoded public key
	 */

	private static final byte[] encKey = { 48, 92, 48, 13, 6, 9, 42, -122, 72,
			-122, -9, 13, 1, 1, 1, 5, 0, 3, 75, 0, 48, 72, 2, 65, 0, -79, 46,
			106, 87, 22, 25, 106, 1, -106, 13, 102, 20, 118, -47, 18, -39, 29,
			72, 100, 64, 25, -59, 18, -94, 107, -5, -56, -58, 25, -114, -68,
			35, 107, -21, 75, -79, -93, -59, -6, 35, 48, 23, -55, -51, -118, 3,
			45, -2, -123, -12, 61, 107, 34, -15, -68, 63, 40, 85, -106, 7, 27,
			46, 77, -31, 2, 3, 1, 0, 1 };

	static {
		X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encKey);
		KeyFactory factory;
		try {
			factory = KeyFactory.getInstance("RSA", "BC");
			key = factory.generatePublic(keySpec);
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchProviderException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidKeySpecException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Private constructor to prevent instantiation.
	 * 
	 * @param data
	 *            The original data.
	 * 
	 * @return The encoded data.
	 * @throws UnsupportedEncodingException
	 * @throws NoSuchPaddingException
	 * @throws NoSuchProviderException
	 * @throws NoSuchAlgorithmException
	 */

	public static String encode(final String recipient, final String currency,
			final String amount) throws GeneralSecurityException,
			UnsupportedEncodingException {
		StringBuilder code = new StringBuilder(recipient.length()
				+ amount.length() + 6);
		code.append("r_");
		code.append(amount);
		code.append('_');
		code.append(currency);
		code.append('_');
		code.append(recipient);

		String dataString = code.toString();
		Cipher rsaCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding", "BC");
		rsaCipher.init(Cipher.ENCRYPT_MODE, key);
		byte[] data = rsaCipher.doFinal(dataString.getBytes("UTF-8"));

		int oDataLen = (data.length * 4 + 2) / 3;
		StringBuilder resultBuilder = new StringBuilder(1+((data.length * 4 + 2)/3));
		resultBuilder.append('1');

		int ip = 0;
		int op = 0;
		while (ip < data.length) {
			int i0 = data[ip++] & 0xff;
			int i1 = ip < data.length ? data[ip++] & 0xff : 0;
			int i2 = ip < data.length ? data[ip++] & 0xff : 0;
			resultBuilder.append(map1[i0 >>> 2]);
			op++;
			resultBuilder.append(map1[((i0 & 3) << 4) | (i1 >>> 4)]);
			op++;
			resultBuilder.append(op < oDataLen ? map1[((i1 & 0xf) << 2) | (i2 >>> 6)] : '=');
			op++;
			resultBuilder.append(op < oDataLen ? map1[  i2 & 0x3F] : '=');
			op++;
		}
		
		return resultBuilder.toString();
	}
}
