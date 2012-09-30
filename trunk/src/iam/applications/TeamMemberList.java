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
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;

/**
 * This Activity displays two lists, one of checked-in team members and one of
 * checked-out team members. Long-clicking on the team member produces various
 * options.
 */
public class TeamMemberList extends Activity {

	/** The database interface. */
	private transient DbAdapter mDbHelper;

	/** The list of checked-in people. */
	private transient ListView mCheckedIn;

	/** The list of checked-out people. */
	private transient ListView mCheckedOut;

	/** An intent filter to catch all broadcast refresh requests. */
	private transient IntentFilter mIntentFilter;

	private transient long mContactId;

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(final Bundle bundle) {
		super.onCreate(bundle);

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
		final String[] fromFields = new String[] { DbAdapter.KEY_NAME,
				DbAdapter.KEY_LABEL };
		final int[] toFields = new int[] { R.id.name, R.id.house };

		final Cursor checkedoutCur = mDbHelper.fetchCheckedOutPeople();
		startManagingCursor(checkedoutCur);

		if (checkedoutCur.getCount() > 0) {
			final SimpleCursorAdapter notes = new SimpleCursorAdapter(this,
					R.layout.teammember_item, checkedoutCur, fromFields, toFields);
			mCheckedOut.setAdapter(notes);
		} else {
			final ViewGroup viewGroup = (ViewGroup) findViewById(R.id.teammember_layout);
			viewGroup.removeView(mCheckedOut);
			viewGroup.removeView(findViewById(R.id.checkedout_label));
		}

		final Cursor checkedinCur = mDbHelper.fetchCheckedInPeople();
		startManagingCursor(checkedinCur);

		final SimpleCursorAdapter notes = new SimpleCursorAdapter(this,
				R.layout.teammember_item, checkedinCur, fromFields, toFields);
		mCheckedIn.setAdapter(notes);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreateContextMenu(android.view.ContextMenu,
	 * android.view.View, android.view.ContextMenu.ContextMenuInfo)
	 */
	@Override
	public void onCreateContextMenu(final ContextMenu menu, final View view,
			final ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, view, menuInfo);
		final MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.teammember_menu, menu);

		final AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
		final long contact_id = info.id;
		final long house_id = mDbHelper.getHouseId(contact_id);

		if (!mDbHelper.getContactHasCheckinOutstanding(contact_id)) {
			menu.removeItem(R.id.resolve_checkin);
		}

		if (!mDbHelper.getCallaroundActive(house_id) || house_id == -1) {
			menu.removeItem(R.id.callaround_resolved);
		} else {
			final MenuItem caResolved = menu
					.findItem(R.id.callaround_resolved);
			caResolved.setCheckable(true);
			caResolved.setChecked(!mDbHelper
					.getCallaroundOutstanding(house_id));
		}

		final MenuItem callaroundEnabled = menu
				.findItem(R.id.callaround_enabled);
		callaroundEnabled.setCheckable(true);
		callaroundEnabled.setChecked(mDbHelper.getCallaroundActive(house_id));

		final MenuItem reportsEnabled = menu
				.findItem(R.id.allow_request_reports);
		reportsEnabled.setCheckable(true);
		reportsEnabled.setChecked(mDbHelper.getContactPermission(contact_id,
				DbAdapter.USER_PERMISSION_REPORT));

