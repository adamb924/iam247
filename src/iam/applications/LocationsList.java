package iam.applications;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.text.InputType;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
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
	private transient DbAdapter mDbHelper;

	/**
	 * Called when the activity is first created.
	 * 
	 * @param bundle
	 *            the saved instance state
	 */
	@Override
	public void onCreate(final Bundle bundle) {
		super.onCreate(bundle);

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
	protected void onListItemClick(final ListView listView, final View view,
			final int position, final long itemId) {
		super.onListItemClick(listView, view, position, itemId);
		mDbHelper.setLocationAllowed(itemId,
				getListView().isItemChecked(position));
	}

	/**
	 * Query the database and refresh the list of locations.
	 */
	private void fillData() {
		final Cursor locationsCur = mDbHelper.fetchAllLocations();
		startManagingCursor(locationsCur);
		/*
		 * String[] from = new String[] { DbAdapter.Columns.LABEL }; int[] to =
		 * new int[] { R.id.item };
		 * 
		 * SimpleCursorAdapter notes = new SimpleCursorAdapter(this,
		 * R.layout.checked_textview_item, locationsCur, from, to);
		 * setListAdapter(notes);
		 */
		final LocationItemAdapter adapter = new LocationItemAdapter(this,
				locationsCur);
		setListAdapter(adapter);

		locationsCur.moveToFirst();
		for (int i = 0; i < locationsCur.getCount(); i++) {
			getListView()
					.setItemChecked(
							i,
							locationsCur.getLong(locationsCur
									.getColumnIndex(DbAdapter.Columns.ALLOWED)) == 0 ? false
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
	public boolean onCreateOptionsMenu(final Menu menu) {
		final MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.location_menu, menu);
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
		if (item.getItemId() == R.id.newlocation) {
			newLocation();
			retVal = true;
		} else {
			retVal = super.onOptionsItemSelected(item);
		}
		return retVal;
		// switch (item.getItemId()) {
		// case R.id.newlocation:
		// newLocation();
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
		inflater.inflate(R.menu.location_context, menu);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onContextItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onContextItemSelected(final MenuItem item) {
		AdapterContextMenuInfo info;
		boolean retVal;
		switch (item.getItemId()) {
		case R.id.location_delete:
			info = (AdapterContextMenuInfo) item.getMenuInfo();
			mDbHelper.deleteLocation(info.id);
			fillData();
			retVal = true;
			break;
		case R.id.location_edit:
			editLocationLabel(item);
			retVal = true;
			break;
		case R.id.location_edit_keyword:
			editLocationKeyword(item);
			retVal = true;
			break;
		default:
			retVal = super.onContextItemSelected(item);
		}
		return retVal;
	}

	/**
	 * Prompt the user to edit the location's keyword.
	 * 
	 * @param item
	 */
	private void editLocationKeyword(final MenuItem item) {
		final AdapterContextMenuInfo info3 = (AdapterContextMenuInfo) item
				.getMenuInfo();
		final AlertDialog.Builder alert = new AlertDialog.Builder(
				LocationsList.this);
		final EditText input = new EditText(LocationsList.this);
		input.setInputType(InputType.TYPE_CLASS_TEXT
				| InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
		input.setText(mDbHelper.getLocationKeyword(info3.id));

		alert.setView(input);
		alert.setPositiveButton(getString(R.string.ok),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(final DialogInterface dialog,
							final int whichButton) {
						final String value = input.getText().toString();
						if (value.length() > 0) {
							mDbHelper.setLocationKeyword(info3.id, value);
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
	 * Prompt the user to edit the location's label.
	 * 
	 * @param item
	 */
	private void editLocationLabel(final MenuItem item) {
		final AdapterContextMenuInfo info2 = (AdapterContextMenuInfo) item
				.getMenuInfo();
		final AlertDialog.Builder alert = new AlertDialog.Builder(
				LocationsList.this);
		final EditText input = new EditText(LocationsList.this);
		input.setInputType(InputType.TYPE_CLASS_TEXT
				| InputType.TYPE_TEXT_FLAG_CAP_WORDS);
		input.setText(mDbHelper.getLocationName(info2.id));
		alert.setView(input);
		alert.setPositiveButton(getString(R.string.ok),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(final DialogInterface dialog,
							final int whichButton) {
						final String value = input.getText().toString();
						if (value.length() > 0) {
							mDbHelper.setLocationName(info2.id, value);
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
	 * Prompts the user to enter the name of a new location, and adds that
	 * location to the database.
	 */
	private void newLocation() {
		final AlertDialog.Builder alert = new AlertDialog.Builder(
				LocationsList.this);
		final EditText input = new EditText(LocationsList.this);
		input.setInputType(InputType.TYPE_CLASS_TEXT
				| InputType.TYPE_TEXT_FLAG_CAP_WORDS);

		alert.setView(input);
		alert.setTitle(R.string.name);
		alert.setPositiveButton(getString(R.string.ok),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(final DialogInterface dialog,
							final int whichButton) {
						final String value = input.getText().toString();
						if (value.length() > 0) {
							final String label = value;

							final AlertDialog.Builder alert2 = new AlertDialog.Builder(
									LocationsList.this);
							final EditText input2 = new EditText(
									LocationsList.this);
							input2.setInputType(InputType.TYPE_CLASS_TEXT
									| InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);

							alert2.setView(input2);
							alert2.setTitle(R.string.keyword);
							alert2.setPositiveButton(getString(R.string.ok),
									new DialogInterface.OnClickListener() {
										@Override
										public void onClick(
												final DialogInterface dialog,
												final int whichButton) {
											final String value = input2
													.getText().toString();
											if (value.length() > 0) {
												mDbHelper.addLocation(label,
														true, value);
												fillData();
											}
										}
									});
							alert2.setNegativeButton(
									getString(R.string.cancel), null);

							final AlertDialog dlg = alert2.create();
							input2.setOnFocusChangeListener(new View.OnFocusChangeListener() {
								@Override
								public void onFocusChange(final View view,
										final boolean hasFocus) {
									if (hasFocus) {

										dlg.getWindow()
												.setSoftInputMode(
														WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
									}
								}
							});
							dlg.show();
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
		public LocationItemAdapter(final Context context, final Cursor cur) {
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
		public View newView(final Context context, final Cursor cur,
				final ViewGroup parent) {
			final LayoutInflater inflator = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			return inflator.inflate(R.layout.checked_textview_item, parent,
					false);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.widget.CursorAdapter#bindView(android.view.View,
		 * android.content.Context, android.database.Cursor)
		 */
		@Override
		public void bindView(final View view, final Context context,
				final Cursor cur) {
			final String label = cur.getString(cur
					.getColumnIndex(DbAdapter.Columns.LABEL));
			final String keyword = cur.getString(cur
					.getColumnIndex(DbAdapter.Columns.KEYWORD));

			((TextView) view.findViewById(R.id.item)).setText(String.format(
					context.getString(R.string.house_label_keyword), label,
					keyword));
		}
	}
}