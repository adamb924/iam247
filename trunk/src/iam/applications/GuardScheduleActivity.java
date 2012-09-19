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
 * @author Adam
 * 
 *         There's a lot that could be done to make this class better. A lot.
 */
public class GuardScheduleActivity extends Activity {

	static public String SET_DEFAULT = "SET_DEFAULT";

	static public String[] DAYS = { "Sunday", "Monday", "Tuesday", "Wednesday",
			"Thursday", "Friday", "Saturday" };

	/** The database interface. */
	private DbAdapter mDbHelper;

	private boolean mSetDefault;
	private long mHouseId;

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

				mSpinners[i] = new GuardSpinner(this, mDbHelper, getColumnName(
						day, mSetDefault), mHouseId);
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

				mSpinners[i] = new GuardSpinner(this, mDbHelper, getColumnName(
						day, mSetDefault), mHouseId);
				layout.addView(mSpinners[i]);

				c.add(Calendar.DATE, 1);
			}

		}

	}

	static private String getColumnName(int i, boolean default_column) {
		if (default_column) {
			return DAYS[i].toLowerCase() + "_guard";
		} else {
			return "typical_" + DAYS[i].toLowerCase() + "_guard";
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

	private void setAll() {
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

	private class GuardSpinner extends Spinner {

		private final DbAdapter mDbHelper;
		private final String mColumn;
		private final long mHouseId;
		private final Cursor mCur;
		private final long mGuard;

		/**
		 * @param context
		 */
		public GuardSpinner(Context context, DbAdapter db, String column,
				long house_id) {
			super(context);

			mDbHelper = db;
			mColumn = column;
			mHouseId = house_id;

			mGuard = mDbHelper.getGuard(mHouseId, mColumn);

			mCur = mDbHelper.fetchAllGuards();
			startManagingCursor(mCur);

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
