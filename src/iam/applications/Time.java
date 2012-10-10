package iam.applications;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * This is a class with miscellaneous static functions to convert
 * <code>Date</code> objects to <code>String</code> objects, and vice versa.
 */
final public class Time {

	private Time() {
	}

	private final static transient String ISO8601 = "yyyy-MM-dd HH:mm";
	private final static transient String ISO8601_DAY = "yyyy-MM-dd";
	private final static transient String ISO8601_TIME = "HH:mm";

	/**
	 * Return the ISO 8601 string of the Date object.
	 * 
	 * @param date
	 *            the date
	 * @return the string
	 */
	static public String iso8601DateTime(final Date date) {
		final SimpleDateFormat format = new SimpleDateFormat(ISO8601, Locale.US);
		return format.format(date);
	}

	/**
	 * Return the ISO 8601 string of the current time.
	 * 
	 * @param d
	 *            the date
	 * @return the string
	 */
	static public String iso8601DateTime() {
		final Calendar today = Calendar.getInstance();
		final SimpleDateFormat format = new SimpleDateFormat(ISO8601, Locale.US);
		return format.format(today.getTime());
	}

	/**
	 * Return the time portion of the ISO 8601 string of the given Date.
	 * 
	 * @param date
	 *            the date
	 * @return the string
	 */
	static public String iso8601Time(final Date date) {
		final SimpleDateFormat format = new SimpleDateFormat(ISO8601_TIME,
				Locale.US);
		return format.format(date);
	}

	/**
	 * Return a Date object from a ISO 8601 string.
	 * 
	 * @param date
	 *            the date
	 * @return the date
	 */
	static public Date iso8601DateTime(final String date) {
		try {
			final SimpleDateFormat format = new SimpleDateFormat(ISO8601,
					Locale.US);
			return format.parse(date);
		} catch (ParseException e) {
			Log.v(HomeActivity.TAG, Log.getStackTraceString(e));
			return null;
		}
	}

	/**
	 * Return the day (not time) portion of the ISO 8601 string of the Date
	 * object.
	 * 
	 * @param date
	 *            the d
	 * @return the string
	 */
	static public String iso8601Date(final Date date) {
		final SimpleDateFormat format = new SimpleDateFormat(ISO8601_DAY,
				Locale.US);
		return format.format(date);
	}

	/**
	 * Return the day (not time) portion of the ISO 8601 string for the current
	 * time.
	 * 
	 * @return the string
	 */
	static public String iso8601Date() {
		final Calendar today = Calendar.getInstance();
		final SimpleDateFormat format = new SimpleDateFormat(ISO8601_DAY,
				Locale.US);
		return format.format(today.getTime());
	}

	/**
	 * Return a Date object from the day portion of the ISO 8601 string. Also
	 * accepts full ISO 8601 strings.
	 * 
	 * @param date
	 *            the d
	 * @return the date
	 */
	static public Date iso8601Date(final String date) {
		try {
			final SimpleDateFormat format = new SimpleDateFormat(ISO8601_DAY,
					Locale.US);
			return format.parse(date);
		} catch (ParseException e) {
			try {
				final SimpleDateFormat format = new SimpleDateFormat(ISO8601,
						Locale.US);
				return format.parse(date);
			} catch (ParseException e2) {
				Log.v(HomeActivity.TAG, Log.getStackTraceString(e));
				Log.v(HomeActivity.TAG, Log.getStackTraceString(e2));
				return null;
			}
		}
	}

	/**
	 * Returns a date object corresponding to the next time it will be the time
	 * specified in the specified preference string. It is acknowleged that this
	 * is poor documentation.
	 * 
	 * @param context
	 *            the context
	 * @param preference
	 *            the preference name
	 * @param defaultValue
	 *            the default value
	 * @return the date
	 */
	public static Date nextDateFromPreferenceString(final Context context,
			final String preference, final String defaultValue) {
		final SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(context);
		final String old = settings.getString(preference, defaultValue);

		// make sure that the time for the alarm is in the future, so that these
		// events aren't really added every time you go to the home activity
		Date timeToAddAt = Time.todayAtGivenTime(old);
		if (timeToAddAt.before(new Date())) {
			timeToAddAt = Time.tomorrowAtGivenTime(old);
		}
		return timeToAddAt;
	}

