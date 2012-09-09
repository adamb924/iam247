package iam.applications;

import java.util.Locale;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
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
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

/**
 * This ListActivity displays a summary of call arounds for a particular day,
 * indicating that a house has either not checked in, or what time it did check
 * in.
 */
public class CallAroundDetailList extends ListActivity implements
		OnInitListener {

	/** The database interface */
	private DbAdapter mDbHelper;

	/** An intent filter to catch all broadcast refresh requests. */
	private IntentFilter mIntentFilter;

	/** An ISO 8601 string indicating the day the report is for. */
	private String mDay;

	/** An object for TTS. */
	TextToSpeech mTts;

	/** An arbitrary code for testing the availability of the TTS service. */
	private static final int TTS_CHECK_CODE = 1234;

	/** The number of missed callarounds for the present day. */
	private long mMissedCallarounds;

	private boolean mIncludeDelayed;

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// make the phone wake up if necessary (for the call around due alarm)
		getWindow().addFlags(
				WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
						| WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
						| WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

		Bundle extras = getIntent().getExtras();
		mDay = extras != null ? extras.getString(DbAdapter.KEY_DUEBY) : null;

		if (extras.containsKey(DbAdapter.KEY_DELAYED)) {
			mIncludeDelayed = true;
		}

		setContentView(R.layout.callaround_detail);

		mIntentFilter = new IntentFilter(AlarmReceiver.ALERT_REFRESH);

		mDbHelper = new DbAdapter(this);
		mDbHelper.open();

		setTitle(String.format(getString(R.string.callaround_title),
				Time.prettyDate(this, mDay)));

		fillData();

		// if the intent tells us that a call around is (over)due, check that,
		// and if it's true sound the alarm
		if (getIntent().hasExtra(AlarmReceiver.ALERT_CALLAROUND_DUE)) {
			mMissedCallarounds = mIncludeDelayed ? mDbHelper
					.getNumberOfDueCallaroundsIncludingDelayed() : mDbHelper
					.getNumberOfDueCallarounds();
			if (mMissedCallarounds > 0) {
				Intent checkIntent = new Intent();
				checkIntent
						.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
				startActivityForResult(checkIntent, TTS_CHECK_CODE);
			}
		}

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

		if (mTts != null) {
			mTts.stop();
			mTts.shutdown();
		}
		mDbHelper.close();
	}

	/**
	 * Query the database and refresh the list.
	 */
	private void fillData() {
		if (mDay == null) {
			return;
		}
		Cursor cur = mDbHelper.fetchCallaroundReportForDay(mDay);
		startManagingCursor(cur);
		CallaroundAdapter mCallaroundAdapter = new CallaroundAdapter(this, cur);
		getListView().setAdapter(mCallaroundAdapter);
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

			mMissedCallarounds = mIncludeDelayed ? mDbHelper
					.getNumberOfDueCallaroundsIncludingDelayed() : mDbHelper
					.getNumberOfDueCallarounds();

			if (mMissedCallarounds == 1) {
				mTts.speak(getString(R.string.tts_missedcallaround),
						TextToSpeech.QUEUE_FLUSH, null);
			} else if (mMissedCallarounds > 1) {
				mTts.speak(getString(R.string.tts_missedcallarounds),
						TextToSpeech.QUEUE_FLUSH, null);
			}
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onPause()
	 */
	@Override
	protected void onPause() {
		super.onPause();

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

	private AlertDialog.Builder mAlert;

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.callaround_detail_menu, menu);

		if (!mDay.equals(Time.iso8601Date())) {
			menu.removeItem(R.id.call_guard);
		}

		AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
		long callaround_id = info.id;
		long house_id = mDbHelper.getHouseIdFromCallaround(callaround_id);

		MenuItem callaroundOutstanding = menu
				.findItem(R.id.callaround_resolved);
		callaroundOutstanding.setCheckable(true);
		callaroundOutstanding.setChecked(mDbHelper
				.getCallaroundResolvedFromId(callaround_id));

		MenuItem callaroundEnabled = menu.findItem(R.id.callaround_enabled);
		callaroundEnabled.setCheckable(true);
		callaroundEnabled.setChecked(mDbHelper.getCallaroundActive(house_id));
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
		final long callaround_id = info.id;
		long house_id = mDbHelper.getHouseIdFromCallaround(callaround_id);

		boolean newValue;
		switch (item.getItemId()) {
		case R.id.call_number:
			callNumber(house_id);
			return true;
		case R.id.call_guard:
			callGuard(house_id);
			return true;
		case R.id.delete_callaround:
			mDbHelper.deleteCallaround(callaround_id);
			fillData();
			return true;
		case R.id.callaround_resolved:
			newValue = !item.isChecked();
			item.setChecked(newValue);
			mDbHelper.setCallaroundResolvedFromId(callaround_id, newValue);
			fillData();
			return true;
		case R.id.callaround_enabled:
			newValue = !item.isChecked();
			item.setChecked(newValue);
			mDbHelper.setCallaroundActive(house_id, newValue);
			fillData();
			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}

	/**
	 * Displays an <code>AlertDialog</code> with a list of contacts who belong
	 * to the house specified by house_id. If the user selects one of the
	 * numbers, the phone calls that number.
	 * 
	 * @param house_id
	 *            The _id of the house for which to display numbers.
	 */
	private void callNumber(long house_id) {
		Cursor c = mDbHelper.fetchContactsForHouse(house_id);
		if (c.getCount() == 0) {
			Toast toast = Toast.makeText(this,
					getString(R.string.no_house_contacts), Toast.LENGTH_LONG);
			toast.show();
			return;
		}

		mAlert = new AlertDialog.Builder(CallAroundDetailList.this);

		final Spinner spinnerinput = new Spinner(CallAroundDetailList.this);
		String[] from = new String[] { DbAdapter.KEY_NAME };
		int[] to = new int[] { android.R.id.text1 };
		SimpleCursorAdapter adapter = new SimpleCursorAdapter(this,
				android.R.layout.simple_spinner_item, c, from, to);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinnerinput.setAdapter(adapter);

		mAlert.setView(spinnerinput);
		mAlert.setPositiveButton("Call", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int whichButton) {
				long number_id = spinnerinput.getSelectedItemId();
				String number = mDbHelper.getNumber(number_id);

				try {
					Intent callIntent = new Intent(Intent.ACTION_CALL);
					callIntent.setData(Uri.parse("tel:" + number));
					startActivity(callIntent);
				} catch (ActivityNotFoundException activityException) {
					Log.e("Calling a Phone Number", "Call failed",
							activityException);
				}
			}
		});
		mAlert.setNegativeButton("Cancel", null);
		mAlert.show();
	}

	/**
	 * Calls the number of the guard for the day.
	 * 
	 * @param house_id
	 *            The _id of the house for which to display numbers.
	 */
	private void callGuard(long house_id) {
		String number = mDbHelper.getGuardNumberFromDate(house_id, mDay);

		Intent callIntent = new Intent(Intent.ACTION_CALL);
		callIntent.setData(Uri.parse("tel:" + number));
		startActivity(callIntent);
	}

	/**
	 * The custom adapter is needed for formatting the time of the requests.
	 */
	private class CallaroundAdapter extends ResourceCursorAdapter {

		/**
		 * Instantiates a new adapter.
		 * 
		 * @param context
		 *            the context
		 * @param cur
		 *            the cur
		 */
		public CallaroundAdapter(Context context, Cursor cur) {
			super(context, R.layout.callaround_detail_item, cur);
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
			return li.inflate(R.layout.callaround_detail_item, parent, false);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.widget.CursorAdapter#bindView(android.view.View,
		 * android.content.Context, android.database.Cursor)
		 */
		@Override
		public void bindView(View view, Context context, Cursor cur) {
			String houseName = cur.getString(cur
					.getColumnIndex(DbAdapter.KEY_NAME));

			if (cur.getInt(cur.getColumnIndex(DbAdapter.KEY_DELAYED)) > 0) {
				houseName = houseName
						+ context.getString(R.string.delayed_suffix);
			}

			String dateLabel = cur.getString(cur
					.getColumnIndex(DbAdapter.KEY_TIMERECEIVED));

			long outstanding = cur.getLong(cur
					.getColumnIndex(DbAdapter.KEY_OUTSTANDING));

			String dueFrom = cur.getString(cur
					.getColumnIndex(DbAdapter.KEY_DUEFROM));
			dueFrom = Time.prettyTime(dueFrom);

			String dueBy = cur.getString(cur
					.getColumnIndex(DbAdapter.KEY_DUEBY));
			dueBy = Time.prettyTime(dueBy);

			if (dateLabel == null || outstanding == 1) {
				dateLabel = context.getString(R.string.not_yet_received);
			} else {
				dateLabel = Time.prettyTime(dateLabel);
			}

			TextView tvTitle = (TextView) view.findViewById(R.id.header);
			tvTitle.setText(houseName);
			TextView tvDetails = (TextView) view.findViewById(R.id.subheader);
			tvDetails.setText(dateLabel);

			TextView tvDueTime = (TextView) view.findViewById(R.id.dueTime);
			tvDueTime.setText(dueBy);

			TextView tvEarlyTime = (TextView) view.findViewById(R.id.earlyTime);
			tvEarlyTime.setText(dueFrom);
		}
	}

}
