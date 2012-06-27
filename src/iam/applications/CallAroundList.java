package iam.applications;

import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

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
		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(getBaseContext());
		boolean showFuture = settings.getBoolean(
				HomeActivity.PREFERENCES_CALLAROUNDS_SHOW_FUTURE, false);

		mReportCur = mDbHelper.fetchCallaroundReport(!showFuture);
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
		case R.id.add_travel_callaround:
			Intent i = new Intent(this, AddTravelCallaround.class);
			startActivity(i);
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

}
