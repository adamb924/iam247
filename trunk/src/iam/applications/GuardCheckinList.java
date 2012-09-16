/**
 * 
 */
package iam.applications;

import android.app.ListActivity;
import android.content.Context;
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
	private DbAdapter mDbHelper;

	private long mGuardId;

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.guard_checkin_list);

		Bundle extras = getIntent().getExtras();
		mGuardId = extras != null ? extras.getLong(DbAdapter.KEY_ROWID) : -1;

		if (mGuardId == -1) {
			return;
		}

		mDbHelper = new DbAdapter(this);
		mDbHelper.open();

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

	/**
	 * Query the database and refresh the list.
	 */
	private void fillData() {
		Cursor c = mDbHelper.fetchGuardCheckinReport(mGuardId);
		GuardReportAdapter adapter = new GuardReportAdapter(this, c);
		getListView().setAdapter(adapter);
	}

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
		public GuardReportAdapter(Context context, Cursor cur) {
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
		public View newView(Context context, Cursor cur, ViewGroup parent) {
			LayoutInflater li = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			return li.inflate(android.R.layout.simple_list_item_1, parent,
					false);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.widget.CursorAdapter#bindView(android.view.View,
		 * android.content.Context, android.database.Cursor)
		 */
		@Override
		public void bindView(View view, Context context, Cursor cur) {
			String day = cur.getString(cur.getColumnIndex(DbAdapter.KEY_TIME));
			boolean missed = cur.getLong(cur
					.getColumnIndex(DbAdapter.KEY_RESPONSE)) == 1 ? false
					: true;

			TextView tv = (TextView) view.findViewById(android.R.id.text1);
			tv.setText(Time.prettyDate(GuardCheckinList.this, day));

			if (missed) {
				tv.setTextColor(Color.RED);
			} else {
				tv.setTextColor(Color.WHITE);
			}
		}
	}

}
