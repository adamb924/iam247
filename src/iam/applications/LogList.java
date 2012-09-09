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
	private DbAdapter mDbHelper;

	/**
	 * Called when the activity is first created.
	 * 
	 * @param savedInstanceState
	 *            the saved instance state
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

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
		Cursor c = mDbHelper.fetchLog();
		startManagingCursor(c);

		LogAdapter listAdapter = new LogAdapter(this, c);
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
		public LogAdapter(Context context, Cursor cur) {
			// TODO should this be log_item?
			super(context, R.layout.checkin_item, cur);
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
			// TODO should this be log_item?
			return li.inflate(R.layout.checkin_item, parent, false);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.widget.CursorAdapter#bindView(android.view.View,
		 * android.content.Context, android.database.Cursor)
		 */
		@Override
		public void bindView(View view, Context context, Cursor cur) {
			TextView date = (TextView) view.findViewById(R.id.log_date);
			TextView message = (TextView) view.findViewById(R.id.log_text);
			TextView type = (TextView) view.findViewById(R.id.log_type);

			Date datedate = Time.iso8601DateTime(cur.getString(cur
					.getColumnIndex(DbAdapter.KEY_TIME)));

			date.setText(Time.timeTodayTomorrow(context, datedate));
			message.setText(cur.getString(cur
					.getColumnIndex(DbAdapter.KEY_MESSAGE)));
			type.setText(cur.getString(cur.getColumnIndex(DbAdapter.KEY_TYPE)));
		}
	}
}
