package iam.applications;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;

/**
 * This <code>BroadcastReceiver</code> subclass receives a text message, and
 * passes the number and the message text on to <code>SmsHandler</code>. This
 * class also handles SMS sent and delivered messages.
 */
public class SmsReceiver extends BroadcastReceiver {
	public static String SMS_SENT = "iam.applications.SmsReceiver.SMS_SENT";
	public static String SMS_DELIVERED = "iam.applications.SmsReceiver.SMS_DELIVERED";

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.content.BroadcastReceiver#onReceive(android.content.Context,
	 * android.content.Intent)
	 */
	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();

		if (action.equals("android.provider.Telephony.SMS_RECEIVED")) {
			processSms(context, intent);
		} else if (action.equals("iam.applications.SmsReceiver.SMS_SENT")) {
			processSmsSent(context, intent);
		} else if (action.equals("iam.applications.SmsReceiver.SMS_DELIVERED")) {
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
	private void processSms(Context context, Intent intent) {
		Bundle bundle = intent.getExtras();
		if (bundle == null || !bundle.containsKey("pdus")) {
			return;
		}
		Object[] pdus = (Object[]) bundle.get("pdus");
		for (int i = 0; i < pdus.length; i++) {
			SmsMessage msg = SmsMessage.createFromPdu((byte[]) pdus[i]);
			String number = msg.getOriginatingAddress();
			String message = msg.getMessageBody();
			String[] commands = message.split("\\.\\.\\.");
			for (int j = 0; j < commands.length; j++) {
				new SmsHandler(context, number, commands[i], false);
			}
		}
	}

	/**
	 * Processes notifications that an SMS was sent (or not).
	 * 
	 * @param context
	 * @param intent
	 */
	private void processSmsSent(Context context, Intent intent) {
		DbAdapter dbHelper = new DbAdapter(context);
		dbHelper.open();

		String number = intent.getStringExtra(SmsHandler.PHONE_NUMBER);
		String message = intent.getStringExtra(SmsHandler.MESSAGE);

		if (getResultCode() == Activity.RESULT_OK) {
			dbHelper.deleteMessagePendingSent(number, message);
			AlarmReceiver.sendRefreshAlert(context);

		}

		dbHelper.close();
	}

	/**
	 * Processes notifications that an SMS has been delivered (or not).
	 * 
	 * @param context
	 * @param intent
	 */
	private void processSmsDelivered(Context context, Intent intent) {
		DbAdapter dbHelper = new DbAdapter(context);
		dbHelper.open();

		String number = intent.getStringExtra(SmsHandler.PHONE_NUMBER);
		String message = intent.getStringExtra(SmsHandler.MESSAGE);

		if (getResultCode() == Activity.RESULT_OK) {
			dbHelper.deleteMessagePendingDelivered(number, message);
			AlarmReceiver.sendRefreshAlert(context);
		}

		dbHelper.close();
	}
}
