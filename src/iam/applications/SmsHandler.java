package iam.applications;

import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.telephony.SmsManager;
import android.util.Log;

/**
 * This class processes SMS messages, and calls <code>DbAdapter</code> methods,
 * and sends response SMS messages, as appropriate.
 */
public class SmsHandler {
	/** The application context. */
	private final Context mContext;

	/** The phone number of the SMS. */
	private final String mPhoneNumber;

	/** The message. */
	private final String mMessage;

	/** The contact id of the sender (or -1). */
	private final long mContactId;

	/** The house id of the sender (or -1). */
	private final long mHouseId;

	/** The database interface. */
	private final DbAdapter mDbHelper;

	/**
	 * Instantiates a new sms handler.
	 * 
	 * @param context
	 *            the context
	 * @param number
	 *            the phone number of the sms
	 * @param text
	 *            the text of the sms
	 */
	SmsHandler(Context context, String number, String text,
			boolean failIfUnknown) {
		mDbHelper = new DbAdapter(context);
		mDbHelper.open();

		// Log.i("Debug", number);
		// Log.i("Debug", text);

		mContext = context;
		mPhoneNumber = getNormalizedPhoneNumber(context, number);
		mMessage = text.trim();
		mContactId = mDbHelper.getContactId(number);
		mHouseId = mDbHelper.getHouseId(mContactId);

		// ignore blocked numbers
		if (mDbHelper.getNumberIsBlocked(mPhoneNumber)) {
			mDbHelper.close();
			return;
		}

		// set a red alert for the distress signal, whether the contact is
		// recognized or not
		if (messageMatches(R.string.re_distress)) {
			redAlert();
			mDbHelper.close();
			return;
		}

		// so far this is used only for responding to a missed call
		if (mContactId == -1 && failIfUnknown) {
			return;
		}

		// is the user unknown?
		if (mContactId == -1) {
			// perhaps he is identifying himself
			if (messageMatches(R.string.re_thisis)) {
				SharedPreferences settings = PreferenceManager
						.getDefaultSharedPreferences(mContext);
				boolean thisisAllowed = settings
						.getBoolean(
								HomeActivity.PREFERENCES_CALLAROUNDS_SHOW_FUTURE,
								false);

				if (!thisisAllowed) {
					sendSms(R.string.sms_this_disabled_notification);
					mDbHelper.close();
					return;
				}

				String[] matches = getMessageMatches(R.string.re_thisis);
				if (matches == null) {
					yourError();
					mDbHelper.close();
					return;
				}
				String name = matches[0];
				mDbHelper.addContact(name, mPhoneNumber);

				String message = String.format(context
						.getString(R.string.sms_requestid_acknowledgement),
						name);
				sendSms(message);

			} else { // otherwise request that he identify himself
				requestId();
			}
			// send a notification for any active activity to update
			AlarmReceiver.sendRefreshAlert(context);
			mDbHelper.close();
			return;
		}
		// otherwise we can begin to parse the messages
		// by now it is certain that it's not an emergency, and the contact id
		// is known
		if (messageMatches(R.string.re_turnoffcallaround)) {
			disableCallaround();
		} else if (messageMatches(R.string.re_turnoncallaround)) {
			enableCallaround();
		} else if (messageMatches(R.string.re_callaround_ok)) {
			resolveCallaround();
		} else if (messageMatches(R.string.re_undocallaround)) {
			unresolveCallaround();
		} else if (messageMatches(R.string.re_checkin_back)) {
			resolveCheckin();
		} else if (messageMatches(R.string.re_startcheckin)) {

			String[] matches = getMessageMatches(R.string.re_startcheckin);
			if (matches == null || matches.length < 2) {
				yourError();
				mDbHelper.close();
				return;
			}
			Date time = Time.timeFromString(context, matches[1]);
			if (time == null) {
				yourError();
				mDbHelper.close();
				return;
			}
			addCheckin(matches[0], time, true);
		} else if (messageMatches(R.string.re_startcheckin_nocheckin)) {
			String[] matches = getMessageMatches(R.string.re_startcheckin_nocheckin);
			if (matches == null || matches.length < 2) {
				yourError();
				mDbHelper.close();
				return;
			}
			Date time = Time.timeFromString(context, matches[1]);
			if (time == null) {
				yourError();
				mDbHelper.close();
				return;
			}
			addCheckin(matches[0], time, false);
		} else if (messageMatches(R.string.re_permission)) {
			String forbidden = mDbHelper.getForbiddenLocations();
			if (forbidden == null) {
				sendSms(R.string.sms_forbidden_none);
			} else {
				String message = String.format(
						context.getString(R.string.sms_forbidden), forbidden);
				sendSms(message);
			}
		} else if (messageMatches(R.string.re_turnoffreminders)) {
			turnOffReminders();
		} else if (messageMatches(R.string.re_turnonreminders)) {
			turnOnReminders();
		} else if (messageMatches(R.string.re_thisis)) {
			// ignore extraneous thisis requests
			sendSms(R.string.sms_contact_exists);
		} else if (messageMatches(R.string.re_report)) {
			if (mDbHelper.getContactPermission(mContactId,
					DbAdapter.USER_PERMISSION_REPORT)) {
				sendSms(mDbHelper.getReport());
			}
		} else {
			yourError();
		}

		// send a notification for any active activity to update
		AlarmReceiver.sendRefreshAlert(context);

		mDbHelper.close();
	}

