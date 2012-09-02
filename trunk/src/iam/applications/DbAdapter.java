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

			onCreate(db);
		}
	}

	public static int USER_PREFERENCE_CHECKIN_REMINDER = 1;

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

	/** The version of the current database. */
	private static final int DATABASE_VERSION = 11;

	/** Create Table Commands. */
	private static final String DATABASE_CREATE_LOCATIONS = "create table if not exists locations (_id integer primary key autoincrement, label text not null, allowed integer default 0);";
	private static final String DATABASE_CREATE_CHECKINS = "create table if not exists checkins (_id integer primary key autoincrement, contact_id integer not null, location string not null, timedue string not null, timereceived string, outstanding integer default 1, checkinrequest integer default 1);";
	private static final String DATABASE_CREATE_CALLAROUNDS = "create table if not exists callarounds (_id integer primary key autoincrement, house_id integer not null, duefrom string not null, dueby string not null, timereceived string, outstanding integer default 1, unique(house_id,dueby) on conflict ignore );";
	private static final String DATABASE_CREATE_CONTACTS = "create table contacts ( _id integer primary key autoincrement , name text not null , preferences int default 0 , permissions int default 0 )";
	private static final String DATABASE_CREATE_CONTACTPHONES = "create table contactphones ( _id integer primary key autoincrement , contact_id integer not null, number text not null, integer precedence default 1 )";
	private static final String DATABASE_CREATE_CONTACTEMAILS = "create table contactemails ( _id integer primary key autoincrement , contact_id integer not null, email text not null, integer precedence default 1 )";
	private static final String DATABASE_CREATE_HOUSES = "create table houses ( _id integer primary key autoincrement , name text not null, active int default 1 )";
	private static final String DATABASE_CREATE_HOUSEMEMBERS = "create table housemembers ( _id integer primary key autoincrement , house_id integer not null, contact_id integer not null, unique(contact_id) on conflict ignore )";
	private static final String DATABASE_CREATE_BLOCKEDNUMBERS = "create table blockednumbers ( _id integer primary key autoincrement , number text not null )";
	private static final String DATABASE_CREATE_LOG = "create table log ( _id integer primary key autoincrement , type text default 'Normal' , message text not null, time text )";
	private static final String DATABASE_CREATE_LOCATION_LOG = "create table locationlog ( _id integer primary key autoincrement , contact_id integer not null, lat real not null, lon real not null, time text )";

	/** Drop Table Commands */
	private static final String DROP_TABLE_LOCATIONS = "DROP TABLE IF EXISTS locations;";
	private static final String DROP_TABLE_CHECKINS = "DROP TABLE IF EXISTS checkins;";
	private static final String DROP_TABLE_CALLAROUNDS = "DROP TABLE IF EXISTS callarounds;";
	private static final String DROP_TABLE_CONTACTS = "DROP TABLE IF EXISTS contacts;";
	private static final String DROP_TABLE_CONTACTPHONES = "DROP TABLE IF EXISTS contactphones;";
	private static final String DROP_TABLE_CONTACTEMAILS = "DROP TABLE IF EXISTS contactemails;";
	private static final String DROP_TABLE_HOUSES = "DROP TABLE IF EXISTS houses;";
	private static final String DROP_TABLE_HOUSEMEMBERS = "DROP TABLE IF EXISTS housemembers;";
	private static final String DROP_TABLE_BLOCKEDNUMBERS = "DROP TABLE IF EXISTS blockednumbers;";
	private static final String DROP_TABLE_LOG = "DROP TABLE IF EXISTS log;";
	private static final String DROP_TABLE_LOCATION_LOG = "DROP TABLE IF EXISTS locationlog;";

	/** The Constant DATABASE_NAME. */
	private static final String DATABASE_NAME = "thedatabase";

	/** Database table names. */
	private static final String DATABASE_TABLE_LOCATIONS = "locations";
	private static final String DATABASE_TABLE_CHECKINS = "checkins";
	private static final String DATABASE_TABLE_CALLAROUNDS = "callarounds";
	private static final String DATABASE_TABLE_CONTACTS = "contacts";
	private static final String DATABASE_TABLE_CONTACTPHONES = "contactphones";
	private static final String DATABASE_TABLE_CONTACTEMAILS = "contactemails";
	private static final String DATABASE_TABLE_HOUSES = "houses";
	private static final String DATABASE_TABLE_HOUSEMEMBERS = "housemembers";
	private static final String DATABASE_TABLE_BLOCKEDNUMBERS = "blockednumbers";
	private static final String DATABASE_TABLE_LOG = "log";
	private static final String DATABASE_TABLE_LOCATION_LOG = "locationlog";

	/** SQL column names constants. */
	public static final String KEY_ROWID = "_id";
	public static final String KEY_LABEL = "label";
	public static final String KEY_ALLOWED = "allowed";
	public static final String KEY_CONTACTID = "contact_id";
	public static final String KEY_LOCATION = "location";
	public static final String KEY_TIMEDUE = "timedue";
	public static final String KEY_TIMERECEIVED = "timereceived";
	public static final String KEY_OUTSTANDING = "outstanding";
	public static final String KEY_RESOLVED = "resolved";
	public static final String KEY_HOUSEID = "house_id";
	public static final String KEY_DUEBY = "dueby";
	public static final String KEY_DUEFROM = "duefrom";
	public static final String KEY_CHECKINREQUEST = "checkinrequest";
	public static final String KEY_ACTIVE = "active";
	public static final String KEY_SUMMARY = "summary";
	public static final String KEY_NAME = "name";
	public static final String KEY_NUMBER = "number";
	public static final String KEY_EMAIL = "email";
	public static final String KEY_COUNT = "count";
	public static final String KEY_PREFERENCES = "preferences";
	public static final String KEY_PERMISSIONS = "permissions";
	public static final String KEY_TYPE = "type";
	public static final String KEY_TIME = "time";
	public static final String KEY_MESSAGE = "message";
	public static final String KEY_LAT = "lat";
	public static final String KEY_LON = "lon";

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

	/**
	 * Instantiates a new db adapter.
	 * 
	 * @param ctx
	 *            the context
	 */
	public DbAdapter(Context ctx) {
		mContext = ctx;
	}

	public void addCallaroundForToday(long house_id) throws SQLException {
		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(mContext);
		String settings_dueby = settings.getString(
				HomeActivity.PREFERENCES_CALLAROUND_DUE_BY, "21:00");
		Date dueby_date = Time.timeFromSimpleTime(settings_dueby);
		Date today_dueby = new Date();
		today_dueby.setHours(dueby_date.getHours());
		today_dueby.setMinutes(dueby_date.getMinutes());

		String settings_duefrom = settings.getString(
				HomeActivity.PREFERENCES_CALLAROUND_DUE_FROM, "00:00");
		Date duefrom_date = Time.timeFromSimpleTime(settings_duefrom);

		Date today_duefrom = new Date();
		today_duefrom.setHours(duefrom_date.getHours());
		today_duefrom.setMinutes(duefrom_date.getMinutes());

		mDb.execSQL("insert or ignore into callarounds (house_id , dueby, duefrom) values ('"
				+ String.valueOf(house_id)
				+ "','"
				+ Time.iso8601DateTime(today_dueby)
				+ "','"
				+ Time.iso8601DateTime(today_duefrom) + "');");
	}

	/**
	 * Adds call arounds to <code>Callarounds</code> for today.
	 * 
	 * @param duefrom
	 *            the date from which the callaround can be satisfied
	 * @throws SQLException
	 *             a SQL exception
	 */
	public void addCallarounds() throws SQLException {
		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(mContext);
		String settings_dueby = settings.getString(
				HomeActivity.PREFERENCES_CALLAROUND_DUE_BY, "21:00");
		Date dueby_date = Time.timeFromSimpleTime(settings_dueby);
		Date today_dueby = new Date();
		today_dueby.setHours(dueby_date.getHours());
		today_dueby.setMinutes(dueby_date.getMinutes());

		String settings_duefrom = settings.getString(
				HomeActivity.PREFERENCES_CALLAROUND_DUE_FROM, "00:00");
		Date duefrom_date = Time.timeFromSimpleTime(settings_duefrom);

		Date today_duefrom = new Date();
		today_duefrom.setHours(duefrom_date.getHours());
		today_duefrom.setMinutes(duefrom_date.getMinutes());

		AlarmReceiver.setCallaroundDueAlarm(mContext, today_dueby);

		mDb.execSQL("insert or ignore into callarounds (house_id , dueby, duefrom) select _id,'"
				+ Time.iso8601DateTime(today_dueby)
				+ "','"
				+ Time.iso8601DateTime(today_duefrom)
				+ "' from houses where active='1';");
	}

	/**
	 * Adds a check-in to the database
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
	public int addCheckin(long contact_id, String place, Date time,
			boolean requestCheckin) throws SQLException {
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
		initialValues.put(KEY_TIMEDUE, Time.iso8601DateTime(time));
		initialValues.put(KEY_TIMERECEIVED, Time.iso8601DateTime());
		initialValues.put(KEY_CHECKINREQUEST, requestCheckin ? 1 : 0);
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
	 * Adds a location to the database
	 * 
	 * @param label
	 *            the label of the new location
	 * @param allowed
	 *            whether the location is permitted for travel
	 * @return the _id of the inserted row
	 * @throws SQLException
	 *             a SQL exception
	 */
	public long addLocation(String label, boolean allowed) throws SQLException {
		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_LABEL, label);
		initialValues.put(KEY_ALLOWED, allowed ? 1 : 0);
		return mDb.insert(DATABASE_TABLE_LOCATIONS, null, initialValues);
	}

	/**
	 * Adds a record to the <code>locationlog</code> table
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
	 * Closes the database
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
	}

	/**
	 * Delete blocked number (= unblock the number)
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
		String weekago = oneWeekAgo();
		mDb.execSQL("delete from callarounds where strftime('%s',dueby) <= strftime('%s','"
				+ weekago + "');");
		mDb.execSQL("delete from checkins where strftime('%s',timereceived) <= strftime('%s','"
				+ weekago + "');");
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
		Log.i("Debug",
				"delete from log where strftime('%s',time) <= strftime('%s','"
						+ oneWeekAgo() + "');");
		mDb.execSQL("delete from log where strftime('%s',time) <= strftime('%s','"
				+ oneWeekAgo() + "');");
	}

	/**
	 * Return a cursor with a list of all check-ins. Columns: KEY_ROWID,
	 * KEY_LOCATION, KEY_TIMEDUE, KEY_NAME, KEY_OUTSTANDING
	 * 
	 * @return the cursor
	 * @throws SQLException
	 *             a SQL exception
	 */
	public Cursor fetchAllCheckins() throws SQLException {
		return mDb
				.rawQuery(
						"select checkins._id,location,timedue,name,outstanding from checkins,contacts where checkins.contact_id=contacts._id order by outstanding desc,timedue desc;",
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
	 * KEY_ALLOWED
	 * 
	 * @return the cursor
	 * @throws SQLException
	 *             a SQL exception
	 */
	public Cursor fetchAllLocations() throws SQLException {
		return mDb.query(DATABASE_TABLE_LOCATIONS, new String[] { KEY_ROWID,
				KEY_LABEL, KEY_ALLOWED }, null, null, null, null, KEY_LABEL);
	}

	/**
	 * Returns a cursor with the blocked numbers Columns: KEY_ROWID, KEY_NUMBER
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
	 * KEY_OUTSTANDING, KEY_DUEFROM, KEY_DUEBY
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
						"select callarounds._id as _id,name,timereceived,outstanding,duefrom,dueby from callarounds,houses where date(dueby)='"
								+ isoday
								+ "' and callarounds.house_id=houses._id order by outstanding asc;",
						null);
		// return mDb.query(DATABASE_TABLE_CALLAROUNDS, new String[] {
		// KEY_ROWID,
		// KEY_HOUSEID, KEY_OUTSTANDING, KEY_TIMERECEIVED }, KEY_FORDAY
		// + "='" + isoday + "'", null, null, null, KEY_OUTSTANDING
		// + " asc");
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
						"select contacts._id,contacts.name as name, houses.name as label from contacts left join housemembers on housemembers.contact_id=contacts._id left join houses on houses._id=housemembers.house_id  where contacts._id not in (select contact_id from checkins where outstanding='1');",
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
		// return mDb
		// .rawQuery(
		// "select contacts._id,contacts.name||' ('||houses.name||')' as name from contacts,houses,housemembers where contacts._id in (select contact_id from checkins where outstanding='1') and housemembers.contact_id=contacts._id and houses._id=housemembers.house_id;",
		// null);
		return mDb
				.rawQuery(
						"select contacts._id,contacts.name as name, houses.name as label from contacts left join housemembers on housemembers.contact_id=contacts._id left join houses on houses._id=housemembers.house_id  where contacts._id in (select contact_id from checkins where outstanding='1');",
						null);
	}

	/**
	 * Returns a cursor with names and phone numbers for a given house. Columns:
	 * KEY_ROWID, KEY_NAME, KEY_NUMBER
	 * 
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#finalize()
	 */
	@Override
	protected void finalize() throws Throwable {
		super.finalize();

		// this may have been causing problems....
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
	 * @param contact_id
	 *            the contact_id
	 * @return true if the check-in is outstanding, otherwise false.
	 * @throws SQLException
	 *             a SQL exception
	 */
	public boolean getCheckinOutstanding(long checkin_id) throws SQLException {
		Cursor c = mDb.query(DATABASE_TABLE_CHECKINS,
				new String[] { KEY_OUTSTANDING }, KEY_ROWID + "=?",
				new String[] { String.valueOf(checkin_id) }, null, null, null);
		c.moveToFirst();
		long r = c.getLong(0);
		c.close();
		return r == 1 ? true : false;
	}

	/**
	 * Returns a formatted string with the number of outstanding checkins.
	 * 
	 * @return the checkin summary
	 * @throws SQLException
	 *             a SQL exception
	 */
	public String getCheckinSummary() throws SQLException {
		Cursor c = mDb.rawQuery(
				"select count(_id) from checkins where outstanding='1';", null);
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
	 * Returns the specified contact's specified preference.
	 * 
	 * @param contact_id
	 *            the contact_id
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
	 * Returns a number given the _id from the contactphones table.
	 * 
	 * @param contactId
	 *            the contact id
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
	 * Returns true if the phone number is on the blocked list, otherwise false
	 * 
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
	 * Returns the number of call arounds that are due
	 * 
	 * @return the number of due call arounds
	 * @throws SQLException
	 *             a SQL exception
	 */
	public long getNumberOfDueCallarounds() throws SQLException {
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
	 * Returns the number of check-ins that are due
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
	 * Returns the ISO 8601 date/time string from one week before the present
	 * time.
	 * 
	 * @return the ISO 8601 date/time string from one week before the present
	 *         time
	 */
	private String oneWeekAgo() {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DATE, -7);
		return Time.iso8601DateTime(cal.getTime());
	}

	/**
	 * Opens the database
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
			addCallaroundForToday(house_id);
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
	 * Set whether an existing call around is resolved or not, for whatever call
	 * arounds can be resolved
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
	 * Set whether an existing call around is resolved or not, for whatever call
	 * arounds can be resolved
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
	public int setCallaroundResolvedFromId(long callaround_id, boolean resolved)
			throws SQLException {
		Log.i("Debug", "Setting resolved: " + (resolved ? "true" : "false"));

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
	 * Resolve a check-in, given its _id.
	 * 
	 * @param checkin_id
	 *            the checkin_id
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
	 * @param permissionId
	 *            the id of the permission being changed
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
	 * Set the name of a house
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
	 * Rename a location.
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
	 * Set the number as blocked or not
	 * 
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
}