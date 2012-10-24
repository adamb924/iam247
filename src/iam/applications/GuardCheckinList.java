/**
 * 
 */
package iam.applications;

import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

/**
 * @author Adam
 * 
 */
public class GuardCheckinList extends ListActivity {

	/** The database interface. */
	private transient DbAdapter mDbHelper;

	private transient long mGuardId;

	/** An intent filter to catch all broadcast refresh requests. */
	private transient IntentFilter mIntentFilter;

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(final Bundle bundle) {
		super.onCreate(bundle);

		setContentView(R.layout.guard_checkin_list);

		final Bundle extras = getIntent().getExtras();
		mGuardId = extras == null ? -1 : extras
				.getLong(DbAdapter.Columns.ROWID);

		if (mGuardId == -1) {
			return;
		}

		mDbHelper = new DbAdapter(this);
		mDbHelper.open();

		mIntentFilter = new IntentFilter(AlarmAdapter.Alerts.REFRESH);

		setTitle(mDbHelper.getGuardName(mGuardId));

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

	/**
	 * Query the database and refresh the list.
	 */
	private void fillData() {
		final Cursor cur = mDbHelper.fetchGuardCheckinReport(mGuardId);
		startManagingCursor(cur);
		final GuardReportAdapter adapter = new GuardReportAdapter(this, cur);
		getListView().setAdapter(adapter);
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
	private class GuardReportAdapter extends ResourceCursorAdapter {

		/**
		 * Instantiates a new adapter.
		 * 
		 * @param context
		 *            the context
		 * @param cur
		 *            the cur
		 */
		public GuardReportAdapter(final Context context, final Cursor cur) {
			super(context, android.R.layout.simple_list_item_1, cur);
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
			return inflater.inflate(android.R.layout.simple_list_item_1,
					parent, false);
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
					.getColumnIndex(DbAdapter.Columns.TIME));
			final boolean response = cur.getLong(cur
					.getColumnIndex(DbAdapter.Columns.RESPONSE)) == 1 ? true
					: false;

			final TextView textview = (TextView) view
					.findViewById(android.R.id.text1);
			textview.setText(Time.prettyDateTime(day));

			if (response) {
				textview.setTextColor(Color.WHITE);
			} else {
				textview.setTextColor(Color.RED);
			}
		}
	}

}
