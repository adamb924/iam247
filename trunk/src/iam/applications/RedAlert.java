package iam.applications;

import java.util.Date;
import java.util.Locale;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

/**
 * This <code>Activity</code> produces the "Red Alert" screen for distress
 * situations.
 */
public class RedAlert extends Activity implements OnInitListener {

	/** The field id. */
	static String FIELD_ID = "contact_id";

	/** The field number. */
	static String FIELD_NUMBER = "number";

	/** The field message. */
	static String FIELD_MESSAGE = "message";

	/** The field time. */
	static String FIELD_TIME = "time";

	/** The database interface. */
	private DbAdapter mDbHelper;

	/** The name <code>TextView</code>. */
	TextView mName;

	/** The number <code>TextView</code>. */
	TextView mNumber;
	// TextView mMessage;
	/** The m time. */
	TextView mTime;

	/** The received contact id. */
	long msContactId;

	/** The received number. */
	String msNumber;

	/** The TSS object for TTS. */
	TextToSpeech mTts;

	/** An arbitrary code for testing the availability of the TTS service. */
	private static final int TTS_CHECK_CODE = 1234;

	/**
	 * Called when the activity is first created.
	 * 
	 * @param savedInstanceState
	 *            the saved instance state
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// make the phone wake up if necessary
		getWindow().addFlags(
				WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
						| WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
						| WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

		setContentView(R.layout.red_alert);

		// play the alert
		Intent checkIntent = new Intent();
		checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
		startActivityForResult(checkIntent, TTS_CHECK_CODE);

		mDbHelper = new DbAdapter(this);
		mDbHelper.open();

		Bundle extras = getIntent().getExtras();
		msContactId = extras != null ? extras.getLong(FIELD_ID) : -1;
		msNumber = extras != null ? extras.getString(FIELD_NUMBER) : null;

		mName = (TextView) findViewById(R.id.redalert_name);
		mNumber = (TextView) findViewById(R.id.redalert_number);
		mTime = (TextView) findViewById(R.id.redalert_time);

		fillData();

		Button callButton = (Button) findViewById(R.id.call_number);
		callButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Intent callIntent = new Intent(Intent.ACTION_CALL);
				callIntent.setData(Uri.parse("tel:" + msNumber));
				startActivity(callIntent);
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

		if (mTts != null) {
			mTts.stop();
			mTts.shutdown();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onActivityResult(int, int,
	 * android.content.Intent)
	 */
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == TTS_CHECK_CODE) {
			if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
				mTts = new TextToSpeech(this, this);
			} else {
				Intent installIntent = new Intent();
				installIntent
						.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
				startActivity(installIntent);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.speech.tts.TextToSpeech.OnInitListener#onInit(int)
	 */
	@Override
	public void onInit(int status) {
		if (status == TextToSpeech.SUCCESS) {
			int result = mTts.setLanguage(Locale.US);
			if (result == TextToSpeech.LANG_MISSING_DATA
					|| result == TextToSpeech.LANG_NOT_SUPPORTED) {
				Log.e("Debug", "Language is not available.");
			}

			mTts.speak(getString(R.string.tts_redalert),
					TextToSpeech.QUEUE_FLUSH, null);
		} else {
			// Initialization failed.
			Log.e("Debug", "Could not initialize TextToSpeech.");
		}
	}

	/**
	 * Fill the data with field data.
	 */
	private void fillData() {
		if (msContactId == -1) {
			mName.setText(getString(R.string.unknown_number));
		} else {
			String name = mDbHelper.getContactName(msContactId);
			mName.setText(name);
		}
		mNumber.setText(msNumber);
		// mMessage.setText(msMessage);
		mTime.setText(Time.prettyDateTime(new Date()));
	}
}
