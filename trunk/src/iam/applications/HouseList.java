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
	private DbAdapter mDbHelper;

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

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
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		mDbHelper
				.setCallaroundActive(id, getListView().isItemChecked(position));
	}

	/**
	 * Query the database and refresh the list.
	 */
	private void fillData() {
		Cursor housesCur = mDbHelper.fetchAllHouses();
		startManagingCursor(housesCur);

		String[] from = new String[] { DbAdapter.KEY_NAME };
		int[] to = new int[] { R.id.item };

		SimpleCursorAdapter notes = new SimpleCursorAdapter(this,
				R.layout.checked_textview_item, housesCur, from, to);
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
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.houses_menu, menu);

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
		case R.id.newHouse:
			newHouse();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
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
		// just re-using this
		inflater.inflate(R.menu.houses_context, menu);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onContextItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onContextItemSelected(MenuItem item) {
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
	 * @param id
	 */
	private void editTypicalGuardSchedule(long id) {
		Intent i = new Intent(this, GuardScheduleActivity.class);
		i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		i.putExtra(GuardScheduleActivity.SET_DEFAULT, true);
		i.putExtra(DbAdapter.KEY_HOUSEID, id);
		startActivity(i);
	}

	/**
	 * @param id
	 */
	private void editTodaysGuardSchedule(long id) {
		Intent i = new Intent(this, GuardScheduleActivity.class);
		i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		i.putExtra(GuardScheduleActivity.SET_DEFAULT, false);
		i.putExtra(DbAdapter.KEY_HOUSEID, id);
		startActivity(i);
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
		AlertDialog.Builder alert = new AlertDialog.Builder(HouseList.this);

		// Set an EditText view to get user input
		final EditText input = new EditText(HouseList.this);
		input.setText(mDbHelper.getHouseName(item));
		alert.setView(input);

		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int whichButton) {
				String value = input.getText().toString();
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
		AlertDialog.Builder alert = new AlertDialog.Builder(HouseList.this);

		// Set an EditText view to get user input
		final EditText input = new EditText(HouseList.this);
		alert.setView(input);

		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int whichButton) {
				String value = input.getText().toString();
				if (value.length() > 0) {
					mDbHelper.addHouse(value);
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
	private void callGuard(long house_id) {
		String number = mDbHelper.getGuardNumberFromDate(house_id,
				Time.iso8601Date());

		Intent callIntent = new Intent(Intent.ACTION_CALL);
		callIntent.setData(Uri.parse("tel:" + number));
		startActivity(callIntent);
	}
}
