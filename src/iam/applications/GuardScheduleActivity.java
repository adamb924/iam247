/**
 * 
 */
package iam.applications;

import java.text.DateFormatSymbols;
import java.util.Calendar;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;

/**
 * The Class GuardScheduleActivity.
 * 
 * @author Adam
 * 
 *         An activity for setting the guard schedule for a house. This can be
 *         the schedule for the day, or a default schedule, depending on a value
 *         passed in the Bundle.
 */
public class GuardScheduleActivity extends Activity {

	/**
	 * Static string used as an extra key, indicating whether this is the
	 * default schedule or not.
	 */
	static public final String SET_TYPICAL = "SET_TYPICAL";

	/** The database interface. */
	private transient DbAdapter mDbHelper;

	/** The m spinners. */
	private transient Spinner[] mSpinners;

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(final Bundle bundle) {
		super.onCreate(bundle);

		setContentView(R.layout.guard_schedule);

		final Bundle extras = getIntent().getExtras();
		final boolean typical = extras == null ? false : extras
				.getBoolean(SET_TYPICAL);
		final long houseId = extras == null ? -1 : extras
				.getLong(DbAdapter.Columns.HOUSEID);

		mDbHelper = new DbAdapter(this);
		mDbHelper.open();

		setTitle(mDbHelper.getHouseName(houseId));

		final LinearLayout layout = (LinearLayout) findViewById(R.id.layout);

		mSpinners = new Spinner[7];

		if (typical) {
			// for the default configuration, just list a typical weekly
			// schedule

			// get the localized first day of the week
			final int first = Integer
					.parseInt(getString(R.string.loc_first_day_of_week));

			DateFormatSymbols symbols;
			String[] dayNames;
			TextView label;
			for (int i = 0; i < 7; i++) {
				final int day = (first + i) % 7;

				symbols = new DateFormatSymbols();
				dayNames = symbols.getWeekdays();

				label = new TextView(this);
				label.setText(dayNames[day + 1]);
				layout.addView(label);

				mSpinners[i] = new GuardSpinner(this, mDbHelper, day, typical,
						houseId);
				layout.addView(mSpinners[i]);
			}
		} else {
			// if you're setting it for particular days, make it different

			// let today be the first day displayed
			final Calendar cur = Calendar.getInstance();
			final int first = cur.get(Calendar.DAY_OF_WEEK) - 1; // subtract 1
																	// to make
																	// it
																	// 0-indexed

			TextView label;
			for (int i = 0; i < 7; i++) {
				final int day = (first + i) % 7;

				label = new TextView(this);
				label.setText(Time.prettyDate(this, cur.getTime()));
				layout.addView(label);

				mSpinners[i] = new GuardSpinner(this, mDbHelper, day, typical,
						houseId);
				layout.addView(mSpinners[i]);

				cur.add(Calendar.DATE, 1);
			}

		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onDestroy()
	 */
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mDbHelper != null) {
			mDbHelper.close();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		final MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.guard_schedule_context, menu);
		return super.onCreateOptionsMenu(menu);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		boolean retVal;
		if (item.getItemId() == R.id.set_all) {
			setAll();
			retVal = true;
		} else {
			retVal = super.onOptionsItemSelected(item);
		}
		return retVal;
		// switch (item.getItemId()) {
		// case R.id.set_all:
		// setAll();
		// return true;
		// default:
		// return super.onContextItemSelected(item);
		// }
	}

	/**
	 * Sets the values of all of the spinners.
	 */
	private void setAll() {
		final AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setCursor(mDbHelper.fetchAllGuards(),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(final DialogInterface dialog,
							final int item) {
						for (int i = 0; i < 7; i++) {
							mSpinners[i].setSelection(item);
						}
					}
				}, DbAdapter.Columns.NAME);
		alert.show();
	}

	/**
	 * The Class GuardSpinner.
	 */
	static private class GuardSpinner extends Spinner {

		/** The database adapter. */
		private transient final DbAdapter mDbHelper;

		/** The house id for the spinner. */
		private transient final long mHouseId;

		/** The cursor for the list of guards. */
		private transient final Cursor mCur;

		/** The 0-indexed day of the week of the spinner. */
		private transient final int mDay;

		/** True if this is a spinner for a typical day. */
		private transient final boolean mTypical;

		/**
		 * Instantiates a new guard spinner.
		 * 
		 * @param context
		 *            the context
		 * @param database
		 *            the database
		 * @param day
		 *            the 0-indexed day of the week
		 * @param typical
		 *            true if this is for setting the typical schedule
		 * @param house_id
		 *            the house_id
		 */
		public GuardSpinner(final Context context, final DbAdapter database,
				final int day, final boolean typical, final long house_id) {
			super(context);

			mDbHelper = database;
			mHouseId = house_id;
			mDay = day;
			mTypical = typical;

			mCur = mDbHelper.fetchAllGuards();

			final String[] fromFields = new String[] { DbAdapter.Columns.NAME };
			final int[] toFields = new int[] { android.R.id.text1 };

			final SimpleCursorAdapter adapter = new SimpleCursorAdapter(
					context, android.R.layout.simple_spinner_item, mCur,
					fromFields, toFields);
			setAdapter(adapter);

			if (mTypical) {
				setCurrent(mDbHelper.getTypicalGuard(mHouseId, mDay));
			} else {
				setCurrent(mDbHelper.getGuard(mHouseId, mDay));
			}

			setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
				@Override
				public void onItemSelected(final AdapterView<?> arg0,
						final View view, final int position, final long itemId) {
					if (mTypical) {
						mDbHelper.setTypicalGuard(house_id, itemId, mDay);
					} else {
						mDbHelper.setGuardForDay(house_id, itemId, mDay);
					}
				}

				@Override
				public void onNothingSelected(final AdapterView<?> arg0) {
					// empty
				}
			});
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Object#finalize()
		 */
		@Override
		protected void finalize() throws Throwable {
			mCur.close();

			super.finalize();
		}

		/**
		 * Sets the current item.
		 */
		private void setCurrent(final long guard) {
			if (guard == -1 || !mCur.moveToFirst()) {
				return;
			}

			int position = 0;
			do {
				if (mCur.getLong(0) == guard) {
					setSelection(position);
					break;
				}
				position++;
			} while (mCur.moveToNext());
		}

	}
}