	/**
	 * Adds a check-in to the database, handling the response values as
	 * appropriate.
	 * 
	 * @param place
	 *            the place
	 * @param returntime
	 *            the return time
	 * @param requestCheckin
	 *            whether the user is requesting a checkin
	 */
	private void addCheckin(String place, Date returntime,
			boolean requestCheckin) {
		int ret = mDbHelper.addCheckin(mContactId, place, returntime,
				requestCheckin);

		if (ret == DbAdapter.NOTIFY_FAILURE) {
			ourError();
		} else if (ret == DbAdapter.NOTIFY_EXISTING_CHECKIN_RESOLVED) {
			String message = String.format(
					mContext.getString(R.string.sms_confirm_checkin_request),
					place, Time.timeTodayTomorrow(mContext, returntime))
					+ " "
					+ mContext
							.getString(R.string.sms_existing_checkin_resolved);
			sendSms(message);
		} else {
			String message = String.format(
					mContext.getString(R.string.sms_confirm_checkin_request),
					place, Time.timeTodayTomorrow(mContext, returntime));
			sendSms(message);
		}

		if (ret != DbAdapter.NOTIFY_FAILURE
				&& mDbHelper.getContactPreference(mContactId,
						DbAdapter.USER_PREFERENCE_CHECKIN_REMINDER)) {
			AlarmReceiver.setCheckinReminderAlert(mContext, returntime,
					mDbHelper.lastInsertId());
		}
	}

	/**
	 * Deactivates call around for the user.
	 */
	private void disableCallaround() {
		if (mHouseId == -1) {
			sendSms(R.string.sms_callaround_nohouse);
			return;
		}

		int ret = mDbHelper.setCallaroundActive(mHouseId, false);
		if (ret == DbAdapter.NOTIFY_SUCCESS) {
			sendSms(R.string.sms_callaround_disabled);
		} else if (ret == DbAdapter.NOTIFY_FAILURE) {
			ourError();
		} else if (ret == DbAdapter.NOTIFY_ALREADY) {
			sendSms(R.string.sms_callaround_disabled_already);
		}
	}

