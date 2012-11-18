/**
 * 
 */
package iam.applications;

import android.app.Activity;

/**
 * Class for holding the names of the PreferenceActivity preferences strings.
 */
public class Preferences extends Activity {
	/** The name of the application preferences. */
	public static final String PREFERENCES_NAME = "247-Preferences-File";
	/** Preference key: what time call around is due by. */
	public static final String CALLAROUND_DUE_BY = "PREFERENCES_CALLAROUND_DUE_BY";
	/** Preference key: the earliest time to call around. */
	public static final String CALLAROUND_DUE_FROM = "PREFERENCES_CALLAROUND_DUE_FROM";
	/** Preference key: what time the call around alarm should actually sound. */
	public static final String CALLAROUND_ALARM_TIME = "PREFERENCES_CALLAROUND_ALARM_TIME";
	/** Preference key: what time the call arounds are added for the day. */
	public static final String CALLAROUND_ADD = "PREFERENCES_CALLAROUND_ADD";
	/** Preference key: whether the application permits "this is" messages. */
	public static final String PERMIT_THISIS = "PREFERENCES_PERMIT_THISIS";
	/**
	 * Preference key: how many minutes before the check-in is due should a
	 * reminder be sent.
	 */
	public static final String CHECKIN_REMINDER_DELAY = "PREFERENCES_CHECKIN_REMINDER_DELAY";
	/**
	 * Preference key: how long after the phone starts ringing should the
	 * application wait before seeing if the person has hung up (and therefore
	 * missed-called).
	 */
	public static final String MISSED_CALL_DELAY = "PREFERENCES_MISSED_CALL_DELAY";
	/**
	 * Preference key: whether call arounds scheduled for the future should be
	 * displayed in the list.
	 */
	public static final String CALLAROUNDS_SHOW_FUTURE = "PREFERENCES_CALLAROUNDS_SHOW_FUTURE";
	/** Preference key: a kill switch to disable 24/7. */
	public static final String DISABLE_247 = "PREFERENCES_DISABLE_247";
	/**
	 * Preference key: the latest possible call around, if someone requests a
	 * delay.
	 */
	public static final String CALLAROUND_DELAYED_TIME = "PREFERENCES_CALLAROUND_DELAYED_TIME";
	/** Preference key: what time 24/7 starts checking in on guards. */
	public static final String GUARD_CHECKIN_START = "PREFERENCES_GUARD_CHECKIN_START";
	/** Preference key: what time 24/7 stops checkin in on guards. */
	public static final String GUARD_CHECKIN_END = "PREFERENCES_GUARD_CHECKIN_END";
	/** Preference key: the fewest possible guard checks. */
	public static final String FEWEST_GUARD_CHECKS = "PREFERENCES_FEWEST_GUARD_CHECKS";
	/** Preference key: the maximum number of random checks allowed. */
	public static final String RANDOM_GUARD_CHECKS = "PREFERENCES_RANDOM_GUARD_CHECKS";
	/** Preference key: how long the guard as to respond. */
	public static final String GUARD_CHECKIN_WINDOW = "PREFERENCES_GUARD_CHECKIN_WINDOW";
	/** Preference key: ignore messages that do not have a certain prefix. */
	public static final String REQUIRE_PREFIX = "PREFERENCES_REQUIRE_PREFIX";
	/** Preference key: allow users to enable or disable call around. */
	public static final String PERMIT_CALLAROUND_CONTROL = "PREFERENCES_PERMIT_CALLAROUND_CONTROL";

}