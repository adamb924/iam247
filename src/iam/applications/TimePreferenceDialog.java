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
 * This is a DialogPreference subclass that creates a generic
 * <code>TimePicker</code> preference window.
 * 
 * Largely from http://stackoverflow.com/questions/5533078/timepicker
 * -in-preferencescreen
 */
public class TimePreferenceDialog extends DialogPreference {

	private transient TimePicker mPicker;

	private transient int mHour = 0;
	private transient int mMinute = 0;

	/**
	 * @param context The application context
	 * @param attrs The attributes
	 */
	public TimePreferenceDialog(final Context context, final AttributeSet attrs) {
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
		return mPicker;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.preference.DialogPreference#onBindDialogView(android.view.View)
	 */
	@Override
	protected void onBindDialogView(final View view) {
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
	protected void onDialogClosed(final boolean positiveResult) {
		super.onDialogClosed(positiveResult);

		if (positiveResult) {
			mHour = mPicker.getCurrentHour();
			mMinute = mPicker.getCurrentMinute();

			final String time = mPicker.getCurrentHour() + ":"
					+ mPicker.getCurrentMinute();

			if (callChangeListener(time)) {
				persistString(time);
			}
		}

	}

	@Override
	protected void onSetInitialValue(final boolean restoreValue,
			final Object defaultValue) {
		String time;

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
	protected Object onGetDefaultValue(final TypedArray array, final int index) {
		return array.getString(index);
	}

	/**
	 * Returns the hour portion of a time formatted thusly: XX:XX
	 * 
	 * @param time the time
	 * @return the hour part of the time
	 */
	public static int getHour(final String time) {
		final String[] pieces = time.split(":");
		return Integer.parseInt(pieces[0]);
	}

	/**
	 * Returns the minute portion of a time formatted thusly: XX:XX
	 * 
	 * @param time the time
	 * @return the minute part of the time
	 */
	public static int getMinute(final String time) {
		final String[] pieces = time.split(":");
		return Integer.parseInt(pieces[1]);
	}

}
