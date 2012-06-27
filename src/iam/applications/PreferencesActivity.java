/**
 * 
 */
package iam.applications;

import android.os.Bundle;
import android.preference.PreferenceActivity;

/**
 * @author Adam
 * 
 */
public class PreferencesActivity extends PreferenceActivity {

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.preference.PreferenceActivity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
	}

}
