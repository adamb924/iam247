package iam.applications;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.database.Cursor;
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
 * This <code>ListActivity</code> produces a list of locations in the database.
 * A checked location is one to which travel is permitted.
 */
public class LocationsList extends ListActivity {

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

		setContentView(R.layout.location_list);

		getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

		mDbHelper = new DbAdapter(this);
		mDbHelper.open();

		// Button addButton = (Button) findViewById(R.id.newlocation);
		// addButton.setOnClickListener(new View.OnClickListener() {
		// @Override
		// public void onClick(View view) {
		// }
		// });

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
		mDbHelper.setLocationAllowed(id, getListView().isItemChecked(position));
	}

	/**
	 * Query the database and refresh the list of locations.
	 */
	private void fillData() {
		Cursor locationsCur = mDbHelper.fetchAllLocations();
		startManagingCursor(locationsCur);

		String[] from = new String[] { DbAdapter.KEY_LABEL };
		int[] to = new int[] { R.id.item };

		SimpleCursorAdapter notes = new SimpleCursorAdapter(this,
				R.layout.checked_textview_item, locationsCur, from, to);
		setListAdapter(notes);

		locationsCur.moveToFirst();
		for (int i = 0; i < locationsCur.getCount(); i++) {
			getListView()
					.setItemChecked(
							i,
							locationsCur.getLong(locationsCur
									.getColumnIndex(DbAdapter.KEY_ALLOWED)) == 0 ? false
									: true);
			locationsCur.moveToNext();
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
		inflater.inflate(R.menu.location_menu, menu);
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
		case R.id.newlocation:
			newLocation();
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
		inflater.inflate(R.menu.location_context, menu);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onContextItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info;
		switch (item.getItemId()) {
		case R.id.location_delete:
			info = (AdapterContextMenuInfo) item.getMenuInfo();
			mDbHelper.deleteLocation(info.id);
			fillData();
			return true;
		case R.id.location_edit:
			final AdapterContextMenuInfo info2 = (AdapterContextMenuInfo) item
					.getMenuInfo();
			AlertDialog.Builder alert = new AlertDialog.Builder(
					LocationsList.this);
			final EditText input = new EditText(LocationsList.this);
			input.setText(mDbHelper.getLocationName(info2.id));
			alert.setView(input);
			alert.setPositiveButton("Ok",
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog,
								int whichButton) {
							String value = input.getText().toString();
							if (value.length() > 0) {
								mDbHelper.setLocationName(info2.id, value);
								fillData();
							}
						}
					});
			alert.setNegativeButton("Cancel", null);
			alert.show();
			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}

	/**
	 * Prompts the user to enter the name of a new location, and adds that
	 * location to the database.
	 */
	private void newLocation() {
		AlertDialog.Builder alert = new AlertDialog.Builder(LocationsList.this);

		// Set an EditText view to get user input
		final EditText input = new EditText(LocationsList.this);
		alert.setView(input);

		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int whichButton) {
				String value = input.getText().toString();
				if (value.length() > 0) {
					mDbHelper.addLocation(value, true);
					fillData();
				}
			}
		});

		alert.setNegativeButton("Cancel", null);

		alert.show();
	}
}