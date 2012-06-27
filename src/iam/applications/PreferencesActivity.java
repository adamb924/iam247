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

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;

/**
 * @author Adam
 * 
 */
public class PreferencesActivity extends PreferenceActivity {

	/** The database interface. */
	private DbAdapter mDbHelper;

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.preference.PreferenceActivity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mDbHelper = new DbAdapter(this);
		mDbHelper.open();

		addPreferencesFromResource(R.xml.preferences);

		Preference emailDatabase = findPreference("email_database");
		emailDatabase
				.setOnPreferenceClickListener(new OnPreferenceClickListener() {
					@Override
					public boolean onPreferenceClick(Preference preference) {
						emailDatabase();
						return false;
					}
				});

		Preference resetDatabase = findPreference("reset_database");
		resetDatabase
				.setOnPreferenceClickListener(new OnPreferenceClickListener() {
					@Override
					public boolean onPreferenceClick(Preference preference) {
						resetDatabase();
						return false;
					}
				});

		Preference uploadDatabase = findPreference("upload_database");
		uploadDatabase
				.setOnPreferenceClickListener(new OnPreferenceClickListener() {
					@Override
					public boolean onPreferenceClick(Preference preference) {
						uploadDatabase();
						return false;
					}
				});

		Preference clearOneWeekDatabase = findPreference("clear_oneweek");
		clearOneWeekDatabase
				.setOnPreferenceClickListener(new OnPreferenceClickListener() {
					@Override
					public boolean onPreferenceClick(Preference preference) {
						mDbHelper.deleteDataBeforeOneWeek();
						return false;
					}
				});
		Preference clearLogDatabase = findPreference("clear_log");
		clearLogDatabase
				.setOnPreferenceClickListener(new OnPreferenceClickListener() {
					@Override
					public boolean onPreferenceClick(Preference preference) {
						mDbHelper.deleteLogBeforeOneWeek();
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
		mDbHelper.close();
	}

	private void emailDatabase() {
		FileChannel source = null;
		FileChannel destination = null;

		final File sdcard = Environment.getExternalStorageDirectory();
		File sourceFile = new File(mDbHelper.getPath());
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
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle(R.string.upload_database);
		alert.setMessage(R.string.upload_database_instructions);
		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int whichButton) {
				FileChannel source = null;
				FileChannel destination = null;

				final File sdcard = Environment.getExternalStorageDirectory();
				File sourceFile = new File(sdcard, "temp/thedatabase");
				File destFile = new File(mDbHelper.getPath());

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
			}
		});
		alert.setNegativeButton("Cancel", null);
		alert.show();
	}

	private void resetDatabase() {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle(R.string.reset_database);
		alert.setMessage(R.string.reset_database_first_warning);
		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int whichButton) {

				AlertDialog.Builder alert2 = new AlertDialog.Builder(
						PreferencesActivity.this);
				alert2.setTitle(R.string.reset_database);
				alert2.setMessage(R.string.reset_database_second_warning);
				alert2.setPositiveButton("Ok",
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int whichButton) {

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
}