	/**
	 * Return a Date object set to today's date, at the time specified by the
	 * preference.
	 * 
	 * @param context
	 *            the context
	 * @param preference
	 *            the preference string, from HomeActivity
	 * @param defaultValue
	 *            the default value of the preference
	 * @return the Date object
	 */
	static public Date todayAtPreferenceTime(final Context context,
			final String preference, final String defaultValue) {
		final SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(context);
		final String simpleTime = settings.getString(preference, defaultValue);

		final Date targetTime = Time.timeFromSimpleTime(simpleTime);
		final Date today = new Date();

		today.setHours(targetTime.getHours());
		today.setMinutes(targetTime.getMinutes());
		today.setSeconds(targetTime.getSeconds());

		return today;
	}

	/**
	 * Return a locale-appropriate representation of a day and time (Medium
	 * date, Short time) of the Date object.
	 * 
	 * @param date
	 *            the date
	 * @return the string
	 */
	static public String prettyDateTime(final Date date) {
		if (date == null) {
			return "";
		} else {
			return java.text.DateFormat.getDateTimeInstance(
					java.text.DateFormat.MEDIUM, java.text.DateFormat.SHORT)
					.format(date);
		}
	}

	/**
	 * Return a locale-appropriate representation of a day and time (Medium
	 * date, Short time), given an ISO 8601 time string.
	 * 
	 * @param date
	 *            the date
	 * @return the string
	 */
	static public String prettyDateTime(final String date) {
		final Date dateDate = iso8601DateTime(date);
		return prettyDateTime(dateDate);
	}

	/**
	 * Return a locale-appropriate representation of a day and time (Medium
	 * date, Short time), from an ISO 8601 string.
	 * 
	 * @param date
	 *            the date
	 * @return the string
	 */
	static public String prettyTime(final String date) {
		return prettyTime(iso8601DateTime(date));
	}

	/**
	 * Return a locale-appropriate representation of a time (Short style), from
	 * an ISO 8601 string.
	 * 
	 * @param date
	 *            the date
	 * @return the string
	 */
	static public String prettyTime(final Date date) {
		if (date == null) {
			return "";
		} else {
			return java.text.DateFormat.getTimeInstance(
					java.text.DateFormat.SHORT).format(date);
		}
	}

	/**
	 * Return a locale-appropriate representation of a date (Medium style), from
	 * an ISO 8601 string.
	 * 
	 * @param context
	 *            the context
	 * @param date
	 *            the date
	 * @return the string
	 */
	static public String prettyDate(final Context context, final String date) {
		final Date dateDate = iso8601Date(date);
		return prettyDate(context, dateDate);
	}

	/**
	 * Return a representation of a date and time with (localized) strings like
	 * "yesterday","today", "tomorrow" when appropriate.
	 * 
	 * @param context
	 *            the context
	 * @param date
	 *            the date
	 * @return the string
	 */
	static public String timeTodayTomorrow(final Context context,
			final Date date) {
		if (date == null) {
			return "";
		} else {
			final String timebit = java.text.DateFormat.getTimeInstance(
					java.text.DateFormat.SHORT).format(date);
			final String datebit = Time.prettyDate(context, date);
			return String.format("%s (%s)", timebit,
					datebit.toLowerCase(Locale.US));
		}
	}

