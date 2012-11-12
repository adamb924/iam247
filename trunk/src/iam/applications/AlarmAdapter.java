/**
 * 
 */
package iam.applications;

import java.util.Calendar;
import java.util.Date;
import java.util.Random;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.PreferenceManager;

/**
 * AlarmAdapter has a bunch of static methods for creating alarms. It's an
 * adapter class for the *horrible* Android alarm system.
 * 
 */
final public class AlarmAdapter {

	/**
	 * Container class for holding alert types
	 */
	public static class Alerts {
		/**
		 * An Intent action string alerting the application to check for overdue
		 * check-ins. Also used as an extra in the Intent to start the
		 * CheckinList activity.
		 */
		public static final String CHECKIN_DUE = "ALERT_CHECKIN_DUE";

		/**
		 * An Intent action string alerting the application to check for overdue
		 * call arounds, and then text those people a reminder.
		 */
		public static final String CALLAROUND_DUE = "ALERT_CALLAROUND_DUE";

		/**
		 * An Intent action string alerting the application to check for overdue
		 * call arounds, and sound the alarm if someone has missed.
		 */
		public static final String CALLAROUND_ALARM = "ALERT_CALLAROUND_ALARM";

		/**
		 * An Intent action string alerting the application to check for delayed
		 * call arounds.
		 */
		public static final String DELAYED_CALLAROUND_DUE = "ALERT_DELAYED_CALLAROUND_DUE";

		/**
		 * An Intent action string alerting the application to add call arounds
		 * for the day
		 */
		public static final String ADD_CALLAROUNDS = "ALERT_ADD_CALLAROUNDS";

		/**
		 * An Intent action string alerting the application send a message for
		 * activities to refresh their screens
		 */
		public static final String REFRESH = "ALERT_REFRESH";

		/**
		 * An Intent action string alerting the application to remind the user
		 * about a check-in
		 */
		public static final String CHECKIN_REMINDER = "ALERT_CHECKIN_REMINDER";

		/**
		 * An Intent action string alerting the application to add the guards'
		 * check-ins.
		 */
		public static final String ADD_GUARD_CHECKINS = "ALERT_ADD_GUARD_CHECKINS";

		/**
		 * An Intent action string alerting the application to perform a guard
		 * check.
		 */
		public static final String GUARD_CHECKIN = "ALERT_GUARD_CHECKIN";

		/**
		 * An Intent action string alerting the application set today's guard
		 * schedule to their typical schedule.
		 */
		public static final String RESET_GUARD_SCHEDULE = "ALERT_RESET_GUARD_SCHEDULE";

		/** An array with all of he repeating alarm types. */
		public static String[] REPEATING = { CALLAROUND_DUE, CALLAROUND_ALARM,
				DELAYED_CALLAROUND_DUE, ADD_CALLAROUNDS, ADD_GUARD_CHECKINS,
				RESET_GUARD_SCHEDULE };

		/** An array with all of he one-off alarm types. */
		public static String[] ONEOFF = { CHECKIN_DUE, CHECKIN_REMINDER,
				GUARD_CHECKIN };
	}

	/**
	 * Adds guard checkins for today.
	 * 
	 * @param context
	 *            the context
	 */
	public static void addGuardCheckins(final Context context) {

		// clear out old ones
		removeAlarmsByType(context, AlarmAdapter.Alerts.GUARD_CHECKIN);

		final SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(context);

		final int fewestCheckins = Integer.parseInt(settings.getString(
				Preferences.FEWEST_GUARD_CHECKS, "3"));
		final int randomCheckins = Integer.parseInt(settings.getString(
				Preferences.RANDOM_GUARD_CHECKS, "3"));

		final String startTimeString = settings.getString(
				Preferences.GUARD_CHECKIN_START, "22:00");
		final String endTimeString = settings.getString(
				Preferences.GUARD_CHECKIN_END, "06:00");

		final Date startTime = Time.todayAtGivenTime(startTimeString);
		final Date endTime = Time.tomorrowAtGivenTime(endTimeString);

		final Random random = new Random();

		// int ought to be at least 32-bits, which provides more way more than
		// 24 hours in milliseconds
		final int range = (int) (endTime.getTime() - startTime.getTime());

		final DbAdapter dbHelper = new DbAdapter(context);
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
				setGuardCheckin(context, house_id, checkinTime);
			}

