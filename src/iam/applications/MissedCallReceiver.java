package iam.applications;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

/**
 * This PhoneStateListener detects when someone has missed-called the phone, and
 * sends <code>SmsHandler</code> a text as if that person had texted in
 * "travel".
 */
public class MissedCallReceiver extends PhoneStateListener {

	/** The delay. */
	private long mDelay = 5000; // five seconds for a missed call

	/** The timer object. */
	private final Handler mHandler;

	/** The application context. */
	private final Context mContext;

	/** The phone number number. */
	private String mNumber;

	/**
	 * Whether 24/7 is disabled. It makes sense to check for this just once in
	 * the constructor, since apparently this object is persistent.
	 */
	private final boolean mDisabled;

	/**
	 * Instantiates a new missed call receiver.
	 * 
	 * @param context
	 *            the context
	 */
	public MissedCallReceiver(Context context) {
		super();
		mContext = context;
		mHandler = new Handler();

		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(context);
		mDelay = Long.valueOf(settings.getString(
				HomeActivity.PREFERENCES_MISSED_CALL_DELAY, "5000"));
		mDisabled = settings.getBoolean(HomeActivity.PREFERENCES_DISABLE_247,
				false);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.telephony.PhoneStateListener#onCallStateChanged(int,
	 * java.lang.String)
	 */
	@Override
	public void onCallStateChanged(int state, String incomingNumber) {
		super.onCallStateChanged(state, incomingNumber);

		if (mDisabled) {
			return;
		}

		mNumber = SmsHandler.getNormalizedPhoneNumber(mContext, incomingNumber);

		// if the phone is ringing set a timer to check the phone status after
		// mDelay milliseconds
		if (state == TelephonyManager.CALL_STATE_RINGING) {
			mHandler.postDelayed(checkForMissedCall, mDelay);
		}
	}

	/** If the phone is not ringing, send the SMS. */
	private final Runnable checkForMissedCall = new Runnable() {
		@Override
		public void run() {
			TelephonyManager telephonyManager = (TelephonyManager) mContext
					.getSystemService(Context.TELEPHONY_SERVICE);

			// if the phone is no longer ringing
			if (telephonyManager.getCallState() == TelephonyManager.CALL_STATE_IDLE) {
				// not proud of this, but it seems the most efficient way
				new SmsHandler(mContext, mNumber,
						mContext.getString(R.string.sms_permission_default),
						true);
			}
		}
	};

}
