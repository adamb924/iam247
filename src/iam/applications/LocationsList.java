package iam.applications;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

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
		/*
		 * String[] from = new String[] { DbAdapter.KEY_LABEL }; int[] to = new
		 * int[] { R.id.item };
		 * 
		 * SimpleCursorAdapter notes = new SimpleCursorAdapter(this,
		 * R.layout.checked_textview_item, locationsCur, from, to);
		 * setListAdapter(notes);
		 */
		LocationItemAdapter adapter = new LocationItemAdapter(this,
				locationsCur);
		setListAdapter(adapter);

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
		case R.id.location_edit_keyword:
			final AdapterContextMenuInfo info3 = (AdapterContextMenuInfo) item
					.getMenuInfo();
			AlertDialog.Builder alert2 = new AlertDialog.Builder(
					LocationsList.this);
			final EditText input2 = new EditText(LocationsList.this);
			input2.setText(mDbHelper.getLocationName(info3.id));
			alert2.setView(input2);
			alert2.setPositiveButton("Ok",
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog,
								int whichButton) {
							String value = input2.getText().toString();
							if (value.length() > 0) {
								mDbHelper.setLocationKeyword(info3.id, value);
								fillData();
							}
						}
					});
			alert2.setNegativeButton("Cancel", null);
			alert2.show();
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
		final EditText input = new EditText(LocationsList.this);
		alert.setView(input);
		alert.setTitle(R.string.name);
		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int whichButton) {
				String value = input.getText().toString();
				if (value.length() > 0) {
					final String label = value;

					AlertDialog.Builder alert2 = new AlertDialog.Builder(
							LocationsList.this);
					final EditText input2 = new EditText(LocationsList.this);
					alert2.setView(input2);
					alert2.setTitle(R.string.keyword);
					alert2.setPositiveButton("Ok",
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int whichButton) {
									String value = input2.getText().toString();
									if (value.length() > 0) {
										mDbHelper.addLocation(label, true,
												value);
										fillData();
									}
								}
							});
					alert2.setNegativeButton("Cancel", null);
					alert2.show();
				}
			}
		});
		alert.setNegativeButton("Cancel", null);
		alert.show();
	}

	/**
	 * The custom adapter is needed to format the time strings.
	 */
	private class LocationItemAdapter extends ResourceCursorAdapter {

		/**
		 * Instantiates a new adapter for formatting location items.
		 * 
		 * @param context
		 *            the context
		 * @param cur
		 *            the cur
		 */
		public LocationItemAdapter(Context context, Cursor cur) {
			super(context, R.layout.checked_textview_item, cur);
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
			return li.inflate(R.layout.checked_textview_item, parent, false);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.widget.CursorAdapter#bindView(android.view.View,
		 * android.content.Context, android.database.Cursor)
		 */
		@Override
		public void bindView(View view, Context context, Cursor cur) {
			String label = cur.getString(cur
					.getColumnIndex(DbAdapter.KEY_LABEL));
			String keyword = cur.getString(cur
					.getColumnIndex(DbAdapter.KEY_KEYWORD));

			((TextView) view.findViewById(R.id.item)).setText(String.format(
					context.getString(R.string.house_label_keyword), label,
					keyword));
		}
	}
}