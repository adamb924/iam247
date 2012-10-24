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
import android.view.Menu;
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
	private transient DbAdapter mDbHelper;

	/** An intent filter to catch all broadcast refresh requests. */
	private transient IntentFilter mIntentFilter;

	/** An ISO 8601 string indicating the day the report is for. */
	private transient String mDay;

	/** An object for TTS. */
	private transient TextToSpeech mTts;

	/** An arbitrary code for testing the availability of the TTS service. */
	private static final int TTS_CHECK_CODE = 1234;

	/** The number of missed callarounds for the present day. */
	private transient long mMissed;

	private transient boolean mIncludeDelayed;

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(final Bundle bundle) {
		super.onCreate(bundle);

		// make the phone wake up if necessary (for the call around due alarm)
		getWindow().addFlags(
				WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
						| WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
						| WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

		final Bundle extras = getIntent().getExtras();
		if (extras == null) {
			return;
		}

		mDay = extras.getString(DbAdapter.Columns.DUEBY);
		if (extras.containsKey(DbAdapter.Columns.DELAYED)) {
			mIncludeDelayed = true;
		}

		setContentView(R.layout.callaround_detail);

		mIntentFilter = new IntentFilter(AlarmAdapter.Alerts.REFRESH);

		mDbHelper = new DbAdapter(this);
		mDbHelper.open();

		setTitle(String.format(getString(R.string.callaround_title),
				Time.prettyDate(this, mDay)));

		fillData();

		// if the intent tells us that a call around is (over)due, check that,
		// and if it's true sound the alarm
		if (getIntent().hasExtra(AlarmAdapter.Alerts.CALLAROUND_DUE)) {
			mMissed = mIncludeDelayed ? mDbHelper.getNumberOfDueCallarounds()
					: mDbHelper.getNumberOfDueCallaroundsNoDelayed();
			if (mMissed > 0) {
				final Intent checkIntent = new Intent();
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
		final Cursor cur = mDbHelper.fetchCallaroundReportForDay(mDay);
		startManagingCursor(cur);
		final CallaroundAdapter mCaAdapter = new CallaroundAdapter(this, cur);
		getListView().setAdapter(mCaAdapter);
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

			mMissed = mIncludeDelayed ? mDbHelper.getNumberOfDueCallarounds()
					: mDbHelper.getNumberOfDueCallaroundsNoDelayed();

			if (mMissed == 1) {
				mTts.speak(getString(R.string.tts_missedcallaround),
						TextToSpeech.QUEUE_FLUSH, null);
			} else if (mMissed > 1) {
				mTts.speak(getString(R.string.tts_missedcallarounds),
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
	public transient BroadcastReceiver mRefreshReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(final Context context, final Intent intent) {
			fillData();
		};
	};

	@Override
	public void onCreateContextMenu(final ContextMenu menu, final View view,
			final ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, view, menuInfo);
		final MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.callaround_detail_context, menu);

		if (!mDay.equals(Time.iso8601Date())) {
			menu.removeItem(R.id.call_guard);
		}

		final AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
		final long callaround_id = info.id;
		final long house_id = mDbHelper.getHouseIdFromCallaround(callaround_id);

		final MenuItem caOutstanding = menu.findItem(R.id.callaround_resolved);
		caOutstanding.setCheckable(true);
		caOutstanding.setChecked(mDbHelper
				.getCallaroundResolvedFromId(callaround_id));

		final MenuItem callaroundEnabled = menu
				.findItem(R.id.callaround_enabled);
		callaroundEnabled.setCheckable(true);
		callaroundEnabled.setChecked(mDbHelper.getCallaroundActive(house_id));
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
		final long callaround_id = info.id;
		final long house_id = mDbHelper.getHouseIdFromCallaround(callaround_id);
		boolean newValue = false;

		boolean retVal = true;

		switch (item.getItemId()) {
		case R.id.call_number:
			callNumber(house_id);
			break;
		case R.id.call_guard:
			callGuard(house_id);
			break;
		case R.id.delete_callaround:
			mDbHelper.deleteCallaround(callaround_id);
			fillData();
			break;
		case R.id.callaround_resolved:
			newValue = !item.isChecked();
			item.setChecked(newValue);
			mDbHelper.setCallaroundResolvedFromId(callaround_id, newValue);
			fillData();
			break;
		case R.id.callaround_enabled:
			newValue = !item.isChecked();
			item.setChecked(newValue);
			mDbHelper.setCallaroundActive(house_id, newValue);
			fillData();
			break;
		default:
			retVal = super.onContextItemSelected(item);
			break;
		}
		return retVal;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		final MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.callaround_detail_options, menu);
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		boolean retVal = true;

		if (item.getItemId() == R.id.previous_days) {
			final Intent intent = new Intent(CallAroundDetailList.this,
					CallAroundList.class);
			startActivity(intent);
		} else {
			retVal = super.onOptionsItemSelected(item);
		}
		return retVal;
	}

	/**
	 * Displays an <code>AlertDialog</code> with a list of contacts who belong
	 * to the house specified by house_id. If the user selects one of the
	 * numbers, the phone calls that number.
	 * 
	 * @param house_id
	 *            The _id of the house for which to display numbers.
	 */
	private void callNumber(final long house_id) {
		final Cursor cur = mDbHelper.fetchContactsForHouse(house_id);
		startManagingCursor(cur);
		if (cur.getCount() == 0) {
			final Toast toast = Toast.makeText(this,
					getString(R.string.no_house_contacts), Toast.LENGTH_LONG);
			toast.show();
			return;
		}

		final AlertDialog.Builder alert = new AlertDialog.Builder(
				CallAroundDetailList.this);

		final Spinner spinnerinput = new Spinner(CallAroundDetailList.this);
		final String[] fromFields = new String[] { DbAdapter.Columns.NAME };
		final int[] toFields = new int[] { android.R.id.text1 };
		final SimpleCursorAdapter adapter = new SimpleCursorAdapter(this,
				android.R.layout.simple_spinner_item, cur, fromFields, toFields);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinnerinput.setAdapter(adapter);

		alert.setView(spinnerinput);
		alert.setPositiveButton("Call", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(final DialogInterface dialog,
					final int whichButton) {
				final long number_id = spinnerinput.getSelectedItemId();
				final String number = mDbHelper.getNumber(number_id);

				try {
					final Intent callIntent = new Intent(Intent.ACTION_CALL);
					callIntent.setData(Uri.parse("tel:" + number));
					startActivity(callIntent);
				} catch (ActivityNotFoundException activityException) {
					Log.e("Calling a Phone Number", "Call failed",
							activityException);
				}
			}
		});
		alert.setNegativeButton(getString(R.string.cancel), null);
		alert.show();
	}

	/**
	 * Calls the number of the guard for the day.
	 * 
	 * @param house_id
	 *            The _id of the house for which to display numbers.
	 */
	private void callGuard(final long house_id) {
		final String number = mDbHelper.getGuardNumberFromDate(house_id, mDay);

		final Intent callIntent = new Intent(Intent.ACTION_CALL);
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
		public CallaroundAdapter(final Context context, final Cursor cur) {
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
		public View newView(final Context context, final Cursor cur,
				final ViewGroup parent) {
			final LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			return inflater.inflate(R.layout.callaround_detail_item, parent,
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
			String houseName;
			if (cur.getInt(cur.getColumnIndex(DbAdapter.Columns.DELAYED)) > 0) {
				houseName = cur.getString(cur
						.getColumnIndex(DbAdapter.Columns.NAME))
						+ context.getString(R.string.delayed_suffix);
			} else {
				houseName = cur.getString(cur
						.getColumnIndex(DbAdapter.Columns.NAME));
			}

			final String timeReceived = cur.getString(cur
					.getColumnIndex(DbAdapter.Columns.TIMERECEIVED));

			final long outstanding = cur.getLong(cur
					.getColumnIndex(DbAdapter.Columns.OUTSTANDING));

			final String dueFrom = Time.prettyTime(cur.getString(cur
					.getColumnIndex(DbAdapter.Columns.DUEFROM)));

			final String dueBy = Time.prettyTime(cur.getString(cur
					.getColumnIndex(DbAdapter.Columns.DUEBY)));

			String dateLabel;
			if (timeReceived == null || outstanding == 1) {
				dateLabel = context.getString(R.string.not_yet_received);
			} else {
				dateLabel = Time.prettyTime(timeReceived);
			}

			final TextView tvTitle = (TextView) view.findViewById(R.id.header);
			tvTitle.setText(houseName);

			final TextView tvDetails = (TextView) view
					.findViewById(R.id.subheader);
			tvDetails.setText(dateLabel);

			final TextView tvDueTime = (TextView) view
					.findViewById(R.id.dueTime);
			tvDueTime.setText(dueBy);

			final TextView tvEarlyTime = (TextView) view
					.findViewById(R.id.earlyTime);
			tvEarlyTime.setText(dueFrom);
		}
	}

}
