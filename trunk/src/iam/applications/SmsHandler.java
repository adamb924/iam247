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
/**
 * @author Adam
 * 
 */
public class SmsHandler {

	/**
	 * Returns a normalized phone number. If the number starts with zero, that
	 * zero is replaced with the (localizable) resource string
	 * R.string.loc_country_phonecode.
	 * 
	 * @param context
	 *            the context
	 * @param old
	 *            the old
	 * @return the normalized phone number
	 */
	static public String getNormalizedPhoneNumber(final Context context,
			final String old) {
		String ret = old;
		if (ret.isEmpty()) {
			return ret;
		}
		if (ret.charAt(0) == '0') {
			ret = ret.replaceFirst("0",
					context.getString(R.string.loc_country_phonecode));
		}
		return ret;
	}

	/**
	 * Send an SMS message to the given phone number, with the given message.
	 * 
	 * @param context
	 *            the context
	 * @param phoneNumber
	 *            the phone number
	 * @param message
	 *            the message
	 */
	static public void sendSms(final Context context, final String phoneNumber,
			final String message) {
		// this application context is required because it's not allowed to
		// register broadcast receivers from a broadcast receiver (which this
		// is, being called from SmsReceiver)
		final Context appContext = context.getApplicationContext();

		final SmsManager sms = SmsManager.getDefault();
		final ArrayList<String> parts = sms.divideMessage(message);

		// these intents are collected by SmsReceiver
		final ArrayList<PendingIntent> sentPIArray = new ArrayList<PendingIntent>();
		final ArrayList<PendingIntent> deliveredPIArray = new ArrayList<PendingIntent>();
		Intent sentIntent, deliveredIntent;
		for (int i = 0; i < parts.size(); i++) {
			// curiously, passing SmsReceiver.SMS_SENT instead of the identical
			// string literal doesn't work
			sentIntent = new Intent("iam.applications.SmsReceiver.SMS_SENT");
			// This extras are used in SmsReceiver.processSmsSent()
			sentIntent.putExtra(SmsHandler.PHONE_NUMBER, phoneNumber);
			sentIntent.putExtra(SmsHandler.MESSAGE, message);
			sentPIArray.add(PendingIntent.getBroadcast(appContext, 0,
					sentIntent, PendingIntent.FLAG_UPDATE_CURRENT));

			// curiously, passing SmsReceiver.SMS_DELIVERED instead of the
			// identical string literal doesn't work
			deliveredIntent = new Intent(
					"iam.applications.SmsReceiver.SMS_DELIVERED");
			// This extras are used in SmsReceiver.processSmsDelivered()
			deliveredIntent.putExtra(SmsHandler.PHONE_NUMBER, phoneNumber);
			deliveredIntent.putExtra(SmsHandler.MESSAGE, message);
			deliveredPIArray.add(PendingIntent.getBroadcast(appContext, 0,
					deliveredIntent, PendingIntent.FLAG_UPDATE_CURRENT));
		}

		HomeActivity.sendRefreshAlert(context);

		// Log.i("Debug", phoneNumber);
		// Log.i("Debug", message);

		sms.sendMultipartTextMessage(phoneNumber, null, parts, sentPIArray,
				deliveredPIArray);

		// add the message to the "pending" SQL table, to be corrected when
		// confirmation of its being sent and being delivered are received
		final DbAdapter dbHelper = new DbAdapter(context);
		dbHelper.open();
		dbHelper.addMessagePending(phoneNumber, message);
		dbHelper.close();
	}

	/** The application context. */
	private transient final Context mContext;

	/** The phone number of the SMS. */
	private transient final String mPhoneNumber;

	/** The message. */
	private transient final String mMessage;

	/** The contact id of the sender (or -1). */
	private transient final long mContactId;

	/** The house id of the sender (or -1). */
	private transient final long mHouseId;
	/** The database interface. */
	private transient final DbAdapter mDbHelper;

