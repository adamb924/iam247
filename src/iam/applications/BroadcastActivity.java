/**
 * 
 */
package iam.applications;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.TextView;

/**
 * @author Adam
 * 
 */
public class BroadcastActivity extends Activity {

	/** The database interface. */
	private transient DbAdapter mDbHelper;

	private transient EditText mMessageBody;

	private transient Spinner mToWhom;
	private Spinner mSmsSpinner;

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(final Bundle bundle) {
		super.onCreate(bundle);

		mDbHelper = new DbAdapter(this);
		mDbHelper.open();

		setContentView(R.layout.broadcast_layout);


		mToWhom = (Spinner) findViewById(R.id.towhom);
		final ArrayAdapter<CharSequence> adapter = ArrayAdapter
				.createFromResource(this, R.array.broadcast_options_array,
						android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mToWhom.setAdapter(adapter);

		initializeSmsSpinner();

		mMessageBody = (EditText) findViewById(R.id.message);
		initializeMessageBody();

		final Button broadcastButton = (Button) findViewById(R.id.broadcast);
		broadcastButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(final View view) {
				sendMessage();
			}
		});
	}

	private void initializeSmsSpinner() {
		mSmsSpinner = (Spinner) findViewById(R.id.sms_spinner);
/*
		final Cursor smsCursor = mDbHelper.fetchAllHouses();
		startManagingCursor(smsCursor);
		final String[] fromFields = new String[] { DbAdapter.Columns.NAME };
		final int[] toFields = new int[] { android.R.id.text1 };

		final SimpleCursorAdapter smsAdapter = new SimpleCursorAdapter(this,
				android.R.layout.simple_spinner_item, smsCursor, fromFields, toFields);
		smsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mSmsSpinner.setAdapter(smsAdapter);
*/
		/// http://stackoverflow.com/questions/848728/how-can-i-read-sms-messages-from-the-inbox-programmatically-in-android
		Cursor smsCursor = getContentResolver().query(Uri.parse("content://sms/inbox"), null, null, null, null);
		final String[] fromFields = new String[] { "body" };
		final int[] toFields = new int[] { android.R.id.text1 };

		final SimpleCursorAdapter smsAdapter = new SimpleCursorAdapter(this,
				android.R.layout.simple_spinner_item, smsCursor, fromFields, toFields);

		smsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mSmsSpinner.setAdapter(smsAdapter);

		// http://stackoverflow.com/questions/1337424/android-spinner-get-the-selected-item-change-event
		mSmsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
				Cursor cursor = (Cursor) smsAdapter.getItem(position);
				mMessageBody.setText( cursor.getString( cursor.getColumnIndex( "body") ) , TextView.BufferType.EDITABLE);
			}

			@Override
			public void onNothingSelected(AdapterView<?> parentView) {
			}
		});
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

	private void initializeMessageBody() {
		final String forbidden = mDbHelper.getForbiddenLocations();
		if (forbidden != null) {
			final String message = String.format(
					getString(R.string.broadcast_forbidden), forbidden);
			mMessageBody.setText(message);
		}
	}

	private void sendMessage() {
		final String message = mMessageBody.getText().toString();
		final String toWhom = mToWhom.getSelectedItem().toString();

		Cursor cur;

		if (toWhom.equals(getString(R.string.broadcast_to_all))) {
			cur = mDbHelper.fetchAllContactNumbers();
		} else if (toWhom.equals(getString(R.string.broadcast_to_active))) {
			cur = mDbHelper.fetchActiveContactNumbers();
		} else if (toWhom.equals(getString(R.string.broadcast_to_out))) {
			cur = mDbHelper.fetchCheckedOutNumbers();
		} else {
			// just to have some sensible default behavior
			cur = mDbHelper.fetchAllContactNumbers();
		}
		startManagingCursor(cur);

		if (cur.moveToFirst()) {
			long count = 0;
			do {
				final String number = cur.getString(cur
						.getColumnIndex(DbAdapter.Columns.NUMBER));
				SmsHandler.sendSms(this, number, message);
				count++;
			} while (cur.moveToNext());
			Toast.makeText(
					this,
					String.format(getString(R.string.broadcast_result),
							String.valueOf(count)), Toast.LENGTH_LONG).show();
		} else {
			Toast.makeText(
					this,
					String.format(getString(R.string.broadcast_result),
							String.valueOf(0)), Toast.LENGTH_LONG).show();
			return;
		}
	}
}
