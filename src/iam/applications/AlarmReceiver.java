package iam.applications;

import java.util.Calendar;
import java.util.Date;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * Alarm receiver receives all alarms from the system, and starts the
 * appropriate corresponding activities in response. It also has static methods
 * for creating alerts.
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

	/** The database interface */
	private DbAdapter mDbHelper;

	private Context mContext;

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.content.BroadcastReceiver#onReceive(android.content.Context,
	 * android.content.Intent)
	 */
	@Override
	public void onReceive(Context context, Intent intent) {
		mContext = context;

		mDbHelper = new DbAdapter(mContext);
		mDbHelper.open();

		Log.i("Debug", intent.getAction());

		String action = intent.getAction();

		if (action.equals(ALERT_CHECKIN_DUE)) {
			checkinDue();
		} else if (action.equals(ALERT_ADD_CALLAROUNDS)) {
			addCallarounds();
		} else if (action.equals(ALERT_CHECKIN_REMINDER)) {
			checkinReminder(intent);
		} else if (action.equals(ALERT_CALLAROUND_DUE)) {
			callaroundDue();
		}

		mDbHelper.close();
	}

	/**
	 * 
	 */
	private void callaroundDue() {
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
	 * @param intent
	 */
	private void checkinReminder(Intent intent) {
		long checkinId = intent.getLongExtra(ALERT_CHECKIN_REMINDER, -1);
		if (mDbHelper.getCheckinOutstanding(checkinId)) {
			SmsHandler.sendSms(mContext,
					mDbHelper.getNumberForCheckin(checkinId),
					mContext.getString(R.string.sms_checkin_reminder));
		}
	}

	/**
	 * 
	 */
	private void addCallarounds() {
		mDbHelper.addCallarounds();
		sendRefreshAlert(mContext);
	}

	/**
	 * 
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

	@Override
	protected void finalize() throws Throwable {
		super.finalize();
	}

	/**
	 * Set an alert to check for overdue check-ins at a certain time
	 * 
	 * @param ctx
	 *            the application context
	 * @param date
	 *            the date/time at which to check for overdue check-ins
	 */
	static public void setCheckinAlert(Context ctx, Date date) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);

		Intent intent = new Intent(ctx, AlarmReceiver.class);
		intent.setAction(ALERT_CHECKIN_DUE);
		PendingIntent sender = PendingIntent.getBroadcast(ctx,
				(int) cal.getTimeInMillis(), intent,
				PendingIntent.FLAG_UPDATE_CURRENT);

		AlarmManager am = (AlarmManager) ctx
				.getSystemService(Context.ALARM_SERVICE);
		am.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), sender);
	}

	/**
	 * Set an alert to remind the user about a due checkin
	 * 
	 * @param ctx
	 *            the application context
	 * @param date
	 *            the date/time at which the check-in is due
	 */
	static public void setCheckinReminderAlert(Context ctx, Date date,
			long checkin_id) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);

		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(ctx);
		int offset = -1
				* settings.getInt(
						HomeActivity.PREFERENCES_CHECKIN_REMINDER_DELAY, 3);

		cal.add(Calendar.MINUTE, offset);

		Intent intent = new Intent(ctx, AlarmReceiver.class);
		intent.setAction(ALERT_CHECKIN_REMINDER);
		intent.putExtra(ALERT_CHECKIN_REMINDER, checkin_id);

		PendingIntent sender = PendingIntent.getBroadcast(ctx,
				(int) cal.getTimeInMillis(), intent,
				PendingIntent.FLAG_UPDATE_CURRENT);

		AlarmManager am = (AlarmManager) ctx
				.getSystemService(Context.ALARM_SERVICE);
		am.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), sender);
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

		Date targetdate = Time.timeFromColonTime(old);
		Date thisdate = new Date();
		thisdate.setHours(targetdate.getHours());
		thisdate.setMinutes(targetdate.getMinutes());

		Intent intent = new Intent(context, AlarmReceiver.class);
		intent.setAction(ALERT_ADD_CALLAROUNDS);
		intent.putExtra("TIME", ALERT_ADD_CALLAROUNDS);
		PendingIntent sender = PendingIntent.getBroadcast(context,
				(int) thisdate.getTime(), intent,
				PendingIntent.FLAG_UPDATE_CURRENT);

		AlarmManager am = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);
		am.cancel(sender);
		am.setRepeating(AlarmManager.RTC_WAKEUP, thisdate.getTime(),
				AlarmManager.INTERVAL_DAY, sender);
	}

	/**
	 * Sets a recurring alarm, which checks for completed call arounds
	 * 
	 * @param context
	 *            the application context
	 */
	static public void setCallaroundDueAlarm(Context context, Date date) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);

		Intent intent = new Intent(context, AlarmReceiver.class);
		intent.setAction(ALERT_CALLAROUND_DUE);
		intent.putExtra("TIME", Time.iso8601DateTime(date));
		PendingIntent sender = PendingIntent.getBroadcast(context,
				(int) cal.getTimeInMillis(), intent,
				PendingIntent.FLAG_UPDATE_CURRENT);

		AlarmManager am = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);
		am.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), sender);
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
}
