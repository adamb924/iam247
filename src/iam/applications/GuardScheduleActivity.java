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
	static public final String SET_DEFAULT = "SET_DEFAULT";

	/** The database interface. */
	private DbAdapter mDbHelper;

	/** The m set default. */
	private boolean mSetDefault;

	/** The m house id. */
	private long mHouseId;

	/** The m spinners. */
	private Spinner[] mSpinners;

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.guard_schedule);

		Bundle extras = getIntent().getExtras();
		mSetDefault = extras != null ? extras.getBoolean(SET_DEFAULT) : false;
		mHouseId = extras != null ? extras.getLong(DbAdapter.KEY_HOUSEID) : -1;

		mDbHelper = new DbAdapter(this);
		mDbHelper.open();

		setTitle(mDbHelper.getHouseName(mHouseId));

		LinearLayout layout = (LinearLayout) findViewById(R.id.layout);

		mSpinners = new Spinner[7];

		if (mSetDefault) {
			// for the default configuration, just list a typical weekly
			// schedule

			// get the localized first day of the week
			int first = Long.valueOf(getString(R.string.loc_first_day_of_week))
					.intValue();

			for (int i = 0; i < 7; i++) {
				int day = (first + i) % 7;

				DateFormatSymbols symbols = new DateFormatSymbols();
				String[] dayNames = symbols.getWeekdays();

				TextView label = new TextView(this);
				label.setText(dayNames[day + 1]);
				layout.addView(label);

				mSpinners[i] = new GuardSpinner(this, mDbHelper,
						DbAdapter.getGuardScheduleColumnName(day, mSetDefault),
						mHouseId);
				layout.addView(mSpinners[i]);
			}
		} else {
			// if you're setting it for particular days, make it different

			// let today be the first day displayed
			Calendar c = Calendar.getInstance();
			int first = c.get(Calendar.DAY_OF_WEEK);

			for (int i = 0; i < 7; i++) {
				int day = (first + i) % 7;

				TextView label = new TextView(this);
				label.setText(Time.prettyDate(this, c.getTime()));
				layout.addView(label);

				mSpinners[i] = new GuardSpinner(this, mDbHelper,
						DbAdapter.getGuardScheduleColumnName(day, mSetDefault),
						mHouseId);
				layout.addView(mSpinners[i]);

				c.add(Calendar.DATE, 1);
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
		if (mDbHelper != null)
			mDbHelper.close();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.guard_schedule_context, menu);
		return super.onCreateOptionsMenu(menu);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.set_all:
			setAll();
			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}

	/**
	 * Sets the values of all of the spinners.
	 */
	private void setAll() {
		// TODO commenting this creates a FindBugs warning, but I am at a loss
		// to understand why that happens here and not elsewhere. At some point
		// I should figure this out.
		if (mDbHelper == null) {
			return;
		}

		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle(getString(R.string.unblock_number));
		alert.setCursor(mDbHelper.fetchAllGuards(),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int item) {
						for (int i = 0; i < 7; i++) {
							mSpinners[i].setSelection(item);
						}
					}
				}, DbAdapter.KEY_NAME);
		alert.show();
	}

	/**
	 * The Class GuardSpinner.
	 */
	static private class GuardSpinner extends Spinner {

		/** The m db helper. */
		private final DbAdapter mDbHelper;

		/** The m column. */
		private final String mColumn;

		/** The m house id. */
		private final long mHouseId;

		/** The m cur. */
		private final Cursor mCur;

		/** The m guard. */
		private final long mGuard;

		/**
		 * Instantiates a new guard spinner.
		 * 
		 * @param context
		 *            the context
		 * @param db
		 *            the db
		 * @param column
		 *            the column
		 * @param house_id
		 *            the house_id
		 */
		public GuardSpinner(Context context, DbAdapter db, String column,
				long house_id) {
			super(context);

			mDbHelper = db;
			mColumn = column;
			mHouseId = house_id;

			mGuard = mDbHelper.getGuard(mHouseId, mColumn);

			mCur = mDbHelper.fetchAllGuards();

			String[] from = new String[] { DbAdapter.KEY_NAME };
			int[] to = new int[] { android.R.id.text1 };

			SimpleCursorAdapter adapter = new SimpleCursorAdapter(context,
					android.R.layout.simple_spinner_item, mCur, from, to);
			setAdapter(adapter);

			setCurrent();

			setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
				@Override
				public void onItemSelected(AdapterView<?> arg0, View view,
						int position, long id) {
					mDbHelper.setGuard(mHouseId, id, mColumn);
				}

				@Override
				public void onNothingSelected(AdapterView<?> arg0) {
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
		private void setCurrent() {
			if (mGuard == -1 || !mCur.moveToFirst()) {
				return;
			}

			int position = 0;
			do {
				if (mCur.getLong(0) == mGuard) {
					setSelection(position);
					break;
				}
				position++;
			} while (mCur.moveToNext());
		}

	}
}
