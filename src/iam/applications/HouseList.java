package iam.applications;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

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
		mDbHelper
				.setCallaroundActive(itemId, getListView().isItemChecked(position));
	}

	/**
	 * Query the database and refresh the list.
	 */
	private void fillData() {
		final Cursor housesCur = mDbHelper.fetchAllHouses();
		startManagingCursor(housesCur);

		final String[] fromFields = new String[] { DbAdapter.KEY_NAME };
		final int[] toFields = new int[] { R.id.item };

		final SimpleCursorAdapter notes = new SimpleCursorAdapter(this,
				R.layout.checked_textview_item, housesCur, fromFields, toFields);
		setListAdapter(notes);

		housesCur.moveToFirst();
		for (int i = 0; i < housesCur.getCount(); i++) {
			getListView().setItemChecked(
					i,
					housesCur.getLong(housesCur
							.getColumnIndex(DbAdapter.KEY_ACTIVE)) == 0 ? false
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
		if (item.getItemId() == R.id.newHouse) {
			newHouse();
			return true;
		}
		return super.onOptionsItemSelected(item);
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

		switch (item.getItemId()) {
		case R.id.call_guard:
			callGuard(info.id);
			return true;
		case R.id.delete:
			deleteHouse(info.id);
			return true;
		case R.id.edit:
			editHouse(info.id);
			return true;
		case R.id.guard_today:
			editTodaysGuardSchedule(info.id);
			return true;
		case R.id.guard_normal:
			editTypicalGuardSchedule(info.id);
			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}

	/**
	 * @param guardId
	 */
	private void editTypicalGuardSchedule(final long guardId) {
		final Intent intent = new Intent(this, GuardScheduleActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.putExtra(GuardScheduleActivity.SET_DEFAULT, true);
		intent.putExtra(DbAdapter.KEY_HOUSEID, guardId);
		startActivity(intent);
	}

	/**
	 * @param guardId
	 */
	private void editTodaysGuardSchedule(final long guardId) {
		final Intent intent = new Intent(this, GuardScheduleActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.putExtra(GuardScheduleActivity.SET_DEFAULT, false);
		intent.putExtra(DbAdapter.KEY_HOUSEID, guardId);
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
		input.setText(mDbHelper.getHouseName(item));
		alert.setView(input);

		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
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

		alert.setNegativeButton("Cancel", null);
		alert.show();
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
		alert.setView(input);

		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(final DialogInterface dialog,
					final int whichButton) {
				final String value = input.getText().toString();
				if (value.length() > 0) {
					mDbHelper.addHouse(value);
					// this call will not create duplicates, so it's convenient
					// just to try to add them all
					mDbHelper.addCallarounds();
					fillData();
				}
			}
		});
		alert.setNegativeButton("Cancel", null);
		alert.show();
	}

	/**
	 * Calls the number of the guard for the day.
	 * 
	 * @param house_id
	 *            The _id of the house for which to display numbers.
	 */
	private void callGuard(final long house_id) {
		final String number = mDbHelper.getGuardNumberFromDate(house_id,
				Time.iso8601Date());

		final Intent callIntent = new Intent(Intent.ACTION_CALL);
		callIntent.setData(Uri.parse("tel:" + number));
		startActivity(callIntent);
	}
}
