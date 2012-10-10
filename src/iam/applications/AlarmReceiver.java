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
		final boolean disabled = settings.getBoolean(
				HomeActivity.PREFERENCES_DISABLE_247, false);
		if (disabled) {
			return;
		}

		mDbHelper = new DbAdapter(mContext);
		mDbHelper.open();

		final int request_id = intent.getIntExtra(DbAdapter.KEY_REQUESTID, -1);
		if (request_id != -1) {
			if (mDbHelper.deleteAlarm(request_id) > 0) {
				// checking to see if the alarm is in the database is not the
				// most elegant solution, but neither is the Android alarm
				// documentation very effective

				// call various functions depending on the action of the intent.
				// the action will have been set in one of the static member
				// functions of this class.
				final String action = intent.getAction();
				if (action.equals(AlarmAdapter.ALERT_CHECKIN_DUE)) {
					checkinDue();
				} else if (action.equals(AlarmAdapter.ALERT_ADD_CALLAROUNDS)) {
					mDbHelper.addCallarounds();
					HomeActivity.sendRefreshAlert(mContext);
				} else if (action.equals(AlarmAdapter.ALERT_CHECKIN_REMINDER)) {
					checkCheckinReminder(intent);
				} else if (action.equals(AlarmAdapter.ALERT_CALLAROUND_ALARM)) {
					checkCallaroundDue();
				} else if (action.equals(AlarmAdapter.ALERT_CALLAROUND_DUE)) {
					sendCallaroundReminders();
				} else if (action
						.equals(AlarmAdapter.ALERT_DELAYED_CALLAROUND_DUE)) {
					checkDelayedCallaroundDue();
				} else if (action.equals(AlarmAdapter.ALERT_ADD_GUARD_CHECKINS)) {
					AlarmAdapter.addGuardCheckins(mContext);
				} else if (action.equals(AlarmAdapter.ALERT_GUARD_CHECKIN)) {
					requestGuardCheckin(intent.getLongExtra(
							DbAdapter.KEY_HOUSEID, -1));
				} else if (action
						.equals(AlarmAdapter.ALERT_RESET_GUARD_SCHEDULE)) {
					mDbHelper.resetGuardSchedule();
				}
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
		if (mDbHelper.getNumberOfDueCallarounds() > 0) {
			mDbHelper.close();

			final Intent intent = new Intent(mContext,
					CallAroundDetailList.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			intent.putExtra(DbAdapter.KEY_DUEBY, Time.iso8601Date());
			intent.putExtra(AlarmAdapter.ALERT_CALLAROUND_DUE,
					AlarmAdapter.ALERT_CALLAROUND_DUE);
			mContext.startActivity(intent);
		}
	}

	/**
	 * Checks if the check-in specified in the intent parameter is still
	 * outstanding, and if so, sends the reminder text message.
	 * 
	 * @param intent
	 *            An intent object with the extra ALERT_CHECKIN_REMINDER, which
	 *            has the _id of the checkin.
	 */
	private void checkCheckinReminder(final Intent intent) {
		final long checkinId = intent.getLongExtra(
				AlarmAdapter.ALERT_CHECKIN_REMINDER, -1);
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
		if (mDbHelper.getNumberOfDueCallaroundsIncludingDelayed() > 0) {
			mDbHelper.close();

			final Intent intent = new Intent(mContext,
					CallAroundDetailList.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			intent.putExtra(DbAdapter.KEY_DUEBY, Time.iso8601Date());
			intent.putExtra(DbAdapter.KEY_DELAYED, DbAdapter.KEY_DELAYED);
			intent.putExtra(AlarmAdapter.ALERT_CALLAROUND_DUE,
					AlarmAdapter.ALERT_CALLAROUND_DUE);
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
			intent.putExtra(AlarmAdapter.ALERT_CHECKIN_DUE, true);
			mContext.startActivity(intent);
		}
	}

	/**
	 * Send the specified guard a message requesting a checkin.
	 * 
	 * @param guard_id
	 */
	private void requestGuardCheckin(final long house_id) {
		if (house_id == -1) {
			return;
		}
		final long guard_id = mDbHelper.getGuardForHouse(house_id);

		if (guard_id == -1) {
			mDbHelper
					.addLogEvent(DbAdapter.LOG_TYPE_SMS_ERROR, String.format(
							mContext.getString(R.string.log_null_guard),
							String.valueOf(house_id),
							mDbHelper.getHouseName(house_id)));
			return;
		}
		final String number = mDbHelper.getGuardNumber(guard_id);
		if (number == null) {
			mDbHelper
					.addLogEvent(DbAdapter.LOG_TYPE_SMS_ERROR, String.format(
							mContext.getString(R.string.log_null_number),
							String.valueOf(guard_id),
							mDbHelper.getGuardName(guard_id)));
			return;
		}

		SmsHandler.sendSms(mContext, number,
				mContext.getString(R.string.sms_guard_checkin));
		mDbHelper.addGuardCheckin(guard_id, Time.iso8601DateTime());
	}

	/**
	 * Checks if there are due call arounds, and if so, sends people a reminder
	 * text.
	 */
	private void sendCallaroundReminders() {
		Cursor cur = mDbHelper.fetchMissedCallaroundNumbers();
		if (cur.moveToFirst()) {
			do {
				String number = cur.getString(cur
						.getColumnIndex(DbAdapter.KEY_NUMBER));
				SmsHandler.sendSms(mContext, number,
						mContext.getString(R.string.sms_callaround_reminder));
			} while (cur.moveToNext());
		}
	}
}
