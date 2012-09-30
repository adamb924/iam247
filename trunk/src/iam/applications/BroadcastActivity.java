/**
 * 
 */
package iam.applications;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

/**
 * @author Adam
 * 
 */
public class BroadcastActivity extends Activity {

	/** The database interface. */
	private transient DbAdapter mDbHelper;

	private transient EditText mMessageBody;

	private transient Spinner mToWhom;

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

		mMessageBody = (EditText) findViewById(R.id.message);
		initializeMessageBody();

		mToWhom = (Spinner) findViewById(R.id.towhom);
		final ArrayAdapter<CharSequence> adapter = ArrayAdapter
				.createFromResource(this, R.array.broadcast_options_array,
						android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mToWhom.setAdapter(adapter);

		final Button broadcastButton = (Button) findViewById(R.id.broadcast);
		broadcastButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(final View view) {
				sendMessage();
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
		} else {
			return;
		}

		if (!cur.moveToFirst()) {
			Toast.makeText(
					this,
					String.format(getString(R.string.broadcast_result),
							String.valueOf(0)), Toast.LENGTH_LONG).show();
			return;
		}
		long count = 0;
		do {
			final String number = cur.getString(0);
			SmsHandler.sendSms(this, number, message);
			count++;
		} while (cur.moveToNext());
		Toast.makeText(
				this,
				String.format(getString(R.string.broadcast_result),
						String.valueOf(count)), Toast.LENGTH_LONG).show();
	}
}
