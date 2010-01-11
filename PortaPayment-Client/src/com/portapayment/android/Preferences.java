package com.portapayment.android;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;

public final class Preferences extends PreferenceActivity implements
		OnSharedPreferenceChangeListener {

	/**
	 * The key for the PayPal username 
	 */
	
	public static final String PAYPAL_USERNAME = "preferences_paypal_username";
	
	/**
	 * The key for the default currency 
	 */
	
	public static final String DEFAULT_CURRENCY = "preferences_default_currency";
	
	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		addPreferencesFromResource(R.xml.preferences);

		PreferenceScreen preferences = getPreferenceScreen();
		preferences.getSharedPreferences()
				.registerOnSharedPreferenceChangeListener(this);
	}

	// Prevent the user from turning off both decode options
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		// Do nothing
	}
}
