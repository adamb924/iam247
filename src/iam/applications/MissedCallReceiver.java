package iam.applications;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.CallLog;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

/**
 * This PhoneStateListener detects when someone has missed-called the phone, and
 * sends <code>SmsHandler</code> a text as if that person had texted in
 * "travel".
 */
public class MissedCallReceiver extends PhoneStateListener {

	/** The delay. */
	private transient final long mDelay;

	/** The timer object. */
	private transient final Handler mHandler;

	/** The application context. */
	private transient final Context mContext;

	/** The phone number number. */
	private transient String mNumber;

	/**
	 * Whether 24/7 is disabled. It makes sense to check for this just once in
	 * the constructor, since apparently this object is persistent.
	 */
	private transient final boolean mDisabled;

	/**
	 * Instantiates a new missed call receiver.
	 * 
	 * @param context
	 *            the context
	 */
	public MissedCallReceiver(final Context context) {
		super();
		mContext = context;
		mHandler = new Handler();

		final SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(context);
		mDelay = Long.valueOf(settings.getString(Preferences.MISSED_CALL_DELAY,
				"5000"));
		mDisabled = settings.getBoolean(Preferences.DISABLE_247, false);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.telephony.PhoneStateListener#onCallStateChanged(int,
	 * java.lang.String)
	 */
	@Override
	public void onCallStateChanged(final int state, final String incomingNumber) {
		if (mDisabled) {
			return;
		}

		mNumber = SmsHandler.getNormalizedPhoneNumber(mContext, incomingNumber);

		// only do the check if a new call is coming in
		if( state == TelephonyManager.CALL_STATE_RINGING ) {
            AudioManager audiomanager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
            final DbAdapter dbHelper = new DbAdapter(mContext);
            dbHelper.open();
            final long putative_guard_id = dbHelper.getGuardIdFromNumber(mNumber);
            final boolean hasPendingCheck = dbHelper.getGuardHasPendingCheck(mContext, putative_guard_id);
            dbHelper.close();

            if (hasPendingCheck) {
                // it it's a guard number, mute the speaker
                audiomanager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
            } else {
                audiomanager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
            }
        }

        // call this after we've (possibly) muted the sound
        super.onCallStateChanged(state, incomingNumber);

		// if the phone is ringing set a timer to check the phone status after
		// mDelay milliseconds
		if (state == TelephonyManager.CALL_STATE_RINGING) {
			mHandler.postDelayed(callListener, mDelay);
		}

	}

	/** If the phone is not ringing, send the SMS. */
	private transient final Runnable callListener = new Runnable() {
		@Override
		public void run() {
			final TelephonyManager telephonyManager = (TelephonyManager) mContext
					.getSystemService(Context.TELEPHONY_SERVICE);

			// if the phone is no longer ringing
			if (telephonyManager.getCallState() == TelephonyManager.CALL_STATE_IDLE) {
				processMissedCall();
			}
		}
	};

	/**
	 * Process a missed call by interpreting it either as a guard's checkin, or
	 * a request for location restrictions.
	 */
	private void processMissedCall() {
		final DbAdapter dbHelper = new DbAdapter(mContext);
		dbHelper.open();
		final long putative_guard_id = dbHelper.getGuardIdFromNumber(mNumber);

		if (putative_guard_id == -1) {
			// if it's not a recognized guard, process it like any other call
			new SmsHandler(mContext, mNumber,
					mContext.getString(R.string.sms_permission_default), true);
		} else {
			tryToResolveGuardCheckin(dbHelper, putative_guard_id);
		}
		dbHelper.close();
    }

	/**
	 * @param dbHelper
	 * @param guard_id
	 */
	private void tryToResolveGuardCheckin(final DbAdapter dbHelper,
			final long guard_id) {

		final int ret = dbHelper.setGuardCheckinResolved(mContext, guard_id);
		if (ret == DbAdapter.Notifications.SUCCESS) {
			SmsHandler
					.sendSms(mContext, mNumber, mContext
							.getString(R.string.sms_guard_checkin_confirmation));
			HomeActivity.sendRefreshAlert(mContext);
		} else {
			SmsHandler.sendSms(mContext, mNumber,
					mContext.getString(R.string.sms_guard_checkin_rejection));
		}
	}
}