	/**
	 * Activates call around for the user.
	 */
	private void enableCallaround() {
		if (mHouseId == -1) {
			sendSms(R.string.sms_callaround_nohouse);
			return;
		}

		int ret = mDbHelper.setCallaroundActive(mHouseId, true);
		if (ret == DbAdapter.NOTIFY_SUCCESS) {
			sendSms(R.string.sms_callaround_enabled);
		} else if (ret == DbAdapter.NOTIFY_FAILURE) {
			ourError();
		} else if (ret == DbAdapter.NOTIFY_ALREADY) {
			sendSms(R.string.sms_callaround_enabled_already);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#finalize()
	 */
	@Override
	protected void finalize() throws Throwable {
		super.finalize();

		mDbHelper.close();
	}

	/**
	 * Returns an array with captured strings from the regular expression.
	 * 
	 * @param stringId
	 *            the resource ID of the regular expression
	 * @return an array with captured strings from the regular expression
	 */
	private String[] getMessageMatches(int stringId) {
		Resources r = mContext.getResources();
		Pattern p = Pattern.compile(r.getString(stringId),
				Pattern.CASE_INSENSITIVE);
		Matcher m = p.matcher(mMessage);
		if (m.find()) {
			String[] matches = new String[m.groupCount()];
			for (int i = 1; i <= m.groupCount(); i++) {
				matches[i - 1] = m.group(i);
			}
			return matches;
		} else {
			return null;
		}
	}

	/**
	 * Returns true if the message matches the regular expression in the given
	 * resource ID, otherwise false.
	 * 
	 * @param stringId
	 *            the resource ID of the regular expression
	 * @return true if the message matches the regular expression in the given
	 *         resource ID, otherwise false
	 */
	private boolean messageMatches(int stringId) {
		Resources r = mContext.getResources();
		Pattern p = Pattern.compile(r.getString(stringId),
				Pattern.CASE_INSENSITIVE);
		Matcher m = p.matcher(mMessage);

		// Log.i("Debug",mMessage);
		// Log.i("Debug",r.getString(stringId));

		return m.matches();
	}

	/**
	 * Send the user an SMS indicating a system error that is not his/her fault.
	 */
	private void ourError() {
		sendSms(R.string.sms_247_error);
	}

	/**
	 * Send the user an SMS indicating that their input was unrecognizable.
	 * 
	 */
	private void yourError() {
		sendSms(R.string.sms_message_error);
	}

	/**
	 * Initiates the Red Alert activity.
	 */
	private void redAlert() {
		Intent i = new Intent(mContext, RedAlert.class);
		i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		i.putExtra(RedAlert.FIELD_ID, mContactId);
		i.putExtra(RedAlert.FIELD_NUMBER, mPhoneNumber);
		mContext.startActivity(i);
	}

	/**
	 * Send the user an SMS requesting that s/he identify him/herself.
	 */
	private void requestId() {
		sendSms(R.string.sms_requestid);
	}

	/**
	 * Resolves the user's call around.
	 */
	private void resolveCallaround() {
		if (mHouseId == -1) {
			sendSms(R.string.sms_callaround_nohouse);
			return;
		}

		int ret = mDbHelper.setCallaroundResolved(mHouseId, true);
		if (ret == DbAdapter.NOTIFY_SUCCESS) {
			sendSms(R.string.sms_callaround_acknowledgement);
		} else if (ret == DbAdapter.NOTIFY_FAILURE) {
			ourError();
		} else if (ret == DbAdapter.NOTIFY_ALREADY) {
			sendSms(R.string.sms_callaround_unnecessary);
		} else if (ret == DbAdapter.NOTIFY_INACTIVE) {
			sendSms(R.string.sms_callaround_is_disabled);
		}
	}

	/**
	 * Undoes the user's call around.
	 */
	private void unresolveCallaround() {
		if (mHouseId == -1) {
			sendSms(R.string.sms_callaround_nohouse);
			return;
		}

		int ret = mDbHelper.setCallaroundResolved(mHouseId, false);
		if (ret == DbAdapter.NOTIFY_SUCCESS) {
			sendSms(R.string.sms_callaround_undo_acknowledgement);
		} else if (ret == DbAdapter.NOTIFY_FAILURE) {
			ourError();
		}
	}

	/**
	 * Resolve the user's check-in.
	 */
	private void resolveCheckin() {
		int ret = mDbHelper.setCheckinResolved(mContactId, true);
		if (ret == DbAdapter.NOTIFY_SUCCESS) {
			sendSms(R.string.sms_acknowledge_checkin);
		} else if (ret == DbAdapter.NOTIFY_FAILURE) {
			ourError();
		} else if (ret == DbAdapter.NOTIFY_ALREADY) {
			sendSms(R.string.sms_alreadyin);
		}
	}

	private void turnOnReminders() {
		int ret = mDbHelper.setContactPreference(mContactId,
				DbAdapter.USER_PREFERENCE_CHECKIN_REMINDER, true);
		if (ret == DbAdapter.NOTIFY_SUCCESS) {
			sendSms(R.string.sms_checkin_reminder_on_confirm);
		} else if (ret == DbAdapter.NOTIFY_FAILURE) {
			ourError();
		} else if (ret == DbAdapter.NOTIFY_ALREADY) {
			sendSms(R.string.sms_already);
		}
	}

	private void turnOffReminders() {
		int ret = mDbHelper.setContactPreference(mContactId,
				DbAdapter.USER_PREFERENCE_CHECKIN_REMINDER, false);
		if (ret == DbAdapter.NOTIFY_SUCCESS) {
			sendSms(R.string.sms_checkin_reminder_off_confirm);
		} else if (ret == DbAdapter.NOTIFY_FAILURE) {
			ourError();
		} else if (ret == DbAdapter.NOTIFY_ALREADY) {
			sendSms(R.string.sms_already);
		}
	}

	/**
	 * Send the string specified by the resource ID as an SMS.
	 * 
	 * @param messageId
	 *            the message id
	 */
	private void sendSms(int messageId) {
		sendSms(mContext, mPhoneNumber, mContext.getString(messageId));
	}

	/**
	 * Send the string in an SMS message.
	 * 
	 * @param message
	 *            the message
	 */
	private void sendSms(String message) {
		sendSms(mContext, mPhoneNumber, message);
	}

	/**
	 * Send an SMS message to the given phone number, witht the given message.
	 * 
	 * @param phoneNumber
	 *            the phone number
	 * @param message
	 *            the message
	 */
	// static public void sendSms(final Context context, String phoneNumber,
	// String message) {
	static public void sendSms(final Context context, String phoneNumber,
			String message) {
		// this application context is required because it's not allowed to
		// register broadcast receivers from a broadcast receiver (which this
		// is, being called from SmsReceiver)
		Context appContext = context.getApplicationContext();

		SmsManager sms = SmsManager.getDefault();
		ArrayList<String> parts = sms.divideMessage(message);

		// these intents are collected by SmsReceiver
		ArrayList<PendingIntent> sentPIArray = new ArrayList<PendingIntent>();
		ArrayList<PendingIntent> deliveredPIArray = new ArrayList<PendingIntent>();
		for (int i = 0; i < parts.size(); i++) {
			sentPIArray.add(PendingIntent.getBroadcast(appContext, 0,
					new Intent("iam.applications.SmsReceiver.SMS_SENT"), 0));

			deliveredPIArray.add(PendingIntent
					.getBroadcast(appContext, 0, new Intent(
							"iam.applications.SmsReceiver.SMS_DELIVERED"), 0));
		}

		sms.sendMultipartTextMessage(phoneNumber, null, parts, sentPIArray,
				deliveredPIArray);

		Log.i("Debug", phoneNumber);
		Log.i("Debug", message);
	}

	/**
	 * Returns a normalized phone number. If the number starts with zero, that
	 * zero is replaced with the (localizable) resource string
	 * R.string.loc_country_phonecode.
	 */
	static public String getNormalizedPhoneNumber(Context context, String old) {
		String r = old;
		if (r.startsWith("0")) {
			r = r.replaceFirst("0",
					context.getString(R.string.loc_country_phonecode));
		}
		return r;
	}

}