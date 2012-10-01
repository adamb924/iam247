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
	private transient DbAdapter mDbHelper;

	/** An intent filter to catch all broadcast refresh requests. */
	private transient IntentFilter mIntentFilter;

	/** An object for TTS. */
	private transient TextToSpeech mTts;

	/** An arbitrary code for testing the availability of the TTS service. */
	private static final int TTS_CHECK_CODE = 1234;

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(final Bundle bundle) {
		super.onCreate(bundle);

		// make the phone wake up if necessary
		getWindow().addFlags(
				WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
						| WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
						| WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

		setContentView(R.layout.checkin_list);

		mIntentFilter = new IntentFilter(AlarmReceiver.ALERT_REFRESH);

		final Intent intent = getIntent();
		if (intent.getBooleanExtra(AlarmReceiver.ALERT_CHECKIN_DUE, false)) {
			// play the alert ... eventually
			final Intent checkIntent = new Intent();
			checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
			startActivityForResult(checkIntent, TTS_CHECK_CODE);
		}

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
	public void onInit(final int status) {
		if (status == TextToSpeech.SUCCESS) {
			final int result = mTts.setLanguage(Locale.US);
			if (result == TextToSpeech.LANG_MISSING_DATA
					|| result == TextToSpeech.LANG_NOT_SUPPORTED) {
				Log.e(HomeActivity.TAG, "Language is not available.");
			}

			if (mDbHelper.getNumberOfDueCheckins() > 0) {
				mTts.speak(getString(R.string.tts_missedcheckin),
						TextToSpeech.QUEUE_FLUSH, null);
			}
		} else {
			// Initialization failed.
			Log.e(HomeActivity.TAG, "Could not initialize TextToSpeech.");
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onActivityResult(int, int,
	 * android.content.Intent)
	 */
	@Override
	protected void onActivityResult(final int requestCode,
			final int resultCode, final Intent data) {
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

	/**
	 * Query the database and refresh the list.
	 */
	private void fillData() {
		// Cursor oustandingCur = mDbHelper.fetchOustandingCheckins();
		final Cursor oustandingCur = mDbHelper.fetchAllCheckins();
		startManagingCursor(oustandingCur);

		final CheckinAdapter listAdapter = new CheckinAdapter(this,
				oustandingCur);
		setListAdapter(listAdapter);
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
		inflater.inflate(R.menu.checkin_menu, menu);

		final AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
		final long checkin_id = info.id;

		if (mDbHelper.getCheckinOutstanding(checkin_id)) {
			menu.removeItem(R.id.unresolve_checkin);
		} else {
			menu.removeItem(R.id.resolve_checkin);
		}
		if (mDbHelper.getTripResolvedFromCheckin(checkin_id)) {
			menu.removeItem(R.id.resolve_trip);
		} else {
			menu.removeItem(R.id.unresolve_trip);
		}
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

		switch (item.getItemId()) {
		case R.id.call_number:
			try {
				final String number = mDbHelper.getNumberForCheckin(info.id);
				if (number != null) {
					final Intent callIntent = new Intent(Intent.ACTION_CALL);
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
		case R.id.resolve_trip:
			mDbHelper.setTripResolvedFromCheckinId(info.id, true);
			SmsHandler.sendSms(this, mDbHelper.getNumberForCheckin(info.id),
					getString(R.string.sms_trip_resolved_foryou));
			fillData();
			return true;
		case R.id.unresolve_trip:
			mDbHelper.setTripResolvedFromCheckinId(info.id, false);
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
	public transient BroadcastReceiver mRefreshReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(final Context context, final Intent intent) {
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
		public CheckinAdapter(final Context context, final Cursor cur) {
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
		public View newView(final Context context, final Cursor cur,
				final ViewGroup parent) {
			final LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			return inflater.inflate(R.layout.checkin_item, parent, false);
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

			final Date returning = Time.iso8601DateTime(cur.getString(cur
					.getColumnIndex(DbAdapter.KEY_TIMEDUE)));

			final boolean outstanding = cur.getLong(cur
					.getColumnIndex(DbAdapter.KEY_OUTSTANDING)) == 1 ? true
					: false;
			final boolean due = returning.before(new Date());

			final boolean tripresolved = cur.getLong(cur
					.getColumnIndex(DbAdapter.KEY_TRIPRESOLVED)) == 1 ? true
					: false;

			// Log.i("Debug", "CheckinAdapter");
			// Log.i("Debug", "Outstanding: " + outstanding);
			// Log.i("Debug", "tripresolved: " + tripresolved);
			// Log.i("Debug", "due: " + due);

			final String sWith = cur.getString(cur
					.getColumnIndex(DbAdapter.KEY_WITH));

			// get the UI items
			final TextView name = (TextView) view
					.findViewById(R.id.checkin_name);
			final TextView location = (TextView) view
					.findViewById(R.id.checkin_location);
			final TextView with = (TextView) view
					.findViewById(R.id.checkin_with);
			final TextView time = (TextView) view
					.findViewById(R.id.checkin_time);

			// set the text values, or hide if appropriate
			name.setText(cur.getString(cur.getColumnIndex(DbAdapter.KEY_NAME)));
			if (!tripresolved && outstanding) {
				location.setText(String.format(context
						.getString(R.string.enroute), cur.getString(cur
						.getColumnIndex(DbAdapter.KEY_LOCATION)), cur
						.getString(cur.getColumnIndex(DbAdapter.KEY_KEYWORD))));
			} else {
				location.setText(String.format(context
						.getString(R.string.arrivedat), cur.getString(cur
						.getColumnIndex(DbAdapter.KEY_LOCATION)), cur
						.getString(cur.getColumnIndex(DbAdapter.KEY_KEYWORD))));
			}

			time.setText(String.format(context.getString(R.string.returning),
					Time.timeTodayTomorrow(context, returning)));
			if (sWith == null) {
				with.setVisibility(View.GONE);
			} else {
				with.setText(String.format(context.getString(R.string.with),
						sWith));
			}

			// set the colors as appropriate
			if (tripresolved) {
				name.setTextColor(Color.DKGRAY);
				location.setTextColor(Color.DKGRAY);
				time.setTextColor(Color.DKGRAY);
				with.setTextColor(Color.DKGRAY);
			} else {
				// it doesn't feel as though this should have to be here, but
				// apparently so
				name.setTextColor(Color.WHITE);
				location.setTextColor(Color.WHITE);
				time.setTextColor(Color.WHITE);
				with.setTextColor(Color.WHITE);
			}

			if (due && outstanding) {
				name.setTextColor(Color.RED);
			}
		}
	}
}
