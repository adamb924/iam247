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

// TODO: Auto-generated Javadoc
/**
 * This is the home activity of the app, i.e., the first screen that is
 * displayed.
 */
public class HomeActivity extends Activity {

	/** The name of the applicationpreferences. */
	static public String PREFERENCES = "247-Preferences-File";

	/** Preference key: what time callaround is due by. */
	static public String PREFERENCES_CALLAROUND_DUE_BY = "PREFERENCES_CALLAROUND_DUE_BY";

	/** Preference key: the earliest time to callaround. */
	static public String PREFERENCES_CALLAROUND_DUE_FROM = "PREFERENCES_CALLAROUND_DUE_FROM";

	/** Preference key: what time the callarounds are added for the day. */
	static public String PREFERENCES_CALLAROUND_ADD = "PREFERENCES_CALLAROUND_ADD";

	/** Preference key: whether the application permits "this is" messages. */
	static public String PREFERENCES_PERMIT_THISIS = "PREFERENCES_PERMIT_THISIS";

	/**
	 * Preference key: how many minutes before the checkin is due should a
	 * reminder be sent.
	 */
	static public String PREFERENCES_CHECKIN_REMINDER_DELAY = "PREFERENCES_CHECKIN_REMINDER_DELAY";

	/**
	 * Preference key: how long after the phone starts ringing should the
	 * application wait before seeing if the person has hung up (and therefore
	 * missed-called).
	 */
	static public String PREFERENCES_MISSED_CALL_DELAY = "PREFERENCES_MISSED_CALL_DELAY";

	/**
	 * Preference key: whether callarounds scheduled for the future should be
	 * displayed in the list.
	 */
	static public String PREFERENCES_CALLAROUNDS_SHOW_FUTURE = "PREFERENCES_CALLAROUNDS_SHOW_FUTURE";

	/** Preference key: a kill switch to disable 24/7. */
	static public String PREFERENCES_DISABLE_247 = "PREFERENCES_DISABLE_247";

	/**
	 * Preference key: the latest possible callaround, if someone requests a
	 * delay.
	 */
	static public String PREFERENCES_CALLAROUND_DELAYED_TIME = "PREFERENCES_CALLAROUND_DELAYED_TIME";

	/** Preference key: what time 24/7 starts checking in on guards. */
	static public String PREFERENCES_GUARD_CHECKIN_START = "PREFERENCES_GUARD_CHECKIN_START";

	/** Preference key: what time 24/7 stops checkin in on guards. */
	static public String PREFERENCES_GUARD_CHECKIN_END = "PREFERENCES_GUARD_CHECKIN_END";

	/** Preference key: the fewest possible guard checks. */
	static public String PREFERENCES_FEWEST_GUARD_CHECKS = "PREFERENCES_FEWEST_GUARD_CHECKS";

	/** Preference key: the maximum number of random checks allowed. */
	static public String PREFERENCES_RANDOM_GUARD_CHECKS = "PREFERENCES_RANDOM_GUARD_CHECKS";

	/** Preference key: how long the guard as to respond. */
	static public String PREFERENCES_GUARD_CHECKIN_WINDOW = "PREFERENCES_GUARD_CHECKIN_WINDOW";

	/** The database interface. */
	private DbAdapter mDbHelper;

	/** An intent filter to catch all broadcast refresh requests. */
	private IntentFilter mIntentFilter;

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// make the phone wake up if necessary (for the call around add alarm)
		getWindow().addFlags(
				WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
						| WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
						| WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

		// add these two alarms for managing call arounds
		AlarmReceiver.setAddCallaroundAlarm(this);
		AlarmReceiver.setAddGuardCheckinAlarms(this);

		setContentView(R.layout.home_activity);

		mIntentFilter = new IntentFilter(AlarmReceiver.ALERT_REFRESH);

		LinearLayout checkinsButton = (LinearLayout) findViewById(R.id.checkins_button);
		checkinsButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Intent i = new Intent(HomeActivity.this, CheckinList.class);
				startActivity(i);
			}
		});

		LinearLayout callaroundButton = (LinearLayout) findViewById(R.id.callarounds_button);
		callaroundButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Intent i = new Intent(HomeActivity.this, CallAroundList.class);
				startActivity(i);
			}
		});

		TextView locationsButton = (TextView) findViewById(R.id.home_locations);
		locationsButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Intent i = new Intent(HomeActivity.this, LocationsList.class);
				startActivity(i);
			}
		});

		TextView teammembersButton = (TextView) findViewById(R.id.home_teammembers);
		teammembersButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Intent i = new Intent(HomeActivity.this, TeamMemberList.class);
				startActivity(i);
			}
		});

		TextView housesButton = (TextView) findViewById(R.id.home_houses);
		housesButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Intent i = new Intent(HomeActivity.this, HouseList.class);
				startActivity(i);
			}
		});

		TextView guardsButton = (TextView) findViewById(R.id.home_guards);
		guardsButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Intent i = new Intent(HomeActivity.this, GuardList.class);
				startActivity(i);
			}
		});

		TextView broadcastButton = (TextView) findViewById(R.id.home_broadcast);
		broadcastButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Intent i = new Intent(HomeActivity.this,
						BroadcastActivity.class);
				startActivity(i);
			}
		});

		LinearLayout messageerrorsButton = (LinearLayout) findViewById(R.id.message_errors);
		messageerrorsButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Intent i = new Intent(HomeActivity.this,
						UnsentMessageList.class);
				startActivity(i);
			}
		});

		mDbHelper = new DbAdapter(this);
		mDbHelper.open();

		fillData();
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
		TextView checkinSummary = (TextView) findViewById(R.id.home_checkins_summary);
		checkinSummary.setText(mDbHelper.getCheckinSummary());

		TextView callaroundSummary = (TextView) findViewById(R.id.home_callaround_summary);
		callaroundSummary.setText(mDbHelper.getCallaroundSummary(new Date()));

		TextView messageErrorReport = (TextView) findViewById(R.id.error_report);
		int count = mDbHelper.getNumberOfMessageErrors();

		switch (count) {
		case 0:
			messageErrorReport.setText(R.string.noerrors);
			break;
		case 1:
			messageErrorReport.setText(R.string.message_error_one);
			break;
		default:
			messageErrorReport.setText(String.format(
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
	public BroadcastReceiver mRefreshReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			fillData();
		};
	};

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean r = super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.home_menu, menu);

		return r;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
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
}
