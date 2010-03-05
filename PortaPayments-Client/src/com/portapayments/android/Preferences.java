package com.portapayments.android;

import com.flurry.android.FlurryAgent;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;

public final class Preferences extends PreferenceActivity implements
		OnSharedPreferenceChangeListener {

	public static final String KEY_PLAY_BEEP = "preferences_play_beep";
	public static final String KEY_VIBRATE = "preferences_vibrate";

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

	/**
     * Start the flurry session
     */
    
    @Override
    public void onStart() {
    	super.onStart();
    	FlurryAgent.onStartSession(this, "F6XKDGEXRCNXKZVIMBID");
    }
    
    /**
     * Stop the flurry session
     */
    
    @Override
    public void onStop() {
    	FlurryAgent.onEndSession(this);
    	super.onStop();
    }
    

	// Prevent the user from turning off both decode options
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		// Do nothing
	}
}
