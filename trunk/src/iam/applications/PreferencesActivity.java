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
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

/**
 * @author Adam
 * 
 */
public class PreferencesActivity extends PreferenceActivity {

	/** The database interface. */
	private transient DbAdapter mDbHelper;

	final private static String DATABASE_SD_PATH = "bluetooth/thedatabase";
	final private static String PREFERENCES_SD_PATH = "bluetooth/preferences";

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
						sendDatabase();
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
						getDatabaseFromSD();
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

		final Preference synchronizeSend = findPreference("synchronize_send");
		synchronizeSend
				.setOnPreferenceClickListener(new OnPreferenceClickListener() {
					@Override
					public boolean onPreferenceClick(final Preference preference) {
						synchronizeSend();
						return false;
					}
				});

		final Preference synchronizeReceive = findPreference("synchronize_receive");
		synchronizeReceive
				.setOnPreferenceClickListener(new OnPreferenceClickListener() {
					@Override
					public boolean onPreferenceClick(final Preference preference) {
						synchronizeReceive();
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
		// TODO replace this with an something like this snippet:
		// SharedPreferences prefs =
		// PreferenceManager.getDefaultSharedPreferences(this);
		// // Instance field for listener
		// listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
		// public void onSharedPreferenceChanged(SharedPreferences prefs, String
		// key) {
		// // Your Implementation
		// }
		// };
		// prefs.registerOnSharedPreferenceChangeListener(listener);

		mDbHelper.addCallarounds();
		AlarmReceiver.sendRefreshAlert(this);

		mDbHelper.close();
	}

	/**
	 * Warns the user, and then copies the file at DATABASE_SD_PATH on the SD
	 * card to replace the current database file.
	 */
	private void getDatabaseFromSD() {
		final AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle(R.string.upload_database);
		alert.setMessage(R.string.upload_database_instructions);
		alert.setPositiveButton(getString(R.string.ok),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(final DialogInterface dialog,
							final int whichButton) {
						getDatabaseFromSD(DATABASE_SD_PATH);
					}
				});
		alert.setNegativeButton(getString(R.string.cancel), null);
		alert.show();
	}

	/**
	 * Copy file from card.
	 * 
	 * @param path
	 *            the path
	 */
	private void getDatabaseFromSD(final String path) {
		FileChannel source = null;
		FileChannel destination = null;

		final File sdcard = Environment.getExternalStorageDirectory();
		final File sourceFile = new File(sdcard, path);
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

	private void getPreferencesFromSD(String path) {
		final SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(this);
		SharedPreferences.Editor editor = settings.edit();

		// wipe away existing preferences
		editor.clear();

		final File sdcard = Environment.getExternalStorageDirectory();
		final File sourceFile = new File(sdcard, path);

		FileInputStream fis;
		try {
			fis = new FileInputStream(sourceFile);
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
			return;
		}
		try {
			StringBuffer buf = new StringBuffer();
			int ch;
			while ((ch = fis.read()) != -1) {
				buf.append((char) ch);
			}
			fis.close();

			String preferenceString = buf.toString();
			String prefs[] = preferenceString.replaceFirst("\\{", "").split(
					"[{},=\\s]+");

			for (int i = 0; i < (prefs.length - 1); i += 2) {
				String key = prefs[i];
				String value = prefs[i + 1];

				if ("true".equals(value) || "false".equals(value)) {
					editor.putBoolean(key, Boolean.parseBoolean(value));
				} else {
					editor.putString(key, value);
				}
			}

			editor.commit();

		} catch (IOException e) {
			e.printStackTrace();
			if (fis != null) {
				try {
					fis.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		}

	}

	/**
	 * Warns the user twice, and then calls DbAdapter.deleteAll() to clear all
	 * data from the database.
	 */
	private void resetDatabase() {
		final AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle(R.string.reset_database);
		alert.setMessage(R.string.reset_database_first_warning);
		alert.setPositiveButton(getString(R.string.ok),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(final DialogInterface dialog,
							final int whichButton) {

						final AlertDialog.Builder alert2 = new AlertDialog.Builder(
								PreferencesActivity.this);
						alert2.setTitle(R.string.reset_database);
						alert2.setMessage(R.string.reset_database_second_warning);
						alert2.setPositiveButton(getString(R.string.ok),
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(
											final DialogInterface dialog,
											final int whichButton) {

										mDbHelper.deleteAll();
									}
								});

						alert2.setNegativeButton(getString(R.string.cancel),
								null);
						alert2.show();
					}
				});
		alert.setNegativeButton(getString(R.string.cancel), null);
		alert.show();
	}

	/**
	 * Copies the database to DATABASE_SD_PATH on the SD card, and then sends
	 * the file. This may be with email or Bluetooth. (The two steps are
	 * required because the database would not otherwise be accessible to be
	 * sent.)
	 */
	private void sendDatabase() {
		final File destFile = saveDatabaseToSD(DATABASE_SD_PATH);

		final Intent sendIntent = new Intent(Intent.ACTION_SEND);
		sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		sendIntent.setType("application/x-sqlite3");
		sendIntent.putExtra(Intent.EXTRA_SUBJECT, "24/7 App Database");
		sendIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(destFile));
		sendIntent.putExtra(Intent.EXTRA_TEXT,
				"As of: " + Time.prettyDateTime(new Date()));
		startActivity(Intent.createChooser(sendIntent, "Send via"));
	}

	/**
	 * Saves the database file to the SD card
	 * 
	 * @return File object for the saved file
	 */
	private File saveDatabaseToSD(final String path) {
		FileChannel source = null;
		FileChannel destination = null;

		final File sdcard = Environment.getExternalStorageDirectory();
		final File sourceFile = new File(mDbHelper.getPath());
		final File destFile = new File(sdcard, path);

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
		return destFile;
	}

	/**
	 * Serialize preferences to sd.
	 * 
	 * @param path
	 *            the path
	 */
	private File savePreferencesToSD(final String path) {
		final SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(this);
		Map<String, ?> preferences = settings.getAll();

		final File sdcard = Environment.getExternalStorageDirectory();
		final File destFile = new File(sdcard, path);

		FileOutputStream fos;
		try {
			fos = new FileOutputStream(destFile);
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
			return null;
		}
		try {
			fos.write(preferences.toString().getBytes("UTF-8"));
			fos.close();
		} catch (IOException e) {
			e.printStackTrace();
			try {
				fos.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}

		return destFile;
	}

	private void synchronizeReceive() {
		final AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle(R.string.synchronize_receive);
		alert.setMessage(R.string.synchronize_receive_warning);
		alert.setPositiveButton(getString(R.string.ok),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(final DialogInterface dialog,
							final int whichButton) {
						getPreferencesFromSD(PREFERENCES_SD_PATH);
						getDatabaseFromSD(DATABASE_SD_PATH);
						AlarmReceiver.resetAlarms(PreferencesActivity.this);

						// this is a cheaty thing, because the preference
						// activity doesn't automatically refresh, so the
						// checkboxes don't actually show the right values
						final Intent intent = new Intent(
								PreferencesActivity.this, HomeActivity.class);
						startActivity(intent);
					}
				});
		alert.setNegativeButton(getString(R.string.cancel), null);
		alert.show();
	}

	/**
	 * This function is called by the user interface, requesting that the
	 * application send its information to another device.
	 */
	private void synchronizeSend() {
		final File databaseFile = saveDatabaseToSD(DATABASE_SD_PATH);
		final File preferencesFile = savePreferencesToSD(PREFERENCES_SD_PATH);

		ArrayList<Uri> uris = new ArrayList<Uri>();
		uris.add(Uri.fromFile(databaseFile));
		uris.add(Uri.fromFile(preferencesFile));

		final Intent sendIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
		sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		sendIntent.setType("*/*");
		sendIntent.putExtra(Intent.EXTRA_SUBJECT, "24/7 App Database");
		sendIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
		sendIntent.putExtra(Intent.EXTRA_TEXT,
				"As of: " + Time.prettyDateTime(new Date()));
		startActivity(Intent.createChooser(sendIntent, "Send via"));
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