	/** The phone number. */
	static public final String PHONE_NUMBER = "PHONE_NUMBER";

	/** The message. */
	static public final String MESSAGE = "MESSAGE";

	/** The m settings. */
	final transient private SharedPreferences mSettings;

	/**
	 * Instantiates a new sms handler.
	 * 
	 * @param context
	 *            the context
	 * @param number
	 *            the phone number of the sms
	 * @param text
	 *            the text of the sms
	 * @param failIfUnknown
	 *            the fail if unknown
	 */
	SmsHandler(final Context context, final String number, final String text,
			final boolean failIfUnknown) {
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
			processUnknownNumber();
		} else if (messageMatches(R.string.re_turnoffcallaround)) {
			disableCallaround();
		} else if (messageMatches(R.string.re_turnoncallaround)) {
			enableCallaround();
		} else if (messageMatches(R.string.re_callaround_ok)) {
			resolveCallaround();
		} else if (messageMatches(R.string.re_undocallaround)) {
			unresolveCallaround();
		} else if (messageMatches(R.string.re_startcheckin_with)) {
			addCheckin(true);
		} else if (messageMatches(R.string.re_startcheckin)) {
			addCheckin(false);
		} else if (messageMatches(R.string.re_checkin_back)) {
			// we must check R.string.re_startcheckin before
			// R.string.re_checkin_back, since the "back" keyword would
			// otherwise be caught. This would be resolved with improved
			// parsing.
			back();
		} else if (messageMatches(R.string.re_checkin_arrived)) {
			arrived();
		} else if (messageMatches(R.string.re_permission)) {
			sendForbiddenLocations(context);
		} else if (messageMatches(R.string.re_turnoffreminders)) {
			turnOffReminders();
		} else if (messageMatches(R.string.re_turnonreminders)) {
			turnOnReminders();
		} else if (messageMatches(R.string.re_returning)) {
			returning();
		} else if (messageMatches(R.string.re_thisis)) {
			sendSms(R.string.sms_contact_exists);
		} else if (messageMatches(R.string.re_report)) {
			requestReport();
		} else if (messageMatches(R.string.re_delay)) {
			delayCallaround();
		} else if (messageMatches(R.string.re_keywords)) {
			sendLocationKeywords();
		} else if (messageMatches(R.string.re_houses)) {
			sendHouses();
		} else if (messageMatches(R.string.re_staying_at)) {
			joinHouse();
		} else if (messageMatches(R.string.re_leaving)) {
			leaveHouse();
		} else {
			yourError();
		}

		// send a notification for any active activity to update
		HomeActivity.sendRefreshAlert(context);

