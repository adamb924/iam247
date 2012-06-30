/**
 * 
 */
package iam.applications;

import android.content.Context;
import android.preference.EditTextPreference;
import android.util.AttributeSet;

/**
 * This is an EditTextPreference for blocking a phone number. Arguably this
 * logic would be better placed in <code>PreferencesActivity</code>.
 * 
 */
public class BlockPhonePreferenceDialog extends EditTextPreference {

	public BlockPhonePreferenceDialog(Context ctxt, AttributeSet attrs) {
		super(ctxt, attrs);

		setPositiveButtonText("Block");
		setNegativeButtonText("Cancel");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.preference.EditTextPreference#onDialogClosed(boolean)
	 */
	@Override
	protected void onDialogClosed(boolean positiveResult) {
		super.onDialogClosed(positiveResult);

		if (positiveResult) {
			DbAdapter dbHelper = new DbAdapter(getContext());
			dbHelper.open();
			dbHelper.setNumberIsBlocked(getText(), true);
			dbHelper.close();
		}
	}
}
