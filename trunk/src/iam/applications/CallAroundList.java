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
	private transient DbAdapter mDbHelper;

	/**
	 * A report cursor, which needs to be a field because it is accessed in
	 * onListItemClick
	 */
	private transient Cursor mReportCur;

	/**
	 * An adapter class to format the list items with the data from the database
	 * query
	 */
	private transient CallaroundAdapter mCaAdapter;

	/** An intent filter to catch all broadcast refresh requests. */
	private transient IntentFilter mIntentFilter;

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(final Bundle bundle) {
		super.onCreate(bundle);

		mIntentFilter = new IntentFilter(AlarmAdapter.Alerts.REFRESH);

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
		mReportCur.close();
		mDbHelper.close();
	}

	/**
	 * Query the database and refresh the list.
	 */
	private void fillData() {
		final SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(getBaseContext());
		final boolean showFuture = settings.getBoolean(
				Preferences.CALLAROUNDS_SHOW_FUTURE, false);

		mReportCur = mDbHelper.fetchCallaroundReport(!showFuture);
		startManagingCursor(mReportCur);
		mCaAdapter = new CallaroundAdapter(this, mReportCur);
		getListView().setAdapter(mCaAdapter);
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
	protected void onListItemClick(final ListView listView, final View view,
			final int position, final long itemId) {
		super.onListItemClick(listView, view, position, itemId);

		mReportCur.moveToPosition(position);
		final String day = mReportCur.getString(mReportCur
				.getColumnIndex(DbAdapter.Columns.DUEBY));

		final Intent intent = new Intent(this, CallAroundDetailList.class);
		intent.putExtra(DbAdapter.Columns.DUEBY, day);
		startActivity(intent);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		final MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.callaround_menu, menu);
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		boolean retVal = true;
		switch (item.getItemId()) {
		case R.id.add_callarounds:
			mDbHelper.addCallarounds();
			fillData();
			break;
		case R.id.add_travel_callaround:
			final Intent intent = new Intent(this, AddTravelCallaround.class);
			startActivity(intent);
			break;
		default:
			retVal = super.onOptionsItemSelected(item);
		}
		return retVal;
	}

	/**
	 * When the refresh request is received, call fillData() to refresh the
	 * screen.
	 */
	public transient BroadcastReceiver mRefreshReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(final Context context, final Intent intent) {
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
		public CallaroundAdapter(final Context context, final Cursor cur) {
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
		public View newView(final Context context, final Cursor cur,
				final ViewGroup parent) {
			final LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			return inflater.inflate(R.layout.twolinelistitem, parent, false);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.widget.CursorAdapter#bindView(android.view.View,
		 * android.content.Context, android.database.Cursor)
		 */
		@Override
		public void bindView(final View view, final Context context,
				final Cursor cur) {
			final String day = cur.getString(cur
					.getColumnIndex(DbAdapter.Columns.DUEBY));
			final long resolved = cur.getLong(cur
					.getColumnIndex(DbAdapter.Columns.RESOLVED));
			final long outstanding = cur.getLong(cur
					.getColumnIndex(DbAdapter.Columns.OUTSTANDING));

			String caSummary;
			if (outstanding + resolved == 0) {
				caSummary = CallAroundList.this
						.getString(R.string.callaround_summary_none);
			} else if (outstanding == 0) {
				caSummary = CallAroundList.this
						.getString(R.string.callaround_summary_allin);
			} else {
				caSummary = String.format(CallAroundList.this
						.getString(R.string.callaround_summary), String
						.valueOf(resolved), String.valueOf(outstanding));
			}

			final TextView tvTitle = (TextView) view.findViewById(R.id.text1);
			tvTitle.setText(Time.prettyDate(CallAroundList.this, day));
			final TextView tvDetails = (TextView) view.findViewById(R.id.text2);
			tvDetails.setText(caSummary);
		}
	}

}
