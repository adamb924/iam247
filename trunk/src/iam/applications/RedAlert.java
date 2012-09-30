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
	public static final String FIELD_ID = "contact_id";

	/** The field number. */
	public static final String FIELD_NUMBER = "number";

	/** The field message. */
	public static final String FIELD_MESSAGE = "message";

	/** The field time. */
	public static final String FIELD_TIME = "time";

	/** The database interface. */
	private transient DbAdapter mDbHelper;

	/** The name <code>TextView</code>. */
	private transient TextView mName;

	/** The number <code>TextView</code>. */
	private transient TextView mNumber;

	/** The m time. */
	private transient TextView mTime;

	/** The received contact id. */
	private transient long msContactId;

	/** The received number. */
	private transient String msNumber;

	/** The TSS object for TTS. */
	private transient TextToSpeech mTts;

	/** An arbitrary code for testing the availability of the TTS service. */
	private static final int TTS_CHECK_CODE = 1234;

	/**
	 * Called when the activity is first created.
	 * 
	 * @param bundle
	 *            the saved instance state
	 */
	@Override
	public void onCreate(final Bundle bundle) {
		super.onCreate(bundle);

		// make the phone wake up if necessary
		getWindow().addFlags(
				WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
						| WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
						| WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

		setContentView(R.layout.red_alert);

		// play the alert
		final Intent checkIntent = new Intent();
		checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
		startActivityForResult(checkIntent, TTS_CHECK_CODE);

		mDbHelper = new DbAdapter(this);
		mDbHelper.open();

		final Bundle extras = getIntent().getExtras();
		msContactId = extras == null ? -1 : extras.getLong(FIELD_ID);
		msNumber = extras == null ? null : extras.getString(FIELD_NUMBER);

		mName = (TextView) findViewById(R.id.redalert_name);
		mNumber = (TextView) findViewById(R.id.redalert_number);
		mTime = (TextView) findViewById(R.id.redalert_time);

		fillData();

		final Button callButton = (Button) findViewById(R.id.call_number);
		callButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(final View view) {
				final Intent callIntent = new Intent(Intent.ACTION_CALL);
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
	public void onActivityResult(final int requestCode, final int resultCode,
			final Intent data) {
		if (requestCode == TTS_CHECK_CODE) {
			if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
				mTts = new TextToSpeech(this, this);
			} else {
				final Intent installIntent = new Intent();
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
	public void onInit(final int status) {
		if (status == TextToSpeech.SUCCESS) {
			final int result = mTts.setLanguage(Locale.US);
			if (result == TextToSpeech.LANG_MISSING_DATA
					|| result == TextToSpeech.LANG_NOT_SUPPORTED) {
				Log.e(HomeActivity.TAG, "Language is not available.");
			}

			mTts.speak(getString(R.string.tts_redalert),
					TextToSpeech.QUEUE_FLUSH, null);
		} else {
			// Initialization failed.
			Log.e(HomeActivity.TAG, "Could not initialize TextToSpeech.");
		}
	}

	/**
	 * Fill the data with field data.
	 */
	private void fillData() {
		if (msContactId == -1) {
			mName.setText(getString(R.string.unknown_number));
		} else {
			final String name = mDbHelper.getContactName(msContactId);
			mName.setText(name);
		}
		mNumber.setText(msNumber);
		// mMessage.setText(msMessage);
		mTime.setText(Time.prettyDateTime(new Date()));
	}
}
