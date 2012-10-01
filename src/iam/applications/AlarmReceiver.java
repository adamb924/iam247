package iam.applications;

import java.util.Calendar;
import java.util.Date;
import java.util.Random;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * Alarm receiver receives all alarms from the system, and starts the
 * appropriate corresponding activities in response. It also has static methods
 * for creating alerts.
 */
/**
 * @author Adam
 * 
 */
public class AlarmReceiver extends BroadcastReceiver {

	/**
	 * An Intent action string alerting the application to check for overdue
	 * check-ins. Also used as an extra in the Intent to start the CheckinList
	 * activity.
	 */
	public static final String ALERT_CHECKIN_DUE = "ALERT_CHECKIN_DUE";

	/**
	 * An Intent action string alerting the application to check for overdue
	 * call arounds.
	 */
	public static final String ALERT_CALLAROUND_DUE = "ALERT_CALLAROUND_DUE";

	/**
	 * An Intent action string alerting the application to check for delayed
	 * call arounds.
	 */
	public static final String ALERT_DELAYED_CALLAROUND_DUE = "ALERT_DELAYED_CALLAROUND_DUE";

	/**
	 * An Intent action string alerting the application to add call arounds for
	 * the day
	 */
	public static final String ALERT_ADD_CALLAROUNDS = "ALERT_ADD_CALLAROUNDS";

	/**
	 * An Intent action string alerting the application send a message for
	 * activities to refresh their screens
	 */
	public static final String ALERT_REFRESH = "ALERT_REFRESH";

	/**
	 * An Intent action string alerting the application to remind the user about
	 * a check-in
	 */
	public static final String ALERT_CHECKIN_REMINDER = "ALERT_CHECKIN_REMINDER";

	/**
	 * An Intent action string alerting the application to add the guards'
	 * check-ins.
	 */
	public static final String ALERT_ADD_GUARD_CHECKINS = "ALERT_ADD_GUARD_CHECKINS";

	/**
	 * An Intent action string alerting the application to perform a guard
	 * check.
	 */
	public static final String ALERT_GUARD_CHECKIN = "ALERT_GUARD_CHECKIN";

