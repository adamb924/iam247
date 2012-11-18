/**
 * 
 */
package iam.applications;

import java.util.Date;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceManager;
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
public class HomeActivity extends Preferences {

	/** The debug tag. */
	static public final String TAG = "Debug";

	/** The database interface. */
	private transient DbAdapter mDbHelper;

	/** An intent filter to catch all broadcast refresh requests. */
	private transient IntentFilter mIntentFilter;

	/** A listener for when the preferences change. */
	private transient OnSharedPreferenceChangeListener mPrefListener;

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

		AlarmAdapter.resetRepeatingAlarms(this);

		setContentView(R.layout.home_activity);

		mIntentFilter = new IntentFilter(AlarmAdapter.Alerts.REFRESH);

		setButtonClickListeners();

		setPreferenceChangeListener();

		mDbHelper = new DbAdapter(this);
		mDbHelper.open();

		fillData();
	}

	/**
	 * 
	 */
	private void setPreferenceChangeListener() {
		final SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		mPrefListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
			@Override
			public void onSharedPreferenceChanged(
					final SharedPreferences prefs, final String key) {
				AlarmAdapter.resetRepeatingAlarms(HomeActivity.this);
				HomeActivity.sendRefreshAlert(HomeActivity.this);
			}
		};
		prefs.registerOnSharedPreferenceChangeListener(mPrefListener);
	}

	/**
	 * 
	 */
	private void setButtonClickListeners() {
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
				intent.putExtra(DbAdapter.Columns.DUEBY, Time.iso8601Date());
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
		if (mDbHelper == null) {
			return;
		}

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
		boolean retVal;
		switch (item.getItemId()) {
		case R.id.customization:
			startActivity(new Intent(this, PreferencesActivity.class));
			retVal = true;
			break;
		case R.id.log:
			startActivity(new Intent(this, LogList.class));
			retVal = true;
			break;
		default:
			retVal = super.onOptionsItemSelected(item);
		}
		return retVal;
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
		final Intent intent = new Intent(AlarmAdapter.Alerts.REFRESH);
		context.sendBroadcast(intent);
	}
}
