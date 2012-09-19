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
	public static String ALERT_CHECKIN_DUE = "ALERT_CHECKIN_DUE";

	/**
	 * An Intent action string alerting the application to check for overdue
	 * call arounds.
	 */
	public static String ALERT_CALLAROUND_DUE = "ALERT_CALLAROUND_DUE";

	/**
	 * An Intent action string alerting the application to check for delayed
	 * call arounds.
	 */
	public static String ALERT_DELAYED_CALLAROUND_DUE = "ALERT_DELAYED_CALLAROUND_DUE";

	/**
	 * An Intent action string alerting the application to add call arounds for
	 * the day
	 */
	public static String ALERT_ADD_CALLAROUNDS = "ALERT_ADD_CALLAROUNDS";

	/**
	 * An Intent action string alerting the application send a message for
	 * activities to refresh their screens
	 */
	public static String ALERT_REFRESH = "ALERT_REFRESH";

	/**
	 * An Intent action string alerting the application to remind the user about
	 * a check-in
	 */
	public static String ALERT_CHECKIN_REMINDER = "ALERT_CHECKIN_REMINDER";

	/**
	 * An Intent action string alerting the application to add the guards'
	 * check-ins.
	 */
	public static String ALERT_ADD_GUARD_CHECKINS = "ALERT_ADD_GUARD_CHECKINS";

	/**
	 * An Intent action string alerting the application to perform a guard
	 * check.
	 */
	public static String ALERT_GUARD_CHECKIN = "ALERT_GUARD_CHECKIN";

	/**
	 * An Intent action string alerting the application set today's guard
	 * schedule to their typical schedule.
	 */
	public static String ALERT_RESET_GUARD_SCHEDULE = "ALERT_RESET_GUARD_SCHEDULE";

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.content.BroadcastReceiver#onReceive(android.content.Context,
	 * android.content.Intent)
	 */
	@Override
	public void onReceive(Context context, Intent intent) {
		mContext = context;

		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(mContext);
		boolean disabled = settings.getBoolean(
				HomeActivity.PREFERENCES_DISABLE_247, false);
		if (disabled) {
			return;
		}

		mDbHelper = new DbAdapter(mContext);
		mDbHelper.open();

		// call various functions depending on the action of the intent. the
		// action will have been set in one of the static member functions of
		// this class.
		String action = intent.getAction();
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

	private static void createGuardCheckin(Context context, long house_id,
			Date checkinTime) {
		Intent intent = new Intent(context, AlarmReceiver.class);
		intent.setAction(ALERT_GUARD_CHECKIN);
		intent.putExtra(DbAdapter.KEY_HOUSEID, house_id);

		PendingIntent sender = PendingIntent.getBroadcast(context,
				(int) checkinTime.getTime(), intent,
				PendingIntent.FLAG_ONE_SHOT);

		AlarmManager am = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);
		am.set(AlarmManager.RTC_WAKEUP, checkinTime.getTime(), sender);
	}

	/**
	 * Cancel all guard check-in alarms.
	 * 
	 * @param context
	 */
	static public void removeGuardCheckins(Context context) {
		Intent toCancel = new Intent(AlarmReceiver.ALERT_GUARD_CHECKIN);
		PendingIntent pendingToCancel = PendingIntent.getBroadcast(context, 0,
				toCancel, PendingIntent.FLAG_CANCEL_CURRENT);

		AlarmManager am = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);
		am.cancel(pendingToCancel);
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
	static public void sendRefreshAlert(Context context) {
		Intent intent = new Intent(AlarmReceiver.ALERT_REFRESH);
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
	static public void setGuardScheduleResetAlarm(Context context) {
		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(context);
		String old = settings.getString(
				HomeActivity.PREFERENCES_GUARD_CHECKIN_END, "06:00");

		// make sure that the time for the alarm is in the future, so that these
		// events aren't really added every time you go to the home activity
		Date timeToAddAt = Time.todayAtGivenTime(old);
		if (timeToAddAt.before(new Date())) {
			timeToAddAt = Time.tomorrowAtGivenTime(old);
		}

		Intent intent = new Intent(context, AlarmReceiver.class);
		intent.setAction(ALERT_RESET_GUARD_SCHEDULE);
		PendingIntent sender = PendingIntent.getBroadcast(context,
				(int) timeToAddAt.getTime(), intent,
				PendingIntent.FLAG_CANCEL_CURRENT);

		AlarmManager am = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);
		am.cancel(sender);
		am.setRepeating(AlarmManager.RTC_WAKEUP, timeToAddAt.getTime(),
				AlarmManager.INTERVAL_DAY, sender);
	}

	/**
	 * Sets a recurring alarm, which when triggered will prompt the application
	 * to add all of the active houses to the day's call around list
	 * 
	 * @param context
	 *            the application context
	 */
	static public void setAddCallaroundAlarm(Context context) {
		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(context);
		String old = settings.getString(
				HomeActivity.PREFERENCES_CALLAROUND_ADD, "06:00");

		// make sure that the time for the alarm is in the future, so that these
		// events aren't really added every time you go to the home activity
		Date timeToAddAt = Time.todayAtGivenTime(old);
		if (timeToAddAt.before(new Date())) {
			timeToAddAt = Time.tomorrowAtGivenTime(old);
		}

		Intent intent = new Intent(context, AlarmReceiver.class);
		intent.setAction(ALERT_ADD_CALLAROUNDS);
		intent.putExtra("TIME", ALERT_ADD_CALLAROUNDS); // 9/16/2012: I don't
														// understand this line
		PendingIntent sender = PendingIntent.getBroadcast(context,
				(int) timeToAddAt.getTime(), intent,
				PendingIntent.FLAG_CANCEL_CURRENT);

		AlarmManager am = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);
		am.cancel(sender);
		am.setRepeating(AlarmManager.RTC_WAKEUP, timeToAddAt.getTime(),
				AlarmManager.INTERVAL_DAY, sender);
	}

	/**
	 * Sets a recurring alarm, which when triggered will prompt the application
	 * to add random checks for the current guards.
	 * 
	 * @param context
	 */
	static public void setAddGuardCheckinAlarms(Context context) {
		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(context);
		String old = settings.getString(
				HomeActivity.PREFERENCES_GUARD_CHECKIN_START, "06:00");

		// make sure that the time for the alarm is in the future, so that these
		// events aren't really added every time you go to the home activity
		Date timeToAddAt = Time.todayAtGivenTime(old);
		if (timeToAddAt.before(new Date())) {
			timeToAddAt = Time.tomorrowAtGivenTime(old);
		}

		Intent intent = new Intent(context, AlarmReceiver.class);
		intent.setAction(ALERT_ADD_GUARD_CHECKINS);
		PendingIntent sender = PendingIntent.getBroadcast(context,
				(int) timeToAddAt.getTime(), intent,
				PendingIntent.FLAG_CANCEL_CURRENT);

		AlarmManager am = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);
		am.cancel(sender);
		am.setRepeating(AlarmManager.RTC_WAKEUP, timeToAddAt.getTime(),
				AlarmManager.INTERVAL_DAY, sender);
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
	static public void setCallaroundDueAlarm(Context context, Date date) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);

		Intent intent = new Intent(context, AlarmReceiver.class);
		intent.setAction(ALERT_CALLAROUND_DUE);
		// this extra is there only so that several ALERT_CALLAROUND_DUE Intents
		// don't overwrite one another
		intent.putExtra("TIME", Time.iso8601DateTime(date));
		PendingIntent sender = PendingIntent.getBroadcast(context,
				(int) cal.getTimeInMillis(), intent,
				PendingIntent.FLAG_CANCEL_CURRENT);

		AlarmManager am = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);
		am.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), sender);
	}

	/**
	 * Set an alert to check for overdue check-ins at a certain time
	 * 
	 * @param context
	 *            the application context
	 * @param date
	 *            the date/time at which to check for overdue check-ins
	 */
	static public void setCheckinAlert(Context context, Date date) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);

		Intent intent = new Intent(context, AlarmReceiver.class);
		intent.setAction(ALERT_CHECKIN_DUE);
		PendingIntent sender = PendingIntent.getBroadcast(context,
				(int) cal.getTimeInMillis(), intent,
				PendingIntent.FLAG_CANCEL_CURRENT);

		AlarmManager am = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);
		am.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), sender);
	}

	/**
	 * Set an alert to remind the user about a due check-in
	 * 
	 * @param context
	 *            the application context
	 * @param checkin_id
	 *            the checkin for the user to be reminded about
	 */
	static public void setCheckinReminderAlert(Context context, long checkin_id) {
		DbAdapter dbHelper = new DbAdapter(context);
		dbHelper.open();
		String duetime = dbHelper.getCheckinTime(checkin_id);
		dbHelper.close();

		Calendar cal = Calendar.getInstance();
		cal.setTime(Time.iso8601Date(duetime));

		// read the reminder offset from the system preferences, and calculate
		// the new time based on that offset
		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(context);
		int offset = -1
				* settings.getInt(
						HomeActivity.PREFERENCES_CHECKIN_REMINDER_DELAY, 3);
		cal.add(Calendar.MINUTE, offset);

		Intent intent = new Intent(context, AlarmReceiver.class);
		intent.setAction(ALERT_CHECKIN_REMINDER);
		intent.putExtra(ALERT_CHECKIN_REMINDER, checkin_id);

		PendingIntent sender = PendingIntent.getBroadcast(context,
				(int) cal.getTimeInMillis(), intent,
				PendingIntent.FLAG_CANCEL_CURRENT);

		AlarmManager am = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);
		am.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), sender);
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
	static public void setDelayedCallaroundAlarm(Context context, Date date) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);

		Intent intent = new Intent(context, AlarmReceiver.class);
		intent.setAction(ALERT_DELAYED_CALLAROUND_DUE);
		// this extra is there only so that several ALERT_DELAYED_CALLAROUND_DUE
		// Intents
		// don't overwrite one another
		intent.putExtra("TIME", Time.iso8601DateTime(date));
		PendingIntent sender = PendingIntent.getBroadcast(context,
				(int) cal.getTimeInMillis(), intent,
				PendingIntent.FLAG_CANCEL_CURRENT);

		AlarmManager am = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);
		am.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), sender);
	}

	/** The database interface */
	private DbAdapter mDbHelper;

	private Context mContext;

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
		removeGuardCheckins(mContext);

		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(mContext);

		int fewestCheckins = Long.valueOf(
				settings.getString(
						HomeActivity.PREFERENCES_FEWEST_GUARD_CHECKS, "3"))
				.intValue();
		int randomCheckins = Long.valueOf(
				settings.getString(
						HomeActivity.PREFERENCES_RANDOM_GUARD_CHECKS, "3"))
				.intValue();

		String startTimeString = settings.getString(
				HomeActivity.PREFERENCES_GUARD_CHECKIN_START, "22:00");
		String endTimeString = settings.getString(
				HomeActivity.PREFERENCES_GUARD_CHECKIN_END, "06:00");

		Date startTime = Time.todayAtGivenTime(startTimeString);
		Date endTime = Time.tomorrowAtGivenTime(endTimeString);

		Random r = new Random();

		// int ought to be at least 32-bits, which provides more way more than
		// 24 hours in milliseconds
		int range = (int) (endTime.getTime() - startTime.getTime());

		DbAdapter dbHelper = new DbAdapter(mContext);
		dbHelper.open();

		Cursor c = dbHelper.fetchAllHouses();

		if (!c.moveToFirst()) {
			dbHelper.close();
			return;
		}

		// cycle through the guards
		do {
			long house_id = c.getLong(0);

			// the fixed checkins
			for (int i = 0; i < fewestCheckins; i++) {
				Date checkinTime = new Date(startTime.getTime()
						+ r.nextInt(range));
				createGuardCheckin(mContext, house_id, checkinTime);
			}

			// the random checkins
			for (int i = 0; i < randomCheckins; i++) {
				if (r.nextBoolean()) {
					Date checkinTime = new Date(startTime.getTime()
							+ r.nextInt(range));
					createGuardCheckin(mContext, house_id, checkinTime);
				}
			}
		} while (c.moveToNext());

		dbHelper.close();
	}

	/**
	 * Checks if there are due call arounds, and if so, starts a
	 * <code>CallAroundDetailList</code> activity with a flag to do the audio
	 * alert.
	 */
	private void checkCallaroundDue() {
		if (mDbHelper.getNumberOfDueCallarounds() > 0) {
			mDbHelper.close();

			Intent i = new Intent(mContext, CallAroundDetailList.class);
			i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			i.putExtra(DbAdapter.KEY_DUEBY, Time.iso8601Date());
			i.putExtra(ALERT_CALLAROUND_DUE, ALERT_CALLAROUND_DUE);
			mContext.startActivity(i);
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
	private void checkCheckinReminder(Intent intent) {
		long checkinId = intent.getLongExtra(ALERT_CHECKIN_REMINDER, -1);
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

			Intent i = new Intent(mContext, CallAroundDetailList.class);
			i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			i.putExtra(DbAdapter.KEY_DUEBY, Time.iso8601Date());
			i.putExtra(DbAdapter.KEY_DELAYED, DbAdapter.KEY_DELAYED);
			i.putExtra(ALERT_CALLAROUND_DUE, ALERT_CALLAROUND_DUE);
			mContext.startActivity(i);
		}
	}

	/**
	 * Checks if there is a check-in due, and if so, starts the
	 * <code>CheckinList</code> activity with a flag to sound the audio alert.
	 */
	private void checkinDue() {
		if (mDbHelper.getNumberOfDueCheckins() > 0) {
			mDbHelper.close();
			Intent i = new Intent(mContext, CheckinList.class);
			i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			i.putExtra(ALERT_CHECKIN_DUE, true);
			mContext.startActivity(i);
		}
	}

	/**
	 * Send the specified guard a message requesting a checkin.
	 * 
	 * @param guard_id
	 */
	private void requestGuardCheckin(long house_id) {
		if (house_id == -1) {
			return;
		}
		long guard_id = mDbHelper.getGuardForHouse(house_id);
		String number = mDbHelper.getGuardNumber(guard_id);

		SmsHandler.sendSms(mContext, number,
				mContext.getString(R.string.sms_guard_checkin));
		mDbHelper.addGuardCheckin(guard_id, Time.iso8601DateTime());
	}
}
