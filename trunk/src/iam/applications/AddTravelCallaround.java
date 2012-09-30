/**
 * 
 */
package iam.applications;

import java.util.Calendar;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TimePicker;

/**
 * This is an <code>Activity</code> subclass, which creates an interface for
 * adding pre-defined call arounds for travel arrangements. Call arounds can be
 * added for a range of days, with one or two call arounds per day. (Currently
 * this activity is accessible from the menu of the CallAroundActivity.)
 * 
 */
public class AddTravelCallaround extends Activity {
	/** The database interface. */
	private transient DbAdapter mDbHelper;

	private transient Spinner mWhichHouse;
	private transient TimePicker mSecondTimePicker;
	private transient EditText mWindow;

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(final Bundle bundle) {
		super.onCreate(bundle);

		setContentView(R.layout.travel_callarounds);

		mDbHelper = new DbAdapter(this);
		mDbHelper.open();

		// populate the spinner with the names of the houses/groups
		mWhichHouse = (Spinner) findViewById(R.id.which_house);
		final Cursor cur = mDbHelper.fetchAllHouses();
		final String[] fromFields = new String[] { DbAdapter.KEY_NAME };
		final int[] toFields = new int[] { android.R.id.text1 };
		final SimpleCursorAdapter adapter = new SimpleCursorAdapter(this,
				android.R.layout.simple_spinner_item, cur, fromFields, toFields);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mWhichHouse.setAdapter(adapter);

		// set the default call around window to 60 minutes
		mWindow = (EditText) findViewById(R.id.callaround_window);
		mWindow.setText(String.valueOf(60));

		// the TimePicker for the second checkin is disabled by default; if the
		// checkbox is checked it is enabled
		mSecondTimePicker = (TimePicker) findViewById(R.id.secondTime);
		mSecondTimePicker.setEnabled(false);
		final CheckBox secondTimeEnabled = (CheckBox) findViewById(R.id.secondTimeEnabled);
		secondTimeEnabled
				.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(
							final CompoundButton buttonView,
							final boolean isChecked) {
						mSecondTimePicker.setEnabled(isChecked);
					}
				});

		// logic for the Ok and Cancel buttons
		final Button okButton = (Button) findViewById(R.id.ok);
		okButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(final View view) {
				addCallarounds();
				finish();
			}
		});

		final Button cancelButton = (Button) findViewById(R.id.cancel);
		cancelButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(final View view) {
				finish();
			}
		});

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onDestroy()
	 */
	@Override
	protected void onDestroy() {
		super.onDestroy();
		mDbHelper.close();
	}

	/**
	 * Grabs the data from the layout, formats it for
	 * DbAdapter.addTravelCallarounds(), and calls that function.
	 */
	private void addCallarounds() {
		final DatePicker fromDatePicker = (DatePicker) findViewById(R.id.fromDate);
		final DatePicker toDatePicker = (DatePicker) findViewById(R.id.toDate);
		final TimePicker firstTimePicker = (TimePicker) findViewById(R.id.firstTime);

		final Cursor cursor = (Cursor) mWhichHouse.getSelectedItem();
		final long house_id = cursor.getLong(cursor.getColumnIndex("_id"));

		final long window = Long.valueOf(mWindow.getText().toString());

		final Calendar fromDate = Calendar.getInstance();
		fromDate.set(fromDatePicker.getYear(), fromDatePicker.getMonth(),
				fromDatePicker.getDayOfMonth());

		final Calendar toDate = Calendar.getInstance();
		toDate.set(toDatePicker.getYear(), toDatePicker.getMonth(),
				toDatePicker.getDayOfMonth());

		final String firstTime = Time.basicTimeFormat(
				firstTimePicker.getCurrentHour(),
				firstTimePicker.getCurrentMinute());

		final Calendar firstTimeEarlyCal = Calendar.getInstance();
		firstTimeEarlyCal.set(Calendar.HOUR_OF_DAY,
				firstTimePicker.getCurrentHour());
		firstTimeEarlyCal.set(Calendar.MINUTE,
				firstTimePicker.getCurrentMinute());
		firstTimeEarlyCal.add(Calendar.MINUTE, -1 * (int) window);
		final String firstTimeEarly = Time.basicTimeFormat(
				firstTimeEarlyCal.get(Calendar.HOUR_OF_DAY),
				firstTimeEarlyCal.get(Calendar.MONTH));

		String secondTime;
		String secondTimeEarly;
		if (mSecondTimePicker.isEnabled()) {
			secondTime = Time.basicTimeFormat(
					mSecondTimePicker.getCurrentHour(),
					mSecondTimePicker.getCurrentMinute());

			final Calendar secondTimeCal = Calendar.getInstance();
			secondTimeCal.set(Calendar.HOUR_OF_DAY,
					mSecondTimePicker.getCurrentHour());
			secondTimeCal.set(Calendar.MINUTE,
					mSecondTimePicker.getCurrentMinute());
			secondTimeCal.add(Calendar.MINUTE, -1 * (int) window);
			secondTimeEarly = Time.basicTimeFormat(
					secondTimeCal.get(Calendar.HOUR_OF_DAY),
					secondTimeCal.get(Calendar.MONTH));
		} else {
			secondTime = "";
			secondTimeEarly = "";
		}

		mDbHelper.addTravelCallarounds(house_id, fromDate, toDate, firstTime,
				firstTimeEarly, secondTime, secondTimeEarly);
	}
}