		final MenuItem remindersEnabled = menu
				.findItem(R.id.checkin_reminders);
		remindersEnabled.setCheckable(true);
		remindersEnabled.setChecked(mDbHelper.getContactPreference(
				contact_id, DbAdapter.USER_PREFERENCE_CHECKIN_REMINDER));

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onContextItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onContextItemSelected(final MenuItem item) {
		final AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
				.getMenuInfo();
		mContactId = info == null ? mContactId : info.id;

		final long house_id = mDbHelper.getHouseId(mContactId);
		boolean newValue = false;

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
			newValue ^= item.isChecked();
			item.setChecked(newValue);
			mDbHelper.setContactPermission(mContactId,
					DbAdapter.USER_PERMISSION_REPORT, newValue);
			return true;
		case R.id.callaround_enabled:
			newValue ^= item.isChecked();
			item.setChecked(newValue);
			mDbHelper.setCallaroundActive(house_id, newValue);
			return true;
		case R.id.callaround_resolved:
			newValue ^= item.isChecked();
			item.setChecked(newValue);
			mDbHelper.setCallaroundResolved(house_id, newValue);
			return true;
		case R.id.checkin_reminders:
			newValue ^= item.isChecked();
			item.setChecked(newValue);
			mDbHelper.setContactPreference(mContactId,
					DbAdapter.USER_PREFERENCE_CHECKIN_REMINDER, newValue);
			return true;
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
		final AlertDialog.Builder alert = new AlertDialog.Builder(
				TeamMemberList.this);
		alert.setTitle(R.string.block_number);
		alert.setMessage(R.string.block_number_warning);
		alert.setPositiveButton(getString(R.string.ok),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(final DialogInterface dialog,
							final int whichButton) {
						mDbHelper.setNumberIsBlocked(
								mDbHelper.getContactNumber(contact_id), true);
					}
				});
		alert.setNegativeButton(getString(R.string.cancel), null);
		alert.show();
	}

	/**
	 * Prompts the user to edit the contact's email address, and saves the
	 * result.
	 * 
	 * @param contact_id
	 */
	private void editEmail(final long contact_id) {
		final EditText editinput = new EditText(TeamMemberList.this);
		editinput.setText(mDbHelper.getContactEmail(contact_id));

		final AlertDialog.Builder alert = new AlertDialog.Builder(
				TeamMemberList.this);
		alert.setView(editinput);
		alert.setPositiveButton(getString(R.string.ok),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(final DialogInterface dialog,
							final int whichButton) {
						final String value = editinput.getText().toString();
						if (value.length() > 0) {
							mDbHelper.setContactEmail(contact_id, value);
							fillData();
						}
					}
				});
		alert.setNegativeButton(getString(R.string.cancel), null);
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
		final EditText editinput = new EditText(TeamMemberList.this);
		editinput.setText(mDbHelper.getContactNumber(contact_id));

		final AlertDialog.Builder alert = new AlertDialog.Builder(
				TeamMemberList.this);
		alert.setView(editinput);
		alert.setPositiveButton(getString(R.string.ok),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(final DialogInterface dialog,
							final int whichButton) {
						final String value = editinput.getText().toString();
						if (value.length() > 0) {
							mDbHelper.setContactPhone(contact_id, value);
							fillData();
						}
					}
				});
		alert.setNegativeButton(getString(R.string.cancel), null);
		alert.show();
	}

	/**
	 * Prompts the user to edit the contact's email address, and saves the
	 * result.
	 * 
	 * @param contact_id
	 */
	private void editName(final long contact_id) {
		final EditText editinput = new EditText(TeamMemberList.this);
		editinput.setText(mDbHelper.getContactName(contact_id));

		final AlertDialog.Builder alert = new AlertDialog.Builder(
				TeamMemberList.this);
		alert.setView(editinput);
		alert.setPositiveButton(getString(R.string.ok),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(final DialogInterface dialog,
							final int whichButton) {
						final String value = editinput.getText().toString();
						if (value.length() > 0) {
							mDbHelper.setContactName(contact_id, value);
							fillData();
						}
					}
				});
		alert.setNegativeButton(getString(R.string.cancel), null);
		alert.show();
	}

	/**
	 * Calls the first phone number associated with the contact.
	 * 
	 * @param contact_id
	 */
	private void callNumber(final long contact_id) {
		try {
			final String number = mDbHelper.getContactNumber(contact_id);
			if (number != null) {
				final Intent callIntent = new Intent(Intent.ACTION_CALL);
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
		final Cursor cur = mDbHelper.fetchAllHouses();
		final String[] fromFields = new String[] { DbAdapter.KEY_NAME };
		final int[] toFields = new int[] { android.R.id.text1 };
		final SimpleCursorAdapter adapter = new SimpleCursorAdapter(this,
				android.R.layout.simple_spinner_item, cur, fromFields, toFields);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinnerinput.setAdapter(adapter);

		alert.setView(spinnerinput);
		alert.setPositiveButton(getString(R.string.ok),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(final DialogInterface dialog,
							final int whichButton) {
						final long house_id = spinnerinput.getSelectedItemId();
						mDbHelper.setHouse(contact_id, house_id);
						fillData();
					}
				});
		alert.setNegativeButton(getString(R.string.cancel), null);
		alert.show();
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

}
