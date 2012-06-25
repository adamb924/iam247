package iam.applications;

import java.util.Date;
import java.util.Locale;

import android.app.ListActivity;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

/**
 * This ListActivity is used to display outstanding checkin requests.
 */
public class CheckinList extends ListActivity implements OnInitListener {

	/** The database interface. */
	private DbAdapter mDbHelper;

	/** An intent filter to catch all broadcast refresh requests. */
	private IntentFilter mIntentFilter;

	/** An object for TTS. */
	TextToSpeech mTts;

	/** An arbitrary code for testing the availability of the TTS service. */
	private static final int TTS_CHECK_CODE = 1234;

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// make the phone wake up if necessary
		getWindow().addFlags(
				WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
						| WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
						| WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

		setContentView(R.layout.checkin_list);

		mIntentFilter = new IntentFilter(AlarmReceiver.ALERT_REFRESH);

		Intent i = getIntent();
		// this may produce an alert when none is required
		if (i.getBooleanExtra(AlarmReceiver.ALERT_CHECKIN_DUE, false)) {
			// play the alert
			Intent checkIntent = new Intent();
			checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
			startActivityForResult(checkIntent, TTS_CHECK_CODE);
		}

		// Intent checkIntent = new Intent();
		// checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
		// startActivityForResult(checkIntent, TTS_CHECK_CODE);

		mDbHelper = new DbAdapter(this);
		mDbHelper.open();

		fillData();

		registerForContextMenu(getListView());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.ListActivity#onDestroy()
	 */
	@Override
	protected void onDestroy() {
		super.onDestroy();

		if (mTts != null) {
			mTts.stop();
			mTts.shutdown();
		}
		mDbHelper.close();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onPause()
	 */
	@Override
	protected void onPause() {
		super.onPause();
		fillData();
		unregisterReceiver(mRefreshReceiver);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume() {
		super.onResume();

		registerReceiver(mRefreshReceiver, mIntentFilter);
		fillData();
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

			mTts.speak(getString(R.string.tts_missedcheckin),
					TextToSpeech.QUEUE_FLUSH, null);
		} else {
			// Initialization failed.
			Log.e("Debug", "Could not initialize TextToSpeech.");
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onActivityResult(int, int,
	 * android.content.Intent)
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
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

	/**
	 * Query the database and refresh the list.
	 */
	private void fillData() {
		// Cursor oustandingCur = mDbHelper.fetchOustandingCheckins();
		Cursor oustandingCur = mDbHelper.fetchAllCheckins();
		startManagingCursor(oustandingCur);

		// String[] from = new String[] { DbAdapter.KEY_NAME,
		// DbAdapter.KEY_LOCATION, DbAdapter.KEY_TIMEDUE };
		// int[] to = new int[] { R.id.checkin_name, R.id.checkin_location,
		// R.id.checkin_time };
		// SimpleCursorAdapter mListAdapter = new SimpleCursorAdapter(this,
		// R.layout.checkin_item, oustandingCur, from, to);

		CheckinAdapter listAdapter = new CheckinAdapter(this, oustandingCur);
		setListAdapter(listAdapter);
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
		inflater.inflate(R.menu.checkin_menu, menu);

		AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
		long checkin_id = info.id;

		// Log.i("Debug", mDbHelper.getCheckinOutstanding(checkin_id) ? "true"
		// : "false");

		if (mDbHelper.getCheckinOutstanding(checkin_id)) {
			menu.removeItem(R.id.unresolve_checkin);
		} else {
			menu.removeItem(R.id.resolve_checkin);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onContextItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
				.getMenuInfo();

		switch (item.getItemId()) {
		case R.id.call_number:
			try {
				String number = mDbHelper.getNumberForCheckin(info.id);
				if (number != null) {
					Intent callIntent = new Intent(Intent.ACTION_CALL);
					callIntent.setData(Uri.parse("tel:" + number));
					startActivity(callIntent);
				}
			} catch (ActivityNotFoundException activityException) {
				Log.e("Calling a Phone Number", "Call failed",
						activityException);
			}
			return true;
		case R.id.resolve_checkin:
			mDbHelper.setCheckinResolvedFromId(info.id, true);
			SmsHandler.sendSms(this, mDbHelper.getNumberForCheckin(info.id),
					getString(R.string.sms_checkin_resolved_foryou));
			fillData();
			return true;
		case R.id.unresolve_checkin:
			mDbHelper.setCheckinResolvedFromId(info.id, false);
			fillData();
			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}

	/**
	 * When the refresh request is received, call fillData() to refresh the
	 * screen.
	 */
	public BroadcastReceiver mRefreshReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			fillData();
		};
	};

	/**
	 * The custom adapter is needed to format the time strings, and to format
	 * the other strings in a localizeable way.
	 */
	private class CheckinAdapter extends ResourceCursorAdapter {

		/**
		 * Instantiates a new checkin adapter.
		 * 
		 * @param context
		 *            the context
		 * @param cur
		 *            the cur
		 */
		public CheckinAdapter(Context context, Cursor cur) {
			super(context, R.layout.checkin_item, cur);
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
			return li.inflate(R.layout.checkin_item, parent, false);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.widget.CursorAdapter#bindView(android.view.View,
		 * android.content.Context, android.database.Cursor)
		 */
		@Override
		public void bindView(View view, Context context, Cursor cur) {
			TextView name = (TextView) view.findViewById(R.id.checkin_name);
			TextView location = (TextView) view
					.findViewById(R.id.checkin_location);
			TextView time = (TextView) view.findViewById(R.id.checkin_time);

			Date returning = Time.iso8601DateTime(cur.getString(cur
					.getColumnIndex(DbAdapter.KEY_TIMEDUE)));

			boolean outstanding = cur.getLong(cur
					.getColumnIndex(DbAdapter.KEY_OUTSTANDING)) == 1 ? true
					: false;
			boolean due = returning.before(new Date());

			name.setText(cur.getString(cur.getColumnIndex(DbAdapter.KEY_NAME)));
			location.setText(String.format(context.getString(R.string.goneto),
					cur.getString(cur.getColumnIndex(DbAdapter.KEY_LOCATION))));
			time.setText(String.format(context.getString(R.string.returning),
					Time.timeTodayTomorrow(context, returning)));

			if (!outstanding) {
				name.setTextColor(Color.DKGRAY);
				location.setTextColor(Color.DKGRAY);
				time.setTextColor(Color.DKGRAY);
			} else {
				// it doesn't feel as though this should have to be here, but
				// apparently so
				name.setTextColor(Color.WHITE);
				location.setTextColor(Color.WHITE);
				time.setTextColor(Color.WHITE);
			}

			if (due && outstanding) {
				name.setTextColor(Color.RED);
			}
		}
	}
}
