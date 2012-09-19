package iam.applications;

import java.util.Calendar;
import java.util.Date;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * The Class DbAdapter. Provides access functions for the app's SQL database.
 */

/**
 * @author Adam
 * 
 */
public class DbAdapter {
	/**
	 * This is the interface class for the SQL database. Typical usage would be
	 * for it to be opened on an Activity's onCreate method and closed in the
	 * onDestroy method.
	 */
	private static class DatabaseHelper extends SQLiteOpenHelper {

		/**
		 * Instantiates a new database helper.
		 * 
		 * @param context
		 *            the context
		 */
		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * android.database.sqlite.SQLiteOpenHelper#onCreate(android.database
		 * .sqlite.SQLiteDatabase)
		 */
		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(DATABASE_CREATE_LOCATIONS);
			db.execSQL(DATABASE_CREATE_CHECKINS);
			db.execSQL(DATABASE_CREATE_CALLAROUNDS);
			db.execSQL(DATABASE_CREATE_CONTACTS);
			db.execSQL(DATABASE_CREATE_CONTACTPHONES);
			db.execSQL(DATABASE_CREATE_CONTACTEMAILS);
			db.execSQL(DATABASE_CREATE_HOUSES);
			db.execSQL(DATABASE_CREATE_HOUSEMEMBERS);
			db.execSQL(DATABASE_CREATE_BLOCKEDNUMBERS);
			db.execSQL(DATABASE_CREATE_LOG);
			db.execSQL(DATABASE_CREATE_LOCATION_LOG);
			db.execSQL(DATABASE_CREATE_PENDING);
			db.execSQL(DATABASE_CREATE_GUARDS);
			db.execSQL(DATABASE_CREATE_GUARD_CHECKINS);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * android.database.sqlite.SQLiteOpenHelper#onUpgrade(android.database
		 * .sqlite.SQLiteDatabase, int, int)
		 */
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
					+ newVersion + ", which will destroy all old data");
			db.execSQL(DROP_TABLE_LOCATIONS);
			db.execSQL(DROP_TABLE_CHECKINS);
			db.execSQL(DROP_TABLE_CALLAROUNDS);
			db.execSQL(DROP_TABLE_CONTACTS);
			db.execSQL(DROP_TABLE_CONTACTPHONES);
			db.execSQL(DROP_TABLE_CONTACTEMAILS);
			db.execSQL(DROP_TABLE_HOUSES);
			db.execSQL(DROP_TABLE_HOUSEMEMBERS);
			db.execSQL(DROP_TABLE_BLOCKEDNUMBERS);
			db.execSQL(DROP_TABLE_LOG);
			db.execSQL(DROP_TABLE_LOCATION_LOG);
			db.execSQL(DROP_TABLE_PENDING);
			db.execSQL(DROP_TABLE_GUARDS);
			db.execSQL(DROP_TABLE_GUARD_CHECKIN);

			onCreate(db);
		}
	}

	/** The user preference checkin reminder. */
	public static int USER_PREFERENCE_CHECKIN_REMINDER = 1;

	/** The user permission report. */
	public static int USER_PERMISSION_REPORT = 1;

	/** Return value to indicate failure. */
	public static int NOTIFY_FAILURE = 0;

	/**
	 * Return value to indicate that an existing check-in was resolved when the
	 * new one was added.
	 */
	public static int NOTIFY_EXISTING_CHECKIN_RESOLVED = 1;

	/** Return value to indicate success. */
	public static int NOTIFY_SUCCESS = 2;

	/**
	 * Return value to indicate that the requested action had already been
	 * completed.
	 */
	public static int NOTIFY_ALREADY = 3;

	/** Return value to indicate that call around is currently inactive. */
	public static int NOTIFY_INACTIVE = 4;

	/** The notify untimely. */
	public static int NOTIFY_UNTIMELY = 5;

	/** The version of the current database. */
	private static final int DATABASE_VERSION = 19;

	/** Create Table Commands. */
	private static final String DATABASE_CREATE_LOCATIONS = "create table if not exists locations (_id integer primary key autoincrement, label text not null, keyword text, allowed integer default 0);";

	/** The Constant DATABASE_CREATE_CHECKINS. */
	private static final String DATABASE_CREATE_CHECKINS = "create table if not exists checkins (_id integer primary key autoincrement, contact_id integer not null, location string not null, keyword string not null, timedue string not null, timereceived string, outstanding integer default 1, tripresolved integer default 0, checkinrequest integer default 1, with string );";

	/** The Constant DATABASE_CREATE_CALLAROUNDS. */
	private static final String DATABASE_CREATE_CALLAROUNDS = "create table if not exists callarounds (_id integer primary key autoincrement, house_id integer not null, duefrom string not null, dueby string not null, timereceived string, outstanding integer default 1, delayed integer default 0, unique(house_id,dueby) on conflict ignore );";

	/** The Constant DATABASE_CREATE_CONTACTS. */
	private static final String DATABASE_CREATE_CONTACTS = "create table contacts ( _id integer primary key autoincrement , name text not null , preferences int default 0 , permissions int default 0 )";

	/** The Constant DATABASE_CREATE_CONTACTPHONES. */
	private static final String DATABASE_CREATE_CONTACTPHONES = "create table contactphones ( _id integer primary key autoincrement , contact_id integer not null, number text not null, integer precedence default 1 )";

	/** The Constant DATABASE_CREATE_CONTACTEMAILS. */
	private static final String DATABASE_CREATE_CONTACTEMAILS = "create table contactemails ( _id integer primary key autoincrement , contact_id integer not null, email text not null, integer precedence default 1 )";

	/** The Constant DATABASE_CREATE_HOUSES. */
	private static final String DATABASE_CREATE_HOUSES = "create table houses ( _id integer primary key autoincrement , name text not null, active int default 1, sunday_guard int default -1, monday_guard int default -1, tuesday_guard int default -1, wednesday_guard int default -1, thursday_guard int default -1, friday_guard int default -1, saturday_guard int default -1 , typical_sunday_guard int default -1, typical_monday_guard int default -1, typical_tuesday_guard int default -1, typical_wednesday_guard int default -1, typical_thursday_guard int default -1, typical_friday_guard int default -1, typical_saturday_guard int default -1 )";

	/** The Constant DATABASE_CREATE_HOUSEMEMBERS. */
	private static final String DATABASE_CREATE_HOUSEMEMBERS = "create table housemembers ( _id integer primary key autoincrement , house_id integer not null, contact_id integer not null, unique(contact_id) on conflict ignore )";

	/** The Constant DATABASE_CREATE_BLOCKEDNUMBERS. */
	private static final String DATABASE_CREATE_BLOCKEDNUMBERS = "create table blockednumbers ( _id integer primary key autoincrement , number text not null )";

	/** The Constant DATABASE_CREATE_LOG. */
	private static final String DATABASE_CREATE_LOG = "create table log ( _id integer primary key autoincrement , type text default 'Normal' , message text not null, time text )";

	/** The Constant DATABASE_CREATE_LOCATION_LOG. */
	private static final String DATABASE_CREATE_LOCATION_LOG = "create table locationlog ( _id integer primary key autoincrement , contact_id integer not null, lat real not null, lon real not null, time text )";

	/** The Constant DATABASE_CREATE_PENDING. */
	private static final String DATABASE_CREATE_PENDING = "create table pending ( _id integer primary key autoincrement , number text not null, message text not null, time text not null , sent int default 0, delivered int default 0 )";

	/** The Constant DATABASE_CREATE_GUARDS. */
	private static final String DATABASE_CREATE_GUARDS = "create table guards ( _id integer primary key autoincrement , name text not null , number text )";

	/** The Constant DATABASE_CREATE_GUARD_CHECKINS. */
	private static final String DATABASE_CREATE_GUARD_CHECKINS = "create table guardcheckins ( _id integer primary key autoincrement , guard_id int not null , time text , response int default 0, unique(time) on conflict ignore )";

	/** Drop Table Commands. */
	private static final String DROP_TABLE_LOCATIONS = "DROP TABLE IF EXISTS locations;";

	/** The Constant DROP_TABLE_CHECKINS. */
	private static final String DROP_TABLE_CHECKINS = "DROP TABLE IF EXISTS checkins;";

	/** The Constant DROP_TABLE_CALLAROUNDS. */
	private static final String DROP_TABLE_CALLAROUNDS = "DROP TABLE IF EXISTS callarounds;";

	/** The Constant DROP_TABLE_CONTACTS. */
	private static final String DROP_TABLE_CONTACTS = "DROP TABLE IF EXISTS contacts;";

	/** The Constant DROP_TABLE_CONTACTPHONES. */
	private static final String DROP_TABLE_CONTACTPHONES = "DROP TABLE IF EXISTS contactphones;";

	/** The Constant DROP_TABLE_CONTACTEMAILS. */
	private static final String DROP_TABLE_CONTACTEMAILS = "DROP TABLE IF EXISTS contactemails;";

	/** The Constant DROP_TABLE_HOUSES. */
	private static final String DROP_TABLE_HOUSES = "DROP TABLE IF EXISTS houses;";

	/** The Constant DROP_TABLE_HOUSEMEMBERS. */
	private static final String DROP_TABLE_HOUSEMEMBERS = "DROP TABLE IF EXISTS housemembers;";

	/** The Constant DROP_TABLE_BLOCKEDNUMBERS. */
	private static final String DROP_TABLE_BLOCKEDNUMBERS = "DROP TABLE IF EXISTS blockednumbers;";

	/** The Constant DROP_TABLE_LOG. */
	private static final String DROP_TABLE_LOG = "DROP TABLE IF EXISTS log;";

	/** The Constant DROP_TABLE_LOCATION_LOG. */
	private static final String DROP_TABLE_LOCATION_LOG = "DROP TABLE IF EXISTS locationlog;";

	/** The Constant DROP_TABLE_PENDING. */
	private static final String DROP_TABLE_PENDING = "DROP TABLE IF EXISTS pending;";

	/** The Constant DROP_TABLE_GUARDS. */
	private static final String DROP_TABLE_GUARDS = "DROP TABLE IF EXISTS guards;";

	/** The Constant DROP_TABLE_GUARD_CHECKIN. */
	private static final String DROP_TABLE_GUARD_CHECKIN = "DROP TABLE IF EXISTS guardcheckins;";

	/** The Constant DATABASE_NAME. */
	private static final String DATABASE_NAME = "thedatabase";

	/** Database table names. */
	private static final String DATABASE_TABLE_LOCATIONS = "locations";

	/** The Constant DATABASE_TABLE_CHECKINS. */
	private static final String DATABASE_TABLE_CHECKINS = "checkins";

	/** The Constant DATABASE_TABLE_CALLAROUNDS. */
	private static final String DATABASE_TABLE_CALLAROUNDS = "callarounds";

	/** The Constant DATABASE_TABLE_CONTACTS. */
	private static final String DATABASE_TABLE_CONTACTS = "contacts";

	/** The Constant DATABASE_TABLE_CONTACTPHONES. */
	private static final String DATABASE_TABLE_CONTACTPHONES = "contactphones";

	/** The Constant DATABASE_TABLE_CONTACTEMAILS. */
	private static final String DATABASE_TABLE_CONTACTEMAILS = "contactemails";

	/** The Constant DATABASE_TABLE_HOUSES. */
	private static final String DATABASE_TABLE_HOUSES = "houses";

	/** The Constant DATABASE_TABLE_HOUSEMEMBERS. */
	private static final String DATABASE_TABLE_HOUSEMEMBERS = "housemembers";

	/** The Constant DATABASE_TABLE_BLOCKEDNUMBERS. */
	private static final String DATABASE_TABLE_BLOCKEDNUMBERS = "blockednumbers";

	/** The Constant DATABASE_TABLE_LOG. */
	private static final String DATABASE_TABLE_LOG = "log";

	/** The Constant DATABASE_TABLE_LOCATION_LOG. */
	private static final String DATABASE_TABLE_LOCATION_LOG = "locationlog";

	/** The Constant DATABASE_TABLE_PENDING. */
	private static final String DATABASE_TABLE_PENDING = "pending";

	/** The Constant DATABASE_TABLE_GUARDS. */
	private static final String DATABASE_TABLE_GUARDS = "guards";

	/** The Constant DATABASE_TABLE_GUARDS_CHECKINS. */
	private static final String DATABASE_TABLE_GUARD_CHECKINS = "guardcheckins";

	/** SQL column names constants. */
	public static final String KEY_ROWID = "_id";

	/** The Constant KEY_LABEL. */
	public static final String KEY_LABEL = "label";

	/** The Constant KEY_ALLOWED. */
	public static final String KEY_ALLOWED = "allowed";

	/** The Constant KEY_CONTACTID. */
	public static final String KEY_CONTACTID = "contact_id";

	/** The Constant KEY_LOCATION. */
	public static final String KEY_LOCATION = "location";

	/** The Constant KEY_TIMEDUE. */
	public static final String KEY_TIMEDUE = "timedue";

	/** The Constant KEY_TIMERECEIVED. */
	public static final String KEY_TIMERECEIVED = "timereceived";

	/** The Constant KEY_OUTSTANDING. */
	public static final String KEY_OUTSTANDING = "outstanding";

	/** The Constant KEY_RESOLVED. */
	public static final String KEY_RESOLVED = "resolved";

	/** The Constant KEY_HOUSEID. */
	public static final String KEY_HOUSEID = "house_id";

	/** The Constant KEY_DUEBY. */
	public static final String KEY_DUEBY = "dueby";

	/** The Constant KEY_DUEFROM. */
	public static final String KEY_DUEFROM = "duefrom";

	/** The Constant KEY_CHECKINREQUEST. */
	public static final String KEY_CHECKINREQUEST = "checkinrequest";

	/** The Constant KEY_ACTIVE. */
	public static final String KEY_ACTIVE = "active";

	/** The Constant KEY_SUMMARY. */
	public static final String KEY_SUMMARY = "summary";

	/** The Constant KEY_NAME. */
	public static final String KEY_NAME = "name";

	/** The Constant KEY_NUMBER. */
	public static final String KEY_NUMBER = "number";

	/** The Constant KEY_EMAIL. */
	public static final String KEY_EMAIL = "email";

	/** The Constant KEY_COUNT. */
	public static final String KEY_COUNT = "count";

	/** The Constant KEY_PREFERENCES. */
	public static final String KEY_PREFERENCES = "preferences";

	/** The Constant KEY_PERMISSIONS. */
	public static final String KEY_PERMISSIONS = "permissions";

	/** The Constant KEY_TYPE. */
	public static final String KEY_TYPE = "type";

	/** The Constant KEY_TIME. */
	public static final String KEY_TIME = "time";

	/** The Constant KEY_MESSAGE. */
	public static final String KEY_MESSAGE = "message";

	/** The Constant KEY_LAT. */
	public static final String KEY_LAT = "lat";

	/** The Constant KEY_LON. */
	public static final String KEY_LON = "lon";

	/** The Constant KEY_KEYWORD. */
	public static final String KEY_KEYWORD = "keyword";

	/** The Constant KEY_DELAYED. */
	public static final String KEY_DELAYED = "delayed";

	/** The Constant KEY_GUARDID. */
	public static final String KEY_GUARDID = "guard_id";

	/** The Constant KEY_RESPONSE. */
	public static final String KEY_RESPONSE = "response";

	public static final String KEY_WITH = "with";
	public static final String KEY_TRIPRESOLVED = "tripresolved";
	public static final String KEY_DELIVERED = "delivered";
	public static final String KEY_SENT = "sent";

	/** Log message types. */
	public static final String LOG_TYPE_SMS_NOTIFICATION = "SMS Event";

	/** The Constant TAG. */
	private static final String TAG = "DbAdapter";

	/** The database helper. */
	private DatabaseHelper mDbHelper;

	/** The database. */
	private SQLiteDatabase mDb;

	/** The application context. */
	private final Context mContext;

	/** The Constant SUNDAY_GUARD. */
	public static final String SUNDAY_GUARD = "sunday_guard";

	/** The Constant SUNDAY_GUARD_DEFAULT. */
	public static final String SUNDAY_GUARD_DEFAULT = "typical_sunday_guard";

	/** The Constant MONDAY_GUARD. */
	public static final String MONDAY_GUARD = "monday_guard";

	/** The Constant MONDAY_GUARD_DEFAULT. */
	public static final String MONDAY_GUARD_DEFAULT = "typical_monday_guard";

	/** The Constant TUESDAY_GUARD. */
	public static final String TUESDAY_GUARD = "tuesday_guard";

	/** The Constant TUESDAY_GUARD_DEFAULT. */
	public static final String TUESDAY_GUARD_DEFAULT = "typical_tuesday_guard";

	/** The Constant WEDNESDAY_GUARD. */
	public static final String WEDNESDAY_GUARD = "wednesday_guard";

	/** The Constant WEDNESDAY_GUARD_DEFAULT. */
	public static final String WEDNESDAY_GUARD_DEFAULT = "typical_wednesday_guard";

	/** The Constant THURSDAY_GUARD. */
	public static final String THURSDAY_GUARD = "thursday_guard";

	/** The Constant THURSDAY_GUARD_DEFAULT. */
	public static final String THURSDAY_GUARD_DEFAULT = "typical_thursday_guard";

	/** The Constant FRIDAY_GUARD. */
	public static final String FRIDAY_GUARD = "friday_guard";

	/** The Constant FRIDAY_GUARD_DEFAULT. */
	public static final String FRIDAY_GUARD_DEFAULT = "typical_friday_guard";

	/** The Constant SATURDAY_GUARD. */
	public static final String SATURDAY_GUARD = "saturday_guard";

	/** The Constant SATURDAY_GUARD_DEFAULT. */
	public static final String SATURDAY_GUARD_DEFAULT = "typical_saturday_guard";

	/**
	 * Instantiates a new db adapter.
	 * 
	 * @param ctx
	 *            the context
	 */
	public DbAdapter(Context ctx) {
		mContext = ctx;
	}

	/**
	 * Adds call arounds to <code>Callarounds</code> for today.
	 * 
	 * @throws SQLException
	 *             a SQL exception
	 */
	public void addCallarounds() throws SQLException {
		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(mContext);

		String settings_dueby = settings.getString(
				HomeActivity.PREFERENCES_CALLAROUND_DUE_BY, "21:00");
		Date today_dueby = Time.todayAtGivenTime(settings_dueby);

		String settings_delayed_dueby = settings.getString(
				HomeActivity.PREFERENCES_CALLAROUND_DELAYED_TIME, "23:59");
		Date today_delayed_dueby = Time
				.todayAtGivenTime(settings_delayed_dueby);

		String settings_duefrom = settings.getString(
				HomeActivity.PREFERENCES_CALLAROUND_DUE_FROM, "17:00");
		Date today_duefrom = Time.todayAtGivenTime(settings_duefrom);

		AlarmReceiver.setCallaroundDueAlarm(mContext, today_dueby);
		AlarmReceiver.setDelayedCallaroundAlarm(mContext, today_delayed_dueby);

		// if a callaround has already been resolved, update the due times so
		// that a duplicate is not created by the insert time. this is
		// admittedly clunky
		ContentValues args = new ContentValues();
		args.put(KEY_DUEBY, Time.iso8601DateTime(today_dueby));
		args.put(KEY_DUEFROM, Time.iso8601DateTime(today_duefrom));
		mDb.update(DATABASE_TABLE_CALLAROUNDS, args, KEY_OUTSTANDING + "='0'",
				null);
		// delete unresolved callarounds
		mDb.delete(DATABASE_TABLE_CALLAROUNDS,
				"date(dueby) = date('now','localtime') and outstanding='1'",
				null);

		mDb.execSQL("insert or ignore into callarounds (house_id , dueby, duefrom) select _id,'"
				+ Time.iso8601DateTime(today_dueby)
				+ "','"
				+ Time.iso8601DateTime(today_duefrom)
				+ "' from houses where active='1';");
	}

	/**
	 * Adds a check-in to the database.
	 * 
	 * @param contact_id
	 *            the contact_id of the person check-in in
	 * @param place
	 *            the place the to where the person is going
	 * @param time
	 *            the time by which the person will return
	 * @param requestCheckin
	 *            whether the person is requesting to be checked in on
	 * @return Possible return values: NOTIFY_SUCCESS, NOTIFY_FAILURE,
	 *         NOTIFY_EXISTING_CHECKIN_RESOLVED
	 * @throws SQLException
	 *             a SQL exception
	 */
	public int addCheckin(long contact_id, String place, String keyword,
			Date time, String with) throws SQLException {
		Cursor c = mDb.rawQuery(
				"select count(_id) from checkins where outstanding='1' and contact_id='"
						+ String.valueOf(contact_id) + "';", null);
		c.moveToFirst();
		int count = c.getInt(0);

		ContentValues args = new ContentValues();
		args.put(KEY_OUTSTANDING, 0);
		mDb.update(DATABASE_TABLE_CHECKINS, args, KEY_CONTACTID + "="
				+ contact_id, null);

		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_CONTACTID, contact_id);
		initialValues.put(KEY_LOCATION, place);
		initialValues.put(KEY_KEYWORD, keyword);
		initialValues.put(KEY_TIMEDUE, Time.iso8601DateTime(time));
		initialValues.put(KEY_TIMERECEIVED, Time.iso8601DateTime());
		if (with != null) {
			initialValues.put(KEY_WITH, with);
		}
		boolean result = mDb.insert(DATABASE_TABLE_CHECKINS, null,
				initialValues) > -1;

		if (result) {
			AlarmReceiver.setCheckinAlert(mContext, time);
		}

		if (!result) {
			return NOTIFY_FAILURE;
		} else if (count > 0) {
			return NOTIFY_EXISTING_CHECKIN_RESOLVED;
		} else {
			return NOTIFY_SUCCESS;
		}
	}

	/**
	 * Adds a contact to the database.
	 * 
	 * @param name
	 *            the name of the person
	 * @param number
	 *            the person's phone number
	 * @throws SQLException
	 *             a SQL exception
	 */
	public void addContact(String name, String number) throws SQLException {
		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_NAME, name);
		mDb.insert(DATABASE_TABLE_CONTACTS, null, initialValues);

		int id = lastInsertId();
		if (id != -1) {
			ContentValues initialValues2 = new ContentValues();
			initialValues2.put(KEY_CONTACTID, id);
			initialValues2.put(KEY_NUMBER, number);
			mDb.insert(DATABASE_TABLE_CONTACTPHONES, null, initialValues2);
		}
	}

	/**
	 * Adds a guard to the database.
	 * 
	 * @param name
	 *            the name of the guard
	 * @throws SQLException
	 *             a SQL exception
	 */
	public void addGuard(String name) throws SQLException {
		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_NAME, name);
		mDb.insert(DATABASE_TABLE_GUARDS, null, initialValues);
	}

	/**
	 * Adds a guard checkin for the given guard at the given time.
	 * 
	 * @param guard_id
	 *            the id of the guard
	 * @param time
	 *            the time of the checkin
	 * @throws SQLException
	 *             the sQL exception
	 */
	public void addGuardCheckin(long guard_id, String time) throws SQLException {
		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_GUARDID, guard_id);
		initialValues.put(KEY_TIME, time);
		mDb.insert(DATABASE_TABLE_GUARD_CHECKINS, null, initialValues);
	}

	/**
	 * Adds a house to the database.
	 * 
	 * @param name
	 *            the name of the house
	 * @throws SQLException
	 *             a SQL exception
	 */
	public void addHouse(String name) throws SQLException {
		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_NAME, name);
		mDb.insert(DATABASE_TABLE_HOUSES, null, initialValues);
	}

	/**
	 * Adds a location to the database.
	 * 
	 * @param label
	 *            the label of the new location
	 * @param allowed
	 *            whether the location is permitted for travel
	 * @param keyword
	 *            the keyword for the location
	 * @return the _id of the inserted row
	 * @throws SQLException
	 *             a SQL exception
	 */
	public long addLocation(String label, boolean allowed, String keyword)
			throws SQLException {
		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_LABEL, label);
		initialValues.put(KEY_ALLOWED, allowed ? 1 : 0);
		initialValues.put(KEY_KEYWORD, keyword);
		return mDb.insert(DATABASE_TABLE_LOCATIONS, null, initialValues);
	}

	/**
	 * Adds a record to the <code>locationlog</code> table.
	 * 
	 * @param contact_id
	 *            the _id of the contact
	 * @param lat
	 *            the latitude
	 * @param lon
	 *            the longitude
	 * @param time
	 *            the time of the SMS
	 * @return the number of rows inserted
	 * @throws SQLException
	 *             the SQL exception
	 */
	public long addLocationLog(long contact_id, double lat, double lon,
			String time) throws SQLException {
		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_CONTACTID, contact_id);
		initialValues.put(KEY_LAT, lat);
		initialValues.put(KEY_LON, lon);
		initialValues.put(KEY_TIME, time);
		return mDb.insert(DATABASE_TABLE_LOCATION_LOG, null, initialValues);
	}

	/**
	 * Adds a record to the event log.
	 * 
	 * @param type
	 *            The type of message (should be a static member of DbAdapter)
	 * @param message
	 *            The message to be logged
	 * @return The number of records inserted into the database
	 */
	public boolean addLogEvent(String type, String message) {
		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_TYPE, type);
		initialValues.put(KEY_MESSAGE, message);
		initialValues.put(KEY_TIME, Time.iso8601DateTime());
		return mDb.insert(DATABASE_TABLE_LOG, null, initialValues) > -1;
	}

	/**
	 * Adds the message to the pending table.
	 * 
	 * @param number
	 *            the phone number
	 * @param message
	 *            the message
	 * @throws SQLException
	 *             the SQL exception
	 */
	public void addMessagePending(String number, String message)
			throws SQLException {
		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_NUMBER, number);
		initialValues.put(KEY_MESSAGE, message);
		initialValues.put(KEY_TIME, Time.iso8601DateTime());
		mDb.insert(DATABASE_TABLE_PENDING, null, initialValues);
	}

	/**
	 * Adds call arounds at specified times, for the range of dates given, for
	 * the given house_id.
	 * 
	 * @param house_id
	 *            the house_id
	 * @param from
	 *            the date at which to start adding call arounds (inclusive)
	 * @param to
	 *            the date to which call arounds should be added (inclusive)
	 * @param firstTime
	 *            the first call around time
	 * @param firstTimeEarliest
	 *            the earliest time the team member can respond to the first
	 *            call around
	 * @param secondTime
	 *            the second call around time, or null if none is desired
	 * @param secondTimeEarliest
	 *            the earliest time the team member can respond to the second
	 *            call around
	 * @throws SQLException
	 *             the SQL exception
	 */
	public void addTravelCallarounds(long house_id, Calendar from, Calendar to,
			String firstTime, String firstTimeEarliest, String secondTime,
			String secondTimeEarliest) throws SQLException {

		while (from.before(to) || from.equals(to)) {
			String first = Time.iso8601Date(from.getTime()) + " " + firstTime;
			String firstEarliest = Time.iso8601Date(from.getTime()) + " "
					+ firstTimeEarliest;

			AlarmReceiver.setCallaroundDueAlarm(mContext,
					Time.iso8601DateTime(first));

			mDb.execSQL("insert or ignore into callarounds (house_id , dueby, duefrom) values ('"
					+ String.valueOf(house_id)
					+ "','"
					+ first
					+ "','"
					+ firstEarliest + "');");

			if (secondTime != null) {
				String second = Time.iso8601Date(from.getTime()) + " "
						+ secondTime;
				String secondEarliest = Time.iso8601Date(from.getTime()) + " "
						+ secondTimeEarliest;

				AlarmReceiver.setCallaroundDueAlarm(mContext,
						Time.iso8601DateTime(second));

				mDb.execSQL("insert or ignore into callarounds (house_id , dueby, duefrom) values ('"
						+ String.valueOf(house_id)
						+ "','"
						+ second
						+ "','"
						+ secondEarliest + "');");
			}
			from.add(Calendar.DAY_OF_MONTH, 1);
		}
	}

	/**
	 * Returns the result of the SQLite changes() function.
	 * 
	 * @return the the result of the SQLite changes() function
	 * @throws SQLException
	 *             a SQL exception
	 */
	private int changes() throws SQLException {
		Cursor c = mDb.rawQuery("select changes();", null);
		if (c.moveToFirst()) {
			return c.getInt(0);
		} else {
			return 0;
		}
	}

	/**
	 * Closes the database.
	 * 
	 * @throws SQLException
	 *             a SQL exception
	 */
	public void close() throws SQLException {
		mDbHelper.close();
	}

	/**
	 * Delete all information from database.
	 * 
	 * @throws SQLException
	 *             a SQL exception
	 */
	public void deleteAll() throws SQLException {
		mDb.delete(DATABASE_TABLE_CHECKINS, null, null);
		mDb.delete(DATABASE_TABLE_CALLAROUNDS, null, null);
		mDb.delete(DATABASE_TABLE_CONTACTS, null, null);
		mDb.delete(DATABASE_TABLE_CONTACTPHONES, null, null);
		mDb.delete(DATABASE_TABLE_CONTACTEMAILS, null, null);
		mDb.delete(DATABASE_TABLE_HOUSES, null, null);
		mDb.delete(DATABASE_TABLE_HOUSEMEMBERS, null, null);
		mDb.delete(DATABASE_TABLE_BLOCKEDNUMBERS, null, null);
		mDb.delete(DATABASE_TABLE_LOG, null, null);
		mDb.delete(DATABASE_TABLE_LOCATION_LOG, null, null);
		mDb.delete(DATABASE_TABLE_PENDING, null, null);
		mDb.delete(DATABASE_CREATE_GUARDS, null, null);
		mDb.delete(DATABASE_CREATE_GUARD_CHECKINS, null, null);
	}

	/**
	 * Delete blocked number (= unblock the number).
	 * 
	 * @param _id
	 *            the contact_id
	 * @throws SQLException
	 *             a SQL exception
	 */
	public void deleteBlockedNumber(long _id) throws SQLException {
		mDb.delete(DATABASE_TABLE_BLOCKEDNUMBERS, KEY_ROWID + "=?",
				new String[] { String.valueOf(_id) });
	}

	/**
	 * Delete a call around from the database.
	 * 
	 * @param rowId
	 *            the row id
	 * @return the number of rows deleted.
	 * @throws SQLException
	 *             a SQL exception
	 */
	public long deleteCallaround(long rowId) throws SQLException {
		return mDb.delete(DATABASE_TABLE_CALLAROUNDS, KEY_ROWID + "=?",
				new String[] { String.valueOf(rowId) });
	}

	/**
	 * Delete contact from the database, along with associated rows from other
	 * tables.
	 * 
	 * @param contact_id
	 *            the contact_id
	 * @throws SQLException
	 *             a SQL exception
	 */
	public void deleteContact(long contact_id) throws SQLException {
		mDb.delete(DATABASE_TABLE_CONTACTS, KEY_ROWID + "=?",
				new String[] { String.valueOf(contact_id) });
		mDb.delete(DATABASE_TABLE_CHECKINS, KEY_CONTACTID + "=?",
				new String[] { String.valueOf(contact_id) });
		mDb.delete(DATABASE_TABLE_HOUSEMEMBERS, KEY_CONTACTID + "=?",
				new String[] { String.valueOf(contact_id) });
		mDb.delete(DATABASE_TABLE_CONTACTPHONES, KEY_CONTACTID + "=?",
				new String[] { String.valueOf(contact_id) });
		mDb.delete(DATABASE_TABLE_CONTACTEMAILS, KEY_CONTACTID + "=?",
				new String[] { String.valueOf(contact_id) });
	}

	/**
	 * Deletes call arounds and checkins that were received/resolved before one
	 * week from the current time.
	 */
	public void deleteDataBeforeOneWeek() {
		mDb.execSQL("delete from callarounds where dueby <= datetime('now','localtime','-7 days');");
		mDb.execSQL("delete from checkins where timereceived <= datetime('now','localtime','-7 days');");
		mDb.execSQL("delete from guardcheckins where time <= datetime('now','localtime','-7 days');");
	}

	/**
	 * Delete a house from the database, along with records of associated
	 * tables.
	 * 
	 * @param rowId
	 *            the row id
	 * @return the number of rows deleted.
	 * @throws SQLException
	 *             a SQL exception
	 */
	public long deleteHouse(long rowId) throws SQLException {
		mDb.delete(DATABASE_TABLE_CALLAROUNDS, KEY_HOUSEID + "=?",
				new String[] { String.valueOf(rowId) });
		mDb.delete(DATABASE_TABLE_HOUSEMEMBERS, KEY_HOUSEID + "=?",
				new String[] { String.valueOf(rowId) });
		return mDb.delete(DATABASE_TABLE_HOUSES, KEY_ROWID + "=?",
				new String[] { String.valueOf(rowId) });
	}

	/**
	 * Delete a guard from the database.
	 * 
	 * @param rowId
	 *            the row id
	 * @return the number of rows deleted.
	 * @throws SQLException
	 *             a SQL exception
	 */
	public long deleteGuard(long rowId) throws SQLException {
		return mDb.delete(DATABASE_TABLE_GUARDS, KEY_ROWID + "=" + rowId, null);
	}

	/**
	 * Delete a location from the database.
	 * 
	 * @param rowId
	 *            the row id
	 * @return the number of rows deleted.
	 * @throws SQLException
	 *             a SQL exception
	 */
	public long deleteLocation(long rowId) throws SQLException {
		return mDb.delete(DATABASE_TABLE_LOCATIONS, KEY_ROWID + "=" + rowId,
				null);
	}

	/**
	 * Deletes log events from before one week ago.
	 */
	public void deleteLogBeforeOneWeek() {
		mDb.execSQL("delete from log where time <= datetime('now','localtime','-7 days');");
	}

	/**
	 * Sets a message as having been delivered. Also deletes messages from the
	 * database that are both sent and delivered.
	 * 
	 * @param number
	 *            the phone number
	 * @param message
	 *            the message
	 * @throws SQLException
	 *             the SQL exception
	 */
	public void setPendingMessageDelivered(String number, String message)
			throws SQLException {
		ContentValues args = new ContentValues();
		args.put(KEY_DELIVERED, 1);
		mDb.update(DATABASE_TABLE_PENDING, args, KEY_NUMBER + "=? and "
				+ KEY_MESSAGE + "=?", new String[] { number, message });
		mDb.delete(DATABASE_TABLE_PENDING, "delivered='1' and sent='1'", null);
	}

	/**
	 * Sets a message as having been sent. Also deletes messages from the
	 * database that are both sent and delivered.
	 * 
	 * @param number
	 *            the phone number
	 * @param message
	 *            the message
	 * @throws SQLException
	 *             the SQL exception
	 */
	public void setPendingMessageSent(String number, String message)
			throws SQLException {
		ContentValues args = new ContentValues();
		args.put(KEY_SENT, 1);
		mDb.update(DATABASE_TABLE_PENDING, args, KEY_NUMBER + "=? and "
				+ KEY_MESSAGE + "=?", new String[] { number, message });
		mDb.delete(DATABASE_TABLE_PENDING, "delivered='1' and sent='1'", null);
	}

	/**
	 * Delete all unsent and undelivered messages from the database.
	 * 
	 * @throws SQLException
	 *             the sQL exception
	 */
	public void deleteUnsentUndelivered() throws SQLException {
		mDb.delete(DATABASE_TABLE_PENDING, null, null);
	}

	/**
	 * Return a cursor with a list of all check-ins. Columns: KEY_ROWID,
	 * KEY_LOCATION, KEY_KEYWORD, KEY_TIMEDUE, KEY_NAME, KEY_OUTSTANDING,
	 * KEY_WITH, KEY_TRIPRESOLVED
	 * 
	 * @return the cursor
	 * @throws SQLException
	 *             a SQL exception
	 */
	public Cursor fetchAllCheckins() throws SQLException {
		return mDb
				.rawQuery(
						"select checkins._id,location,keyword,timedue,name,outstanding,with,tripresolved from checkins,contacts where checkins.contact_id=contacts._id order by name,tripresolved asc,outstanding desc,timedue desc;",
						null);
	}

	/**
	 * Fetch all contacts' numbers. Columns: KEY_NUMBER
	 * 
	 * @return the cursor
	 * @throws SQLException
	 *             a SQL exception
	 */
	public Cursor fetchAllContactNumbers() throws SQLException {
		return mDb.rawQuery("select number from contactphones;", null);
	}

	/**
	 * Fetch contacts' numbers who are associated with houses receiving an
	 * active call around. Columns: KEY_NUMBER
	 * 
	 * @return the cursor
	 * @throws SQLException
	 *             a SQL exception
	 */
	public Cursor fetchActiveContactNumbers() throws SQLException {
		return mDb
				.rawQuery(
						"select contactphones.number from contactphones,housemembers,houses on houses.active='1' and housemembers.house_id=houses._id and housemembers.contact_id=contactphones.contact_id;",
						null);
	}

	/**
	 * Return a cursor with all houses. Columns: KEY_ROWID, KEY_NAME, KEY_ACTIVE
	 * 
	 * @return the cursor
	 * @throws SQLException
	 *             a SQL exception
	 */
	public Cursor fetchAllHouses() throws SQLException {
		return mDb.query(DATABASE_TABLE_HOUSES, new String[] { KEY_ROWID,
				KEY_NAME, KEY_ACTIVE }, null, null, null, null, KEY_NAME);
	}

	/**
	 * Return a cursor with all locations. Columns: KEY_ROWID, KEY_LABEL,
	 * KEY_ALLOWED, KEY_KEYWORD
	 * 
	 * @return the cursor
	 * @throws SQLException
	 *             a SQL exception
	 */
	public Cursor fetchAllLocations() throws SQLException {
		return mDb.query(DATABASE_TABLE_LOCATIONS, new String[] { KEY_ROWID,
				KEY_LABEL, KEY_ALLOWED, KEY_KEYWORD }, null, null, null, null,
				KEY_LABEL);
	}

	/**
	 * Returns a cursor with the blocked numbers Columns: KEY_ROWID, KEY_NUMBER.
	 * 
	 * @return the cursor
	 * @throws SQLException
	 *             a SQL exception
	 */
	public Cursor fetchBlockedNumbers() throws SQLException {
		return mDb.rawQuery("select _id,number from blockednumbers;", null);
	}

	/**
	 * Returns a cursor with call around reports for all days in the database.
	 * Columns: KEY_ROWID,KEY_DUEBY,KEY_OUTSTANDING,KEY_RESOLVED
	 * 
	 * @param showFuture
	 *            the show future
	 * @return the cursor
	 * @throws SQLException
	 *             a SQL exception
	 */
	public Cursor fetchCallaroundReport(boolean showFuture) throws SQLException {
		String future = "";
		if (showFuture) {
			future = " where date(dueby) <= date('" + Time.iso8601Date() + "')";
		}
		return mDb
				.rawQuery(
						"select _id,date(dueby) as dueby,count(nullif(outstanding,0)) as outstanding,count(nullif(outstanding,1)) as resolved from callarounds"
								+ future
								+ " group by date(dueby) order by dueby desc",
						null);
	}

	/**
	 * Returns a cursor with the call around report for one day. KEY_NAME is the
	 * name of the house. KEY_TIMERECEIVED is empty or null if the call around
	 * has not been resolved. Columns: KEY_ROWID, KEY_NAME, KEY_TIMERECEIVED,
	 * KEY_OUTSTANDING, KEY_DUEFROM, KEY_DUEBY, KEY_DELAYED
	 * 
	 * @param isoday
	 *            the requested day, in ISO 8601 format (2012-06-18)
	 * @return the cursor
	 * @throws SQLException
	 *             a SQL exception
	 */
	public Cursor fetchCallaroundReportForDay(String isoday)
			throws SQLException {
		return mDb
				.rawQuery(
						"select callarounds._id as _id,name,timereceived,outstanding,duefrom,dueby,delayed from callarounds,houses where date(dueby)='"
								+ isoday
								+ "' and callarounds.house_id=houses._id order by outstanding asc;",
						null);
	}

	/**
	 * Returns a cursor with all checked in people. KEY_LABEL is the house name.
	 * Columns: KEY_ROWID, KEY_NAME, KEY_LABEL
	 * 
	 * @return the cursor
	 * @throws SQLException
	 *             a SQL exception
	 */
	public Cursor fetchCheckedInPeople() throws SQLException {
		return mDb
				.rawQuery(
						"select contacts._id,contacts.name as name, houses.name as label from contacts left join housemembers on housemembers.contact_id=contacts._id left join houses on houses._id=housemembers.house_id  where contacts._id not in (select contact_id from checkins where tripresolved='0');",
						null);
	}

	/**
	 * Returns a cursor with all people not checked out. KEY_LABEL is the house
	 * name. Columns: KEY_ROWID, KEY_NAME, KEY_LABEL
	 * 
	 * @return the cursor
	 * @throws SQLException
	 *             a SQL exception
	 */
	public Cursor fetchCheckedOutPeople() throws SQLException {
		return mDb
				.rawQuery(
						"select contacts._id,contacts.name as name, houses.name as label from contacts left join housemembers on housemembers.contact_id=contacts._id left join houses on houses._id=housemembers.house_id  where contacts._id in (select contact_id from checkins where tripresolved='0');",
						null);
	}

	/**
	 * Returns a cursor with names and phone numbers for a given house. Columns:
	 * KEY_ROWID, KEY_NAME, KEY_NUMBER
	 * 
	 * @param house_id
	 *            the house_id
	 * @return the cursor
	 * @throws SQLException
	 *             a SQL exception
	 */
	public Cursor fetchContactsForHouse(long house_id) throws SQLException {
		return mDb
				.rawQuery(
						"select contactphones._id,name,number from contacts,housemembers,contactphones on contacts._id=contactphones.contact_id and contacts._id=housemembers.contact_id where house_id='"
								+ String.valueOf(house_id) + "';", null);
	}

	/**
	 * Return a cursor with all of the guards. Columns: KEY_ROWID, KEY_NAME,
	 * KEY_NUMBER
	 * 
	 * @return the cursor
	 * @throws SQLException
	 *             a SQL exception
	 */
	public Cursor fetchAllGuards() throws SQLException {
		return mDb.query(DATABASE_TABLE_GUARDS, new String[] { KEY_ROWID,
				KEY_NAME, KEY_NUMBER }, null, null, null, null, KEY_NAME);
	}

	/**
	 * Get a report of a guard's check-ins that were due before the present
	 * time. Columns: KEY_ID, KEY_TIME, KEY_RESPONSE
	 * 
	 * @param guard_id
	 * @return a cursor with the report
	 * @throws SQLException
	 */
	public Cursor fetchGuardCheckinReport(long guard_id) throws SQLException {
		return mDb
				.rawQuery(
						"select _id,time,response from guardcheckins where guard_id=? order by time desc;",
						new String[] { String.valueOf(guard_id) });
	}

	/**
	 * Return a cursor with a list of all check-ins. Columns: KEY_ROWID,
	 * KEY_LOCATION, KEY_TIMEDUE, KEY_NAME, KEY_OUTSTANDING
	 * 
	 * @return the cursor
	 * @throws SQLException
	 *             a SQL exception
	 */
	public Cursor fetchLog() throws SQLException {
		return mDb.rawQuery(
				"select _id,type,message,time from log order by time desc;",
				null);
	}

	/**
	 * Return a cursor with a list of outstanding check-ins. Columns: KEY_ROWID,
	 * KEY_LOCATION, KEY_TIMEDUE, KEY_NAME
	 * 
	 * @return the cursor
	 * @throws SQLException
	 *             a SQL exception
	 */
	public Cursor fetchOustandingCheckins() throws SQLException {
		return mDb
				.rawQuery(
						"select checkins._id,location,timedue,name from checkins,contacts where checkins.contact_id=contacts._id and outstanding='1';",
						null);
	}

	/**
	 * Fetch unsent and undelivered messages. Columns KEY_ID, KEY_NUMBER,
	 * KEY_MESSAGE, KEY_TIME, KEY_SENT, KEY_DELIVERED
	 * 
	 * @return the cursor
	 */
	public Cursor fetchUnsentUndeliveredMessages() {
		return mDb.rawQuery(
				"select _id,number,message,time,sent,delivered from pending;",
				null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#finalize()
	 */
	@Override
	protected void finalize() throws Throwable {
		super.finalize();

		// this may have been causing problems....
		// 9/3/2012: I don't understand the above comment
		// close();
	}

	/**
	 * Returns true if the house is expected to be doing call around, otherwise
	 * false.
	 * 
	 * @param house_id
	 *            the house_id
	 * @return True if the call around is active, otherwise false.
	 * @throws SQLException
	 *             a SQL exception
	 */
	public boolean getCallaroundActive(long house_id) throws SQLException {
		Cursor c = mDb.query(DATABASE_TABLE_HOUSES, new String[] { KEY_ROWID },
				KEY_ROWID + "='" + house_id + "' and " + KEY_ACTIVE + "='1'",
				null, null, null, null);
		boolean r = c.moveToFirst();
		c.close();
		return r;
	}

	/**
	 * Returns true if a current call around is outstanding, otherwise false.
	 * 
	 * @param house_id
	 *            the house_id of the call around
	 * @return True if the call around is outstanding, otherwise false.
	 * @throws SQLException
	 *             a SQL exception
	 */
	public boolean getCallaroundOutstanding(long house_id) throws SQLException {
		Cursor c = mDb.rawQuery(
				"select count(_id) as count from callarounds where house_id='"
						+ String.valueOf(house_id) + "' and outstanding='1';",
				null);

		if (c.moveToFirst()) {
			return c.getLong(0) > 0 ? true : false;
		} else {
			return false;
		}
	}

	/**
	 * Returns true if a current call around is outstanding and is eligible to
	 * be resolved at the current moment, otherwise false.
	 * 
	 * @param house_id
	 *            the house_id of the call around
	 * @return True if the call around is outstanding, otherwise false.
	 * @throws SQLException
	 *             a SQL exception
	 */
	public boolean getCallaroundTimely(long house_id) throws SQLException {
		String now = Time.iso8601DateTime();

		Cursor c = mDb.rawQuery(
				"select count(_id) as count from callarounds where house_id='"
						+ String.valueOf(house_id)
						+ "' and outstanding='1' and datetime('" + now
						+ "') >= datetime(duefrom) and datetime('" + now
						+ "') <= datetime(dueby);", null);

		if (c.moveToFirst()) {
			return c.getLong(0) > 0 ? true : false;
		} else {
			return false;
		}
	}

	/**
	 * Returns true if the specified call around is outstanding, otherwise
	 * false.
	 * 
	 * @param rowId
	 *            the _id of the call around
	 * @return True if the call around is outstanding, otherwise false.
	 * @throws SQLException
	 *             a SQL exception
	 */
	public boolean getCallaroundOutstandingFromId(long rowId)
			throws SQLException {
		Cursor c = mDb.query(DATABASE_TABLE_CALLAROUNDS,
				new String[] { KEY_OUTSTANDING }, KEY_ROWID + "=?",
				new String[] { String.valueOf(rowId) }, null, null, null);
		if (c.moveToFirst()) {
			return c.getLong(0) > 0 ? true : false;
		} else {
			return false;
		}
	}

	/**
	 * Returns true if a current call around is resolved, otherwise false.
	 * 
	 * @param house_id
	 *            the house_id of the call around
	 * @return True if the call around is outstanding, otherwise false.
	 * @throws SQLException
	 *             a SQL exception
	 */
	public boolean getCallaroundResolved(long house_id) throws SQLException {
		String now = Time.iso8601DateTime();

		Cursor c = mDb.rawQuery(
				"select count(_id) as count from callarounds where house_id='"
						+ String.valueOf(house_id)
						+ "' and outstanding='1' and datetime('" + now
						+ "') >= datetime(duefrom) and datetime('" + now
						+ "') <= datetime(dueby);", null);

		if (c.moveToFirst()) {
			return c.getLong(0) > 0 ? false : true;
		} else {
			return false;
		}
	}

	/**
	 * Returns true if the specified call around is resolved, otherwise false.
	 * 
	 * @param rowId
	 *            the _id of the call around
	 * @return True if the call around is outstanding, otherwise false.
	 * @throws SQLException
	 *             a SQL exception
	 */
	public boolean getCallaroundResolvedFromId(long rowId) throws SQLException {
		Cursor c = mDb.query(DATABASE_TABLE_CALLAROUNDS,
				new String[] { KEY_OUTSTANDING }, KEY_ROWID + "=?",
				new String[] { String.valueOf(rowId) }, null, null, null);
		if (c.moveToFirst()) {
			return c.getLong(0) > 0 ? false : true;
		} else {
			return false;
		}
	}

	/**
	 * Returns a formatted string with a summary of the call arounds for the
	 * date (i.e., how many called in, how many not, etc.).
	 * 
	 * @param date
	 *            the date
	 * @return the call around summary
	 * @throws SQLException
	 *             a SQL exception
	 */
	public String getCallaroundSummary(Date date) throws SQLException {
		Cursor c = mDb
				.rawQuery(
						"select count(nullif(outstanding,0)) as outstanding,count(nullif(outstanding,1)) as resolved from callarounds where date(dueby)='"
								+ Time.iso8601Date(date) + "';", null);
		if (c.moveToFirst()) {
			long outstanding = c.getLong(0);
			long resolved = c.getLong(1);
			if (outstanding + resolved == 0) {
				return mContext.getString(R.string.callaround_summary_none);
			} else if (outstanding == 0) {
				return mContext.getString(R.string.callaround_summary_allin);
			} else {
				return String.format(
						mContext.getString(R.string.callaround_summary),
						String.valueOf(resolved), String.valueOf(outstanding));
			}
		} else {
			return null;
		}
	}

	/**
	 * Returns true if the check-in is outstanding, otherwise false.
	 * 
	 * @param checkin_id
	 *            the checkin_id
	 * @return true if the check-in is outstanding, otherwise false.
	 * @throws SQLException
	 *             a SQL exception
	 */
	public boolean getCheckinOutstanding(long checkin_id) throws SQLException {
		Cursor c = mDb.query(DATABASE_TABLE_CHECKINS,
				new String[] { KEY_OUTSTANDING }, KEY_ROWID + "=?",
				new String[] { String.valueOf(checkin_id) }, null, null, null);
		if (!c.moveToFirst()) {
			return false;
		}
		long r = c.getLong(0);
		c.close();
		return r == 1 ? true : false;
	}

	/**
	 * Returns true if the check-in is trip-resolved, otherwise false.
	 * 
	 * @param checkin_id
	 *            the checkin_id
	 * @return true if the check-in is outstanding, otherwise false.
	 * @throws SQLException
	 *             a SQL exception
	 */
	public boolean getCheckinTripResolved(long checkin_id) throws SQLException {
		Cursor c = mDb.query(DATABASE_TABLE_CHECKINS,
				new String[] { KEY_TRIPRESOLVED }, KEY_ROWID + "=?",
				new String[] { String.valueOf(checkin_id) }, null, null, null);
		if (!c.moveToFirst()) {
			return false;
		}
		long r = c.getLong(0);
		c.close();
		return r == 1 ? true : false;
	}

	/**
	 * Gets the current checkin for contact.
	 * 
	 * @param contact_id
	 *            the contact_id
	 * @return the current checkin for contact
	 * @throws SQLException
	 *             the sQL exception
	 */
	public long getCurrentCheckinForContact(long contact_id)
			throws SQLException {
		Cursor c = mDb.rawQuery("select _id from checkins where contact_id='"
				+ String.valueOf(contact_id) + "' and outstanding='1';", null);
		if (c.moveToFirst()) {
			return c.getLong(0);
		} else {
			return -1;
		}
	}

	/**
	 * Returns the time that the checkin is due for the given checkin.
	 * 
	 * @param checkin_id
	 *            the _id of the checkin
	 * @return the time the checkin is due
	 * @throws SQLException
	 *             a SQL exception
	 */
	public String getCheckinTime(long checkin_id) throws SQLException {
		Cursor c = mDb.query(DATABASE_TABLE_CHECKINS,
				new String[] { KEY_TIMEDUE }, KEY_ROWID + "=?",
				new String[] { String.valueOf(checkin_id) }, null, null, null);
		if (!c.moveToFirst()) {
			return null;
		}
		String r = c.getString(0);
		c.close();
		return r;
	}

	/**
	 * Returns a formatted string with the number of outstanding checkins.
	 * 
	 * @return the checkin summary
	 * @throws SQLException
	 *             a SQL exception
	 */
	public String getCheckinSummary() throws SQLException {
		Cursor c = mDb
				.rawQuery(
						"select count(contact_id) from (select distinct contact_id from checkins where tripresolved='0');",
						null);
		if (c.moveToFirst()) {
			long outstanding = c.getLong(0);
			if (outstanding == 0) {
				return mContext.getString(R.string.checkin_summary_none);
			} else if (outstanding == 1) {
				return mContext.getString(R.string.checkin_summary_singular);
			} else {
				return String.format(
						mContext.getString(R.string.checkin_summary),
						String.valueOf(outstanding));
			}
		} else {
			return null;
		}
	}

	/**
	 * Returns the first email in the database for the contact.
	 * 
	 * @param contactId
	 *            the contact id
	 * @return the contact email
	 * @throws SQLException
	 *             a SQL exception
	 */
	public String getContactEmail(long contactId) throws SQLException {
		Cursor c = mDb.rawQuery(
				"select email from contactemails where contact_id=? limit 1;",
				new String[] { String.valueOf(contactId) });
		if (c.moveToFirst()) {
			return c.getString(0);
		} else {
			return null;
		}
	}

	/**
	 * Returns true if the contact has an outstanding check-in, otherwise false.
	 * 
	 * @param contact_id
	 *            the contact_id
	 * @return True if the contact has an outstanding check-in, otherwise false.
	 * @throws SQLException
	 *             a SQL exception
	 */
	public boolean getContactHasCheckinOutstanding(long contact_id)
			throws SQLException {
		Cursor c = mDb.query(DATABASE_TABLE_CHECKINS,
				new String[] { KEY_ROWID }, KEY_CONTACTID + "='" + contact_id
						+ "' and " + KEY_OUTSTANDING + "='1'", null, null,
				null, null);
		boolean r = c.moveToFirst();
		c.close();
		return r;
	}

	/**
	 * Returns the _id of the contact associated with the phone number.
	 * 
	 * @param phoneNumber
	 *            the phone number
	 * @return the contact id
	 * @throws SQLException
	 *             a SQL exception
	 */
	public long getContactId(String phoneNumber) throws SQLException {
		Cursor c = mDb.rawQuery(
				"select contact_id from contactphones where number=? limit 1;",
				new String[] { phoneNumber });
		if (c.moveToFirst()) {
			return c.getLong(0);
		} else {
			return -1;
		}
	}

	/**
	 * Returns the name of the contact.
	 * 
	 * @param contactId
	 *            the contact id
	 * @return the contact name
	 * @throws SQLException
	 *             a SQL exception
	 */
	public String getContactName(long contactId) throws SQLException {
		Cursor c = mDb.rawQuery("select name from contacts where _id=?;",
				new String[] { String.valueOf(contactId) });
		if (c.moveToFirst()) {
			return c.getString(0);
		} else {
			return null;
		}
	}

	/**
	 * Returns the number of the contact.
	 * 
	 * @param contactId
	 *            the contact id
	 * @return the contact number
	 * @throws SQLException
	 *             a SQL exception
	 */
	public String getContactNumber(long contactId) throws SQLException {
		Cursor c = mDb.rawQuery(
				"select number from contactphones where contact_id=? limit 1;",
				new String[] { String.valueOf(contactId) });
		if (c.moveToFirst()) {
			return c.getString(0);
		} else {
			return null;
		}
	}

	/**
	 * Returns the name of the guard.
	 * 
	 * @param guardId
	 *            the guard id
	 * @return the guard name
	 * @throws SQLException
	 *             a SQL exception
	 */
	public String getGuardName(long guardId) throws SQLException {
		Cursor c = mDb.rawQuery("select name from guards where _id=?;",
				new String[] { String.valueOf(guardId) });
		if (c.moveToFirst()) {
			return c.getString(0);
		} else {
			return null;
		}
	}

	/**
	 * Returns the number of the guard.
	 * 
	 * @param guardId
	 *            the guard id
	 * @return the guard number
	 * @throws SQLException
	 *             a SQL exception
	 */
	public String getGuardNumber(long guardId) throws SQLException {
		Cursor c = mDb.rawQuery("select number from guards where _id=?;",
				new String[] { String.valueOf(guardId) });
		if (c.moveToFirst()) {
			return c.getString(0);
		} else {
			return null;
		}
	}

	/**
	 * Gets the guard id from number.
	 * 
	 * @param number
	 *            the number
	 * @return the guard id from number
	 * @throws SQLException
	 *             the sQL exception
	 */
	public long getGuardIdFromNumber(String number) throws SQLException {
		Cursor c = mDb.rawQuery("select _id from guards where number=?;",
				new String[] { number });
		if (c.moveToFirst()) {
			return c.getLong(0);
		} else {
			return -1;
		}
	}

	/**
	 * Returns the specified contact's specified preference.
	 * 
	 * @param contact_id
	 *            the contact_id
	 * @param preferenceId
	 *            the preference id
	 * @return the value of the preference
	 * @throws SQLException
	 *             a SQL exception
	 */
	public boolean getContactPermission(long contact_id, long preferenceId)
			throws SQLException {
		Cursor c = mDb.query(DATABASE_TABLE_CONTACTS,
				new String[] { KEY_PERMISSIONS }, KEY_ROWID + "=?",
				new String[] { String.valueOf(contact_id) }, null, null, null);
		c.moveToFirst();
		long result = c.getLong(0);
		c.close();
		return (preferenceId & result) > 0;
	}

	/**
	 * Returns the specified contact's specified permission.
	 * 
	 * @param contact_id
	 *            the contact_id
	 * @param permissionId
	 *            the permission id
	 * @return the value of the preference
	 * @throws SQLException
	 *             a SQL exception
	 */
	public boolean getContactPreference(long contact_id, long permissionId)
			throws SQLException {
		Cursor c = mDb.query(DATABASE_TABLE_CONTACTS,
				new String[] { KEY_PREFERENCES }, KEY_ROWID + "=?",
				new String[] { String.valueOf(contact_id) }, null, null, null);
		c.moveToFirst();
		long result = c.getLong(0);
		c.close();
		return (permissionId & result) > 0;
	}

	/**
	 * Return a list of forbidden locations in a CSV string.
	 * 
	 * @return the string
	 * @throws SQLException
	 *             a SQL exception
	 */
	public String getForbiddenLocations() throws SQLException {
		Cursor cursor = mDb.query(DATABASE_TABLE_LOCATIONS, new String[] {
				KEY_ROWID, KEY_LABEL }, KEY_ALLOWED + "='0'", null, null, null,
				KEY_LABEL);
		if (cursor.moveToFirst()) {
			String r = "";
			for (int i = 0; i < cursor.getCount(); i++) {
				r += cursor.getString(cursor
						.getColumnIndexOrThrow(DbAdapter.KEY_LABEL));
				if (!cursor.isLast()) {
					r += ", ";
				}
				cursor.moveToNext();
			}
			cursor.close();
			return r;
		} else {
			return null;
		}
	}

	/**
	 * Gets the location keywords.
	 * 
	 * @return the location keywords
	 * @throws SQLException
	 *             the sQL exception
	 */
	public String getLocationKeywords() throws SQLException {
		Cursor cursor = mDb.query(DATABASE_TABLE_LOCATIONS,
				new String[] { KEY_KEYWORD }, null, null, null, null,
				KEY_KEYWORD);
		if (cursor.moveToFirst()) {
			String r = "";
			for (int i = 0; i < cursor.getCount(); i++) {
				r += cursor.getString(cursor
						.getColumnIndexOrThrow(DbAdapter.KEY_KEYWORD));
				if (!cursor.isLast()) {
					r += ", ";
				}
				cursor.moveToNext();
			}
			cursor.close();
			return r;
		} else {
			return null;
		}
	}

	/**
	 * Returns the _id of the house associated with the contact.
	 * 
	 * @param contact_id
	 *            the contact_id
	 * @return the house id
	 * @throws SQLException
	 *             a SQL exception
	 */
	public long getHouseId(long contact_id) throws SQLException {
		Cursor c = mDb.query(DATABASE_TABLE_HOUSEMEMBERS,
				new String[] { KEY_HOUSEID }, KEY_CONTACTID + "= ?",
				new String[] { String.valueOf(contact_id) }, null, null, null);
		int r = -1;
		if (c.moveToFirst()) {
			r = c.getInt(c.getColumnIndex(KEY_HOUSEID));
		}
		c.close();
		return r;
	}

	/**
	 * Returns the _id of the house associated with the call around.
	 * 
	 * @param callaround_id
	 *            the calll around id
	 * @return the house id
	 * @throws SQLException
	 *             a SQL exception
	 */
	public long getHouseIdFromCallaround(long callaround_id)
			throws SQLException {
		Cursor c = mDb.query(DATABASE_TABLE_CALLAROUNDS,
				new String[] { KEY_HOUSEID }, KEY_ROWID + "= ?",
				new String[] { String.valueOf(callaround_id) }, null, null,
				null);
		int r = -1;
		if (c.moveToFirst()) {
			r = c.getInt(c.getColumnIndex(KEY_HOUSEID));
		}
		c.close();
		return r;
	}

	/**
	 * Returns the name of the house.
	 * 
	 * @param rowId
	 *            the row id
	 * @return the house name
	 * @throws SQLException
	 *             a SQL exception
	 */
	public String getHouseName(long rowId) throws SQLException {
		Cursor c = mDb.query(DATABASE_TABLE_HOUSES, new String[] { KEY_NAME },
				KEY_ROWID + "= ?", new String[] { String.valueOf(rowId) },
				null, null, null);
		if (c.moveToFirst()) {
			return c.getString(0);
		} else {
			return null;
		}
	}

	/**
	 * Returns whether the callaround is delayed or not.
	 * 
	 * @param rowId
	 *            the row id
	 * @return True if the callaround is delayed, otherwise false.
	 * @throws SQLException
	 *             the sQL exception
	 */
	public boolean getCallaroundDelayed(long rowId) throws SQLException {
		Cursor c = mDb.query(DATABASE_TABLE_CALLAROUNDS,
				new String[] { KEY_DELAYED }, KEY_HOUSEID + "= ?",
				new String[] { String.valueOf(rowId) }, null, null, null);
		if (c.moveToFirst()) {
			return c.getInt(0) == 1 ? true : false;
		} else {
			return false;
		}
	}

	/**
	 * Returns the name of the location.
	 * 
	 * @param rowId
	 *            the row id
	 * @return the location name
	 * @throws SQLException
	 *             a SQL exception
	 */
	public String getLocationName(long rowId) throws SQLException {
		Cursor c = mDb.query(DATABASE_TABLE_LOCATIONS,
				new String[] { KEY_LABEL }, KEY_ROWID + "= ?",
				new String[] { String.valueOf(rowId) }, null, null, null);
		if (c.moveToFirst()) {
			return c.getString(0);
		} else {
			return null;
		}
	}

	/**
	 * Return true if the given location keyword exists in the database;
	 * otherwise returns false.
	 * 
	 * @param keyword
	 *            the keyword
	 * @return true if the given location keyword exists in the database,
	 *         otherwise false
	 * @throws SQLException
	 *             the sQL exception
	 */
	public boolean getLocationKeywordExists(String keyword) throws SQLException {
		Cursor c = mDb.rawQuery(
				"select _id from locations where lower(keyword)=lower(?);",
				new String[] { keyword });
		if (c.moveToFirst()) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Return true if the given location keyword exists in the database;
	 * otherwise returns false.
	 * 
	 * @param keyword
	 *            the keyword
	 * @return true if the given location keyword exists in the database,
	 *         otherwise false
	 * @throws SQLException
	 *             the sQL exception
	 */
	public boolean getLocationKeywordPermitted(String keyword)
			throws SQLException {
		Cursor c = mDb.rawQuery(
				"select allowed from locations where lower(keyword)=lower(?);",
				new String[] { keyword });
		if (c.moveToFirst()) {
			return c.getLong(0) == 1 ? true : false;
		} else {
			return false;
		}
	}

	/**
	 * Returns a number given the _id from the contactphones table.
	 * 
	 * @param contactphoneId
	 *            the contactphone id
	 * @return the contact number
	 * @throws SQLException
	 *             a SQL exception
	 */
	public String getNumber(long contactphoneId) throws SQLException {
		Cursor c = mDb.rawQuery(
				"select number from contactphones where _id=? limit 1;",
				new String[] { String.valueOf(contactphoneId) });
		if (c.moveToFirst()) {
			return c.getString(0);
		} else {
			return null;
		}
	}

	/**
	 * Returns the phone number associated with a check-in request.
	 * 
	 * @param checkinId
	 *            the checkin id
	 * @return the number for checkin
	 * @throws SQLException
	 *             a SQL exception
	 */
	public String getNumberForCheckin(long checkinId) throws SQLException {
		Cursor c = mDb
				.rawQuery(
						"select number from contactphones,checkins where checkins.contact_id=contactphones.contact_id and checkins._id=? limit 1;",
						new String[] { String.valueOf(checkinId) });
		if (c.moveToFirst()) {
			return c.getString(0);
		} else {
			return null;
		}
	}

	/**
	 * Returns true if the phone number is on the blocked list, otherwise false.
	 * 
	 * @param number
	 *            the number
	 * @return true if the phone number is on the blocked list, otherwise false
	 * @throws SQLException
	 *             a SQL exception
	 */
	public boolean getNumberIsBlocked(String number) throws SQLException {
		Cursor c = mDb
				.rawQuery(
						"select count(_id) as count from blockednumbers where number=?;",
						new String[] { number });
		if (c.moveToFirst()) {
			return c.getLong(c.getColumnIndex(KEY_COUNT)) > 0 ? true : false;
		} else {
			return false;
		}
	}

	/**
	 * Returns the number of call arounds that are due, excluding delayed
	 * callarounds.
	 * 
	 * @return the number of due call arounds
	 * @throws SQLException
	 *             a SQL exception
	 */
	public long getNumberOfDueCallarounds() throws SQLException {
		Cursor c = mDb.rawQuery(
				"select count(_id) as count from callarounds where date(dueby)='"
						+ Time.iso8601Date()
						+ "' and outstanding='1' and delayed='0';", null);
		if (c.moveToFirst()) {
			return c.getLong(c.getColumnIndex(KEY_COUNT));
		} else {
			return 0;
		}
	}

	/**
	 * Returns the number of call arounds that are due, including delayed
	 * callarounds.
	 * 
	 * @return the number of due call arounds
	 * @throws SQLException
	 *             a SQL exception
	 */
	public long getNumberOfDueCallaroundsIncludingDelayed() throws SQLException {
		Cursor c = mDb.rawQuery(
				"select count(_id) as count from callarounds where date(dueby)='"
						+ Time.iso8601Date() + "' and outstanding='1';", null);
		if (c.moveToFirst()) {
			return c.getLong(c.getColumnIndex(KEY_COUNT));
		} else {
			return 0;
		}
	}

	/**
	 * Returns the number of check-ins that are due.
	 * 
	 * @return the number of due check-ins
	 * @throws SQLException
	 *             a SQL exception
	 */
	public long getNumberOfDueCheckins() throws SQLException {
		Cursor c = mDb
				.rawQuery(
						// "select count(_id) as count from checkins where outstanding='1' and strftime('%s',timedue) <= strftime('%s','now');",
						"select count(_id) as count from checkins where outstanding='1' and datetime(timedue) <= datetime('now','localtime');",
						null);
		if (c.moveToFirst()) {
			return c.getLong(c.getColumnIndex(KEY_COUNT));
		} else {
			return 0;
		}
	}

	/**
	 * Returns the combined number of unsent and undelivered messages in the
	 * database.
	 * 
	 * @return the combined number of unsent and undelivered messages in the
	 *         database
	 */
	public int getNumberOfMessageErrors() {
		Cursor c = mDb.rawQuery("select count(_id) from pending;", null);
		return c.moveToFirst() ? c.getInt(0) : 0;
	}

	/**
	 * Returns the path to the database file.
	 * 
	 * @return the path to the database file
	 */
	public String getPath() {
		return mDb.getPath();
	}

	/**
	 * Returns a formatted report out outstanding call arounds and check-ins.
	 * 
	 * @return the value of SQLite's last_insert_rowid()
	 * @throws SQLException
	 *             a SQL exception
	 */
	public String getReport() throws SQLException {
		String checkin_people = "";
		String callaround_houses = "";

		Cursor c = mDb
				.rawQuery(
						"select name,location from checkins left join contacts on contacts._id=checkins.contact_id where outstanding='1';",
						null);
		if (c.moveToFirst()) {
			for (int i = 0; i < c.getCount(); i++) {
				checkin_people += c.getString(0) + " (" + c.getString(1) + ")";
				if (!c.isLast()) {
					checkin_people += ", ";
				}
				c.moveToNext();
			}
		} else {
			checkin_people = mContext.getString(R.string.none);
		}

		c = mDb.rawQuery(
				"select name from callarounds left join houses on callarounds.house_id=houses._id where outstanding='1' and date(dueby)=date('now');",
				null);
		if (c.moveToFirst()) {
			for (int i = 0; i < c.getCount(); i++) {
				callaround_houses += c.getString(0);
				if (!c.isLast()) {
					callaround_houses += ", ";
				}
				c.moveToNext();
			}
		} else {
			checkin_people = mContext.getString(R.string.none);
		}

		String checkin_report = String.format(
				mContext.getString(R.string.sms_report_checkins),
				checkin_people);
		String callaround_report = String.format(
				mContext.getString(R.string.sms_report_callarounds),
				callaround_houses);
		String report = String.format(mContext.getString(R.string.sms_report),
				checkin_report, callaround_report);

		return report;
	}

	/**
	 * Returns the value of SQLite's last_insert_rowid() function.
	 * 
	 * @return the value of SQLite's last_insert_rowid()
	 * @throws SQLException
	 *             a SQL exception
	 */
	public int lastInsertId() throws SQLException {
		Cursor c = mDb.rawQuery("select last_insert_rowid();", null);
		if (c.moveToFirst()) {
			return c.getInt(0);
		} else {
			return -1;
		}
	}

	/**
	 * Opens the database.
	 * 
	 * @return the database helper object
	 * @throws SQLException
	 *             a SQL exception
	 */
	public DbAdapter open() throws SQLException {
		mDbHelper = new DatabaseHelper(mContext);
		mDb = mDbHelper.getWritableDatabase();

		return this;
	}

	/**
	 * Sets the call around status of the house (i.e., whether the house is
	 * expected to do call around or not.)
	 * 
	 * @param house_id
	 *            the house_id
	 * @param active
	 *            the active
	 * @return the int
	 * @throws SQLException
	 *             a SQL exception
	 */
	public int setCallaroundActive(long house_id, boolean active)
			throws SQLException {
		// add today's, or remove it
		if (active) {
			addCallarounds();
		} else {
			String now = Time.iso8601DateTime();
			mDb.delete(DATABASE_TABLE_CALLAROUNDS, "datetime('" + now
					+ "') >= datetime(duefrom) and datetime('" + now
					+ "') <= datetime(dueby) and " + KEY_HOUSEID + "='"
					+ String.valueOf(house_id) + "' and outstanding='1'", null);
		}

		ContentValues args = new ContentValues();
		args.put(KEY_ACTIVE, active ? 1 : 0);
		if (mDb.update(DATABASE_TABLE_HOUSES, args, KEY_ROWID + "=" + house_id,
				null) > 0) {
			return NOTIFY_SUCCESS;
		} else {
			return NOTIFY_FAILURE;
		}
	}

	/**
	 * Sets whether the callaround is delayed or not.
	 * 
	 * @param house_id
	 *            the house id
	 * @param delayed
	 *            whether the callaround should be delayed or... un-delayed
	 * @return either NOTIFY_SUCCESS or NOTIFY_FAILURE
	 * @throws SQLException
	 *             the sQL exception
	 */
	public int setCallaroundDelayed(long house_id, boolean delayed)
			throws SQLException {
		ContentValues args = new ContentValues();
		args.put(KEY_DELAYED, delayed ? 1 : 0);
		if (mDb.update(DATABASE_TABLE_CALLAROUNDS, args, KEY_HOUSEID + "="
				+ house_id, null) > 0) {
			return NOTIFY_SUCCESS;
		} else {
			return NOTIFY_FAILURE;
		}
	}

	/**
	 * Sets whether an existing call around is resolved or not, for whatever
	 * call arounds can be resolved.
	 * 
	 * @param house_id
	 *            the house_id of the call around to update
	 * @param resolved
	 *            whether the call around is to be resolved or not
	 * @return Possible return values: NOTIFY_SUCCESS, NOTIFY_FAILURE,
	 *         NOTIFY_ALREADY, NOTIFY_INACTIVE
	 * @throws SQLException
	 *             a SQL exception
	 */
	public int setCallaroundResolved(long house_id, boolean resolved)
			throws SQLException {
		boolean outstanding = getCallaroundOutstanding(house_id);
		boolean active = getCallaroundActive(house_id);
		// if we're not expecting a call around from the house, say so
		if (!active) {
			return NOTIFY_INACTIVE;
		}
		// if it's not a timely callaround
		if (!getCallaroundTimely(house_id)) {
			return NOTIFY_UNTIMELY;
		}
		// if this is already in effect
		if (outstanding == !resolved) {
			return NOTIFY_ALREADY;
		}

		String sOutstanding = resolved ? "0" : "1";
		String now = Time.iso8601DateTime();
		mDb.execSQL("update callarounds set outstanding='" + sOutstanding
				+ "',timereceived='" + now + "' where datetime('" + now
				+ "') >= datetime(duefrom) and datetime('" + now
				+ "') <= datetime(dueby) and house_id='"
				+ String.valueOf(house_id) + "';");

		if (changes() > 0) {
			return NOTIFY_SUCCESS;
		} else {
			return NOTIFY_FAILURE;
		}
	}

	/**
	 * Sets whether an existing call around is resolved or not.
	 * 
	 * @param callaround_id
	 *            the callaround_id
	 * @param resolved
	 *            whether the call around is to be resolved or not
	 * @return Possible return values: NOTIFY_SUCCESS, NOTIFY_FAILURE,
	 *         NOTIFY_ALREADY, NOTIFY_INACTIVE
	 * @throws SQLException
	 *             a SQL exception
	 */
	public int setCallaroundResolvedFromId(long callaround_id, boolean resolved)
			throws SQLException {
		ContentValues args = new ContentValues();
		args.put(KEY_OUTSTANDING, resolved ? 0 : 1);
		args.put(KEY_TIMERECEIVED, resolved ? Time.iso8601DateTime() : "");
		if (mDb.update(DATABASE_TABLE_CALLAROUNDS, args, KEY_ROWID + "=?",
				new String[] { String.valueOf(callaround_id) }) > 0) {
			return NOTIFY_SUCCESS;
		} else {
			return NOTIFY_FAILURE;
		}
	}

	/**
	 * Set the resolution status of any check-ins associated with a given
	 * contact.
	 * 
	 * @param contact_id
	 *            the contact_id
	 * @param resolved
	 *            the resolved
	 * @return Possible values: NOTIFY_SUCCESS, NOTIFY_FAILURE
	 * @throws SQLException
	 *             a SQL exception
	 */
	public int setCheckinResolved(long contact_id, boolean resolved)
			throws SQLException {
		ContentValues args = new ContentValues();
		args.put(KEY_OUTSTANDING, resolved ? 0 : 1);
		if (mDb.update(DATABASE_TABLE_CHECKINS, args, KEY_CONTACTID + "=?",
				new String[] { String.valueOf(contact_id) }) > 0) {
			return NOTIFY_SUCCESS;
		} else {
			return NOTIFY_FAILURE;
		}
	}

	/**
	 * Set the trip-resolution status of any check-ins associated with a given
	 * contact. If the trip is resolved, the checkin is also marked as not
	 * outstanding.
	 * 
	 * @param contact_id
	 *            the contact_id
	 * @param resolved
	 *            the resolved
	 * @return Possible values: NOTIFY_SUCCESS, NOTIFY_FAILURE
	 * @throws SQLException
	 *             a SQL exception
	 */
	public int setCheckinTripResolved(long contact_id, boolean resolved)
			throws SQLException {
		ContentValues args = new ContentValues();
		args.put(KEY_TRIPRESOLVED, resolved ? 1 : 0);
		if (resolved) {
			args.put(KEY_OUTSTANDING, 0);
		}
		if (mDb.update(DATABASE_TABLE_CHECKINS, args, KEY_CONTACTID + "=?",
				new String[] { String.valueOf(contact_id) }) > 0) {
			return NOTIFY_SUCCESS;
		} else {
			return NOTIFY_FAILURE;
		}
	}

	/**
	 * Resolve a check-in, given its _id.
	 * 
	 * @param checkin_id
	 *            the checkin_id
	 * @param resolved
	 *            the resolved
	 * @return Possible values: NOTIFY_SUCCESS, NOTIFY_FAILURE
	 * @throws SQLException
	 *             a SQL exception
	 */
	public int setCheckinResolvedFromId(long checkin_id, boolean resolved)
			throws SQLException {
		ContentValues args = new ContentValues();
		args.put(KEY_OUTSTANDING, resolved ? 0 : 1);
		if (mDb.update(DATABASE_TABLE_CHECKINS, args, KEY_ROWID + "=?",
				new String[] { String.valueOf(checkin_id) }) > 0) {
			return NOTIFY_SUCCESS;
		} else {
			return NOTIFY_FAILURE;
		}
	}

	/**
	 * Trip-resolve a check-in, given its _id.
	 * 
	 * @param checkin_id
	 *            the checkin_id
	 * @param resolved
	 *            the resolved
	 * @return Possible values: NOTIFY_SUCCESS, NOTIFY_FAILURE
	 * @throws SQLException
	 *             a SQL exception
	 */
	public int setCheckinTripResolvedFromId(long checkin_id, boolean resolved)
			throws SQLException {
		ContentValues args = new ContentValues();
		args.put(KEY_TRIPRESOLVED, resolved ? 0 : 1);
		if (mDb.update(DATABASE_TABLE_CHECKINS, args, KEY_ROWID + "=?",
				new String[] { String.valueOf(checkin_id) }) > 0) {
			return NOTIFY_SUCCESS;
		} else {
			return NOTIFY_FAILURE;
		}
	}

	/**
	 * Sets the email associated with a particular contact.
	 * 
	 * @param contact_id
	 *            the contact_id
	 * @param newemail
	 *            the new email address
	 * @throws SQLException
	 *             a SQL exception
	 */
	public void setContactEmail(long contact_id, String newemail)
			throws SQLException {
		ContentValues args = new ContentValues();
		args.put(KEY_EMAIL, newemail);
		mDb.update(DATABASE_TABLE_CONTACTEMAILS, args, KEY_CONTACTID + "= ?",
				new String[] { String.valueOf(contact_id) });
		if (changes() == 0) {
			args.put(KEY_CONTACTID, contact_id);
			mDb.insert(DATABASE_TABLE_CONTACTEMAILS, null, args);
		}
	}

	/**
	 * Sets the name of a contact.
	 * 
	 * @param contact_id
	 *            the contact_id
	 * @param newname
	 *            the new name
	 * @throws SQLException
	 *             a SQL exception
	 */
	public void setContactName(long contact_id, String newname)
			throws SQLException {
		ContentValues args = new ContentValues();
		args.put(KEY_NAME, newname);
		mDb.update(DATABASE_TABLE_CONTACTS, args, KEY_ROWID + "= ?",
				new String[] { String.valueOf(contact_id) });
	}

	/**
	 * Sets the name of a guard.
	 * 
	 * @param guard_id
	 *            the guard's id
	 * @param newname
	 *            the new name
	 * @throws SQLException
	 *             a SQL exception
	 */
	public void setGuardName(long guard_id, String newname) throws SQLException {
		ContentValues args = new ContentValues();
		args.put(KEY_NAME, newname);
		mDb.update(DATABASE_TABLE_GUARDS, args, KEY_ROWID + "= ?",
				new String[] { String.valueOf(guard_id) });
	}

	/**
	 * Sets the phone number of a guard.
	 * 
	 * @param guard_id
	 *            the guard's id
	 * @param newnumber
	 *            the new name
	 * @throws SQLException
	 *             a SQL exception
	 */
	public void setGuardNumber(long guard_id, String newnumber)
			throws SQLException {
		ContentValues args = new ContentValues();
		args.put(KEY_NUMBER, newnumber);
		mDb.update(DATABASE_TABLE_GUARDS, args, KEY_ROWID + "= ?",
				new String[] { String.valueOf(guard_id) });
	}

	/**
	 * Sets the specified contact's specified permission.
	 * 
	 * @param contact_id
	 *            the contact_id
	 * @param permissionId
	 *            the id of the permission being changed
	 * @param pref
	 *            the new permission value
	 * @return Possible values: NOTIFY_SUCCESS, NOTIFY_FAILURE, NOTIFY_ALREADY
	 * @throws SQLException
	 *             a SQL exception
	 */
	public int setContactPermission(long contact_id, long permissionId,
			boolean pref) throws SQLException {
		Cursor c = mDb.query(DATABASE_TABLE_CONTACTS,
				new String[] { KEY_PERMISSIONS }, KEY_ROWID + "=?",
				new String[] { String.valueOf(contact_id) }, null, null, null);
		c.moveToFirst();
		long result = c.getLong(0);
		c.close();

		if (((result & permissionId) > 0) && pref) {
			return NOTIFY_ALREADY;
		}
		if (((result & permissionId) == 0) && !pref) {
			return NOTIFY_ALREADY;
		}

		if (pref) {
			result = result | permissionId;
		} else {
			result = result & (~permissionId);
		}

		ContentValues args = new ContentValues();
		args.put(KEY_PERMISSIONS, result);
		int nrow = mDb.update(DATABASE_TABLE_CONTACTS, args, KEY_ROWID + "= ?",
				new String[] { String.valueOf(contact_id) });
		if (nrow > 0) {
			return NOTIFY_SUCCESS;
		} else {
			return NOTIFY_FAILURE;
		}
	}

	/**
	 * Sets the phone number of a contact.
	 * 
	 * @param contact_id
	 *            the contact_id
	 * @param newPhoneNumber
	 *            the new phone number
	 * @throws SQLException
	 *             a SQL exception
	 */
	public void setContactPhone(long contact_id, String newPhoneNumber)
			throws SQLException {
		ContentValues args = new ContentValues();
		args.put(KEY_NUMBER, newPhoneNumber);
		mDb.update(DATABASE_TABLE_CONTACTPHONES, args, KEY_CONTACTID + "= ?",
				new String[] { String.valueOf(contact_id) });
	}

	/**
	 * Sets the specified contact's specified preference.
	 * 
	 * @param contact_id
	 *            the contact_id
	 * @param preferenceId
	 *            the preference id
	 * @param pref
	 *            the new permission value
	 * @return Possible values: NOTIFY_SUCCESS, NOTIFY_FAILURE, NOTIFY_ALREADY
	 * @throws SQLException
	 *             a SQL exception
	 */
	public int setContactPreference(long contact_id, long preferenceId,
			boolean pref) throws SQLException {
		Cursor c = mDb.query(DATABASE_TABLE_CONTACTS,
				new String[] { KEY_PREFERENCES }, KEY_ROWID + "=?",
				new String[] { String.valueOf(contact_id) }, null, null, null);
		c.moveToFirst();
		long result = c.getLong(0);
		c.close();

		if (((result & preferenceId) > 0) && pref) {
			return NOTIFY_ALREADY;
		}
		if (((result & preferenceId) == 0) && !pref) {
			return NOTIFY_ALREADY;
		}

		if (pref) {
			result = result | preferenceId;
		} else {
			result = result & (~preferenceId);
		}

		ContentValues args = new ContentValues();
		args.put(KEY_PREFERENCES, result);
		int nrow = mDb.update(DATABASE_TABLE_CONTACTS, args, KEY_ROWID + "= ?",
				new String[] { String.valueOf(contact_id) });
		if (nrow > 0) {
			return NOTIFY_SUCCESS;
		} else {
			return NOTIFY_FAILURE;
		}
	}

	/**
	 * Sets the house associated with a particular contact.
	 * 
	 * @param contact_id
	 *            the contact_id
	 * @param house_id
	 *            the house_id
	 * @throws SQLException
	 *             a SQL exception
	 */
	public void setHouse(long contact_id, long house_id) throws SQLException {
		mDb.delete(DATABASE_TABLE_HOUSEMEMBERS, KEY_CONTACTID + "="
				+ contact_id, null);

		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_CONTACTID, contact_id);
		initialValues.put(KEY_HOUSEID, house_id);
		mDb.insert(DATABASE_TABLE_HOUSEMEMBERS, null, initialValues);
	}

	/**
	 * Set the name of a house.
	 * 
	 * @param rowId
	 *            the row id
	 * @param name
	 *            the new name
	 * @throws SQLException
	 *             a SQL exception
	 */
	public void setHouseName(long rowId, String name) throws SQLException {
		ContentValues args = new ContentValues();
		args.put(KEY_NAME, name);
		mDb.update(DATABASE_TABLE_HOUSES, args, KEY_ROWID + "= ?",
				new String[] { String.valueOf(rowId) });
	}

	/**
	 * Sets whether travel to an existing location is permissible.
	 * 
	 * @param rowId
	 *            the row id
	 * @param allowed
	 *            whether the location is permitted for travel
	 * @return true, if successful
	 * @throws SQLException
	 *             a SQL exception
	 */
	public boolean setLocationAllowed(long rowId, boolean allowed)
			throws SQLException {
		ContentValues args = new ContentValues();
		args.put(KEY_ALLOWED, allowed ? 1 : 0);
		return mDb.update(DATABASE_TABLE_LOCATIONS, args, KEY_ROWID + "="
				+ rowId, null) > 0;
	}

	/**
	 * Sets all values associated with a location.
	 * 
	 * @param rowId
	 *            the row id (_id)
	 * @param label
	 *            the new label of the location
	 * @param allowed
	 *            whether the location is permitted for travel
	 * @return true, if successful
	 * @throws SQLException
	 *             a SQL exception
	 */
	public boolean setLocationDetails(long rowId, String label, boolean allowed)
			throws SQLException {
		ContentValues args = new ContentValues();
		args.put(KEY_LABEL, label);
		args.put(KEY_ALLOWED, allowed ? 1 : 0);
		return mDb.update(DATABASE_TABLE_LOCATIONS, args, KEY_ROWID + "="
				+ rowId, null) > 0;
	}

	/**
	 * Renames a location.
	 * 
	 * @param rowId
	 *            the row id
	 * @param name
	 *            the new name
	 * @throws SQLException
	 *             a SQL exception
	 */
	public void setLocationName(long rowId, String name) throws SQLException {
		ContentValues args = new ContentValues();
		args.put(KEY_LABEL, name);
		mDb.update(DATABASE_TABLE_LOCATIONS, args, KEY_ROWID + "= ?",
				new String[] { String.valueOf(rowId) });
	}

	/**
	 * Sets a location's keyword.
	 * 
	 * @param rowId
	 *            the row id
	 * @param keyword
	 *            the new keyword
	 * @throws SQLException
	 *             a SQL exception
	 */
	public void setLocationKeyword(long rowId, String keyword)
			throws SQLException {
		ContentValues args = new ContentValues();
		args.put(KEY_KEYWORD, keyword);
		mDb.update(DATABASE_TABLE_LOCATIONS, args, KEY_ROWID + "= ?",
				new String[] { String.valueOf(rowId) });
	}

	/**
	 * Sets the number as blocked or not.
	 * 
	 * @param number
	 *            the number
	 * @param blocked
	 *            the blocked
	 * @throws SQLException
	 *             a SQL exception
	 */
	public void setNumberIsBlocked(String number, boolean blocked)
			throws SQLException {
		if (blocked) {
			ContentValues initialValues = new ContentValues();
			initialValues.put(KEY_NUMBER, number);
			mDb.insert(DATABASE_TABLE_BLOCKEDNUMBERS, null, initialValues);
		} else {
			mDb.delete(DATABASE_TABLE_BLOCKEDNUMBERS, KEY_NUMBER + "=?",
					new String[] { number });
		}
	}

	/**
	 * Toggles the specified contact's specified preference.
	 * 
	 * @param contact_id
	 *            the contact_id
	 * @param preferenceId
	 *            the id of the preference
	 * @return the value of setContactPermission()
	 * @throws SQLException
	 *             a SQL exception
	 */
	public int toggleContactPermission(long contact_id, long preferenceId)
			throws SQLException {
		return setContactPermission(contact_id, preferenceId,
				!getContactPermission(contact_id, preferenceId));
	}

	/**
	 * Sets the guard.
	 * 
	 * @param house_id
	 *            the id of the house to change
	 * @param guard_id
	 *            the id of the guard to assign
	 * @param which
	 *            one of SUNDAY_GUARD, SUNDAY_GUARD_DEFAULT, etc. from DbAdapter
	 * @throws SQLException
	 *             the SQL exception
	 */
	public void setGuard(long house_id, long guard_id, String which)
			throws SQLException {
		ContentValues args = new ContentValues();
		args.put(which, guard_id);
		mDb.update(DATABASE_TABLE_HOUSES, args, KEY_ROWID + "= ?",
				new String[] { String.valueOf(house_id) });
	}

	/**
	 * Returns the ID of the guard given the house_id and the 'which' parameter.
	 * 
	 * @param house_id
	 *            the house_id
	 * @param which
	 *            the which
	 * @return the guard
	 * @throws SQLException
	 *             the sQL exception
	 */
	public long getGuard(long house_id, String which) throws SQLException {
		Cursor c = mDb.rawQuery(
				"select " + which + " from houses where _id=?;",
				new String[] { String.valueOf(house_id) });
		if (c.moveToFirst()) {
			return c.getLong(0);
		} else {
			return -1;
		}
	}

	/**
	 * Gets the scheduled guard's phone number for the day of the week of the
	 * given date.
	 * 
	 * @param house_id
	 *            the house_id
	 * @param date
	 *            the date
	 * @return the guard number from date
	 */
	public String getGuardNumberFromDate(long house_id, String date) {
		String dayOfWeek = Time.dayOfWeek(date).toLowerCase();
		long guardId = getGuard(house_id, dayOfWeek + "_guard");
		return getGuardNumber(guardId);
	}

	/**
	 * Gets the guard checkin time.
	 * 
	 * @param guard_id
	 *            the guard_id
	 * @return the guard checkin time
	 */
	public String getGuardCheckinTime(long guard_id) {
		// this query should select the most recent checkin time for the guard
		Cursor c = mDb
				.rawQuery(
						"select time from guardcheckins where guard_id=? order by time desc limit 1;",
						new String[] { String.valueOf(guard_id) });
		if (c.moveToFirst()) {
			return c.getString(0);
		} else {
			return null;
		}
	}

	/**
	 * Sets the guard checkin resolved.
	 * 
	 * @param context
	 *            the context
	 * @param guard_id
	 *            the guard_id
	 * @return the int
	 */
	public int setGuardCheckinResolved(Context context, long guard_id) {
		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(mContext);
		int window = Long.valueOf(
				settings.getString(
						HomeActivity.PREFERENCES_GUARD_CHECKIN_WINDOW, "5"))
				.intValue();

		String checkinTime = getGuardCheckinTime(guard_id);
		if (checkinTime == null) {
			return NOTIFY_FAILURE;
		}

		ContentValues args = new ContentValues();
		args.put(KEY_RESPONSE, 1);

		if (mDb.update(
				DATABASE_TABLE_GUARD_CHECKINS,
				args,
				"guard_id=? and datetime('now','localtime') >= time and datetime('now','localtime') <= datetime(time,'+"
						+ window + " minutes')",
				new String[] { String.valueOf(guard_id) }) > 0) {
			return NOTIFY_SUCCESS;
		} else {
			return NOTIFY_FAILURE;
		}
	}

	/**
	 * Returns the id of the guard currently assigned to the specified house for
	 * today.
	 * 
	 * @param house_id
	 * @return
	 */
	public long getGuardForHouse(long house_id) {
		String todaysDayOfWeek = Time.dayOfWeek(new Date()).toLowerCase();

		Cursor c = mDb.rawQuery("select " + todaysDayOfWeek
				+ "_guard from houses where _id=?;",
				new String[] { String.valueOf(house_id) });
		if (c.moveToFirst()) {
			return c.getLong(0);
		} else {
			return -1;
		}
	}

	/**
	 * Resets the guards' schedules for the day to whatever is set in the
	 * typical fields.
	 */
	public void resetGuardSchedule() {
		mDb.execSQL("sunday_guard=typical_sunday_guard,monday_guard=typical_monday_guard,tuesday_guard=typical_tuesday_guard,wednesday_guard=typical_wednesday_guard,thursday_guard=typical_thursday_guard,friday_guard=typical_friday_guard,saturday_guard=typical_saturday_guard;");
	}
}