package iam.applications;

import java.util.Date;

import android.app.ListActivity;
import android.app.TimePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;
import android.widget.TimePicker;

/**
 * A ListActivity that shows a list of call around summaries (how many in, how
 * many out) for all of the days in the database.
 */
public class CallAroundList extends ListActivity {

	/** The database interface */
	private DbAdapter mDbHelper;

	/**
	 * A report cursor, which needs to be a field because it is accessed in
	 * onListItemClick
	 */
	private Cursor mReportCur;

	/**
	 * An adapter class to format the list items with the data from the database
	 * query
	 */
	private CallaroundAdapter mCallaroundAdapter;

	/** An intent filter to catch all broadcast refresh requests. */
	private IntentFilter mIntentFilter;

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mIntentFilter = new IntentFilter(AlarmReceiver.ALERT_REFRESH);

		mDbHelper = new DbAdapter(this);
		mDbHelper.open();

		fillData();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.ListActivity#onDestroy()
	 */
	@Override
	protected void onDestroy() {
		super.onDestroy();

		mDbHelper.close();
	}

	/**
	 * Query the database and refresh the list.
	 */
	private void fillData() {
		mReportCur = mDbHelper.fetchCallaroundReport(getShowFuture(this));
		startManagingCursor(mReportCur);
		mCallaroundAdapter = new CallaroundAdapter(this, mReportCur);
		getListView().setAdapter(mCallaroundAdapter);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onPause()
	 */
	@Override
	protected void onPause() {
		super.onPause();
		unregisterReceiver(mRefreshReceiver);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume() {
		super.onResume();
		fillData();
		registerReceiver(mRefreshReceiver, mIntentFilter);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.ListActivity#onListItemClick(android.widget.ListView,
	 * android.view.View, int, long)
	 */
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);

		mReportCur.moveToPosition(position);
		String day = mReportCur.getString(mReportCur
				.getColumnIndex(DbAdapter.KEY_DUEBY));

		Intent i = new Intent(this, CallAroundDetailList.class);
		i.putExtra(DbAdapter.KEY_DUEBY, day);
		startActivity(i);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.callaround_menu, menu);

