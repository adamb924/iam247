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
	private DbAdapter mDbHelper;

	private Spinner mWhichHouse;
	private TimePicker mSecondTimePicker;
	private EditText mWindow;

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.travel_callarounds);

		mDbHelper = new DbAdapter(this);
		mDbHelper.open();

		// populate the spinner with the names of the houses/groups
		mWhichHouse = (Spinner) findViewById(R.id.which_house);
		Cursor c = mDbHelper.fetchAllHouses();
		String[] from = new String[] { DbAdapter.KEY_NAME };
		int[] to = new int[] { android.R.id.text1 };
		SimpleCursorAdapter adapter = new SimpleCursorAdapter(this,
				android.R.layout.simple_spinner_item, c, from, to);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mWhichHouse.setAdapter(adapter);

		// set the default call around window to 60 minutes
		mWindow = (EditText) findViewById(R.id.callaround_window);
		mWindow.setText(String.valueOf(60));

		// the TimePicker for the second checkin is disabled by default; if the
		// checkbox is checked it is enabled
		mSecondTimePicker = (TimePicker) findViewById(R.id.secondTime);
		mSecondTimePicker.setEnabled(false);
		CheckBox secondTimeEnabled = (CheckBox) findViewById(R.id.secondTimeEnabled);
		secondTimeEnabled
				.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
						mSecondTimePicker.setEnabled(isChecked);
					}
				});

		// logic for the Ok and Cancel buttons
		Button okButton = (Button) findViewById(R.id.ok);
		okButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				addCallarounds();
				finish();
			}
		});

		Button cancelButton = (Button) findViewById(R.id.cancel);
		cancelButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
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
		DatePicker fromDatePicker = (DatePicker) findViewById(R.id.fromDate);
		DatePicker toDatePicker = (DatePicker) findViewById(R.id.toDate);
		TimePicker firstTimePicker = (TimePicker) findViewById(R.id.firstTime);

		Cursor cursor = (Cursor) mWhichHouse.getSelectedItem();
		long house_id = cursor.getLong(cursor.getColumnIndex("_id"));

		long window = Long.valueOf(mWindow.getText().toString());

		Calendar fromDate = Calendar.getInstance();
		fromDate.set(fromDatePicker.getYear(), fromDatePicker.getMonth(),
				fromDatePicker.getDayOfMonth());

		Calendar toDate = Calendar.getInstance();
		toDate.set(toDatePicker.getYear(), toDatePicker.getMonth(),
				toDatePicker.getDayOfMonth());

		String firstTime = String.format("%02d:%02d",
				firstTimePicker.getCurrentHour(),
				firstTimePicker.getCurrentMinute());

		Calendar firstTimeEarlyCal = Calendar.getInstance();
		firstTimeEarlyCal.set(Calendar.HOUR_OF_DAY,
				firstTimePicker.getCurrentHour());
		firstTimeEarlyCal.set(Calendar.MINUTE,
				firstTimePicker.getCurrentMinute());
		firstTimeEarlyCal.add(Calendar.MINUTE, -1 * (int) window);
		String firstTimeEarly = String.format("%02d:%02d",
				firstTimeEarlyCal.get(Calendar.HOUR_OF_DAY),
				firstTimeEarlyCal.get(Calendar.MONTH));

		String secondTime = null;
		String secondTimeEarly = null;
		if (mSecondTimePicker.isEnabled()) {
			secondTime = String.format("%02d:%02d",
					mSecondTimePicker.getCurrentHour(),
					mSecondTimePicker.getCurrentMinute());

			Calendar secondTimeEarlyCal = Calendar.getInstance();
			secondTimeEarlyCal.set(Calendar.HOUR_OF_DAY,
					mSecondTimePicker.getCurrentHour());
			secondTimeEarlyCal.set(Calendar.MINUTE,
					mSecondTimePicker.getCurrentMinute());
			secondTimeEarlyCal.add(Calendar.MINUTE, -1 * (int) window);
			secondTimeEarly = String.format("%02d:%02d",
					secondTimeEarlyCal.get(Calendar.HOUR_OF_DAY),
					secondTimeEarlyCal.get(Calendar.MONTH));
		}

		mDbHelper.addTravelCallarounds(house_id, fromDate, toDate, firstTime,
				firstTimeEarly, secondTime, secondTimeEarly);
	}
}
