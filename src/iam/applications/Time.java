package iam.applications;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;

/**
 * This is a class with miscellaneous static functions to convert
 * <code>Date</code> objects to <code>String</code> objects, and vice versa.
 */
public class Time {

	/**
	 * Return the ISO 8601 string of the Date object.
	 * 
	 * @param d
	 *            the date
	 * @return the string
	 */
	static public String iso8601DateTime(Date d) {
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		return format.format(d);
	}

	/**
	 * Return the ISO 8601 string of the current time.
	 * 
	 * @param d
	 *            the date
	 * @return the string
	 */
	static public String iso8601DateTime() {
		Calendar today = Calendar.getInstance();
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		return format.format(today.getTime());
	}

	/**
	 * Return the time portion of the ISO 8601 string of the given Date.
	 * 
	 * @param d
	 *            the date
	 * @return the string
	 */
	static public String iso8601Time(Date d) {
		SimpleDateFormat format = new SimpleDateFormat("HH:mm");
		return format.format(d);
	}

	/**
	 * Return a Date object from a ISO 8601 string.
	 * 
	 * @param d
	 *            the date
	 * @return the date
	 */
	static public Date iso8601DateTime(String d) {
		try {
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm");
			return format.parse(d);
		} catch (ParseException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Return the day (not time) portion of the ISO 8601 string of the Date
	 * object.
	 * 
	 * @param d
	 *            the d
	 * @return the string
	 */
	static public String iso8601Date(Date d) {
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		return format.format(d);
	}

	/**
	 * Return the day (not time) portion of the ISO 8601 string for the current
	 * time.
	 * 
	 * @return the string
	 */
	static public String iso8601Date() {
		Calendar today = Calendar.getInstance();
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		return format.format(today.getTime());
	}

	/**
	 * Return a Date object from the day portion of the ISO 8601 string. Also
	 * accepts full ISO 8601 strings.
	 * 
	 * @param d
	 *            the d
	 * @return the date
	 */
	static public Date iso8601Date(String d) {
		try {
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
			return format.parse(d);
		} catch (ParseException e) {
			try {
				SimpleDateFormat format = new SimpleDateFormat(
						"yyyy-MM-dd HH:mm");
				return format.parse(d);
			} catch (ParseException e2) {
				e.printStackTrace();
				e2.printStackTrace();
				return null;
			}
		}
	}

	/**
	 * Return a locale-appropriate representation of a day and time (Medium
	 * date, Short time) of the Date object.
	 * 
	 * @param date
	 *            the date
	 * @return the string
	 */
	static public String prettyDateTime(Date date) {
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
	static public String prettyDateTime(String date) {
		Date d = iso8601DateTime(date);
		return prettyDateTime(d);
	}

	/**
	 * Return a locale-appropriate representation of a day and time (Medium
	 * date, Short time), from an ISO 8601 string.
	 * 
	 * @param date
	 *            the date
	 * @return the string
	 */
	static public String prettyTime(String date) {
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
	static public String prettyTime(Date date) {
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
	static public String prettyDate(Context context, String date) {
		Date d = iso8601Date(date);
		return prettyDate(context, d);
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
	static public String timeTodayTomorrow(Context context, Date date) {
		if (date == null) {
			return "";
		} else {
			String timebit = java.text.DateFormat.getTimeInstance(
					java.text.DateFormat.SHORT).format(date);
			String datebit = Time.prettyDate(context, date);
			return String.format("%s (%s)", timebit, datebit.toLowerCase());
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
	static public String prettyDate(Context context, Date date) {
		if (date == null) {
			return "";
		} else {
			Calendar today = Calendar.getInstance();

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
	 * @param timeString
	 *            the string containing the time representation
	 * @return a <code>Date</code> object with the corresponding time, or null.
	 */
	static public Date timeFromString(Context context, String timeString) {
		Resources r = context.getResources();
		Pattern p = Pattern.compile(r.getString(R.string.re_time),
				Pattern.CASE_INSENSITIVE);
		Matcher matcher = p.matcher(timeString);
		if (matcher.matches()) {
			Calendar date = Calendar.getInstance();

			long h = matcher.group(1).length() == 0 ? 0 : Long.valueOf(
					matcher.group(1)).longValue();
			long m = matcher.group(2).length() == 0 ? 0 : Long.valueOf(
					matcher.group(2)).longValue();
			String ap = (matcher.group(3) == null || matcher.group(3).length() == 0) ? ""
					: matcher.group(3);

			// obvious nonsense
			if (h < 0 || h > 23 || m < 0 || m > 59 || (!ap.isEmpty() && h > 12)) {
				Log.e("Parsing time",
						"Numbers out of bounds, or inconsistent with am/pm");
				if (ap == null)
					Log.e("Parsing time", "Null ap");
				if (ap.isEmpty())
					Log.e("Parsing time", "Empty");
				return null;
			}

			long nowH = date.get(Calendar.HOUR_OF_DAY);
			long nowM = date.get(Calendar.MINUTE);

			if (ap.equals("a") && h == 12)
				h -= 12;
			else if (ap.equals("p") && h < 12)
				h += 12;

			// if it's later than it is already, then it must be about tomorrow
			boolean nextDay = false;
			if ((nowH == h && nowM > m) || nowH > h)
				nextDay = true;

			date.set(date.get(Calendar.YEAR), date.get(Calendar.MONTH),
					date.get(Calendar.DAY_OF_MONTH), (int) h, (int) m, 0);
			if (nextDay)
				date.add(Calendar.DAY_OF_MONTH, 1);

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
	static public Date timeFromSimpleTime(String timeString) {
		String[] pieces = timeString.split(":");
		if (pieces.length != 2) {
			return null;
		} else {
			Date r = new Date();
			r.setHours(Integer.parseInt(pieces[0]));
			r.setMinutes(Integer.parseInt(pieces[1]));
			return r;
		}
	}

	/**
	 * Returns a full day of the week (Sunday, Monday, ...) for the given time
	 * string (which can be either yyyy-MM-dd or yyyy-MM-dd HH:mm).
	 * 
	 * @param d
	 *            the date
	 * @return the day of the week
	 */
	static public String dayOfWeek(String d) {
		try {
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
			return dayOfWeek(format.parse(d));
		} catch (ParseException e) {
			try {
				SimpleDateFormat format = new SimpleDateFormat(
						"yyyy-MM-dd HH:mm");
				return dayOfWeek(format.parse(d));
			} catch (ParseException e2) {
				e.printStackTrace();
				e2.printStackTrace();
				return null;
			}
		}
	}

	// TODO this returns a three-day abbreviation
	/**
	 * Returns a full day of the week (Sunday, Monday, ...) for the given Date.
	 * 
	 * @param d
	 *            the date
	 * @return the day of the week
	 */
	static public String dayOfWeek(Date d) {
		Calendar c = Calendar.getInstance();
		c.setTime(d);
		int dayOfWeek = c.get(Calendar.DAY_OF_WEEK);
		switch (dayOfWeek) {
		case 1:
			return "Sunday";
		case 2:
			return "Monday";
		case 3:
			return "Tuesday";
		case 4:
			return "Wednesday";
		case 5:
			return "Thursday";
		case 6:
			return "Friday";
		case 7:
			return "Saturday";
		}
		return "";
	}
}