	/**
	 * Return a representation of a date with (localized) strings like
	 * "yesterday","today", "tomorrow" when appropriate.
	 * 
	 * @param context
	 *            the context
	 * @param date
	 *            the date
	 * @return the string
	 */
	static public String prettyDate(final Context context, final Date date) {
		if (date == null) {
			return "";
		} else {
			final Calendar today = Calendar.getInstance();

			if (iso8601Date(date).equals(iso8601Date(today.getTime()))) {
				return context.getString(R.string.today);
			}
			today.add(Calendar.DAY_OF_MONTH, -1); // now it's yesterday
			if (iso8601Date(date).equals(iso8601Date(today.getTime()))) {
				return context.getString(R.string.yesterday);
			}
			today.add(Calendar.DAY_OF_MONTH, 2); // now it's tomorrow
			if (iso8601Date(date).equals(iso8601Date(today.getTime()))) {
				return context.getString(R.string.tomorrow);
			}
			return java.text.DateFormat.getDateInstance(
					java.text.DateFormat.MEDIUM).format(date);
		}
	}

	/**
	 * Returns the a <code>Date</code> object from a user's representation of a
	 * time, or returns null. Parsing is determined by the regular expression in
	 * R.string.re_time
	 * 
	 * @param context
	 *            the context
	 * @param timeString
	 *            the string containing the time representation
	 * @return a <code>Date</code> object with the corresponding time, or null.
	 */
	static public Date timeFromString(final Context context,
			final String timeString) {
		final Resources resources = context.getResources();
		final Pattern pattern = Pattern
				.compile(resources.getString(R.string.re_time),
						Pattern.CASE_INSENSITIVE);
		final Matcher matcher = pattern.matcher(timeString);
		if (matcher.matches()) {
			final Calendar date = Calendar.getInstance();

			long hour = matcher.group(1).length() == 0 ? 0 : Long
					.parseLong(matcher.group(1));
			final long minute = matcher.group(2).length() == 0 ? 0 : Long
					.parseLong(matcher.group(2));
			final String ampm = (matcher.group(3) == null || matcher.group(3)
					.length() == 0) ? "" : matcher.group(3);

			// obvious nonsense
			if (hour < 0 || hour > 23 || minute < 0 || minute > 59
					|| (!ampm.isEmpty() && hour > 12)) {
				Log.e(HomeActivity.TAG,
						"Numbers out of bounds, or inconsistent with am/pm");
				if (ampm == null) {
					Log.e(HomeActivity.TAG, "Null ap");
				} else if (ampm.isEmpty()) {
					Log.e(HomeActivity.TAG, "Empty");
				}
				return null;
			}

			final long nowH = date.get(Calendar.HOUR_OF_DAY);
			final long nowM = date.get(Calendar.MINUTE);

			if ("a".equals(ampm) && hour == 12) {
				hour -= 12;
			} else if ("p".equals(ampm) && hour < 12) {
				hour += 12;
			}

			// if it's later than it is already, then it must be about tomorrow
			boolean nextDay = false;
			if ((nowH == hour && nowM > minute) || nowH > hour) {
				nextDay = true;
			}

			date.set(date.get(Calendar.YEAR), date.get(Calendar.MONTH),
					date.get(Calendar.DAY_OF_MONTH), (int) hour, (int) minute,
					0);
			if (nextDay) {
				date.add(Calendar.DAY_OF_MONTH, 1);
			}

			return date.getTime();
		} else {
			Log.e("Parsing time", "Simple non-match");
			return null;
		}
	}

	/**
	 * Return a Date object with the time set to that specified in timeString.
	 * timeString is assumed to be two colon-delimited integers (e.g., 10:00 or
	 * 10:0). No error checking is done to ensure that the time is valid or
	 * meaninful.
	 * 
	 * @param timeString
	 *            the date
	 * @return the date object
	 */
	static public Date timeFromSimpleTime(final String timeString) {
		final String[] pieces = timeString.split(":");
		if (pieces.length == 2) {
			final Date ret = new Date();
			ret.setHours(Integer.parseInt(pieces[0]));
			ret.setMinutes(Integer.parseInt(pieces[1]));
			ret.setSeconds(0);
			return ret;
		} else {
			return null;
		}
	}