		MenuItem showFuture = menu.findItem(R.id.show_future);
		showFuture.setCheckable(true);
		showFuture.setChecked(getShowFuture(this));

		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.add_callarounds:
			mDbHelper.addCallarounds();
			fillData();
			return true;
		case R.id.callaround_due:
			callaroundDueSetting();
			return true;
		case R.id.callaround_add:
			callaroundAddSetting();
			return true;
		case R.id.callaround_earliest:
			callaroundEarliestSetting();
			return true;
		case R.id.add_travel_callaround:
			Intent i = new Intent(this, AddTravelCallaround.class);
			startActivity(i);
			return true;
		case R.id.show_future:
			boolean newValue = !item.isChecked();
			item.setChecked(newValue);
			setShowFuture(newValue);
			fillData();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	/**
	 * When the refresh request is received, call fillData() to refresh the
	 * screen.
	 */
	public BroadcastReceiver mRefreshReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			fillData();
		};
	};

	/**
	 * An adapter for formatting the database query result into a proper list
	 * item. This need to be a custom adapter because of (1) the need to format
	 * the date, (2) it's better to format the summary line with a localizable
	 * string rather than hard-wiring it into the database query.
	 */
	private class CallaroundAdapter extends ResourceCursorAdapter {

		/**
		 * Instantiates a new adapter.
		 * 
		 * @param context
		 *            the context
		 * @param cur
		 *            the cur
		 */
		public CallaroundAdapter(Context context, Cursor cur) {
			super(context, R.layout.twolinelistitem, cur);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * android.widget.ResourceCursorAdapter#newView(android.content.Context,
		 * android.database.Cursor, android.view.ViewGroup)
		 */
		@Override
		public View newView(Context context, Cursor cur, ViewGroup parent) {
			LayoutInflater li = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			return li.inflate(R.layout.twolinelistitem, parent, false);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.widget.CursorAdapter#bindView(android.view.View,
		 * android.content.Context, android.database.Cursor)
		 */
		@Override
		public void bindView(View view, Context context, Cursor cur) {
			String day = cur.getString(cur.getColumnIndex(DbAdapter.KEY_DUEBY));
			long resolved = cur.getLong(cur
					.getColumnIndex(DbAdapter.KEY_RESOLVED));
			long outstanding = cur.getLong(cur
					.getColumnIndex(DbAdapter.KEY_OUTSTANDING));

			// Log.i("Debug", "Resolved: " + String.valueOf(resolved));
			// Log.i("Debug", "Outstanding: " + String.valueOf(outstanding));

			String callaround_summary;
			if (outstanding + resolved == 0) {
				callaround_summary = CallAroundList.this
						.getString(R.string.callaround_summary_none);
			} else if (outstanding == 0) {
				callaround_summary = CallAroundList.this
						.getString(R.string.callaround_summary_allin);
			} else {
				callaround_summary = String.format(CallAroundList.this
						.getString(R.string.callaround_summary), String
						.valueOf(resolved), String.valueOf(outstanding));
			}

			TextView tvTitle = (TextView) view.findViewById(R.id.text1);
			tvTitle.setText(Time.prettyDate(CallAroundList.this, day));
			TextView tvDetails = (TextView) view.findViewById(R.id.text2);
			tvDetails.setText(callaround_summary);
		}
	}

	public void callaroundDueSetting() {
		// setTimePreferencesString(HomeActivity.PREFERENCES_CALLAROUND_DUE,
		// "21:00");

		SharedPreferences settings = getSharedPreferences(
				HomeActivity.PREFERENCES, 0);

		String old = settings.getString(
				HomeActivity.PREFERENCES_CALLAROUND_DUE_BY, "21:00");
		Date date = Time.timeFromString(this, old);

		TimePickerDialog dialog = new TimePickerDialog(this, 0,
				new TimePickerDialog.OnTimeSetListener() {
					@Override
					public void onTimeSet(TimePicker view, int hourOfDay,
							int minute) {
						SharedPreferences settings = getSharedPreferences(
								HomeActivity.PREFERENCES, 0);

						String newTime = String.format("%02d:%02d", hourOfDay,
								minute);

						SharedPreferences.Editor editor = settings.edit();

						editor.putString(
								HomeActivity.PREFERENCES_CALLAROUND_DUE_BY,
								newTime);
						editor.commit();

						// AlarmReceiver
						// .setCallaroundDueAlarm(CallAroundList.this);

					}
				}, date.getHours(), date.getMinutes(), false);

		dialog.show();
	}

	public void callaroundAddSetting() {
		SharedPreferences settings = getSharedPreferences(
				HomeActivity.PREFERENCES, 0);

		String old = settings.getString(
				HomeActivity.PREFERENCES_CALLAROUND_ADD, "21:00");
		Date date = Time.timeFromString(this, old);

		TimePickerDialog dialog = new TimePickerDialog(this, 0,
				new TimePickerDialog.OnTimeSetListener() {
					@Override
					public void onTimeSet(TimePicker view, int hourOfDay,
							int minute) {
						SharedPreferences settings = getSharedPreferences(
								HomeActivity.PREFERENCES, 0);

						String newTime = String.format("%02d:%02d", hourOfDay,
								minute);

						SharedPreferences.Editor editor = settings.edit();

						editor.putString(
								HomeActivity.PREFERENCES_CALLAROUND_ADD,
								newTime);
						editor.commit();

						AlarmReceiver
								.setAddCallaroundAlarm(CallAroundList.this);
					}
				}, date.getHours(), date.getMinutes(), false);

		dialog.show();

	}

	public void callaroundEarliestSetting() {
		SharedPreferences settings = getSharedPreferences(
				HomeActivity.PREFERENCES, 0);

		String old = settings.getString(
				HomeActivity.PREFERENCES_CALLAROUND_DUE_FROM, "00:00");
		Date date = Time.timeFromString(this, old);

		TimePickerDialog dialog = new TimePickerDialog(this, 0,
				new TimePickerDialog.OnTimeSetListener() {
					@Override
					public void onTimeSet(TimePicker view, int hourOfDay,
							int minute) {
						SharedPreferences settings = getSharedPreferences(
								HomeActivity.PREFERENCES, 0);

						String newTime = String.format("%02d:%02d", hourOfDay,
								minute);

						SharedPreferences.Editor editor = settings.edit();

						editor.putString(
								HomeActivity.PREFERENCES_CALLAROUND_DUE_FROM,
								newTime);
						editor.commit();

						AlarmReceiver
								.setAddCallaroundAlarm(CallAroundList.this);
					}
				}, date.getHours(), date.getMinutes(), false);

		dialog.show();

	}

	public boolean getShowFuture(Context context) {
		SharedPreferences settings = context.getSharedPreferences(
				HomeActivity.PREFERENCES, 0);
		return settings.getBoolean(
				HomeActivity.PREFERENCES_CALLAROUNDS_SHOW_FUTURE, false);
	}

	private void setShowFuture(boolean allowed) {
		SharedPreferences settings = getSharedPreferences(
				HomeActivity.PREFERENCES, 0);

		SharedPreferences.Editor editor = settings.edit();
		editor.putBoolean(HomeActivity.PREFERENCES_CALLAROUNDS_SHOW_FUTURE,
				allowed);
		editor.commit();
	}

}
