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
	private DbAdapter mDbHelper;

	private EditText mMessageBody;

	private Spinner mToWhom;

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mDbHelper = new DbAdapter(this);
		mDbHelper.open();

		setContentView(R.layout.broadcast_layout);

		mMessageBody = (EditText) findViewById(R.id.message);
		initializeMessageBody();

		mToWhom = (Spinner) findViewById(R.id.towhom);
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
				this, R.array.broadcast_options_array,
				android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mToWhom.setAdapter(adapter);

		Button broadcastButton = (Button) findViewById(R.id.broadcast);
		broadcastButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
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
		String forbidden = mDbHelper.getForbiddenLocations();
		if (forbidden != null) {
			String message = String.format(
					getString(R.string.broadcast_forbidden), forbidden);
			mMessageBody.setText(message);
		}
	}

	private void sendMessage() {
		String message = mMessageBody.getText().toString();
		String to = mToWhom.getSelectedItem().toString();

		Cursor c;

		if (to.equals(getString(R.string.broadcast_to_all))) {
			c = mDbHelper.fetchAllContactNumbers();
		} else if (to.equals(getString(R.string.broadcast_to_active))) {
			c = mDbHelper.fetchActiveContactNumbers();
		} else {
			return;
		}

		if (!c.moveToFirst()) {
			Toast.makeText(
					this,
					String.format(getString(R.string.broadcast_result),
							String.valueOf(0)), Toast.LENGTH_LONG).show();
			return;
		}
		long count = 0;
		do {
			String number = c.getString(0);
			SmsHandler.sendSms(this, number, message);
			count++;
		} while (c.moveToNext());
		Toast.makeText(
				this,
				String.format(getString(R.string.broadcast_result),
						String.valueOf(count)), Toast.LENGTH_LONG).show();
	}
}