	/**
	 * Returns a full day of the week (Sunday, Monday, ...) for the given time
	 * string (which can be either yyyy-MM-dd or yyyy-MM-dd HH:mm).
	 * 
	 * @param date
	 *            the date
	 * @return the day of the week
	 */
	static public String dayOfWeek(final String date) {
		try {
			final SimpleDateFormat format = new SimpleDateFormat(ISO8601_DAY,
					Locale.US);
			return dayOfWeek(format.parse(date));
		} catch (ParseException e) {
			try {
				final SimpleDateFormat format = new SimpleDateFormat(ISO8601,
						Locale.US);
				return dayOfWeek(format.parse(date));
			} catch (ParseException e2) {
				Log.v(HomeActivity.TAG, Log.getStackTraceString(e));
				Log.v(HomeActivity.TAG, Log.getStackTraceString(e2));
				return null;
			}
		}
	}

	/**
	 * Returns a full day of the week (Sunday, Monday, ...) for the given Date.
	 * 
	 * @param date
	 *            the date
	 * @return the day of the week
	 */
	static public String dayOfWeek(final Date date) {
		String returnValue;
		final Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		final int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
		switch (dayOfWeek) {
		case 1:
			returnValue = "Sunday";
			break;
		case 2:
			returnValue = "Monday";
			break;
		case 3:
			returnValue = "Tuesday";
			break;
		case 4:
			returnValue = "Wednesday";
			break;
		case 5:
			returnValue = "Thursday";
			break;
		case 6:
			returnValue = "Friday";
			break;
		case 7:
			returnValue = "Saturday";
			break;
		default:
			returnValue = "";
			break;
		}
		return returnValue;
	}

	/**
	 * Returns a Date object at today's date, and with the time specified by the
	 * supplied simple time.
	 * 
	 * @param simpleTime
	 *            the simple time
	 * @return the date
	 */
	static public Date todayAtGivenTime(final String simpleTime) {
		final Date targetTime = Time.timeFromSimpleTime(simpleTime);
		final Date today = new Date();

		today.setHours(targetTime.getHours());
		today.setMinutes(targetTime.getMinutes());
		today.setSeconds(targetTime.getSeconds());

		return today;
	}

	/**
	 * Returns a Date object at tomorrow's date, and with the time specified by
	 * the supplied simple time.
	 * 
	 * @param simpleTime
	 *            the simple time
	 * @return the date
	 */
	static public Date tomorrowAtGivenTime(final String simpleTime) {
		final Date targetTime = Time.timeFromSimpleTime(simpleTime);

		final Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.DATE, 1);

		final Date tomorrow = calendar.getTime();
		tomorrow.setHours(targetTime.getHours());
		tomorrow.setMinutes(targetTime.getMinutes());
		tomorrow.setSeconds(targetTime.getSeconds());
		return tomorrow;
	}

	/**
	 * Returns the ISO 8601 time stamp of the time n minutes after the time
	 * indicated.
	 * 
	 * @param isoTime
	 * @param nMinutes
	 * @return a time object n minutes after the time indicated
	 */
	static public String nMinutesAfter(final String isoTime, final int nMinutes) {
		try {
			final SimpleDateFormat format = new SimpleDateFormat(ISO8601,
					Locale.US);
			final Date initialTime = format.parse(isoTime);
			final Calendar cal = Calendar.getInstance();
			cal.setTime(initialTime);
			cal.add(Calendar.MINUTE, nMinutes);
			return Time.iso8601DateTime(cal.getTime());
		} catch (ParseException e) {
			Log.v(HomeActivity.TAG, Log.getStackTraceString(e));
			return null;
		}
	}

	/**
	 * Returns a time formatted like "18:30".
	 * 
	 * @param hour
	 * @param minute
	 * @return the formatted time
	 */
	static public String basicTimeFormat(final int hour, final int minute) {
		return String.format("%02d:%02d", hour, minute);
	}
}
