/**
 * 
 */
package iam.applications;

import java.util.Date;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * This is the home activity of the app, i.e., the first screen that is
 * displayed.
 */
public class HomeActivity extends Activity {

	/** The debug tag */
	static public final String TAG = "Debug";

	/** The name of the application preferences. */
	static public final String PREFERENCES = "247-Preferences-File";

	/** Preference key: what time callaround is due by. */
	static public final String PREFERENCES_CALLAROUND_DUE_BY = "PREFERENCES_CALLAROUND_DUE_BY";

	/** Preference key: the earliest time to callaround. */
	static public final String PREFERENCES_CALLAROUND_DUE_FROM = "PREFERENCES_CALLAROUND_DUE_FROM";

	/** Preference key: what time the call around alarm should actually sound. */
	static public final String PREFERENCES_CALLAROUND_ALARM_TIME = "PREFERENCES_CALLAROUND_ALARM_TIME";

	/** Preference key: what time the callarounds are added for the day. */
	static public final String PREFERENCES_CALLAROUND_ADD = "PREFERENCES_CALLAROUND_ADD";

	/** Preference key: whether the application permits "this is" messages. */
	static public final String PREFERENCES_PERMIT_THISIS = "PREFERENCES_PERMIT_THISIS";

	/**
	 * Preference key: how many minutes before the checkin is due should a
	 * reminder be sent.
	 */
	static public final String PREFERENCES_CHECKIN_REMINDER_DELAY = "PREFERENCES_CHECKIN_REMINDER_DELAY";

	/**
	 * Preference key: how long after the phone starts ringing should the
	 * application wait before seeing if the person has hung up (and therefore
	 * missed-called).
	 */
	static public final String PREFERENCES_MISSED_CALL_DELAY = "PREFERENCES_MISSED_CALL_DELAY";

	/**
	 * Preference key: whether callarounds scheduled for the future should be
	 * displayed in the list.
	 */
	static public final String PREFERENCES_CALLAROUNDS_SHOW_FUTURE = "PREFERENCES_CALLAROUNDS_SHOW_FUTURE";

	/** Preference key: a kill switch to disable 24/7. */
	static public final String PREFERENCES_DISABLE_247 = "PREFERENCES_DISABLE_247";

	/**
	 * Preference key: the latest possible callaround, if someone requests a
	 * delay.
	 */
	static public final String PREFERENCES_CALLAROUND_DELAYED_TIME = "PREFERENCES_CALLAROUND_DELAYED_TIME";

	/** Preference key: what time 24/7 starts checking in on guards. */
	static public final String PREFERENCES_GUARD_CHECKIN_START = "PREFERENCES_GUARD_CHECKIN_START";

	/** Preference key: what time 24/7 stops checkin in on guards. */
	static public final String PREFERENCES_GUARD_CHECKIN_END = "PREFERENCES_GUARD_CHECKIN_END";

	/** Preference key: the fewest possible guard checks. */
	static public final String PREFERENCES_FEWEST_GUARD_CHECKS = "PREFERENCES_FEWEST_GUARD_CHECKS";

	/** Preference key: the maximum number of random checks allowed. */
	static public final String PREFERENCES_RANDOM_GUARD_CHECKS = "PREFERENCES_RANDOM_GUARD_CHECKS";

	/** Preference key: how long the guard as to respond. */
	static public final String PREFERENCES_GUARD_CHECKIN_WINDOW = "PREFERENCES_GUARD_CHECKIN_WINDOW";

	/** Preference key: ignore messages that do not have a certain prefix. */
	static public final String PREFERENCES_REQUIRE_PREFIX = "PREFERENCES_REQUIRE_PREFIX";

	/** The database interface. */
	private transient DbAdapter mDbHelper;

	/** An intent filter to catch all broadcast refresh requests. */
	private transient IntentFilter mIntentFilter;

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(final Bundle bundle) {
		super.onCreate(bundle);

		// make the phone wake up if necessary (for the call around add alarm)
		getWindow().addFlags(
				WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
						| WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
						| WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

		setAlarms();

		setContentView(R.layout.home_activity);

		mIntentFilter = new IntentFilter(AlarmAdapter.ALERT_REFRESH);

		final LinearLayout checkinsButton = (LinearLayout) findViewById(R.id.checkins_button);
		checkinsButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(final View view) {
				final Intent intent = new Intent(HomeActivity.this,
						CheckinList.class);
				startActivity(intent);
			}
		});

