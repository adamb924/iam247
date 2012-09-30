/**
 * 
 */
package iam.applications;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Date;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.widget.Toast;

/**
 * @author Adam
 * 
 */
/**
 * @author Adam
 * 
 */
public class PreferencesActivity extends PreferenceActivity {

	/** The database interface. */
	private transient DbAdapter mDbHelper;

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.preference.PreferenceActivity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(final Bundle bundle) {
		super.onCreate(bundle);

		mDbHelper = new DbAdapter(this);
		mDbHelper.open();

		addPreferencesFromResource(R.xml.preferences);

		final Preference emailDatabase = findPreference("email_database");
		emailDatabase
				.setOnPreferenceClickListener(new OnPreferenceClickListener() {
					@Override
					public boolean onPreferenceClick(final Preference preference) {
						emailDatabase();
						return false;
					}
				});

		final Preference resetDatabase = findPreference("reset_database");
		resetDatabase
				.setOnPreferenceClickListener(new OnPreferenceClickListener() {
					@Override
					public boolean onPreferenceClick(final Preference preference) {
						resetDatabase();
						return false;
					}
				});

		final Preference uploadDatabase = findPreference("upload_database");
		uploadDatabase
				.setOnPreferenceClickListener(new OnPreferenceClickListener() {
					@Override
					public boolean onPreferenceClick(final Preference preference) {
						uploadDatabase();
						return false;
					}
				});

		final Preference clearWeekDatabase = findPreference("clear_oneweek");
		clearWeekDatabase
				.setOnPreferenceClickListener(new OnPreferenceClickListener() {
					@Override
					public boolean onPreferenceClick(final Preference preference) {
						mDbHelper.deleteDataBeforeOneWeek();
						return false;
					}
				});
		final Preference clearLogDatabase = findPreference("clear_log");
		clearLogDatabase
				.setOnPreferenceClickListener(new OnPreferenceClickListener() {
					@Override
					public boolean onPreferenceClick(final Preference preference) {
						mDbHelper.deleteLogBeforeOneWeek();
						return false;
					}
				});
		final Preference unblockNumber = findPreference("unblock_number");
		unblockNumber
				.setOnPreferenceClickListener(new OnPreferenceClickListener() {
					@Override
					public boolean onPreferenceClick(final Preference preference) {
						unblockNumber();
						return false;
					}
				});
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.preference.PreferenceActivity#onDestroy()
	 */
	@Override
	protected void onDestroy() {
		super.onDestroy();

		// I'm at a loss how else to make sure this gets called
		mDbHelper.addCallarounds();
		AlarmReceiver.sendRefreshAlert(this);

		mDbHelper.close();
	}

	/**
	 * Copies the database to "/temp/thedatabase" on the SD card, and then
	 * attaches this file to a new email. (The two steps are required because
	 * the database would not otherwise be accessible to the mail program.)
	 */
	private void emailDatabase() {
		FileChannel source = null;
		FileChannel destination = null;

		final File sdcard = Environment.getExternalStorageDirectory();
		final File sourceFile = new File(mDbHelper.getPath());
		final File destFile = new File(sdcard, "temp/thedatabase");

		try {
			source = new FileInputStream(sourceFile).getChannel();
			destination = new FileOutputStream(destFile).getChannel();
			destination.transferFrom(source, 0, source.size());
		} catch (FileNotFoundException e) {
			try {
				Log.i(HomeActivity.TAG, Log.getStackTraceString(e));

				if (source != null) {
					source.close();
				}
				if (destination != null) {
					destination.close();
				}
			} catch (IOException e1) {
				Log.i(HomeActivity.TAG, Log.getStackTraceString(e1));
			}
		} catch (IOException e) {
			Log.i(HomeActivity.TAG, Log.getStackTraceString(e));
		}

		final Intent sendIntent = new Intent(Intent.ACTION_SEND);
		sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		sendIntent.setType("application/x-sqlite3");
		sendIntent.putExtra(Intent.EXTRA_SUBJECT, "24/7 App Database");
		sendIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(destFile));
		sendIntent.putExtra(Intent.EXTRA_TEXT,
				"As of: " + Time.prettyDateTime(new Date()));
		startActivity(Intent.createChooser(sendIntent, "Email:"));
	}

	/**
	 * Warns the user, and then copies the file at "/temp/thedatabase" on the SD
	 * card to replace the current database file.
	 */
	private void uploadDatabase() {
		final AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle(R.string.upload_database);
		alert.setMessage(R.string.upload_database_instructions);
		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(final DialogInterface dialog,
					final int whichButton) {
				FileChannel source = null;
				FileChannel destination = null;

				final File sdcard = Environment.getExternalStorageDirectory();
				final File sourceFile = new File(sdcard, "temp/thedatabase");
				final File destFile = new File(mDbHelper.getPath());

				try {
					source = new FileInputStream(sourceFile).getChannel();
					destination = new FileOutputStream(destFile).getChannel();
					destination.transferFrom(source, 0, source.size());
				} catch (FileNotFoundException e) {
					try {
						Log.i(HomeActivity.TAG, Log.getStackTraceString(e));

						if (source != null) {
							source.close();
						}
						if (destination != null) {
							destination.close();
						}
					} catch (IOException e1) {
						Log.i(HomeActivity.TAG, Log.getStackTraceString(e1));
					}
				} catch (IOException e) {
					Log.i(HomeActivity.TAG, Log.getStackTraceString(e));
				}
			}
		});
		alert.setNegativeButton("Cancel", null);
		alert.show();
	}

	/**
	 * Warns the user twice, and then calls DbAdapter.deleteAll() to clear all
	 * data from the database.
	 */
	private void resetDatabase() {
		final AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle(R.string.reset_database);
		alert.setMessage(R.string.reset_database_first_warning);
		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(final DialogInterface dialog,
					final int whichButton) {

				final AlertDialog.Builder alert2 = new AlertDialog.Builder(
						PreferencesActivity.this);
				alert2.setTitle(R.string.reset_database);
				alert2.setMessage(R.string.reset_database_second_warning);
				alert2.setPositiveButton("Ok",
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(final DialogInterface dialog,
									final int whichButton) {

								mDbHelper.deleteAll();
							}
						});

				alert2.setNegativeButton("Cancel", null);
				alert2.show();
			}
		});
		alert.setNegativeButton("Cancel", null);
		alert.show();
	}

	private void unblockNumber() {
		final Cursor cur = mDbHelper.fetchBlockedNumbers();
		startManagingCursor(cur);
		if (cur.getCount() == 0) {
			final Toast toast = Toast.makeText(this,
					getString(R.string.no_blocked_numbers), Toast.LENGTH_SHORT);
			toast.show();
			return;
		}

		final CharSequence[] entries = new CharSequence[cur.getCount()];

		if (cur.getCount() > 0) {
			cur.moveToFirst();
			int position = 0;
			do {
				entries[position] = cur.getString(1);
				++position;
			} while (cur.moveToNext());
		}

		final AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle(getString(R.string.unblock_number));
		alert.setItems(entries, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(final DialogInterface dialog, final int item) {
				mDbHelper.setNumberIsBlocked((String) entries[item], false);
			}
		});
		alert.show();
	}
}
