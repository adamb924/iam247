package iam.applications;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;

/**
 * This <code>BroadcastReceiver</code> subclass receives a text message, and
 * passes the number and the message text on to <code>SmsHandler</code>. This
 * class also handles SMS sent and delivered messages.
 */
public class SmsReceiver extends BroadcastReceiver {
	public static String SMS_SENT = "SMS_SENT";
	public static String SMS_DELIVERED = "SMS_DELIVERED";

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
	 * @param context
	 * @param intent
	 */
	private void processSmsSent(Context context, Intent intent) {
		DbAdapter dbHelper = new DbAdapter(context);
		dbHelper.open();

		switch (getResultCode()) {
		case Activity.RESULT_OK:
			// dbHelper.addLogEvent(DbAdapter.LOG_TYPE_SMS_NOTIFICATION,
			// "SMS sent");
			break;
		case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
			dbHelper.addLogEvent(DbAdapter.LOG_TYPE_SMS_NOTIFICATION,
					"Generic failure");
			break;
		case SmsManager.RESULT_ERROR_NO_SERVICE:
			dbHelper.addLogEvent(DbAdapter.LOG_TYPE_SMS_NOTIFICATION,
					"No service");
			break;
		case SmsManager.RESULT_ERROR_NULL_PDU:
			dbHelper.addLogEvent(DbAdapter.LOG_TYPE_SMS_NOTIFICATION,
					"Null PDU");
			break;
		case SmsManager.RESULT_ERROR_RADIO_OFF:
			dbHelper.addLogEvent(DbAdapter.LOG_TYPE_SMS_NOTIFICATION,
					"Radio off");
			break;
		}

		dbHelper.close();
	}

	/**
	 * @param context
	 * @param intent
	 */
	private void processSmsDelivered(Context context, Intent intent) {
		DbAdapter dbHelper = new DbAdapter(context);
		dbHelper.open();

		switch (getResultCode()) {
		case Activity.RESULT_OK:
			// dbHelper.addLogEvent(DbAdapter.LOG_TYPE_SMS_NOTIFICATION,
			// "SMS delivered");
			break;
		case Activity.RESULT_CANCELED:
			dbHelper.addLogEvent(DbAdapter.LOG_TYPE_SMS_NOTIFICATION,
					"SMS not delivered");
			break;
		}

		dbHelper.close();
	}
}
