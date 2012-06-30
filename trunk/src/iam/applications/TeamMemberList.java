package iam.applications;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ResourceCursorAdapter;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;

/**
 * This Activity displays two lists, one of checked-in team members and one of
 * checked-out team members. Long-clicking on the team member produces various
 * options.
 */
public class TeamMemberList extends Activity {

	/** The database interface. */
	private DbAdapter mDbHelper;

	/** The list of checked-in people. */
	private ListView mCheckedIn;

	/** The list of checked-out people. */
	private ListView mCheckedOut;

	/** An intent filter to catch all broadcast refresh requests. */
	private IntentFilter mIntentFilter;

	private long mContactId;

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.teammember_list);

		mDbHelper = new DbAdapter(this);
		mDbHelper.open();

		mIntentFilter = new IntentFilter(AlarmReceiver.ALERT_REFRESH);

		mCheckedOut = (ListView) findViewById(R.id.checkedout);
		mCheckedIn = (ListView) findViewById(R.id.checkedin);

		fillData();

		registerForContextMenu(mCheckedOut);
		registerForContextMenu(mCheckedIn);
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

	@Override
	protected void onPause() {
		unregisterReceiver(mRefreshReceiver);

		super.onPause();
	}

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
		// Log.i("Debug", "TeamMembersList fill data");

		Cursor checkedoutCur = mDbHelper.fetchCheckedOutPeople();
		startManagingCursor(checkedoutCur);

		if (checkedoutCur.getCount() > 0) {
			TeammemberAdapter mCheckedOutListAdapter = new TeammemberAdapter(
					this, checkedoutCur);
			mCheckedOut.setAdapter(mCheckedOutListAdapter);
		} else {
			ViewGroup vg = (ViewGroup) findViewById(R.id.teammember_layout);
			vg.removeView(mCheckedOut);
			vg.removeView(findViewById(R.id.checkedout_label));
		}

		Cursor checkedinCur = mDbHelper.fetchCheckedInPeople();
		startManagingCursor(checkedinCur);

		TeammemberAdapter mCheckedInListAdapter = new TeammemberAdapter(this,
				checkedinCur);
		mCheckedIn.setAdapter(mCheckedInListAdapter);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreateContextMenu(android.view.ContextMenu,
	 * android.view.View, android.view.ContextMenu.ContextMenuInfo)
	 */
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.teammember_menu, menu);

		AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
		long contact_id = info.id;
		long house_id = mDbHelper.getHouseId(contact_id);

		if (!mDbHelper.getContactHasCheckinOutstanding(contact_id)) {
			menu.removeItem(R.id.resolve_checkin);
		}

		if (!mDbHelper.getCallaroundActive(house_id) || house_id == -1) {
			menu.removeItem(R.id.callaround_resolved);
		} else {
			MenuItem callaroundResolved = menu
					.findItem(R.id.callaround_resolved);
			callaroundResolved.setCheckable(true);
			callaroundResolved.setChecked(!mDbHelper
					.getCallaroundOutstanding(house_id));
		}

		MenuItem callaroundEnabled = menu.findItem(R.id.callaround_enabled);
		callaroundEnabled.setCheckable(true);
		callaroundEnabled.setChecked(mDbHelper.getCallaroundActive(house_id));

		MenuItem reportsEnabled = menu.findItem(R.id.allow_request_reports);
		reportsEnabled.setCheckable(true);
		reportsEnabled.setChecked(mDbHelper.getContactPermission(contact_id,
				DbAdapter.USER_PERMISSION_REPORT));

		MenuItem checkinRemindersEnabled = menu
				.findItem(R.id.checkin_reminders);
		checkinRemindersEnabled.setCheckable(true);
		checkinRemindersEnabled.setChecked(mDbHelper.getContactPreference(
				contact_id, DbAdapter.USER_PREFERENCE_CHECKIN_REMINDER));

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onContextItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
				.getMenuInfo();
		mContactId = info == null ? mContactId : info.id;

		long house_id = mDbHelper.getHouseId(mContactId);
		boolean newValue;

		switch (item.getItemId()) {
		case R.id.call_number:
			callNumber(mContactId);
			return true;
		case R.id.set_house:
			setHouse(mContactId);
			return true;
		case R.id.resolve_checkin:
			mDbHelper.setCheckinResolved(mContactId, true);
			SmsHandler.sendSms(this, mDbHelper.getContactNumber(mContactId),
					getString(R.string.sms_checkin_resolved_foryou));
			fillData();
			return true;
		case R.id.delete_teammember:
			mDbHelper.deleteContact(mContactId);
			fillData();
			return true;
		case R.id.edit_name:
			editName(mContactId);
			return true;
		case R.id.edit_phone:
			editPhone(mContactId);
			return true;
		case R.id.edit_email:
			editEmail(mContactId);
			return true;
		case R.id.block_number:
			blockNumber(mContactId);
			return true;
		case R.id.allow_request_reports:
			newValue = !item.isChecked();
			item.setChecked(newValue);
			mDbHelper.setContactPermission(mContactId,
					DbAdapter.USER_PERMISSION_REPORT, newValue);
			return true;
		case R.id.callaround_enabled:
			newValue = !item.isChecked();
			item.setChecked(newValue);
			mDbHelper.setCallaroundActive(house_id, newValue);
			return true;
		case R.id.callaround_resolved:
			newValue = !item.isChecked();
			item.setChecked(newValue);
			mDbHelper.setCallaroundResolved(house_id, newValue);
			return true;
		case R.id.checkin_reminders:
			newValue = !item.isChecked();
			item.setChecked(newValue);
			mDbHelper.setContactPreference(mContactId,
					DbAdapter.USER_PREFERENCE_CHECKIN_REMINDER, newValue);
		default:
			return super.onContextItemSelected(item);
		}
	}

	/**
	 * Warns the user, and then adds the contact's number to the blocked numbers
	 * list.
	 * 
	 * @param contact_id
	 */
	private void blockNumber(final long contact_id) {
		AlertDialog.Builder alert = new AlertDialog.Builder(TeamMemberList.this);
		alert.setTitle(R.string.block_number);
		alert.setMessage(R.string.block_number_warning);
		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int whichButton) {
				mDbHelper.setNumberIsBlocked(
						mDbHelper.getContactNumber(contact_id), true);
			}
		});
		alert.setNegativeButton("Cancel", null);
		alert.show();
	}

	/**
	 * Prompts the user to edit the contact's email address, and saves the
	 * result.
	 * 
	 * @param contact_id
	 */
	private void editEmail(final long contact_id) {
		AlertDialog.Builder alert;
		final EditText editinput;
		alert = new AlertDialog.Builder(TeamMemberList.this);
		editinput = new EditText(TeamMemberList.this);
		editinput.setText(mDbHelper.getContactEmail(contact_id));
		alert.setView(editinput);
		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int whichButton) {
				String value = editinput.getText().toString();
				if (value.length() > 0) {
					mDbHelper.setContactEmail(contact_id, value);
					fillData();
				}
			}
		});
		alert.setNegativeButton("Cancel", null);
		alert.show();
	}

	/**
	 * Prompts the user to edit the contact's phone number, and saves the
	 * result. Currently only the first phone number associated with an account
	 * is used.
	 * 
	 * @param contact_id
	 */
	private void editPhone(final long contact_id) {
		AlertDialog.Builder alert;
		final EditText editinput;
		alert = new AlertDialog.Builder(TeamMemberList.this);
		editinput = new EditText(TeamMemberList.this);
		editinput.setText(mDbHelper.getContactNumber(contact_id));
		alert.setView(editinput);
		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int whichButton) {
				String value = editinput.getText().toString();
				if (value.length() > 0) {
					mDbHelper.setContactPhone(contact_id, value);
					fillData();
				}
			}
		});
		alert.setNegativeButton("Cancel", null);
		alert.show();
	}

	/**
	 * Prompts the user to edit the contact's email address, and saves the
	 * result.
	 * 
	 * @param contact_id
	 */
	private void editName(final long contact_id) {
		AlertDialog.Builder alert;
		final EditText editinput;
		alert = new AlertDialog.Builder(TeamMemberList.this);
		editinput = new EditText(TeamMemberList.this);
		editinput.setText(mDbHelper.getContactName(contact_id));
		alert.setView(editinput);
		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int whichButton) {
				String value = editinput.getText().toString();
				if (value.length() > 0) {
					mDbHelper.setContactName(contact_id, value);
					fillData();
				}
			}
		});
		alert.setNegativeButton("Cancel", null);
		alert.show();
	}

	/**
	 * Calls the first phone number associated with the contact.
	 * 
	 * @param contact_id
	 */
	private void callNumber(final long contact_id) {
		try {
			String number = mDbHelper.getContactNumber(contact_id);
			if (number != null) {
				Intent callIntent = new Intent(Intent.ACTION_CALL);
				callIntent.setData(Uri.parse("tel:" + number));
				startActivity(callIntent);
			}
		} catch (ActivityNotFoundException activityException) {
			Log.e("Calling a Phone Number", "Call failed", activityException);
		}
	}

	/**
	 * Prompts the user to select the house/group with which the contact is
	 * associated, and saves the result.
	 * 
	 * @param contact_id
	 */
	private void setHouse(final long contact_id) {
		AlertDialog.Builder alert;
		alert = new AlertDialog.Builder(TeamMemberList.this);

		final Spinner spinnerinput = new Spinner(TeamMemberList.this);
		Cursor c = mDbHelper.fetchAllHouses();
		String[] from = new String[] { DbAdapter.KEY_NAME };
		int[] to = new int[] { android.R.id.text1 };
		SimpleCursorAdapter adapter = new SimpleCursorAdapter(this,
				android.R.layout.simple_spinner_item, c, from, to);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinnerinput.setAdapter(adapter);

		alert.setView(spinnerinput);
		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int whichButton) {
				long house_id = spinnerinput.getSelectedItemId();
				mDbHelper.setHouse(contact_id, house_id);
				fillData();
			}
		});
		alert.setNegativeButton("Cancel", null);
		alert.show();
	}

	/**
	 * The custom adapter is needed simply to be able to display the team member
	 * along with his/her associated house. (In the future I hope to make that
	 * look better.)
	 */
	private class TeammemberAdapter extends ResourceCursorAdapter {

		/**
		 * Instantiates a new teammember adapter.
		 * 
		 * @param context
		 *            the context
		 * @param cur
		 *            the cur
		 */
		public TeammemberAdapter(Context context, Cursor cur) {
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
			TextView text = (TextView) view.findViewById(android.R.id.text1);
			String name = cur.getString(cur.getColumnIndex(DbAdapter.KEY_NAME));
			String house = cur.getString(cur
					.getColumnIndex(DbAdapter.KEY_LABEL));

			if (house == null) {
				text.setText(String.format("%s", name));
			} else {
				text.setText(String.format("%s (%s)", name, house));
			}
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

}
