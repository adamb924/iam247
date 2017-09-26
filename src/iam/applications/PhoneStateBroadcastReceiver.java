/**
 * 
 */
package iam.applications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

/**
 * A BroadcastReceiver that registers when the phone state has changed. When it
 * has changed, a new <code>MissedCallReceiver</code> is launched.
 */
public class PhoneStateBroadcastReceiver extends BroadcastReceiver {
	/*
	 * (non-Javadoc)
	 * 
	 * @see android.content.BroadcastReceiver#onReceive(android.content.Context,
	 * android.content.Intent)
	 */
	@Override
	public void onReceive(final Context context, final Intent intent) {
		final TelephonyManager telephonyManager = (TelephonyManager) context
				.getSystemService(Context.TELEPHONY_SERVICE);
		telephonyManager.listen(new MissedCallReceiver(context),
				PhoneStateListener.LISTEN_CALL_STATE);
	}

}
