/**
 * 
 */
package iam.applications;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TimePicker;

/**
 * @author Adam Largely from
 *         http://stackoverflow.com/questions/5533078/timepicker
 *         -in-preferencescreen
 */
public class TimePreferenceDialog extends DialogPreference {

	TimePicker mPicker;

	private int mHour = 0;
	private int mMinute = 0;

	/**
	 * @param context
	 * @param attrs
	 */
	public TimePreferenceDialog(Context context, AttributeSet attrs) {
		super(context, attrs);

		setPositiveButtonText("Set");
		setNegativeButtonText("Cancel");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.preference.DialogPreference#onCreateDialogView()
	 */
	@Override
	protected View onCreateDialogView() {
		mPicker = new TimePicker(getContext());
		return (mPicker);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.preference.DialogPreference#onBindDialogView(android.view.View)
	 */
	@Override
	protected void onBindDialogView(View view) {
		super.onBindDialogView(view);

		mPicker.setCurrentHour(mHour);
		mPicker.setCurrentMinute(mMinute);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.preference.DialogPreference#onDialogClosed(boolean)
	 */
	@Override
	protected void onDialogClosed(boolean positiveResult) {
		super.onDialogClosed(positiveResult);

		if (positiveResult) {
			mHour = mPicker.getCurrentHour();
			mMinute = mPicker.getCurrentMinute();

			String time = String.valueOf(mPicker.getCurrentHour()) + ":"
					+ String.valueOf(mPicker.getCurrentMinute());

			if (callChangeListener(time)) {
				persistString(time);
			}
		}

	}

	@Override
	protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
		String time = null;

		if (restoreValue) {
			if (defaultValue == null) {
				time = getPersistedString("06:00");
			} else {
				time = getPersistedString(defaultValue.toString());
			}
		} else {
			time = defaultValue.toString();
		}

		mHour = getHour(time);
		mMinute = getMinute(time);
	}

	@Override
	protected Object onGetDefaultValue(TypedArray a, int index) {
		return (a.getString(index));
	}

	public static int getHour(String time) {
		String[] pieces = time.split(":");

		return (Integer.parseInt(pieces[0]));
	}

	public static int getMinute(String time) {
		String[] pieces = time.split(":");

		return (Integer.parseInt(pieces[1]));
	}

}
