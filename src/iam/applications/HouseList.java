package iam.applications;

import java.util.Date;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

/**
 * This <code>ListActivity</code> displays a checked list of houses/groups, with
 * an option to add a house/group. Unchecking the activity means that no call
 * around is expected from that house.
 */
public class HouseList extends ListActivity {

	/** The database interface. */
	private transient DbAdapter mDbHelper;

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(final Bundle bundle) {
		super.onCreate(bundle);

		setContentView(R.layout.house_list);

		getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

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
		mDbHelper.setCallaroundActive(itemId,
				getListView().isItemChecked(position));
	}

	/**
	 * Query the database and refresh the list.
	 */
	private void fillData() {
		final Cursor housesCur = mDbHelper.fetchAllHouses();
		startManagingCursor(housesCur);

		final String[] fromFields = new String[] { DbAdapter.Columns.NAME };
		final int[] toFields = new int[] { R.id.item };

		final SimpleCursorAdapter notes = new SimpleCursorAdapter(this,
				R.layout.checked_textview_item, housesCur, fromFields, toFields);
		setListAdapter(notes);

		housesCur.moveToFirst();
		for (int i = 0; i < housesCur.getCount(); i++) {
			getListView()
					.setItemChecked(
							i,
							housesCur.getLong(housesCur
									.getColumnIndex(DbAdapter.Columns.ACTIVE)) == 0 ? false
									: true);
			housesCur.moveToNext();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		final MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.houses_menu, menu);

		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		boolean retVal;
		if (item.getItemId() == R.id.newHouse) {
			newHouse();
			retVal = true;
		} else {
			retVal = super.onOptionsItemSelected(item);
		}
		return retVal;
		// switch (item.getItemId()) {
		// case R.id.newHouse:
		// newHouse();
		// return true;
		// default:
		// return super.onOptionsItemSelected(item);
		// }
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
		// just re-using this
		inflater.inflate(R.menu.houses_context, menu);
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

		boolean retVal;
		switch (item.getItemId()) {
		case R.id.call_guard:
			callGuard(info.id);
			retVal = true;
			break;
		case R.id.delete:
			deleteHouse(info.id);
			retVal = true;
			break;
		case R.id.edit:
			editHouse(info.id);
			retVal = true;
			break;
		case R.id.guard_today:
			editTodaysGuardSchedule(info.id);
			retVal = true;
			break;
		case R.id.guard_normal:
			editTypicalGuardSchedule(info.id);
			retVal = true;
			break;
		case R.id.request_guard_checkin:
			requestGuardCheckin(info.id);
			retVal = true;
			break;
		default:
			retVal = super.onContextItemSelected(item);
		}
		return retVal;
	}

	/**
	 * @param info
	 */
	private void requestGuardCheckin(final long id) {
		Date start = Time.todayAtPreferenceTime(this,
				Preferences.GUARD_CHECKIN_START, "22:00");
		Date end = Time.tomorrowAtPreferenceTime(this,
				Preferences.GUARD_CHECKIN_END, "06:00");
		Date now = new Date();

		if (now.after(start) && now.before(end)) {
			final long guardId = AlarmReceiver.requestGuardCheckin(this,
					mDbHelper, id);
			if (guardId == -1) {
				final Toast toast = Toast.makeText(this,
						getString(R.string.request_guard_checkin_error),
						Toast.LENGTH_LONG);
				toast.show();
			} else {
				final Intent intent = new Intent(this, GuardCheckinList.class);
				intent.putExtra(DbAdapter.Columns.ROWID, guardId);
				startActivity(intent);
			}
		} else {
			final Toast toast = Toast.makeText(this,
					getString(R.string.request_guard_checkin_untimely),
					Toast.LENGTH_LONG);
			toast.show();
		}
	}

