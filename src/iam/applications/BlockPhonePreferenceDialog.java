/**
 * 
 */
package iam.applications;

import android.content.Context;
import android.preference.EditTextPreference;
import android.util.AttributeSet;

/**
 * @author Adam
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
