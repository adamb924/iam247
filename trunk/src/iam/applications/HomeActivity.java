/**
 * 
 */
package iam.applications;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Date;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

/**
 * This is the home activity of the app, i.e., the first screen that is
 * displayed.
 */
public class HomeActivity extends Activity {
	static public String PREFERENCES = "247-Preferences-File";
	static public String PREFERENCES_CALLAROUND_DUE_BY = "PREFERENCES_CALLAROUND_DUE_BY";
	static public String PREFERENCES_CALLAROUND_DUE_FROM = "PREFERENCES_CALLAROUND_DUE_FROM";
	static public String PREFERENCES_CALLAROUND_ADD = "PREFERENCES_CALLAROUND_ADD";
	static public String PREFERENCES_PERMIT_THISIS = "PREFERENCES_PERMIT_THISIS";
	static public String PREFERENCES_CHECKIN_REMINDER_DELAY = "PREFERENCES_CHECKIN_REMINDER_DELAY";
	static public String PREFERENCES_MISSED_CALL_DELAY = "PREFERENCES_MISSED_CALL_DELAY";
	static public String PREFERENCES_CALLAROUNDS_SHOW_FUTURE = "PREFERENCES_CALLAROUNDS_SHOW_FUTURE";

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

		// add these two alarms for managing callarounds
		AlarmReceiver.setAddCallaroundAlarm(this);

		setContentView(R.layout.home_activity);

		mIntentFilter = new IntentFilter(AlarmReceiver.ALERT_REFRESH);

