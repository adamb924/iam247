package iam.applications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.PreferenceManager;

/**
 * Alarm receiver receives all alarms from the system, and starts the
 * appropriate corresponding activities in response.
 */
public class AlarmReceiver extends BroadcastReceiver {

	/** The database interface */
	private transient DbAdapter mDbHelper;

	/** The context */
	private transient Context mContext;

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.content.BroadcastReceiver#onReceive(android.content.Context,
	 * android.content.Intent)
	 */
	@Override
	public void onReceive(final Context context, final Intent intent) {
		mContext = context;

		final SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(mContext);
		final boolean disabled = settings.getBoolean(Preferences.DISABLE_247,
				false);
		if (disabled) {
			return;
		}

		mDbHelper = new DbAdapter(mContext);
		mDbHelper.open();

		final int request_id = intent.getIntExtra(DbAdapter.Columns.REQUESTID,
				-1);

		final String action = intent.getAction();

		if (mDbHelper.getAlarmExists(request_id)) {
			// checking to see if the alarm is in the database is not the
			// most elegant solution, but neither is the Android alarm
			// documentation very effective

			if (!AlarmAdapter.Alerts.REPEATING.contains(action)) {
				mDbHelper.deleteAlarm(request_id);
			}

			// call various functions depending on the action of the intent.
			// the action will have been set in one of the static member
			// functions of this class.
			if (action.equals(AlarmAdapter.Alerts.CHECKIN_DUE)) {
				checkinDue();
			} else if (action.equals(AlarmAdapter.Alerts.ADD_CALLAROUNDS)) {
				mDbHelper.addCallarounds();
				HomeActivity.sendRefreshAlert(mContext);
			} else if (action.equals(AlarmAdapter.Alerts.CHECKIN_REMINDER)) {
				checkCheckinReminder(intent);
			} else if (action.equals(AlarmAdapter.Alerts.CALLAROUND_ALARM)) {
				checkCallaroundDue();
			} else if (action.equals(AlarmAdapter.Alerts.CALLAROUND_DUE)) {
				sendCallaroundReminders();
			} else if (action
					.equals(AlarmAdapter.Alerts.DELAYED_CALLAROUND_DUE)) {
				checkDelayedCallaroundDue();
			} else if (action.equals(AlarmAdapter.Alerts.ADD_GUARD_CHECKINS)) {
				mDbHelper.addLogEvent(DbAdapter.LogTypes.DEBUG,
						"Adding the guard check-ins");
				AlarmAdapter.addGuardCheckins(mContext);
			} else if (action.equals(AlarmAdapter.Alerts.GUARD_CHECKIN)) {
				requestGuardCheckin(mContext, mDbHelper,
						intent.getLongExtra(DbAdapter.Columns.HOUSEID, -1));
			} else if (action.equals(AlarmAdapter.Alerts.RESET_GUARD_SCHEDULE)) {
				mDbHelper.resetGuardSchedule();
			}
		}

		mDbHelper.close();
	}

	/**
	 * Checks if there are due call arounds, and if so, starts a
	 * <code>CallAroundDetailList</code> activity with a flag to do the audio
	 * alert.
	 */
	private void checkCallaroundDue() {
		if (mDbHelper.getNumberOfDueCallaroundsNoDelayed() > 0) {
			mDbHelper.close();

			final Intent intent = new Intent(mContext,
					CallAroundDetailList.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // this is necessary
															// because the
															// activity is being
															// started from a
															// receiver and not
															// from an activity
			intent.putExtra(DbAdapter.Columns.DUEBY, Time.iso8601Date());
			intent.putExtra(AlarmAdapter.Alerts.CALLAROUND_DUE,
					AlarmAdapter.Alerts.CALLAROUND_DUE);
			mContext.startActivity(intent);
		}
	}

	/**
	 * Checks if the check-in specified in the intent parameter is still
	 * outstanding, and if so, sends the reminder text message.
	 * 
	 * @param intent
	 *            An intent object with the extra Alerts.ALERT_CHECKIN_REMINDER,
	 *            which has the _id of the checkin.
	 */
	private void checkCheckinReminder(final Intent intent) {
		final long checkinId = intent.getLongExtra(
				AlarmAdapter.Alerts.CHECKIN_REMINDER, -1);
		if (mDbHelper.getCheckinOutstanding(checkinId)) {
			SmsHandler.sendSms(mContext,
					mDbHelper.getNumberForCheckin(checkinId),
					mContext.getString(R.string.sms_checkin_reminder));
		}
	}

	/**
	 * Checks if there are due delayed call arounds, and if so, starts a
	 * <code>CallAroundDetailList</code> activity with a flag to do the audio
	 * alert.
	 */
	private void checkDelayedCallaroundDue() {
		if (mDbHelper.getNumberOfDueCallarounds() > 0) {
			mDbHelper.close();

			final Intent intent = new Intent(mContext,
					CallAroundDetailList.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			intent.putExtra(DbAdapter.Columns.DUEBY, Time.iso8601Date());
			intent.putExtra(DbAdapter.Columns.DELAYED,
					DbAdapter.Columns.DELAYED);
			intent.putExtra(AlarmAdapter.Alerts.CALLAROUND_DUE,
					AlarmAdapter.Alerts.CALLAROUND_DUE);
			mContext.startActivity(intent);
		}
	}

	/**
	 * Checks if there is a check-in due, and if so, starts the
	 * <code>CheckinList</code> activity with a flag to sound the audio alert.
	 */
	private void checkinDue() {
		if (mDbHelper.getNumberOfDueCheckins() > 0) {
			mDbHelper.close();
			final Intent intent = new Intent(mContext, CheckinList.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			intent.putExtra(AlarmAdapter.Alerts.CHECKIN_DUE, true);
			mContext.startActivity(intent);
		}
	}

	/**
	 * Send the specified guard a message requesting a check-in.
	 * 
	 * @param context
	 *            the context
	 * @param db
	 *            the database
	 * @param house_id
	 *            the house_id for which the check-in should be requested
	 * @return the _id of the guard on duty
	 */
	static public long requestGuardCheckin(final Context context,
			final DbAdapter db, final long house_id) {
		long retVal = -1;
		if (house_id != -1) {
			final long guard_id = db.getCurrentGuardForHouse(house_id);

			if (guard_id != -1) {
				final String number = db.getGuardNumber(guard_id);
				if (number.isEmpty()) {
					db.addLogEvent(
							DbAdapter.LogTypes.SMS_ERROR,
							String.format(
									context.getString(R.string.log_null_number),
									String.valueOf(guard_id),
									db.getGuardName(guard_id)));
				} else {
					SmsHandler.sendSms(context, number,
							context.getString(R.string.sms_guard_checkin));
					db.addGuardCheckin(guard_id, Time.iso8601DateTime());

					retVal = guard_id;
				}
			}
		}
		return retVal;
	}

	/**
	 * Checks if there are due call arounds, and if so, sends people a reminder
	 * text.
	 */
	private void sendCallaroundReminders() {
		final Cursor cur = mDbHelper.fetchMissedCallaroundNumbers();
		if (cur.moveToFirst()) {
			do {
				final String number = cur.getString(cur
						.getColumnIndex(DbAdapter.Columns.NUMBER));
				SmsHandler.sendSms(mContext, number,
						mContext.getString(R.string.sms_callaround_reminder));
			} while (cur.moveToNext());
		}
		cur.close();
	}
}
