package iam.applications;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.SmsMessage;

/**
 * This <code>BroadcastReceiver</code> subclass receives a text message, and
 * passes the number and the message text on to <code>SmsHandler</code>. This
 * class also handles SMS sent and delivered messages.
 */
public class SmsReceiver extends BroadcastReceiver {
	/*
	 * (non-Javadoc)
	 * 
	 * @see android.content.BroadcastReceiver#onReceive(android.content.Context,
	 * android.content.Intent)
	 */
	@Override
	public void onReceive(final Context context, final Intent intent) {
		final String action = intent.getAction();

		final SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(context);
		final boolean disabled = settings.getBoolean(
				HomeActivity.PREFERENCES_DISABLE_247, false);
		if (disabled) {
			return;
		}

		// process the message according to whether it is a received SMS, or a
		// sent confirmation, or a delivery confirmation
		if ("android.provider.Telephony.SMS_RECEIVED".equals(action)) {
			processSms(context, intent);
		} else if ("iam.applications.SmsReceiver.SMS_SENT".equals(action)) {
			processSmsSent(context, intent);
		} else if ("iam.applications.SmsReceiver.SMS_DELIVERED".equals(action)) {
			processSmsDelivered(context, intent);
		}
	}

	/**
	 * Processes SMS messages, sending them to <code>SmsHandler</code>. Messages
	 * with text delimited by "..." are split and send as separate SMS messages.
	 * 
	 * @param context
	 * @param intent
	 */
	private void processSms(final Context context, final Intent intent) {
		final SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(context);
		final boolean requirePrefix = settings.getBoolean(
				HomeActivity.PREFERENCES_REQUIRE_PREFIX, false);

		final Bundle bundle = intent.getExtras();
		if (bundle == null || !bundle.containsKey("pdus")) {
			return;
		}
		final Object[] pdus = (Object[]) bundle.get("pdus");
		for (int i = 0; i < pdus.length; i++) {
			final SmsMessage msg = SmsMessage.createFromPdu((byte[]) pdus[i]);
			final String number = msg.getOriginatingAddress();
			final String message = msg.getMessageBody();

			if (!requirePrefix
					|| message.startsWith(context
							.getString(R.string.re_command_prefix))) {
				// multiple commands can be sent if they are delimited by ...
				// This
				// might be useful for some applications.
				final String[] commands = message.split("\\.\\.\\.");
				for (int j = 0; j < commands.length; j++) {
					new SmsHandler(context, number, commands[i], false);
				}
			}
		}
	}

	/**
	 * Processes notifications that an SMS was sent (or not).
	 * 
	 * @param context
	 *            the current context
	 * @param intent
	 *            the Intent object from the message
	 */
	private void processSmsSent(final Context context, final Intent intent) {
		final DbAdapter dbHelper = new DbAdapter(context);
		dbHelper.open();

		// this information is added to the Intent in SmsHandler.sendSms()
		final String number = intent.getStringExtra(SmsHandler.PHONE_NUMBER);
		final String message = intent.getStringExtra(SmsHandler.MESSAGE);

		// if the message was successful, delete it from the pending-sent table
		if (getResultCode() == Activity.RESULT_OK) {
			dbHelper.setPendingMessageSent(number, message);
			AlarmReceiver.sendRefreshAlert(context);
		}

		dbHelper.close();
	}

	/**
	 * Processes notifications that an SMS has been delivered (or not).
	 * 
	 * @param context
	 *            the current context
	 * @param intent
	 *            the Intent object from the message
	 */
	private void processSmsDelivered(final Context context, final Intent intent) {
		final DbAdapter dbHelper = new DbAdapter(context);
		dbHelper.open();

		// this information is added to the Intent in SmsHandler.sendSms()
		final String number = intent.getStringExtra(SmsHandler.PHONE_NUMBER);
		final String message = intent.getStringExtra(SmsHandler.MESSAGE);

		// if the message was successful, delete it from the pending-delivered
		// table
		if (getResultCode() == Activity.RESULT_OK) {
			dbHelper.setPendingMessageDelivered(number, message);
			AlarmReceiver.sendRefreshAlert(context);
		}

		dbHelper.close();
	}
}
