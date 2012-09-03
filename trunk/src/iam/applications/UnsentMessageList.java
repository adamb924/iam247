package iam.applications;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

/**
 * @author Adam
 * 
 *         This is a <code>ListActivity</code> subclass for keeping track of
 *         sent messages. It displays a list of messages which have not (yet)
 *         been confirmed as having been sent or delivered. It is accessed from
 *         the <code>HomeActivity</code>.
 * 
 */
public class UnsentMessageList extends ListActivity {

	/** The database interface */
	private DbAdapter mDbHelper;

	/**
	 * Cursor for the database query; this can be left as a field in case we
	 * want to go back and allow users to click on individual messages.
	 */
	private Cursor mReportCur;

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

		setContentView(R.layout.unsent_messages);

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

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onPause()
	 */
	@Override
	protected void onPause() {
		super.onPause();

		fillData();
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

		registerReceiver(mRefreshReceiver, mIntentFilter);
		fillData();
	}

	/**
	 * Refresh the data from the database.
	 */
	protected void fillData() {
		mReportCur = mDbHelper.fetchUnsentUndeliveredMessages();
		ListAdapter listAdapter = new ListAdapter(this, mReportCur);
		setListAdapter(listAdapter);
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.unsentmessage_menu, menu);
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
		case R.id.delete_all:
			AlertDialog.Builder alert = new AlertDialog.Builder(this);
			alert.setTitle(R.string.delete_all);
			alert.setMessage(R.string.delete_all_unsent_warning);
			alert.setPositiveButton("Ok",
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog,
								int whichButton) {

							mDbHelper.deleteUnsentUndelivered();
							fillData();
							AlarmReceiver
									.sendRefreshAlert(UnsentMessageList.this);
						}
					});
			alert.setNegativeButton("Cancel", null);
			alert.show();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	/**
	 * An adapter for formatting the database query result into a proper list
	 * item. This need to be a custom adapter because of (1) the need to format
	 * the date, (2) it's better to format the summary line with a localizable
	 * string rather than hard-wiring it into the database query.
	 */
	private class ListAdapter extends ResourceCursorAdapter {

		/**
		 * Instantiates a new adapter.
		 * 
		 * @param context
		 *            the context
		 * @param cur
		 *            the cur
		 */
		public ListAdapter(Context context, Cursor cur) {
			super(context, R.layout.unsent_message_item, cur);
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
			return li.inflate(R.layout.unsent_message_item, parent, false);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.widget.CursorAdapter#bindView(android.view.View,
		 * android.content.Context, android.database.Cursor)
		 */
		@Override
		public void bindView(View view, Context context, Cursor cur) {
			String phoneNumber = cur.getString(cur
					.getColumnIndex(DbAdapter.KEY_NUMBER));
			long contactId = mDbHelper.getContactId(phoneNumber);
			String recipient = contactId == -1 ? phoneNumber : mDbHelper
					.getContactName(contactId);

			String type = cur.getString(cur.getColumnIndex(DbAdapter.KEY_TYPE))
					.equals("Unsent") ? context.getString(R.string.unsent)
					: context.getString(R.string.unconfirmed);

			String time = Time.prettyDateTime(cur.getString(cur
					.getColumnIndex(DbAdapter.KEY_TIME)));
			String message = cur.getString(cur
					.getColumnIndex(DbAdapter.KEY_MESSAGE));

			((TextView) view.findViewById(R.id.messageRecipient))
					.setText(recipient);
			((TextView) view.findViewById(R.id.errorType)).setText(type);
			((TextView) view.findViewById(R.id.time)).setText(time);
			((TextView) view.findViewById(R.id.messageText)).setText(message);
		}
	}
}
