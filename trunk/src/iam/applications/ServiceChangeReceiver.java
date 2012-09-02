package iam.applications;

import android.content.Context;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.util.Log;

/**
 * 
 */

/**
 * @author Adam
 * 
 */
public class ServiceChangeReceiver extends PhoneStateListener {

	public ServiceChangeReceiver(Context context) {
		super();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.telephony.PhoneStateListener#onServiceStateChanged(android.telephony
	 * .ServiceState)
	 */
	@Override
	public void onServiceStateChanged(ServiceState serviceState) {
		Log.i("Debug", "onServiceStateChanged");
		Log.i("Debug", serviceState.getOperatorNumeric());
		Log.i("Debug", String.valueOf(serviceState.getState()));

		super.onServiceStateChanged(serviceState);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.telephony.PhoneStateListener#onSignalStrengthsChanged(android
	 * .telephony.SignalStrength)
	 */
	@Override
	public void onSignalStrengthsChanged(SignalStrength signalStrength) {
		super.onSignalStrengthsChanged(signalStrength);

		// TODO this works
		// Log.i("Debug", "onSignalStrengthsChanged: " +
		// signalStrength.toString());
	}

}
