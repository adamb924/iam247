/**
 * 
 */
package iam.applications;

import java.util.Date;

import android.app.ListActivity;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

/**
 * This is a simple <code>ListActivity</code> subclass for displaying the
 * event/error log. (Currently this is accessed from the menu of the
 * <code>HomeActivity</code>.
 * 
 */
public class LogList extends ListActivity {

	/** The database interface. */
	private transient DbAdapter mDbHelper;

	/**
	 * Called when the activity is first created.
	 * 
	 * @param bundle
	 *            the saved instance state
	 */
	@Override
	public void onCreate(final Bundle bundle) {
		super.onCreate(bundle);

		setContentView(R.layout.log_list);

		mDbHelper = new DbAdapter(this);
		mDbHelper.open();

		fillData();

		registerForContextMenu(getListView());
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
	 * 
	 */
	private void fillData() {
		final Cursor cur = mDbHelper.fetchLog();
		startManagingCursor(cur);

		final LogAdapter listAdapter = new LogAdapter(this, cur);
		setListAdapter(listAdapter);
	}

	/**
	 * The custom adapter is needed to format the time strings.
	 */
	private class LogAdapter extends ResourceCursorAdapter {

		/**
		 * Instantiates a new checkin adapter.
		 * 
		 * @param context
		 *            the context
		 * @param cur
		 *            the cur
		 */
		public LogAdapter(final Context context, final Cursor cur) {
			super(context, R.layout.log_item, cur);
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
			return inflater.inflate(R.layout.log_item, parent, false);
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
			final TextView date = (TextView) view.findViewById(R.id.log_date);
			final TextView message = (TextView) view
					.findViewById(R.id.log_text);
			final TextView type = (TextView) view.findViewById(R.id.log_type);

			final Date datedate = Time.iso8601DateTime(cur.getString(cur
					.getColumnIndex(DbAdapter.Columns.TIME)));

			date.setText(Time.timeTodayTomorrow(context, datedate));
			message.setText(cur.getString(cur
					.getColumnIndex(DbAdapter.Columns.MESSAGE)));
			type.setText(cur.getString(cur
					.getColumnIndex(DbAdapter.Columns.TYPE)));
		}
	}
}