		TextView checkinsButton = (TextView) findViewById(R.id.home_checkins);
		checkinsButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Intent i = new Intent(HomeActivity.this, CheckinList.class);
				startActivity(i);
			}
		});

		TextView callaroundButton = (TextView) findViewById(R.id.home_callaround);
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

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean r = super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.home_menu, menu);

		MenuItem thisIsAllowed = menu.findItem(R.id.thisis_enabled);
		thisIsAllowed.setCheckable(true);
		thisIsAllowed.setChecked(getThisIsAllowed(this));

		return r;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.email_database:
			emailDatabase();
			return true;
		case R.id.upload_database:
			uploadDatabase();
			return true;
		case R.id.reset_database:
			resetDatabase();
			return true;
		case R.id.clear_data:
			clearDataBeforeOneWeek();
			return true;
		case R.id.clear_log:
			clearLogBeforeOneWeek();
			return true;
		case R.id.block_number:
			blockNumber();
		case R.id.unblock_number:
			unblockNumber();
			return true;
		case R.id.thisis_enabled:
			boolean newValue = !item.isChecked();
			item.setChecked(newValue);
			setThisIsAllowed(newValue);
			return true;
		case R.id.missed_call_delay:
			setMissedCallDelay();
			return true;
		case R.id.log:
			Intent i = new Intent(this, LogList.class);
			startActivity(i);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void emailDatabase() {
		FileChannel source = null;
		FileChannel destination = null;

		Log.i("Debug", mDbHelper.getPath());

		final File sdcard = Environment.getExternalStorageDirectory();
		File sourceFile = new File(
				"/data/data/iam.applications/databases/thedatabase");
		File destFile = new File(sdcard, "temp/thedatabase");

		try {
			source = new FileInputStream(sourceFile).getChannel();
			destination = new FileOutputStream(destFile).getChannel();
			destination.transferFrom(source, 0, source.size());
		} catch (Exception e) {
			try {
				e.printStackTrace();

				if (source != null) {
					source.close();
				}
				if (destination != null) {
					destination.close();
				}
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}

		Intent sendIntent = new Intent(Intent.ACTION_SEND);
		sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		sendIntent.setType("application/x-sqlite3");
		sendIntent.putExtra(Intent.EXTRA_SUBJECT, "24/7 App Database");
		sendIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(destFile));
		sendIntent.putExtra(Intent.EXTRA_TEXT,
				"As of: " + Time.prettyDateTime(new Date()));
		startActivity(Intent.createChooser(sendIntent, "Email:"));
	}

	private void uploadDatabase() {
		AlertDialog.Builder alert = new AlertDialog.Builder(HomeActivity.this);
		alert.setTitle(R.string.upload_database);
		alert.setMessage(R.string.upload_database_instructions);
		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int whichButton) {
				FileChannel source = null;
				FileChannel destination = null;

				final File sdcard = Environment.getExternalStorageDirectory();
				File sourceFile = new File(sdcard, "temp/thedatabase");
				File destFile = new File(
						"/data/data/iam.applications/databases/thedatabase");

				try {
					source = new FileInputStream(sourceFile).getChannel();
					destination = new FileOutputStream(destFile).getChannel();
					destination.transferFrom(source, 0, source.size());
				} catch (Exception e) {
					try {
						e.printStackTrace();

						if (source != null) {
							source.close();
						}
						if (destination != null) {
							destination.close();
						}
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}

				fillData();
			}
		});
		alert.setNegativeButton("Cancel", null);
		alert.show();
	}

	private void resetDatabase() {
		AlertDialog.Builder alert = new AlertDialog.Builder(HomeActivity.this);
		alert.setTitle(R.string.reset_database);
		alert.setMessage(R.string.reset_database_first_warning);
		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int whichButton) {

				AlertDialog.Builder alert2 = new AlertDialog.Builder(
						HomeActivity.this);
				alert2.setTitle(R.string.reset_database);
				alert2.setMessage(R.string.reset_database_second_warning);
				alert2.setPositiveButton("Ok",
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int whichButton) {

								mDbHelper.deleteAll();
								fillData();

							}
						});
				alert2.setNegativeButton("Cancel", null);
				alert2.show();
			}
		});
		alert.setNegativeButton("Cancel", null);
		alert.show();
	}

	private void clearDataBeforeOneWeek() {
		mDbHelper.deleteDataBeforeOneWeek();
	}

	private void clearLogBeforeOneWeek() {
		mDbHelper.deleteLogBeforeOneWeek();
	}

	private void blockNumber() {
		AlertDialog.Builder alert;
		final EditText editinput;
		alert = new AlertDialog.Builder(HomeActivity.this);
		editinput = new EditText(HomeActivity.this);
		alert.setView(editinput);
		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int whichButton) {
				String value = editinput.getText().toString();
				if (value.length() > 0) {
					mDbHelper.setNumberIsBlocked(value, true);
					fillData();
				}
			}
		});
		alert.setNegativeButton("Cancel", null);
		alert.show();
	}

	private void unblockNumber() {
		Cursor c = mDbHelper.fetchBlockedNumbers();
		if (c.getCount() == 0) {
			Toast toast = Toast.makeText(this,
					getString(R.string.no_blocked_numbers), Toast.LENGTH_SHORT);
			toast.show();
			return;
		}

		AlertDialog.Builder alert;
		alert = new AlertDialog.Builder(HomeActivity.this);

		final Spinner spinnerinput = new Spinner(HomeActivity.this);
		String[] from = new String[] { DbAdapter.KEY_NUMBER };
		int[] to = new int[] { android.R.id.text1 };
		SimpleCursorAdapter adapter = new SimpleCursorAdapter(this,
				android.R.layout.simple_spinner_item, c, from, to);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinnerinput.setAdapter(adapter);

		alert.setView(spinnerinput);
		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int whichButton) {
				long number_id = spinnerinput.getSelectedItemId();
				mDbHelper.deleteBlockedNumber(number_id);
				fillData();
			}
		});
		alert.setNegativeButton("Cancel", null);
		alert.show();
	}

	static public boolean getThisIsAllowed(Context context) {
		SharedPreferences settings = context.getSharedPreferences(
				HomeActivity.PREFERENCES, 0);
		return settings
				.getBoolean(HomeActivity.PREFERENCES_PERMIT_THISIS, true);
	}

	private void setThisIsAllowed(boolean allowed) {
		SharedPreferences settings = getSharedPreferences(
				HomeActivity.PREFERENCES, 0);

		SharedPreferences.Editor editor = settings.edit();
		editor.putBoolean(HomeActivity.PREFERENCES_PERMIT_THISIS, allowed);
		editor.commit();
	}

	private void setMissedCallDelay() {
		SharedPreferences settings = getSharedPreferences(
				HomeActivity.PREFERENCES, 0);
		long delay = settings.getLong(
				HomeActivity.PREFERENCES_MISSED_CALL_DELAY, 5000);

		AlertDialog.Builder alert;
		final EditText editinput;
		alert = new AlertDialog.Builder(HomeActivity.this);
		editinput = new EditText(HomeActivity.this);
		editinput.setText(String.valueOf(delay));
		alert.setView(editinput);
		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int whichButton) {
				long newdelay = -1;
				try {
					newdelay = Long.valueOf(editinput.getText().toString());
				} catch (Exception e) {
				}
				if (newdelay > 0) {
					SharedPreferences settings = getSharedPreferences(
							HomeActivity.PREFERENCES, 0);
					SharedPreferences.Editor editor = settings.edit();
					editor.putLong(HomeActivity.PREFERENCES_MISSED_CALL_DELAY,
							newdelay);
					editor.commit();
				}
			}
		});
		alert.setNegativeButton("Cancel", null);
		alert.show();
	}
}