			// the random checkins
			for (int i = 0; i < randomCheckins; i++) {
				if (random.nextBoolean()) {
					checkinTime = new Date(startTime.getTime()
							+ random.nextInt(range));
					setGuardCheckin(context, house_id, checkinTime);
				}
			}
		} while (cur.moveToNext());

		cur.close();
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

		if (!cur.moveToFirst()) {
			dbHelper.close();
			return;
		}

		Intent toCancel;
		PendingIntent pendingToCancel;
		final AlarmManager alarmManager = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);
		do {
			final int _id = cur.getInt(0);
			toCancel = new Intent(context, AlarmReceiver.class);
			toCancel.setAction(type);
			pendingToCancel = PendingIntent.getBroadcast(context, _id,
					toCancel, PendingIntent.FLAG_CANCEL_CURRENT);
			alarmManager.cancel(pendingToCancel);
		} while (cur.moveToNext());

		dbHelper.deleteAlarmsByType(type);

		cur.close();
		dbHelper.close();
	}

	/**
	 * Resets all of the one-off alarms, based on database and preference
	 * information.
	 * 
	 * @param context
	 *            the context
	 */
	public static void resetOneOffAlarms(final Context context) {
		final DbAdapter dbHelper = new DbAdapter(context);
		dbHelper.open();

		// clear the database of any one-off alarms
		dbHelper.deleteAlarmsByType(Alerts.CHECKIN_DUE);
		dbHelper.deleteAlarmsByType(Alerts.CHECKIN_REMINDER);
		dbHelper.deleteAlarmsByType(Alerts.GUARD_CHECKIN);

		// Alerts.CHECKIN_DUE
		// Alerts.CHECKIN_REMINDER
		final Cursor cur = dbHelper.fetchUnresolvedCheckins();
		if (cur.moveToFirst()) {
			do {
				final Date timeDue = Time.iso8601DateTime(cur.getString(cur
						.getColumnIndex(DbAdapter.Columns.TIMEDUE)));
				final long checkinId = cur.getLong(cur
						.getColumnIndex(DbAdapter.Columns.ROWID));
				final long contactId = cur.getLong(cur
						.getColumnIndex(DbAdapter.Columns.CONTACTID));
				AlarmAdapter.setCheckinAlert(context, timeDue);

				if (dbHelper.getContactPreference(contactId,
						DbAdapter.UserPreferences.CHECKIN_REMINDER)) {
					AlarmAdapter.setCheckinReminderAlert(context, checkinId);
				}
			} while (cur.moveToNext());
		}

		// Alerts.GUARD_CHECKIN
		AlarmAdapter.addGuardCheckins(context);

		cur.close();
		dbHelper.close();
	}

	/**
	 * Resets all repeating alarms, and the call around alarms.
	 * 
	 * @param context
	 *            the context
	 */
	public static void resetRepeatingAlarms(final Context context) {
		final DbAdapter dbHelper = new DbAdapter(context);
		dbHelper.open();

		// clear the database of any existing repeating alarms
		dbHelper.deleteAlarmsByType(Alerts.CALLAROUND_DUE);
		dbHelper.deleteAlarmsByType(Alerts.DELAYED_CALLAROUND_DUE);
		dbHelper.deleteAlarmsByType(Alerts.CALLAROUND_ALARM);
		dbHelper.deleteAlarmsByType(Alerts.ADD_CALLAROUNDS);
		dbHelper.deleteAlarmsByType(Alerts.ADD_GUARD_CHECKINS);
		dbHelper.deleteAlarmsByType(Alerts.RESET_GUARD_SCHEDULE);

		// Both these handled by one function call
		// Alerts.CALLAROUND_DUE
		// Alerts.DELAYED_CALLAROUND_DUE
		// Alerts.CALLAROUND_ALARM
		dbHelper.addCallarounds();

		// Alerts.ADD_CALLAROUNDS
		AlarmAdapter.setAddCallaroundAlarm(context);

		// Alerts.ADD_GUARD_CHECKINS
		AlarmAdapter.setAddGuardCheckinAlarms(context);

		// Alerts.RESET_GUARD_SCHEDULE
		AlarmAdapter.setGuardScheduleResetAlarm(context);

		dbHelper.close();
	}

	/**
	 * Sets a recurring alarm, which when triggered will prompt the application
	 * to add all of the active houses to the day's call around list
	 * 
	 * @param context
	 *            the application context
	 */
	static public void setAddCallaroundAlarm(final Context context) {
		final Date timeToAddAt = Time.nextDateFromPreferenceString(context,
				Preferences.CALLAROUND_ADD, "06:00");
		setDailyAlarm(context, AlarmAdapter.Alerts.ADD_CALLAROUNDS, timeToAddAt);
	}

	/**
	 * Sets a recurring alarm, which when triggered will prompt the application
	 * to add random checks for the current guards.
	 * 
	 * @param context
	 */
	static public void setAddGuardCheckinAlarms(final Context context) {
		final Date timeToAddAt = Time.nextDateFromPreferenceString(context,
				Preferences.GUARD_CHECKIN_START, "21:00");
		setDailyAlarm(context, AlarmAdapter.Alerts.ADD_GUARD_CHECKINS,
				timeToAddAt);
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
		final long time = cal.getTimeInMillis();

		final Intent intent = new Intent(context, AlarmReceiver.class);
		intent.setAction(AlarmAdapter.Alerts.CHECKIN_DUE);

		setOneOffAlarm(context, time, intent);
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
		dbHelper.close();

		final Calendar cal = Calendar.getInstance();
		cal.setTime(Time.iso8601Date(duetime));

		// read the reminder offset from the system preferences, and calculate
		// the new time based on that offset
		final SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(context);
		final int offset = -1
				* Integer.parseInt(settings.getString(
						Preferences.CHECKIN_REMINDER_DELAY, "3"));
		cal.add(Calendar.MINUTE, offset);

		final Intent intent = new Intent(context, AlarmReceiver.class);
		intent.setAction(AlarmAdapter.Alerts.CHECKIN_REMINDER);
		intent.putExtra(AlarmAdapter.Alerts.CHECKIN_REMINDER, checkin_id);

		setOneOffAlarm(context, cal.getTimeInMillis(), intent);
	}

	/**
	 * Create a repeating alarm of the given type, starting on the given date.
	 * If the date is before the current time, then the alarm is set for
	 * tomorrow. All other alarms of the same type are deleted from the
	 * database.
	 * 
	 * @param context
	 *            the context
	 * @param type
	 *            the type of alarm to set (as defined in AlarmReceiver)
	 * @param date
	 *            the date for the first occasion of the alarm
	 */
	static public void setDailyAlarm(final Context context, final String type,
			final Date date) {
		final Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		if (date.before(new Date())) {
			cal.add(Calendar.DATE, 1);
		}

		final int _id = (int) System.currentTimeMillis();
		final Intent intent = new Intent(context, AlarmReceiver.class);
		intent.setAction(type);
		intent.putExtra(DbAdapter.Columns.REQUESTID, _id);

		removeAlarmsByType(context, type);

		final PendingIntent sender = PendingIntent.getBroadcast(context, _id,
				intent, 0);

		final AlarmManager alarmManager = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);
		alarmManager.setRepeating(AlarmManager.RTC_WAKEUP,
				cal.getTimeInMillis(), AlarmManager.INTERVAL_DAY, sender);

		final DbAdapter dbHelper = new DbAdapter(context);
		dbHelper.open();
		dbHelper.addAlarm(_id, type);
		dbHelper.close();
	}

	/**
	 * Creates a checkin alarm for the given guard and checkin time.
	 * 
	 * @param house_id
	 * @param checkinTime
	 */

	private static void setGuardCheckin(final Context context,
			final long house_id, final Date checkinTime) {
		final Intent intent = new Intent(context, AlarmReceiver.class);
		intent.setAction(AlarmAdapter.Alerts.GUARD_CHECKIN);
		intent.putExtra(DbAdapter.Columns.HOUSEID, house_id);

		setOneOffAlarm(context, checkinTime.getTime(), intent);
	}

	/**
	 * Sets a recurring alarm to reset the guards' schedules to their typical
	 * schedule. This occurs at the end of the guards' check-in window (by
	 * default 6am).
	 * 
	 * @param context
	 *            the application context
	 * @return true if the alarm will go off today, false if it will go off
	 *         tomorrow
	 */
	static public boolean setGuardScheduleResetAlarm(final Context context) {
		boolean addingToday = true;
		final SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(context);
		final String old = settings.getString(Preferences.GUARD_CHECKIN_END,
				"06:00");

		// make sure that the time for the alarm is in the future, so that these
		// events aren't really added every time you go to the home activity
		Date timeToAddAt = Time.todayAtGivenTime(old);
		if (timeToAddAt.before(new Date())) {
			timeToAddAt = Time.tomorrowAtGivenTime(old);
			addingToday = false;
		}

		setDailyAlarm(context, AlarmAdapter.Alerts.RESET_GUARD_SCHEDULE,
				timeToAddAt);
		return addingToday;
	}

	/**
	 * Creates a one-off (cancelable) alarm with AlarmManager, at the given time
	 * and for the given type, and with the given Intent. The extra
	 * DbAdapter.Columns.REQUESTID is added to the Intent to indicate the
	 * requestId of the alarm.
	 * 
	 * @param context
	 * @param time
	 * @param intent
	 */
	private static void setOneOffAlarm(final Context context, final long time,
			final Intent intent) {
		final int _id = (int) System.currentTimeMillis();
		intent.putExtra(DbAdapter.Columns.REQUESTID, _id);

		final PendingIntent sender = PendingIntent.getBroadcast(context, _id,
				intent, 0);

		final AlarmManager alarmManager = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);
		alarmManager.set(AlarmManager.RTC_WAKEUP, time, sender);

		final DbAdapter dbHelper = new DbAdapter(context);
		dbHelper.open();
		dbHelper.addAlarm(_id, intent.getAction());
		dbHelper.close();
	}

	private AlarmAdapter() {
	}

}
