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

/**
 * This class processes SMS messages, and calls <code>DbAdapter</code> methods,
 * and sends response SMS messages, as appropriate.
 */
public class SmsHandler {
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

	/**
	 * Send an SMS message to the given phone number, witht the given message.
	 * 
	 * @param phoneNumber
	 *            the phone number
	 * @param message
	 *            the message
	 */
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
			// curiously, passing SmsReceiver.SMS_SENT instead of the identical
			// string literal doesn't work
			Intent sentIntent = new Intent(
					"iam.applications.SmsReceiver.SMS_SENT");
			// This extras are used in SmsReceiver.processSmsSent()
			sentIntent.putExtra(SmsHandler.PHONE_NUMBER, phoneNumber);
			sentIntent.putExtra(SmsHandler.MESSAGE, message);
			sentPIArray.add(PendingIntent.getBroadcast(appContext, 0,
					sentIntent, PendingIntent.FLAG_UPDATE_CURRENT));

			// curiously, passing SmsReceiver.SMS_DELIVERED instead of the
			// identical string literal doesn't work
			Intent deliveredIntent = new Intent(
					"iam.applications.SmsReceiver.SMS_DELIVERED");
			// This extras are used in SmsReceiver.processSmsDelivered()
			deliveredIntent.putExtra(SmsHandler.PHONE_NUMBER, phoneNumber);
			deliveredIntent.putExtra(SmsHandler.MESSAGE, message);
			deliveredPIArray.add(PendingIntent.getBroadcast(appContext, 0,
					deliveredIntent, PendingIntent.FLAG_UPDATE_CURRENT));
		}

		AlarmReceiver.sendRefreshAlert(context);

		sms.sendMultipartTextMessage(phoneNumber, null, parts, sentPIArray,
				deliveredPIArray);

		// add the message to SQL tables, to be deleted when confirmation of
		// being sent and being delivered are received
		DbAdapter dbHelper = new DbAdapter(context);
		dbHelper.open();
		dbHelper.addMessagePendingSent(phoneNumber, message);
		dbHelper.addMessagePendingDelivered(phoneNumber, message);
		dbHelper.close();
	}

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

	static public String PHONE_NUMBER = "PHONE_NUMBER";

	static public String MESSAGE = "MESSAGE";

	private SharedPreferences mSettings;

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

		mContext = context;
		mPhoneNumber = getNormalizedPhoneNumber(context, number);
		mMessage = text.trim();
		mContactId = mDbHelper.getContactId(number);
		mHouseId = mDbHelper.getHouseId(mContactId);

		mSettings = PreferenceManager.getDefaultSharedPreferences(mContext);

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
				boolean thisisAllowed = mSettings.getBoolean(
						HomeActivity.PREFERENCES_PERMIT_THISIS, false);
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
			// I'm removing this feature till I can recall what it was for.
			// } else if (messageMatches(R.string.re_startcheckin_nocheckin)) {
			// // this condition must come before that of
			// R.string.re_startcheckin
			// addCheckin(false);
		} else if (messageMatches(R.string.re_startcheckin)) {
			addCheckin(true);
		} else if (messageMatches(R.string.re_permission)) {
			sendForbiddenLocations(context);
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
		} else if (messageMatches(R.string.re_delay)) {
			delayCallaround();
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
	 * @param requestCheckin
	 *            whether the user is requesting a checkin
	 */
	private void addCheckin(boolean requestCheckin) {
		String[] matches;
		if (requestCheckin) {
			matches = getMessageMatches(R.string.re_startcheckin);
		} else {
			matches = getMessageMatches(R.string.re_startcheckin_nocheckin);
		}

		if (matches == null || matches.length < 2) {
			yourError();
			mDbHelper.close();
			return;
		}
		String place = matches[0];
		Date time = Time.timeFromString(mContext, matches[1]);
		if (time == null) {
			yourError();
			mDbHelper.close();
			return;
		}

		// check that there is a real location keyword
		String keyword = parseLocationKeyword(place);
		if (keyword == null) {
			needLegitimateKeyword();
			mDbHelper.close();
			return;
		}

		// send a message if the person is not allowed to go there
		if (!mDbHelper.getLocationKeywordPermitted(keyword)) {
			sendSms(R.string.sms_refuse_permission);
			mDbHelper.close();
			return;
		}

		int ret = mDbHelper.addCheckin(mContactId, place, time, requestCheckin);

		if (ret == DbAdapter.NOTIFY_FAILURE) {
			ourError();
		} else if (ret == DbAdapter.NOTIFY_EXISTING_CHECKIN_RESOLVED) {
			String message = String.format(
					mContext.getString(R.string.sms_confirm_checkin_request),
					place, Time.timeTodayTomorrow(mContext, time))
					+ " "
					+ mContext
							.getString(R.string.sms_existing_checkin_resolved);
			sendSms(message);
		} else {
			String message = String.format(
					mContext.getString(R.string.sms_confirm_checkin_request),
					place, Time.timeTodayTomorrow(mContext, time));
			sendSms(message);
		}

		if (ret != DbAdapter.NOTIFY_FAILURE
				&& mDbHelper.getContactPreference(mContactId,
						DbAdapter.USER_PREFERENCE_CHECKIN_REMINDER)) {
			AlarmReceiver.setCheckinReminderAlert(mContext, time,
					mDbHelper.lastInsertId());
		}
	}

	/**
	 * Takes the last word from the place string, returning the place string if
	 * it is a legitimate keyword, or otherwise returning a null pointer.
	 * 
	 * @param place
	 *            The place string
	 * @return The place string, if it is legitimate, otherwise null.
	 */
	private String parseLocationKeyword(String place) {
		place = place.trim();

		int space = place.lastIndexOf(" ");

		if (space == -1 || space == place.length()) {
			return null;
		}
		String putativeKeyword = place.substring(space + 1);

		if (mDbHelper.getLocationKeywordExists(putativeKeyword)) {
			return putativeKeyword;
		} else {
			return null;
		}
	}

	/**
	 * Delays callaround for the user.
	 */
	private void delayCallaround() {
		if (mHouseId == -1) {
			sendSms(R.string.sms_callaround_nohouse);
			return;
		}

		int ret = mDbHelper.setCallaroundDelayed(mHouseId, true);
		if (ret == DbAdapter.NOTIFY_SUCCESS) {
			String time = Time.iso8601Time(Time.timeFromSimpleTime(mSettings
					.getString(
							HomeActivity.PREFERENCES_CALLAROUND_DELAYED_TIME,
							"23:59")));
			String message = String.format(mContext
					.getString(R.string.sms_callaround_delay_confirmation),
					time);
			sendSms(message);
		} else if (ret == DbAdapter.NOTIFY_FAILURE) {
			ourError();
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

		return m.matches();
	}

	private void needLegitimateKeyword() {
		String msg = String.format(
				mContext.getString(R.string.sms_need_location_keyword),
				mDbHelper.getLocationKeywords());
		sendSms(msg);
	}

	/**
	 * Send the user an SMS indicating a system error that is not his/her fault.
	 */
	private void ourError() {
		sendSms(R.string.sms_247_error);
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
		} else if (ret == DbAdapter.NOTIFY_UNTIMELY) {
			sendSms(R.string.sms_callaround_not_timely);
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

	/**
	 * Sends the user the current list of forbidden locations.
	 * 
	 * @param context
	 */
	private void sendForbiddenLocations(Context context) {
		String forbidden = mDbHelper.getForbiddenLocations();
		if (forbidden == null) {
			sendSms(R.string.sms_forbidden_none);
		} else {
			String message = String.format(
					context.getString(R.string.sms_forbidden), forbidden);
			sendSms(message);
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
	 * Send the user an SMS indicating that their input was unrecognizable.
	 * 
	 */
	private void yourError() {
		sendSms(R.string.sms_message_error);
	}

}