	/**
	 * An Intent action string alerting the application set today's guard
	 * schedule to their typical schedule.
	 */
	public static final String ALERT_RESET_GUARD_SCHEDULE = "ALERT_RESET_GUARD_SCHEDULE";

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
			mDbHelper.deleteAlarm(request_id);
		}

		// call various functions depending on the action of the intent. the
		// action will have been set in one of the static member functions of
		// this class.
		final String action = intent.getAction();
		if (action.equals(ALERT_CHECKIN_DUE)) {
			checkinDue();
		} else if (action.equals(ALERT_ADD_CALLAROUNDS)) {
			addCallarounds();
		} else if (action.equals(ALERT_CHECKIN_REMINDER)) {
			checkCheckinReminder(intent);
		} else if (action.equals(ALERT_CALLAROUND_DUE)) {
			checkCallaroundDue();
		} else if (action.equals(ALERT_DELAYED_CALLAROUND_DUE)) {
			checkDelayedCallaroundDue();
		} else if (action.equals(ALERT_ADD_GUARD_CHECKINS)) {
			addGuardCheckins();
		} else if (action.equals(ALERT_GUARD_CHECKIN)) {
			requestGuardCheckin(intent.getLongExtra(DbAdapter.KEY_HOUSEID, -1));
		} else if (action.equals(ALERT_RESET_GUARD_SCHEDULE)) {
			mDbHelper.resetGuardSchedule();
		}

		mDbHelper.close();
	}

	/**
	 * Creates a checkin alarm for the given guard and checkin time.
	 * 
	 * @param house_id
	 * @param checkinTime
	 */

	private static void createGuardCheckin(final Context context,
			final long house_id, final Date checkinTime) {

		final int _id = (int) System.currentTimeMillis();
		final Intent intent = new Intent(context, AlarmReceiver.class);
		intent.setAction(AlarmReceiver.ALERT_GUARD_CHECKIN);
		intent.putExtra(DbAdapter.KEY_HOUSEID, house_id);
		intent.putExtra(DbAdapter.KEY_REQUESTID, _id);

		final PendingIntent sender = PendingIntent.getBroadcast(context, _id,
				intent, 0);

		final AlarmManager alarmManager = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);
		alarmManager
				.set(AlarmManager.RTC_WAKEUP, checkinTime.getTime(), sender);

		final DbAdapter dbHelper = new DbAdapter(context);
		dbHelper.open();
		dbHelper.addAlarm(_id, AlarmReceiver.ALERT_GUARD_CHECKIN);
		dbHelper.close();
	}

	/**
	 * Cancel all alarms of the given type.
	 * 
	 * @param context
	 * @param type
	 *            the type of alarm to remove
	 */
	static public void removeAlarmsByType(final Context context,
			final String type) {
		final DbAdapter dbHelper = new DbAdapter(context);
		dbHelper.open();
		final Cursor cur = dbHelper.fetchAlarmsForType(type);
		dbHelper.close();

		if (!cur.moveToFirst()) {
			return;
		}

		Intent toCancel;
		PendingIntent pendingToCancel;
		final AlarmManager alarmManager = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);
		do {
			final int _id = cur.getInt(0);
			toCancel = new Intent(type);
			pendingToCancel = PendingIntent.getBroadcast(context, _id,
					toCancel, PendingIntent.FLAG_CANCEL_CURRENT);

			alarmManager.cancel(pendingToCancel);
		} while (cur.moveToNext());
	}

	/**
	 * Send an alert that activities should refresh their screens. Currently
	 * this is called only in the constructor of SmsHandler. Any activity which
	 * has SMS-dependent information should update in response to this message.
	 * This method is not particularly needed here, but it included as it's a
	 * scheduling-related task.
	 * 
	 * @param context
	 *            the application context
	 */
	static public void sendRefreshAlert(final Context context) {
		final Intent intent = new Intent(AlarmReceiver.ALERT_REFRESH);
		context.sendBroadcast(intent);
	}

	/**
	 * Sets a recurring alarm to reset the guards' schedules to their typical
	 * schedule. This occurs at the end of the guards' check-in window (by
	 * default 6am).
	 * 
	 * @param context
	 *            the application context
	 */
	static public void setGuardScheduleResetAlarm(final Context context) {
		final SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(context);
		final String old = settings.getString(
				HomeActivity.PREFERENCES_GUARD_CHECKIN_END, "06:00");

		// make sure that the time for the alarm is in the future, so that these
		// events aren't really added every time you go to the home activity
		Date timeToAddAt = Time.todayAtGivenTime(old);
		if (timeToAddAt.before(new Date())) {
			timeToAddAt = Time.tomorrowAtGivenTime(old);
		}

		// removeAlarmsByType(context,
		// AlarmReceiver.ALERT_RESET_GUARD_SCHEDULE);

		final int _id = (int) System.currentTimeMillis();
		final Intent intent = new Intent(context, AlarmReceiver.class);
		intent.setAction(AlarmReceiver.ALERT_RESET_GUARD_SCHEDULE);
		intent.putExtra(DbAdapter.KEY_REQUESTID, _id);

		final PendingIntent sender = PendingIntent.getBroadcast(context, _id,
				intent, 0);

		final AlarmManager alarmManager = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);
		// TODO does this call to cancel work?
		alarmManager.cancel(sender);
		alarmManager.setRepeating(AlarmManager.RTC_WAKEUP,
				timeToAddAt.getTime(), AlarmManager.INTERVAL_DAY, sender);
	}

	/**
	 * Sets a recurring alarm, which when triggered will prompt the application
	 * to add all of the active houses to the day's call around list
	 * 
	 * @param context
	 *            the application context
	 */
	static public void setAddCallaroundAlarm(final Context context) {
		final SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(context);
		final String old = settings.getString(
				HomeActivity.PREFERENCES_CALLAROUND_ADD, "06:00");

		// make sure that the time for the alarm is in the future, so that these
		// events aren't really added every time you go to the home activity
		Date timeToAddAt = Time.todayAtGivenTime(old);
		if (timeToAddAt.before(new Date())) {
			timeToAddAt = Time.tomorrowAtGivenTime(old);
		}

		// removeAlarmsByType(context, AlarmReceiver.ALERT_ADD_CALLAROUNDS);

		final int _id = (int) System.currentTimeMillis();
		final Intent intent = new Intent(context, AlarmReceiver.class);
		intent.setAction(AlarmReceiver.ALERT_ADD_CALLAROUNDS);
		intent.putExtra(DbAdapter.KEY_REQUESTID, _id);

		final PendingIntent sender = PendingIntent.getBroadcast(context, _id,
				intent, 0);

		final AlarmManager alarmManager = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);
		// TODO does this call to cancel work?
		alarmManager.cancel(sender);
		alarmManager.setRepeating(AlarmManager.RTC_WAKEUP,
				timeToAddAt.getTime(), AlarmManager.INTERVAL_DAY, sender);

		// DbAdapter dbHelper = new DbAdapter(context);
		// dbHelper.open();
		// dbHelper.addAlarm(_id, AlarmReceiver.ALERT_ADD_CALLAROUNDS);
		// dbHelper.close();
	}

	/**
	 * Sets a recurring alarm, which when triggered will prompt the application
	 * to add random checks for the current guards.
	 * 
	 * @param context
	 */
	static public void setAddGuardCheckinAlarms(final Context context) {
		final SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(context);
		final String old = settings.getString(
				HomeActivity.PREFERENCES_GUARD_CHECKIN_START, "21:00");

		// make sure that the time for the alarm is in the future, so that these
		// events aren't really added every time you go to the home activity
		Date timeToAddAt = Time.todayAtGivenTime(old);
		if (timeToAddAt.before(new Date())) {
			timeToAddAt = Time.tomorrowAtGivenTime(old);
		}

		final int _id = (int) System.currentTimeMillis();
		final Intent intent = new Intent(context, AlarmReceiver.class);
		intent.setAction(AlarmReceiver.ALERT_ADD_GUARD_CHECKINS);
		intent.putExtra(DbAdapter.KEY_REQUESTID, _id);

		// removeAlarmsByType(context, AlarmReceiver.ALERT_ADD_GUARD_CHECKINS);

		final PendingIntent sender = PendingIntent.getBroadcast(context, _id,
				intent, 0);

		final AlarmManager alarmManager = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);
		// TODO does this call to cancel work?
		alarmManager.cancel(sender);
		alarmManager.setRepeating(AlarmManager.RTC_WAKEUP,
				timeToAddAt.getTime(), AlarmManager.INTERVAL_DAY, sender);

		// DbAdapter dbHelper = new DbAdapter(context);
		// dbHelper.open();
		// dbHelper.addAlarm(_id, AlarmReceiver.ALERT_ADD_GUARD_CHECKINS);
		// dbHelper.close();
	}

	/**
	 * Sets a call around due alarm at the specified date. This alarm will later
	 * be processed by the onReceive() method of this class.
	 * 
	 * @param context
	 *            the application context
	 * @param date
	 *            the date for the callaround due alarm
	 */
	static public void setCallaroundDueAlarm(final Context context,
			final Date date) {
		final Calendar cal = Calendar.getInstance();
		cal.setTime(date);

		final int _id = (int) System.currentTimeMillis();
		final Intent intent = new Intent(context, AlarmReceiver.class);
		intent.setAction(AlarmReceiver.ALERT_CALLAROUND_DUE);
		intent.putExtra(DbAdapter.KEY_REQUESTID, _id);

		removeAlarmsByType(context, AlarmReceiver.ALERT_CALLAROUND_DUE);

		final PendingIntent sender = PendingIntent.getBroadcast(context, _id,
				intent, 0);

		final AlarmManager alarmManager = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);
		alarmManager
				.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), sender);

		final DbAdapter dbHelper = new DbAdapter(context);
		dbHelper.open();
		dbHelper.addAlarm(_id, AlarmReceiver.ALERT_CALLAROUND_DUE);
		dbHelper.close();
	}

	/**
	 * Set an alert to check for overdue check-ins at a certain time
	 * 
	 * @param context
	 *            the application context
	 * @param date
	 *            the date/time at which to check for overdue check-ins
	 */
	static public void setCheckinAlert(final Context context, final Date date) {
		final Calendar cal = Calendar.getInstance();
		cal.setTime(date);

		final int _id = (int) System.currentTimeMillis();
		final Intent intent = new Intent(context, AlarmReceiver.class);
		intent.setAction(AlarmReceiver.ALERT_CHECKIN_DUE);
		intent.putExtra(DbAdapter.KEY_REQUESTID, _id);

		final PendingIntent sender = PendingIntent.getBroadcast(context, _id,
				intent, 0);

		final AlarmManager alarmManager = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);
		alarmManager
				.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), sender);

		final DbAdapter dbHelper = new DbAdapter(context);
		dbHelper.open();
		dbHelper.addAlarm(_id, AlarmReceiver.ALERT_CHECKIN_DUE);
		dbHelper.close();
	}

	/**
	 * Set an alert to remind the user about a due check-in
	 * 
	 * @param context
	 *            the application context
	 * @param checkin_id
	 *            the checkin for the user to be reminded about
	 */
	static public void setCheckinReminderAlert(final Context context,
			final long checkin_id) {
		final DbAdapter dbHelper = new DbAdapter(context);
		dbHelper.open();
		final String duetime = dbHelper.getCheckinTime(checkin_id);

		final Calendar cal = Calendar.getInstance();
		cal.setTime(Time.iso8601Date(duetime));

		// read the reminder offset from the system preferences, and calculate
		// the new time based on that offset
		final SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(context);
		final int offset = -1
				* settings.getInt(
						HomeActivity.PREFERENCES_CHECKIN_REMINDER_DELAY, 3);
		cal.add(Calendar.MINUTE, offset);

		final int _id = (int) System.currentTimeMillis();
		final Intent intent = new Intent(context, AlarmReceiver.class);
		intent.setAction(AlarmReceiver.ALERT_CHECKIN_REMINDER);
		intent.putExtra(AlarmReceiver.ALERT_CHECKIN_REMINDER, checkin_id);
		intent.putExtra(DbAdapter.KEY_REQUESTID, _id);

		final PendingIntent sender = PendingIntent.getBroadcast(context, _id,
				intent, 0);

		final AlarmManager alarmManager = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);
		alarmManager
				.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), sender);

		dbHelper.addAlarm(_id, AlarmReceiver.ALERT_CHECKIN_REMINDER);
		dbHelper.close();
	}

	/**
	 * Sets a delayed call around due alarm. This alarm will later be processed
	 * by the onReceive() method of this class.
	 * 
	 * @param context
	 *            the application context
	 * @param date
	 *            the date
	 */
	static public void setDelayedCallaroundAlarm(final Context context,
			final Date date) {
		final Calendar cal = Calendar.getInstance();
		cal.setTime(date);

		final int _id = (int) System.currentTimeMillis();
		final Intent intent = new Intent(context, AlarmReceiver.class);
		intent.setAction(ALERT_DELAYED_CALLAROUND_DUE);
		intent.putExtra(DbAdapter.KEY_REQUESTID, _id);

		final PendingIntent sender = PendingIntent.getBroadcast(context, _id,
				intent, 0);

		final AlarmManager alarmManager = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);
		alarmManager
				.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), sender);

		final DbAdapter dbHelper = new DbAdapter(context);
		dbHelper.open();
		dbHelper.addAlarm(_id, AlarmReceiver.ALERT_CHECKIN_DUE);
		dbHelper.close();
	}

	/** The database interface */
	private transient DbAdapter mDbHelper;

	private transient Context mContext;

	/**
	 * Calls DbAdapter.addCallarounds() to add the day's call arounds, and sends
	 * a signal for activities to refresh their data.
	 */
	private void addCallarounds() {
		mDbHelper.addCallarounds();
		sendRefreshAlert(mContext);
	}

	/**
	 * Adds guard checkins for today.
	 */
	private void addGuardCheckins() {

		// clear out old ones
		removeAlarmsByType(mContext, AlarmReceiver.ALERT_GUARD_CHECKIN);

		final SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(mContext);

		final int fewestCheckins = settings.getInt(
				HomeActivity.PREFERENCES_FEWEST_GUARD_CHECKS, 3);
		final int randomCheckins = settings.getInt(
				HomeActivity.PREFERENCES_RANDOM_GUARD_CHECKS, 3);

		final String startTimeString = settings.getString(
				HomeActivity.PREFERENCES_GUARD_CHECKIN_START, "22:00");
		final String endTimeString = settings.getString(
				HomeActivity.PREFERENCES_GUARD_CHECKIN_END, "06:00");

		final Date startTime = Time.todayAtGivenTime(startTimeString);
		final Date endTime = Time.tomorrowAtGivenTime(endTimeString);

		final Random random = new Random();

		// int ought to be at least 32-bits, which provides more way more than
		// 24 hours in milliseconds
		final int range = (int) (endTime.getTime() - startTime.getTime());

		final DbAdapter dbHelper = new DbAdapter(mContext);
		dbHelper.open();

		final Cursor cur = dbHelper.fetchAllHouses();

		if (!cur.moveToFirst()) {
			dbHelper.close();
			return;
		}

		Date checkinTime;
		// cycle through the houses
		do {
			final long house_id = cur.getLong(0);

			// the fixed checkins
			for (int i = 0; i < fewestCheckins; i++) {
				checkinTime = new Date(startTime.getTime()
						+ random.nextInt(range));
				createGuardCheckin(mContext, house_id, checkinTime);
			}

			// the random checkins
			for (int i = 0; i < randomCheckins; i++) {
				if (random.nextBoolean()) {
					checkinTime = new Date(startTime.getTime()
							+ random.nextInt(range));
					createGuardCheckin(mContext, house_id, checkinTime);
				}
			}
		} while (cur.moveToNext());

		// Log this function call while we're at it
		dbHelper.addLogEvent(DbAdapter.LOG_TYPE_SMS_NOTIFICATION,
				"Just finished executing AlarmReceiver.addGuardCheckins()");

		dbHelper.close();
	}

	/**
	 * Checks if there are due call arounds, and if so, starts a
	 * <code>CallAroundDetailList</code> activity with a flag to do the audio
	 * alert.
	 */
	private void checkCallaroundDue() {
		Log.i("Debug", "checkCallaroundDue()");

		if (mDbHelper.getNumberOfDueCallarounds() > 0) {
			mDbHelper.close();

			final Intent intent = new Intent(mContext,
					CallAroundDetailList.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			intent.putExtra(DbAdapter.KEY_DUEBY, Time.iso8601Date());
			intent.putExtra(ALERT_CALLAROUND_DUE, ALERT_CALLAROUND_DUE);
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
		final long checkinId = intent.getLongExtra(ALERT_CHECKIN_REMINDER, -1);
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
			intent.putExtra(ALERT_CALLAROUND_DUE, ALERT_CALLAROUND_DUE);
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
			intent.putExtra(ALERT_CHECKIN_DUE, true);
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
}