		mDbHelper.close();
	}

	/**
	 * Adds a check-in to the database, handling the response values as
	 * appropriate.
	 * 
	 * @param with
	 *            whether the user is adding a "with" keyword
	 */
	private void addCheckin(final boolean with) {
		String[] matches;
		String place, keyword, withWhom, returnBy;
		if (with) {
			matches = getMessageMatches(R.string.re_startcheckin_with);
			if (matches.length < 4) {
				yourError();
				return;
			}
			place = matches[0];
			keyword = matches[1];
			withWhom = matches[2];
			returnBy = matches[3];
		} else {
			matches = getMessageMatches(R.string.re_startcheckin);
			if (matches.length < 3) {
				yourError();
				return;
			}
			place = matches[0];
			keyword = matches[1];
			withWhom = "";
			returnBy = matches[2];
		}

		final Date time = Time.timeFromString(mContext, returnBy);
		if (time == null) {
			yourError();
			return;
		}

		if (!mDbHelper.getLocationKeywordExists(keyword)) {
			needLegitimateKeyword();
			return;
		}

		// send a message if the person is not allowed to go there
		if (!mDbHelper.getLocationKeywordPermitted(keyword)) {
			sendSms(R.string.sms_refuse_permission);
			return;
		}

		final int ret = mDbHelper.addCheckin(mContactId, place, keyword, time,
				withWhom);

		if (ret == DbAdapter.NOTIFY_FAILURE) {
			ourError();
		} else if (ret == DbAdapter.NOTIFY_EXISTING_CHECKIN_RESOLVED) {
			final String message = String.format(
					mContext.getString(R.string.sms_confirm_checkin_request),
					place, Time.timeTodayTomorrow(mContext, time))
					+ " "
					+ mContext
							.getString(R.string.sms_existing_checkin_resolved);
			sendSms(message);
		} else {
			final String message = String.format(
					mContext.getString(R.string.sms_confirm_checkin_request),
					place, Time.timeTodayTomorrow(mContext, time));
			sendSms(message);
		}

		if (ret != DbAdapter.NOTIFY_FAILURE
				&& mDbHelper.getContactPreference(mContactId,
						DbAdapter.USER_PREFERENCE_CHECKIN_REMINDER)) {
			AlarmAdapter.setCheckinReminderAlert(mContext,
					mDbHelper.lastInsertId());
		}
	}

	/**
	 * Process an "arrived" message by resolving the check-in.
	 */
	private void arrived() {
		final int ret = mDbHelper.setCheckinResolved(mContactId, true);
		if (ret == DbAdapter.NOTIFY_SUCCESS) {
			sendSms(R.string.sms_acknowledge_arrived_checkin);
		} else if (ret == DbAdapter.NOTIFY_FAILURE) {
			ourError();
		} else if (ret == DbAdapter.NOTIFY_ALREADY) {
			sendSms(R.string.sms_alreadyin);
		}
	}

	/**
	 * Process a "back" message by resolving an entire trip.
	 */
	private void back() {
		final int ret = mDbHelper.setTripResolvedFromContact(mContactId);
		if (ret == DbAdapter.NOTIFY_SUCCESS) {
			sendSms(R.string.sms_acknowledge_trip_resolved_checkin);
		} else if (ret == DbAdapter.NOTIFY_FAILURE) {
			ourError();
		} else if (ret == DbAdapter.NOTIFY_ALREADY) {
			sendSms(R.string.sms_alreadyin);
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

		final int ret = mDbHelper.setCallaroundDelayed(mHouseId, true);
		if (ret == DbAdapter.NOTIFY_SUCCESS) {
			final String time = Time.iso8601Time(Time
					.timeFromSimpleTime(mSettings.getString(
							HomeActivity.PREFERENCES_CALLAROUND_DELAYED_TIME,
							"23:59")));
			final String message = String.format(mContext
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
		if (!mSettings.getBoolean(
				HomeActivity.PREFERENCES_PERMIT_CALLAROUND_CONTROL, false)) {
			return;
		}

		if (mHouseId == -1) {
			sendSms(R.string.sms_callaround_nohouse);
			return;
		}

		final int ret = mDbHelper.setCallaroundActive(mHouseId, false);
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
		if (!mSettings.getBoolean(
				HomeActivity.PREFERENCES_PERMIT_CALLAROUND_CONTROL, false)) {
			return;
		}

		if (mHouseId == -1) {
			sendSms(R.string.sms_callaround_nohouse);
			return;
		}

		final int ret = mDbHelper.setCallaroundActive(mHouseId, true);
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
		mDbHelper.close();
		super.finalize();
	}

	/**
	 * Returns an array with captured strings from the regular expression.
	 * 
	 * @param stringId
	 *            the resource ID of the regular expression
	 * @return an array with captured strings from the regular expression
	 */
	private String[] getMessageMatches(final int stringId) {
		final Resources resources = mContext.getResources();
		final Pattern pattern = Pattern.compile(resources.getString(stringId),
				Pattern.CASE_INSENSITIVE);
		final Matcher matcher = pattern.matcher(mMessage);
		if (matcher.find()) {
			String[] matches = new String[matcher.groupCount()];
			for (int i = 1; i <= matcher.groupCount(); i++) {
				matches[i - 1] = matcher.group(i);
			}
			return matches;
		} else {
			return new String[0];
		}
	}

	private void joinHouse() {
		final String[] matches = getMessageMatches(R.string.re_staying_at);
		if (matches.length < 1) {
			yourError();
			mDbHelper.close();
		}
		final String house = matches[0];
		final long houseId = mDbHelper.getHouseId(house);

		if (houseId == -1) {
			final String message = String.format(
					mContext.getString(R.string.sms_not_a_house),
					mDbHelper.getHouses());
			sendSms(message);
			return;
		}

		if (mDbHelper.setHouse(mContactId, houseId) == DbAdapter.NOTIFY_HASHOUSE) {
			final String message = String.format(
					mContext.getString(R.string.sms_staying_confirmation),
					house);
			sendSms(message);
		} else {
			ourError();
		}
	}

	private void leaveHouse() {
		if (mDbHelper.setHouse(mContactId, -1) == DbAdapter.NOTIFY_FAILURE) {
			ourError();
		} else {
			sendSms(R.string.sms_leaving_confirmation);
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
	private boolean messageMatches(final int stringId) {
		final Resources resources = mContext.getResources();
		final Pattern pattern = Pattern.compile(resources.getString(stringId),
				Pattern.CASE_INSENSITIVE);
		final Matcher matcher = pattern.matcher(mMessage);

		return matcher.matches();
	}

	/**
	 * Need legitimate keyword.
	 */
	private void needLegitimateKeyword() {
		final String message = String.format(
				mContext.getString(R.string.sms_need_location_keyword),
				mDbHelper.getLocationKeywords());
		sendSms(message);
	}

	/**
	 * Send the user an SMS indicating a system error that is not his/her fault.
	 */
	private void ourError() {
		sendSms(R.string.sms_247_error);
	}

	/**
	 * Handle the situation where the phone number is unrecognized.
	 */
	private void processUnknownNumber() {
		// perhaps he is identifying himself
		if (messageMatches(R.string.re_thisis)) {
			final boolean thisisAllowed = mSettings.getBoolean(
					HomeActivity.PREFERENCES_PERMIT_THISIS, false);
			if (!thisisAllowed) {
				sendSms(R.string.sms_this_disabled_notification);
				return;
			}

			final String[] matches = getMessageMatches(R.string.re_thisis);
			if (matches.length < 2) {
				yourError();
				mDbHelper.close();
			}
			final String name = matches[0];
			final String house = matches[1];
			final long contactId = mDbHelper.addContact(name, mPhoneNumber);
			final long houseId = mDbHelper.getHouseId(house);

			if (houseId == -1) {
				final String message = String.format(
						mContext.getString(R.string.sms_not_a_house),
						mDbHelper.getHouses());
				sendSms(message);
				return;
			}

			mDbHelper.setHouse(contactId, houseId);

			if (contactId > -1) {
				final String message = String.format(mContext
						.getString(R.string.sms_requestid_acknowledgement),
						name);
				sendSms(message);
			} else {
				ourError();
			}

		} else { // otherwise request that he identify himself
			requestId();
		}
	}

	/**
	 * Initiates the Red Alert activity.
	 */
	private void redAlert() {
		final Intent intent = new Intent(mContext, RedAlert.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.putExtra(RedAlert.FIELD_ID, mContactId);
		intent.putExtra(RedAlert.FIELD_NUMBER, mPhoneNumber);
		mContext.startActivity(intent);
	}

	/**
	 * Send the user an SMS requesting that s/he identify him/herself.
	 */
	private void requestId() {
		sendSms(R.string.sms_requestid);
	}

	/**
	 * Respond to a request for a report. If the person has permission, send the
	 * report.
	 */
	private void requestReport() {
		if (mDbHelper.getContactPermission(mContactId,
				DbAdapter.USER_PERMISSION_REPORT)) {
			sendSms(mDbHelper.getReport());
		}
	}

	/**
	 * Resolves the user's call around.
	 */
	private void resolveCallaround() {
		if (mHouseId == -1) {
			sendSms(R.string.sms_callaround_nohouse);
			return;
		}

		final int ret = mDbHelper.setCallaroundResolved(mHouseId, true);
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
	 * Records the user as being heading for home.
	 */
	private void returning() {
		final String[] matches = getMessageMatches(R.string.re_returning);
		if (matches.length != 1) {
			return;
		}

		final Date time = Time.timeFromString(mContext, matches[0]);
		if (time == null) {
			yourError();
			return;
		}

		final String place = mContext.getString(R.string.home);

		final int ret = mDbHelper.addCheckin(mContactId, place, "--", time, "");

		if (ret == DbAdapter.NOTIFY_FAILURE) {
			ourError();
		} else if (ret == DbAdapter.NOTIFY_EXISTING_CHECKIN_RESOLVED) {
			final String message = String.format(
					mContext.getString(R.string.sms_confirm_checkin_request),
					place, Time.timeTodayTomorrow(mContext, time))
					+ " "
					+ mContext
							.getString(R.string.sms_existing_checkin_resolved);
			sendSms(message);
		} else {
			final String message = String.format(
					mContext.getString(R.string.sms_confirm_checkin_request),
					place, Time.timeTodayTomorrow(mContext, time));
			sendSms(message);
		}

		if (ret != DbAdapter.NOTIFY_FAILURE
				&& mDbHelper.getContactPreference(mContactId,
						DbAdapter.USER_PREFERENCE_CHECKIN_REMINDER)) {
			AlarmAdapter.setCheckinReminderAlert(mContext,
					mDbHelper.lastInsertId());
		}
	}

	/**
	 * Sends the user the current list of forbidden locations.
	 * 
	 * @param context
	 *            the context
	 */
	private void sendForbiddenLocations(final Context context) {
		final String forbidden = mDbHelper.getForbiddenLocations();
		if (forbidden == null) {
			sendSms(R.string.sms_forbidden_none);
		} else {
			final String message = String.format(
					context.getString(R.string.sms_forbidden), forbidden);
			sendSms(message);
		}
	}

	/**
	 * Send the user the list of houses.
	 */
	private void sendHouses() {
		final String message = String.format(
				mContext.getString(R.string.sms_houses), mDbHelper.getHouses());
		sendSms(message);
	}

	/**
	 * Send the user the list of location keywords.
	 */
	private void sendLocationKeywords() {
		final String message = String.format(
				mContext.getString(R.string.sms_location_keywords),
				mDbHelper.getLocationKeywords());
		sendSms(message);
	}

	/**
	 * Send the string specified by the resource ID as an SMS.
	 * 
	 * @param messageId
	 *            the message id
	 */
	private void sendSms(final int messageId) {
		sendSms(mContext, mPhoneNumber, mContext.getString(messageId));
	}

	/**
	 * Send the string in an SMS message.
	 * 
	 * @param message
	 *            the message
	 */
	private void sendSms(final String message) {
		sendSms(mContext, mPhoneNumber, message);
	}

	/**
	 * Turn off checkin reminders for the user.
	 */
	private void turnOffReminders() {
		final int ret = mDbHelper.setContactPreference(mContactId,
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
	 * Turn on checkin reminders for the user.
	 */
	private void turnOnReminders() {
		final int ret = mDbHelper.setContactPreference(mContactId,
				DbAdapter.USER_PREFERENCE_CHECKIN_REMINDER, true);
		if (ret == DbAdapter.NOTIFY_SUCCESS) {
			sendSms(R.string.sms_checkin_reminder_on_confirm);

			// add a check-in reminder for the person, if they have an active
			// check-in
			final long current_checkin = mDbHelper
					.getCurrentCheckinForContact(mContactId);
			if (current_checkin != -1) {
				AlarmAdapter.setCheckinReminderAlert(mContext, current_checkin);
			}
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

		final int ret = mDbHelper.setCallaroundResolved(mHouseId, false);
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
