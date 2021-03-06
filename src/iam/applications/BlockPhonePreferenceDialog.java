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

	/**
	 * Instantiates a new block phone preference dialog.
	 * 
	 * @param ctxt
	 *            the ctxt
	 * @param attrs
	 *            the attrs
	 */
	public BlockPhonePreferenceDialog(final Context ctxt,
			final AttributeSet attrs) {
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
	protected void onDialogClosed(final boolean positiveResult) {
		super.onDialogClosed(positiveResult);

		if (positiveResult) {
			final DbAdapter dbHelper = new DbAdapter(getContext());
			dbHelper.open();
			dbHelper.setNumberIsBlocked(getText(), true);
			dbHelper.close();
		}
	}
}
