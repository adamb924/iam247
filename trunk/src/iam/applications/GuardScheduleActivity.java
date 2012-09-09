/**
 * 
 */
package iam.applications;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;

/**
 * @author Adam
 * 
 *         There's a lot that could be done to make this class better. A lot.
 */
public class GuardScheduleActivity extends Activity {

	static public String SET_DEFAULT = "SET_DEFAULT";

	/** The database interface. */
	private DbAdapter mDbHelper;

	private boolean mSetDefault;
	private long mHouseId;

	private Spinner mSunday;
	private Spinner mMonday;
	private Spinner mTuesday;
	private Spinner mWednesday;
	private Spinner mThursday;
	private Spinner mFriday;
	private Spinner mSaturday;

	private SimpleCursorAdapter mAdapter;

	private Cursor mCur;

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

		mSunday = (Spinner) findViewById(R.id.sunday);
		mMonday = (Spinner) findViewById(R.id.monday);
		mTuesday = (Spinner) findViewById(R.id.tuesday);
		mWednesday = (Spinner) findViewById(R.id.wednesday);
		mThursday = (Spinner) findViewById(R.id.thursday);
		mFriday = (Spinner) findViewById(R.id.friday);
		mSaturday = (Spinner) findViewById(R.id.saturday);

		mSunday.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> arg0, View view,
					int position, long id) {
				mDbHelper.setGuard(mHouseId, id,
						mSetDefault ? DbAdapter.SUNDAY_GUARD_DEFAULT
								: DbAdapter.SUNDAY_GUARD);
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});

		mMonday.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> arg0, View view,
					int position, long id) {
				mDbHelper.setGuard(mHouseId, id,
						mSetDefault ? DbAdapter.MONDAY_GUARD_DEFAULT
								: DbAdapter.MONDAY_GUARD);
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});

		mTuesday.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> arg0, View view,
					int position, long id) {
				mDbHelper.setGuard(mHouseId, id,
						mSetDefault ? DbAdapter.TUESDAY_GUARD_DEFAULT
								: DbAdapter.TUESDAY_GUARD);

			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});

		mWednesday
				.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
					@Override
					public void onItemSelected(AdapterView<?> arg0, View view,
							int position, long id) {
						mDbHelper.setGuard(mHouseId, id,
								mSetDefault ? DbAdapter.WEDNESDAY_GUARD_DEFAULT
										: DbAdapter.WEDNESDAY_GUARD);

					}

					@Override
					public void onNothingSelected(AdapterView<?> arg0) {
					}
				});

		mThursday
				.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
					@Override
					public void onItemSelected(AdapterView<?> arg0, View view,
							int position, long id) {
						mDbHelper.setGuard(mHouseId, id,
								mSetDefault ? DbAdapter.THURSDAY_GUARD_DEFAULT
										: DbAdapter.THURSDAY_GUARD);

					}

					@Override
					public void onNothingSelected(AdapterView<?> arg0) {
					}
				});

		mFriday.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> arg0, View view,
					int position, long id) {
				mDbHelper.setGuard(mHouseId, id,
						mSetDefault ? DbAdapter.FRIDAY_GUARD_DEFAULT
								: DbAdapter.FRIDAY_GUARD);

			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});

		mSaturday
				.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
					@Override
					public void onItemSelected(AdapterView<?> arg0, View view,
							int position, long id) {
						mDbHelper.setGuard(mHouseId, id,
								mSetDefault ? DbAdapter.SATURDAY_GUARD_DEFAULT
										: DbAdapter.SATURDAY_GUARD);
					}

					@Override
					public void onNothingSelected(AdapterView<?> arg0) {
					}
				});

		fillData();
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

	private void fillData() {
		mCur = mDbHelper.fetchAllGuards();
		startManagingCursor(mCur);

		String[] from = new String[] { DbAdapter.KEY_NAME };
		int[] to = new int[] { android.R.id.text1 };

		mAdapter = new SimpleCursorAdapter(this,
				android.R.layout.simple_spinner_item, mCur, from, to);

		mSunday.setAdapter(mAdapter);
		mMonday.setAdapter(mAdapter);
		mTuesday.setAdapter(mAdapter);
		mWednesday.setAdapter(mAdapter);
		mThursday.setAdapter(mAdapter);
		mFriday.setAdapter(mAdapter);
		mSaturday.setAdapter(mAdapter);

		setCurrent(mSunday, mSetDefault ? DbAdapter.SUNDAY_GUARD_DEFAULT
				: DbAdapter.SUNDAY_GUARD);
		setCurrent(mMonday, mSetDefault ? DbAdapter.MONDAY_GUARD_DEFAULT
				: DbAdapter.MONDAY_GUARD);
		setCurrent(mTuesday, mSetDefault ? DbAdapter.TUESDAY_GUARD_DEFAULT
				: DbAdapter.TUESDAY_GUARD);
		setCurrent(mWednesday, mSetDefault ? DbAdapter.WEDNESDAY_GUARD_DEFAULT
				: DbAdapter.WEDNESDAY_GUARD);
		setCurrent(mThursday, mSetDefault ? DbAdapter.THURSDAY_GUARD_DEFAULT
				: DbAdapter.THURSDAY_GUARD);
		setCurrent(mFriday, mSetDefault ? DbAdapter.FRIDAY_GUARD_DEFAULT
				: DbAdapter.FRIDAY_GUARD);
		setCurrent(mSaturday, mSetDefault ? DbAdapter.SATURDAY_GUARD_DEFAULT
				: DbAdapter.SATURDAY_GUARD);
	}

	private void setCurrent(Spinner s, String label) {
		if (!mCur.moveToFirst()) {
			return;
		}

		int position = 0;
		do {
			if (mCur.getLong(0) == mDbHelper.getGuard(mHouseId, label)) {
				s.setSelection(position);
				break;
			}
			position++;
		} while (mCur.moveToNext());
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
						mCur.moveToPosition(item);
						long id = mCur.getLong(0);

						mDbHelper.setGuard(mHouseId, id,
								mSetDefault ? DbAdapter.SUNDAY_GUARD_DEFAULT
										: DbAdapter.SUNDAY_GUARD);
						mDbHelper.setGuard(mHouseId, id,
								mSetDefault ? DbAdapter.MONDAY_GUARD_DEFAULT
										: DbAdapter.MONDAY_GUARD);
						mDbHelper.setGuard(mHouseId, id,
								mSetDefault ? DbAdapter.TUESDAY_GUARD_DEFAULT
										: DbAdapter.TUESDAY_GUARD);
						mDbHelper.setGuard(mHouseId, id,
								mSetDefault ? DbAdapter.WEDNESDAY_GUARD_DEFAULT
										: DbAdapter.WEDNESDAY_GUARD);
						mDbHelper.setGuard(mHouseId, id,
								mSetDefault ? DbAdapter.THURSDAY_GUARD_DEFAULT
										: DbAdapter.THURSDAY_GUARD);
						mDbHelper.setGuard(mHouseId, id,
								mSetDefault ? DbAdapter.FRIDAY_GUARD_DEFAULT
										: DbAdapter.FRIDAY_GUARD);
						mDbHelper.setGuard(mHouseId, id,
								mSetDefault ? DbAdapter.SATURDAY_GUARD_DEFAULT
										: DbAdapter.SATURDAY_GUARD);

						fillData();
					}
				}, DbAdapter.KEY_NAME);
		alert.show();
	}
}