	/**
	 * @param guardId
	 */
	private void editTypicalGuardSchedule(final long guardId) {
		final Intent intent = new Intent(this, GuardScheduleActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.putExtra(GuardScheduleActivity.SET_TYPICAL, true);
		intent.putExtra(DbAdapter.Columns.HOUSEID, guardId);
		startActivity(intent);
	}

	/**
	 * @param guardId
	 */
	private void editTodaysGuardSchedule(final long guardId) {
		final Intent intent = new Intent(this, GuardScheduleActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.putExtra(GuardScheduleActivity.SET_TYPICAL, false);
		intent.putExtra(DbAdapter.Columns.HOUSEID, guardId);
		startActivity(intent);
	}

	/**
	 * Deletes the house associated with the item
	 * 
	 * @param item
	 *            The clicked menu item.
	 */
	private void deleteHouse(final long item) {
		mDbHelper.deleteHouse(item);
		fillData();
	}

	/**
	 * Prompts the user to edit the name of the house associated with the menu
	 * item.
	 * 
	 * @param item
	 *            The clicked menu item.
	 */
	private void editHouse(final long item) {
		final AlertDialog.Builder alert = new AlertDialog.Builder(
				HouseList.this);

		// Set an EditText view to get user input
		final EditText input = new EditText(HouseList.this);
		input.setInputType(InputType.TYPE_CLASS_TEXT
				| InputType.TYPE_TEXT_FLAG_CAP_WORDS);
		input.setText(mDbHelper.getHouseName(item));
		alert.setView(input);

		alert.setPositiveButton(getString(R.string.ok),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(final DialogInterface dialog,
							final int whichButton) {
						final String value = input.getText().toString();
						if (value.length() > 0) {
							mDbHelper.setHouseName(item, value);
							fillData();
						}
					}
				});

		alert.setNegativeButton(getString(R.string.cancel), null);

		final AlertDialog dlg = alert.create();
		input.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(final View view, final boolean hasFocus) {
				if (hasFocus) {

					dlg.getWindow()
							.setSoftInputMode(
									WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
				}
			}
		});
		dlg.show();
	}

	/**
	 * Prompts the user to enter the name of a new house, and adds that house to
	 * the database.
	 */
	private void newHouse() {
		final AlertDialog.Builder alert = new AlertDialog.Builder(
				HouseList.this);

		// Set an EditText view to get user input
		final EditText input = new EditText(HouseList.this);
		input.setInputType(InputType.TYPE_CLASS_TEXT
				| InputType.TYPE_TEXT_FLAG_CAP_WORDS);

		alert.setView(input);

		alert.setPositiveButton(getString(R.string.ok),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(final DialogInterface dialog,
							final int whichButton) {
						final String value = input.getText().toString();
						if (value.length() > 0) {
							mDbHelper.addHouse(value);
							// this call will not create duplicates, so it's
							// convenient just to try to add them all
							mDbHelper.addCallarounds();
							fillData();
						}
					}
				});
		alert.setNegativeButton(getString(R.string.cancel), null);

		final AlertDialog dlg = alert.create();
		input.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(final View view, final boolean hasFocus) {
				if (hasFocus) {

					dlg.getWindow()
							.setSoftInputMode(
									WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
				}
			}
		});
		dlg.show();
	}

	/**
	 * Calls the number of the guard for the day.
	 * 
	 * @param house_id
	 *            The _id of the house for which to display numbers.
	 */
	private void callGuard(final long house_id) {
		final long guard_id = mDbHelper.getCurrentGuardForHouse(house_id);
		if (guard_id == -1) {
			final Toast toast = Toast.makeText(this,
					getString(R.string.request_guard_checkin_error),
					Toast.LENGTH_LONG);
			toast.show();
		} else {
			final String number = mDbHelper.getGuardNumber(guard_id);
			if (number.length() == 0) {
				final Toast toast = Toast.makeText(this,
						getString(R.string.guard_has_no_number),
						Toast.LENGTH_LONG);
				toast.show();
			} else {
				final Intent callIntent = new Intent(Intent.ACTION_CALL);
				callIntent.setData(Uri.parse("tel:" + number));
				startActivity(callIntent);
			}
		}
	}
}
