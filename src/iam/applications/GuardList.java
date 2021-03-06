package iam.applications;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
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

/**
 * 
 */
public class GuardList extends ListActivity {

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

		setContentView(R.layout.guard_list);

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

		final Intent intent = new Intent(this, GuardCheckinList.class);
		intent.putExtra(DbAdapter.Columns.ROWID, itemId);
		startActivity(intent);
	}

	/**
	 * Query the database and refresh the list.
	 */
	private void fillData() {
		final Cursor cur = mDbHelper.fetchAllGuards();
		startManagingCursor(cur);

		final String[] fromFields = new String[] { DbAdapter.Columns.NAME,
				DbAdapter.Columns.NUMBER };
		final int[] toFields = new int[] { R.id.text1, R.id.text2 };

		final SimpleCursorAdapter notes = new SimpleCursorAdapter(this,
				R.layout.twolinelistitem, cur, fromFields, toFields);
		setListAdapter(notes);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		final MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.guard_options, menu);
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
		if (item.getItemId() == R.id.newGuard) {
			newGuard();
			retVal = true;
		} else {
			retVal = super.onOptionsItemSelected(item);
		}
		return retVal;

		// switch (item.getItemId()) {
		// case R.id.newGuard:
		// newGuard();
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
		inflater.inflate(R.menu.guard_context, menu);
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
		final long guardId = info == null ? -1 : info.id;

		boolean retVal;

		switch (item.getItemId()) {
		case R.id.call_number:
			callNumber(guardId);
			retVal = true;
			break;
		case R.id.edit_name:
			editName(guardId);
			retVal = true;
			break;
		case R.id.edit_phone:
			editPhone(guardId);
			retVal = true;
			break;
		case R.id.delete:
			mDbHelper.deleteGuard(guardId);
			fillData();
			retVal = true;
			break;
		default:
			retVal = super.onContextItemSelected(item);
		}
		return retVal;
	}

	/**
	 * Prompts the user to edit the guard's name, and saves the result.
	 * 
	 * @param guard_id
	 */
	private void editName(final long guard_id) {
		final EditText editinput = new EditText(GuardList.this);
		editinput.setText(mDbHelper.getGuardName(guard_id));
		editinput.setInputType(InputType.TYPE_CLASS_TEXT
				| InputType.TYPE_TEXT_FLAG_CAP_WORDS);

		AlertDialog.Builder alert;
		alert = new AlertDialog.Builder(GuardList.this);
		alert.setTitle(getString(R.string.name));
		alert.setView(editinput);
		alert.setPositiveButton(getString(R.string.ok),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(final DialogInterface dialog,
							final int whichButton) {
						final String value = editinput.getText().toString();
						if (value.length() > 0) {
							mDbHelper.setGuardName(guard_id, value);
							fillData();
						}
					}
				});
		alert.setNegativeButton(getString(R.string.cancel), null);
		alert.show();
	}

	/**
	 * Prompts the user to edit the guard's phone number, and saves the result.
	 * 
	 * @param guard_id
	 */
	private void editPhone(final long guard_id) {
		AlertDialog.Builder alert;
		final EditText editinput = new EditText(GuardList.this);
		editinput.setText(mDbHelper.getContactNumber(guard_id));
		editinput.setInputType(InputType.TYPE_CLASS_PHONE);

		alert = new AlertDialog.Builder(GuardList.this);
		alert.setTitle(getString(R.string.number));
		alert.setView(editinput);
		alert.setPositiveButton(getString(R.string.ok),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(final DialogInterface dialog,
							final int whichButton) {
						final String value = editinput.getText().toString();
						if (value.length() > 0) {
							mDbHelper.setGuardNumber(guard_id, SmsHandler
									.getNormalizedPhoneNumber(GuardList.this,
											value));
							fillData();
						}
					}
				});
		alert.setNegativeButton(getString(R.string.cancel), null);

		final AlertDialog dlg = alert.create();
		editinput.setOnFocusChangeListener(new View.OnFocusChangeListener() {
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
	 * Prompts the user to enter the name of a new guard, and adds that guard to
	 * the database.
	 */
	private void newGuard() {
		AlertDialog.Builder alert;
		final EditText editinput = new EditText(GuardList.this);
		editinput.setInputType(InputType.TYPE_CLASS_TEXT
				| InputType.TYPE_TEXT_FLAG_CAP_WORDS);

		alert = new AlertDialog.Builder(GuardList.this);
		alert.setView(editinput);
		alert.setTitle(getString(R.string.name));
		alert.setPositiveButton(getString(R.string.ok),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(final DialogInterface dialog,
							final int whichButton) {
						final String value = editinput.getText().toString();
						if (value.length() > 0) {
							mDbHelper.addGuard(value);
							final long lastId = mDbHelper.lastInsertId();
							editPhone(lastId);
							fillData();
						}
					}
				});
		alert.setNegativeButton(getString(R.string.cancel), null);

		final AlertDialog dlg = alert.create();
		editinput.setOnFocusChangeListener(new View.OnFocusChangeListener() {
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
	 * Calls the first phone number associated with the guard.
	 * 
	 * @param guard_id
	 */
	private void callNumber(final long guard_id) {
		try {
			final String number = mDbHelper.getGuardNumber(guard_id);
			if (number != null) {
				final Intent callIntent = new Intent(Intent.ACTION_CALL);
				callIntent.setData(Uri.parse("tel:" + number));
				startActivity(callIntent);
			}
		} catch (ActivityNotFoundException activityException) {
			Log.e("Calling a Phone Number", "Call failed", activityException);
		}
	}
}
