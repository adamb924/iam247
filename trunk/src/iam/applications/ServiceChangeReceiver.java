package iam.applications;

import android.content.Context;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;

/**
 * @author Adam
 * 
 *         This is a class that will detect levels in phone service state.
 *         Eventually this may be used to monitor SIM status. Currently
 *         (9.3.2012) it seems unlikely that this will have a practical
 *         application, beyond notifying the user that the network is down. So,
 *         the feature is on hold indefinitely.
 * 
 */
public class ServiceChangeReceiver extends PhoneStateListener {

	/**
	 * @param context
	 */
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
		// my recollection is that this method is not be called

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

		// this method is being called
	}

}
