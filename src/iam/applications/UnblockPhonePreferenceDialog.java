/**
 * 
 */
package iam.applications;

import android.content.Context;
import android.database.Cursor;
import android.preference.ListPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Toast;

/**
 * A <code>ListPreference</code> subclass for removing phone numbers from the
 * blocked numbers list. Arguably this logic would be better placed in
 * <code>PreferencesActivity</code>.
 * 
 */
public class UnblockPhonePreferenceDialog extends ListPreference {
	private final DbAdapter mDbHelper;
	private final Context mContext;

	/**
	 * @param context
	 * @param attrs
	 */
	public UnblockPhonePreferenceDialog(Context context, AttributeSet attrs) {
		super(context, attrs);

		mDbHelper = new DbAdapter(getContext());
		mDbHelper.open();

		mContext = context;

		this.setEntries(new CharSequence[0]);
		this.setEntryValues(new CharSequence[0]);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#finalize()
	 */
	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		mDbHelper.close();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.preference.DialogPreference#onCreateDialogView()
	 */
	@Override
	protected View onCreateDialogView() {
		View v = super.onCreateDialogView();

		fillData();

		return v;
	}

	protected void fillData() {
		Cursor c = mDbHelper.fetchBlockedNumbers();
		if (c.getCount() == 0) {
			Toast toast = Toast.makeText(mContext,
					mContext.getString(R.string.no_blocked_numbers),
					Toast.LENGTH_SHORT);
			toast.show();
		}

		CharSequence[] entries = new CharSequence[c.getCount()];

		if (c.getCount() > 0) {
			c.moveToFirst();
			int i = 0;
			do {
				entries[i] = c.getString(1);
				++i;
			} while (c.moveToNext());
		}

		c.close();

		this.setEntries(entries);
		this.setEntryValues(entries);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.preference.ListPreference#onDialogClosed(boolean)
	 */
	@Override
	protected void onDialogClosed(boolean positiveResult) {
		super.onDialogClosed(positiveResult);

		if (positiveResult) {
			mDbHelper.setNumberIsBlocked(getValue(), false);
		}
	}

}