		final LinearLayout callaroundButton = (LinearLayout) findViewById(R.id.callarounds_button);
		callaroundButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(final View view) {
				final Intent intent = new Intent(HomeActivity.this,
						CallAroundDetailList.class);
				intent.putExtra(DbAdapter.KEY_DUEBY, Time.iso8601Date());
				startActivity(intent);
				// final Intent intent = new Intent(HomeActivity.this,
				// CallAroundList.class);
				// startActivity(intent);
			}
		});

		final TextView locationsButton = (TextView) findViewById(R.id.home_locations);
		locationsButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(final View view) {
				final Intent intent = new Intent(HomeActivity.this,
						LocationsList.class);
				startActivity(intent);
			}
		});

		final TextView teammembersButton = (TextView) findViewById(R.id.home_teammembers);
		teammembersButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(final View view) {
				final Intent intent = new Intent(HomeActivity.this,
						TeamMemberList.class);
				startActivity(intent);
			}
		});

		final TextView housesButton = (TextView) findViewById(R.id.home_houses);
		housesButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(final View view) {
				final Intent intent = new Intent(HomeActivity.this,
						HouseList.class);
				startActivity(intent);
			}
		});

		final TextView guardsButton = (TextView) findViewById(R.id.home_guards);
		guardsButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(final View view) {
				final Intent intent = new Intent(HomeActivity.this,
						GuardList.class);
				startActivity(intent);
			}
		});

		final TextView broadcastButton = (TextView) findViewById(R.id.home_broadcast);
		broadcastButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(final View view) {
				final Intent intent = new Intent(HomeActivity.this,
						BroadcastActivity.class);
				startActivity(intent);
			}
		});

		final LinearLayout msgErrButton = (LinearLayout) findViewById(R.id.message_errors);
		msgErrButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(final View view) {
				final Intent intent = new Intent(HomeActivity.this,
						UnsentMessageList.class);
				startActivity(intent);
			}
		});

		mDbHelper = new DbAdapter(this);
		mDbHelper.open();

		fillData();
	}

	/**
	 * This method sets alarms daily alarms that we want to be sure are going
	 * off.
	 */
	private void setAlarms() {
		AlarmAdapter.setAddCallaroundAlarm(this);
		AlarmAdapter.setAddGuardCheckinAlarms(this);
		AlarmAdapter.setGuardScheduleResetAlarm(this);
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

	/**
	 * Query the database and refresh the list.
	 */
	private void fillData() {
		final TextView checkinSummary = (TextView) findViewById(R.id.home_checkins_summary);
		checkinSummary.setText(mDbHelper.getCheckinSummary());

		final TextView callaroundSummary = (TextView) findViewById(R.id.home_callaround_summary);
		callaroundSummary.setText(mDbHelper.getCallaroundSummary(new Date()));

		final TextView msgErrReport = (TextView) findViewById(R.id.error_report);
		final int count = mDbHelper.getNumberOfMessageErrors();

		switch (count) {
		case 0:
			msgErrReport.setText(R.string.noerrors);
			break;
		case 1:
			msgErrReport.setText(R.string.message_error_one);
			break;
		default:
			msgErrReport.setText(String.format(
					getString(R.string.message_errors), String.valueOf(count)));
			break;
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

		fillData();
		registerReceiver(mRefreshReceiver, mIntentFilter);
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		final boolean returnValue = super.onCreateOptionsMenu(menu);
		final MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.home_menu, menu);

		return returnValue;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case R.id.customization:
			startActivity(new Intent(this, PreferencesActivity.class));
			return true;
		case R.id.log:
			startActivity(new Intent(this, LogList.class));
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	/**
	 * Send an alert that activities should refresh their screens. Currently
	 * this is called only in the constructor of SmsHandler. Any activity which
	 * has SMS-dependent information should update in response to this message.
	 * 
	 * @param context
	 *            the application context
	 */
	static public void sendRefreshAlert(final Context context) {
		final Intent intent = new Intent(AlarmAdapter.ALERT_REFRESH);
		context.sendBroadcast(intent);
	}
}
