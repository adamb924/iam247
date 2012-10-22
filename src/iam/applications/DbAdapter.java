package iam.applications;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

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
	 * Class containing column names of database tables
	 */
	@SuppressWarnings("javadoc")
	public static class Columns {
		public static final String ACTIVE = "active";
		public static final String ALLOWED = "allowed";
		public static final String CHECKINID = "checkin_id";
		public static final String CHECKINREQUEST = "checkinrequest";
		public static final String CONTACTID = "contact_id";
		public static final String COUNT = "count";
		public static final String DELAYED = "delayed";
		public static final String DELIVERED = "delivered";
		public static final String DUEBY = "dueby";
		public static final String DUEFROM = "duefrom";
		public static final String EMAIL = "email";
		public static final String GUARDID = "guard_id";
		public static final String HOUSEID = "house_id";
		public static final String KEYWORD = "keyword";
		public static final String LABEL = "label";
		public static final String LAT = "lat";
		public static final String LOCATION = "location";
		public static final String LON = "lon";
		public static final String MESSAGE = "message";
		public static final String NAME = "name";
		public static final String NUMBER = "number";
		public static final String OUTSTANDING = "outstanding";
		public static final String PERMISSIONS = "permissions";
		public static final String PREFERENCES = "preferences";
		public static final String REQUESTID = "request_id";
		public static final String RESOLVED = "resolved";
		public static final String RESPONSE = "response";
		public static final String ROWID = "_id";
		public static final String SENT = "sent";
		public static final String SUMMARY = "summary";
		public static final String TIME = "time";
		public static final String TIMEDUE = "timedue";
		public static final String TIMERECEIVED = "timereceived";
		public static final String TRIPID = "trip_id";
		public static final String TRIPRESOLVED = "tripresolved";
		public static final String TYPE = "type";
		public static final String WITH = "with";
	}

	private static final class CreateStatements {
		private static final String ALARMS = "create table alarms ( _id integer primary key autoincrement , request_id int not null , type text )";
		private static final String BLOCKEDNUMBERS = "create table blockednumbers ( _id integer primary key autoincrement , number text not null )";
		private static final String CALLAROUNDS = "create table if not exists callarounds (_id integer primary key autoincrement, house_id integer not null, duefrom string not null, dueby string not null, timereceived string, outstanding integer default 1, delayed integer default 0, unique(house_id,dueby) on conflict ignore );";
		private static final String CHECKINS = "create table if not exists checkins (_id integer primary key autoincrement, contact_id integer not null, location string not null, keyword string not null, timedue string not null, timereceived string, outstanding integer default 1, checkinrequest integer default 1);";
		private static final String CONTACTEMAILS = "create table contactemails ( _id integer primary key autoincrement , contact_id integer not null, email text not null, integer precedence default 1 )";
		private static final String CONTACTPHONES = "create table contactphones ( _id integer primary key autoincrement , contact_id integer not null, number text not null, integer precedence default 1 )";
		private static final String CONTACTS = "create table contacts ( _id integer primary key autoincrement , name text not null , preferences int default 0 , permissions int default 0 )";
		private static final String GUARD_CHECKINS = "create table guardcheckins ( _id integer primary key autoincrement , guard_id int not null , time text , response int default 0, unique(time) on conflict ignore )";
		private static final String GUARDS = "create table guards ( _id integer primary key autoincrement , name text not null , number text )";
		private static final String HOUSEMEMBERS = "create table housemembers ( _id integer primary key autoincrement , house_id integer not null, contact_id integer not null, unique(contact_id) on conflict ignore )";
		private static final String HOUSES = "create table houses ( _id integer primary key autoincrement , name text not null, active int default 1, sunday_guard int default -1, monday_guard int default -1, tuesday_guard int default -1, wednesday_guard int default -1, thursday_guard int default -1, friday_guard int default -1, saturday_guard int default -1 , typical_sunday_guard int default -1, typical_monday_guard int default -1, typical_tuesday_guard int default -1, typical_wednesday_guard int default -1, typical_thursday_guard int default -1, typical_friday_guard int default -1, typical_saturday_guard int default -1 )";
		private static final String LOCATION_LOG = "create table locationlog ( _id integer primary key autoincrement , contact_id integer not null, lat real not null, lon real not null, time text )";
		private static final String LOCATIONS = "create table if not exists locations (_id integer primary key autoincrement, label text not null, keyword text, allowed integer default 0);";
		private static final String LOG = "create table log ( _id integer primary key autoincrement , type text default 'Normal' , message text not null, time text )";
		private static final String PENDING = "create table pending ( _id integer primary key autoincrement , number text not null, message text not null, time text not null , sent int default 0, delivered int default 0 )";
		private static final String TRIP_MEMBERS = "create table if not exists tripmembers ( trip_id integer, checkin_id ) ";
		private static final String TRIPS = "create table if not exists trips ( _id integer primary key autoincrement, contact_id integer not null, with string, tripresolved integer default 0 ) ";
	}

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
		DatabaseHelper(final Context context) {
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
		public void onCreate(final SQLiteDatabase database) {
			database.execSQL(CreateStatements.LOCATIONS);
			database.execSQL(CreateStatements.CHECKINS);
			database.execSQL(CreateStatements.CALLAROUNDS);
			database.execSQL(CreateStatements.CONTACTS);
			database.execSQL(CreateStatements.CONTACTPHONES);
			database.execSQL(CreateStatements.CONTACTEMAILS);
			database.execSQL(CreateStatements.HOUSES);
			database.execSQL(CreateStatements.HOUSEMEMBERS);
			database.execSQL(CreateStatements.BLOCKEDNUMBERS);
			database.execSQL(CreateStatements.LOG);
			database.execSQL(CreateStatements.LOCATION_LOG);
			database.execSQL(CreateStatements.PENDING);
			database.execSQL(CreateStatements.GUARDS);
			database.execSQL(CreateStatements.GUARD_CHECKINS);
			database.execSQL(CreateStatements.ALARMS);
			database.execSQL(CreateStatements.TRIPS);
			database.execSQL(CreateStatements.TRIP_MEMBERS);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * android.database.sqlite.SQLiteOpenHelper#onUpgrade(android.database
		 * .sqlite.SQLiteDatabase, int, int)
		 */
		@Override
		public void onUpgrade(final SQLiteDatabase database,
				final int oldVersion, final int newVersion) {
			Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
					+ newVersion + ", which will destroy all old data");
			database.execSQL(DropStatements.LOCATIONS);
			database.execSQL(DropStatements.CHECKINS);
			database.execSQL(DropStatements.CALLAROUNDS);
			database.execSQL(DropStatements.CONTACTS);
			database.execSQL(DropStatements.CONTACTPHONES);
			database.execSQL(DropStatements.CONTACTEMAILS);
			database.execSQL(DropStatements.HOUSES);
			database.execSQL(DropStatements.HOUSEMEMBERS);
			database.execSQL(DropStatements.BLOCKEDNUMBERS);
			database.execSQL(DropStatements.LOG);
			database.execSQL(DropStatements.LOCATION_LOG);
			database.execSQL(DropStatements.PENDING);
			database.execSQL(DropStatements.GUARDS);
			database.execSQL(DropStatements.GUARD_CHECKIN);
			database.execSQL(DropStatements.ALARMS);
			database.execSQL(DropStatements.TRIPS);
			database.execSQL(DropStatements.TRIP_MEMBERS);

			onCreate(database);
		}
	}

	private static final class DropStatements {
		private static final String ALARMS = "DROP TABLE IF EXISTS alarms;";
		private static final String BLOCKEDNUMBERS = "DROP TABLE IF EXISTS blockednumbers;";
		private static final String CALLAROUNDS = "DROP TABLE IF EXISTS callarounds;";
		private static final String CHECKINS = "DROP TABLE IF EXISTS checkins;";
		private static final String CONTACTEMAILS = "DROP TABLE IF EXISTS contactemails;";
		private static final String CONTACTPHONES = "DROP TABLE IF EXISTS contactphones;";
		private static final String CONTACTS = "DROP TABLE IF EXISTS contacts;";
		private static final String GUARD_CHECKIN = "DROP TABLE IF EXISTS guardcheckins;";
		private static final String GUARDS = "DROP TABLE IF EXISTS guards;";
		private static final String HOUSEMEMBERS = "DROP TABLE IF EXISTS housemembers;";
		private static final String HOUSES = "DROP TABLE IF EXISTS houses;";
		private static final String LOCATION_LOG = "DROP TABLE IF EXISTS locationlog;";
		private static final String LOCATIONS = "DROP TABLE IF EXISTS locations;";
		private static final String LOG = "DROP TABLE IF EXISTS log;";
		private static final String PENDING = "DROP TABLE IF EXISTS pending;";
		private static final String TRIP_MEMBERS = "DROP TABLE IF EXISTS tripmembers;";
		private static final String TRIPS = "DROP TABLE IF EXISTS trips;";
	}

	/**
	 * Class containing log event types
	 * 
	 */
	public static final class LogTypes {

		/** Log message types. */
		public static final String SMS_ERROR = "SMS Error";

		/** Log message types. */
		public static final String SMS_NOTIFICATION = "SMS Event";

	}

	/**
	 * Class with static members indicating various notification values for
	 * return statements
	 * 
	 */
	public static final class Notifications {

		/**
		 * Return value to indicate that the requested action had already been
		 * completed.
		 */
		public static final int ALREADY = 3;

		/**
		 * Return value to indicate that an existing check-in was resolved when
		 * the new one was added.
		 */
		public static final int EXISTING_CHECKIN_RESOLVED = 1;

		/** Return value to indicate failure. */
		public static final int FAILURE = 0;

		/** Return value to indicate the user is associated with a house. */
		public static final int HASHOUSE = 5;

		/** Return value to indicate that call around is currently inactive. */
		public static final int INACTIVE = 4;

		/** Return value to indicate the user is not associated with a house. */
		public static final int NOHOUSE = 5;

		/** Return value to indicate success. */
		public static final int SUCCESS = 2;

		/** The notify untimely. */
		public static final int UNTIMELY = 5;

	}

	private static class Tables {
		private static final String ALARMS = "alarms";
		private static final String BLOCKEDNUMBERS = "blockednumbers";
		private static final String CALLAROUNDS = "callarounds";
		private static final String CHECKINS = "checkins";
		private static final String CONTACTEMAILS = "contactemails";
		private static final String CONTACTPHONES = "contactphones";
		private static final String CONTACTS = "contacts";
		private static final String GUARD_CHECKINS = "guardcheckins";
		private static final String GUARDS = "guards";
		private static final String HOUSEMEMBERS = "housemembers";
		private static final String HOUSES = "houses";
		private static final String LOCATION_LOG = "locationlog";
		private static final String LOCATIONS = "locations";
		private static final String LOG = "log";
		private static final String PENDING = "pending";
		private static final String TRIP_MEMBERS = "tripmembers";
		private static final String TRIPS = "trips";
	}

	/**
	 * Class with static members indicating various user permissions
	 * 
	 */
	public static final class UserPermissions {
		/** The user permission report. */
		public static final int REPORT = 1;
	}

	/**
	 * Class with static members indicating various user preferences
	 * 
	 */
	public static final class UserPreferences {
		/** The user preference checkin reminder. */
		public static final int CHECKIN_REMINDER = 1;
	}

	/** The Constant DATABASE_NAME. */
	private static final String DATABASE_NAME = "thedatabase";

	/** The version of the current database. */
	private static final int DATABASE_VERSION = 22;

	/**
	 * The days of the week in lowercase, to be used to build database column
	 * names.
	 */
	private final static String[] DAYS = { "sunday", "monday", "tuesday",
			"wednesday", "thursday", "friday", "saturday" };

	/** The Constant TAG. */
	private static final String TAG = "DbAdapter";

	/**
	 * Gets the column name for the guard schedule, for the given day.
	 * 
	 * @param forDay
	 *            the desired day (0-6)
	 * @param typicalColumn
	 *            true if the column name for the typical schedule should be
	 *            returned
	 * @return the column name
	 */
	static public String getGuardScheduleColumnName(final int forDay,
			final boolean typicalColumn) {
		return typicalColumn ? "typical_" + DbAdapter.DAYS[forDay] + "_guard"
				: DbAdapter.DAYS[forDay] + "_guard";
	}

	/** The application context. */
	private transient final Context mContext;

	/** The database. */
	private transient SQLiteDatabase mDb;

	/** The database helper. */
	private transient DatabaseHelper mDbHelper;

	/**
	 * Instantiates a new db adapter.
	 * 
	 * @param ctx
	 *            the context
	 */
	public DbAdapter(final Context ctx) {
		mContext = ctx;
	}

	/**
	 * Adds a record of an alarm to the database.
	 * 
	 * @param request_id
	 *            the request_id of the alarm
	 * @param type
	 *            the type of alarm (as defined in AlarmManager)
	 * @return the id of the newly inserted row
	 * @throws SQLException
	 *             the sQL exception
	 */
	public long addAlarm(final int request_id, final String type)
			throws SQLException {
		final ContentValues initialValues = new ContentValues();
		initialValues.put(Columns.REQUESTID, request_id);
		initialValues.put(Columns.TYPE, type);
		return mDb.insert(Tables.ALARMS, null, initialValues);
	}

	/**
	 * Adds call arounds to <code>Callarounds</code> for today.
	 * 
	 * @throws SQLException
	 *             a SQL exception
	 */
	public void addCallarounds() throws SQLException {
		// 10/13/2012: I forget right now why I had been calling
		// nextDateFromPreferenceString. But this function should always set the
		// alarms for the current day. If we start requesting the next time it's
		// 9:00pm, for instance, that can lead into tomorrow, and if we're
		// between the duefrom and the dueby, that will make the dueby *after*
		// the duefrom, which makes it impossible to do call around.
		final Date dueBy = Time.todayAtPreferenceTime(mContext,
				Preferences.CALLAROUND_DUE_BY, "21:00");
		final Date dueFrom = Time.todayAtPreferenceTime(mContext,
				Preferences.CALLAROUND_DUE_FROM, "17:00");
		final Date alarmTime = Time.todayAtPreferenceTime(mContext,
				Preferences.CALLAROUND_ALARM_TIME, "21:10");
		final Date delayed = Time.todayAtPreferenceTime(mContext,
				Preferences.CALLAROUND_DELAYED_TIME, "23:59");

		// (re)set daily alarms for when the call around is due, when the alarm
		// should sound, and when the delayed callaround time is
		AlarmAdapter.setDailyAlarm(mContext,
				AlarmAdapter.Alerts.CALLAROUND_DUE, dueBy);
		AlarmAdapter.setDailyAlarm(mContext,
				AlarmAdapter.Alerts.CALLAROUND_ALARM, alarmTime);
		AlarmAdapter.setDailyAlarm(mContext,
				AlarmAdapter.Alerts.DELAYED_CALLAROUND_DUE, delayed);

		updateTimesOfResolvedDelayedCallarounds(dueBy, dueFrom);

		deleteTodaysUnresolvedUndelayedCallarounds();

		mDb.execSQL("insert or ignore into callarounds (house_id , dueby, duefrom) select _id,'"
				+ Time.iso8601DateTime(dueBy)
				+ "','"
				+ Time.iso8601DateTime(dueFrom)
				+ "' from houses where active='1';");
	}

	/**
	 * Adds a check-in to the database. If the contact has no unresolved trip, a
	 * new one is created.
	 * 
	 * @param contact_id
	 *            the contact_id of the person check-in in
	 * @param place
	 *            the place the to where the person is going
	 * @param keyword
	 *            the keyword
	 * @param time
	 *            the time by which the person will return
	 * @param with
	 *            the with
	 * @return Possible return values: Notifications.NOTIFY_SUCCESS,
	 *         Notifications.NOTIFY_FAILURE,
	 *         Notifications.NOTIFY_EXISTING_CHECKIN_RESOLVED
	 * @throws SQLException
	 *             a SQL exception
	 */
	public int addCheckin(final long contact_id, final String place,
			final String keyword, final Date time, final String with)
			throws SQLException {
		final int count = resolveExistingCheckins(contact_id);

		long tripId = getContactUnresolvedTrip(contact_id);
		if (tripId == -1) {
			tripId = addTrip(contact_id, with);
		}

		final ContentValues initialValues = new ContentValues();
		initialValues.put(Columns.CONTACTID, contact_id);
		initialValues.put(Columns.LOCATION, place);
		initialValues.put(Columns.KEYWORD, keyword);
		initialValues.put(Columns.TIMEDUE, Time.iso8601DateTime(time));
		initialValues.put(Columns.TIMERECEIVED, Time.iso8601DateTime());
		final long rowId = mDb.insert(Tables.CHECKINS, null, initialValues);

		final ContentValues memberValues = new ContentValues();
		memberValues.put(Columns.TRIPID, tripId);
		memberValues.put(Columns.CHECKINID, rowId);
		mDb.insert(Tables.TRIP_MEMBERS, null, memberValues);

		int retVal;
		if (rowId > -1) {
			AlarmAdapter.setCheckinAlert(mContext, time);
			if (count > 0) {
				retVal = Notifications.EXISTING_CHECKIN_RESOLVED;
			} else {
				retVal = Notifications.SUCCESS;
			}
		} else {
			retVal = Notifications.FAILURE;
		}
		return retVal;
	}

	/**
	 * Adds a contact to the database.
	 * 
	 * @param name
	 *            the name of the person
	 * @param number
	 *            the person's phone number
	 * @return the row id of the added contact, or -1 if the operation fails
	 * @throws SQLException
	 *             a SQL exception
	 */
	public long addContact(final String name, final String number)
			throws SQLException {
		final ContentValues initialValues = new ContentValues();
		initialValues.put(Columns.NAME, name);
		final long newId = mDb.insert(Tables.CONTACTS, null, initialValues);

		final int lastId = lastInsertId();
		if (lastId != -1) {
			final ContentValues initialValues2 = new ContentValues();
			initialValues2.put(Columns.CONTACTID, lastId);
			initialValues2.put(Columns.NUMBER, number);
			mDb.insert(Tables.CONTACTPHONES, null, initialValues2);
		}
		return newId;
	}

	/**
	 * Adds a guard to the database.
	 * 
	 * @param name
	 *            the name of the guard
	 * @throws SQLException
	 *             a SQL exception
	 */
	public void addGuard(final String name) throws SQLException {
		final ContentValues initialValues = new ContentValues();
		initialValues.put(Columns.NAME, name);
		mDb.insert(Tables.GUARDS, null, initialValues);
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
	public void addGuardCheckin(final long guard_id, final String time)
			throws SQLException {
		final ContentValues initialValues = new ContentValues();
		initialValues.put(Columns.GUARDID, guard_id);
		initialValues.put(Columns.TIME, time);
		mDb.insert(Tables.GUARD_CHECKINS, null, initialValues);
	}

	/**
	 * Adds a house to the database.
	 * 
	 * @param name
	 *            the name of the house
	 * @throws SQLException
	 *             a SQL exception
	 */
	public void addHouse(final String name) throws SQLException {
		final ContentValues initialValues = new ContentValues();
		initialValues.put(Columns.NAME, name);
		mDb.insert(Tables.HOUSES, null, initialValues);
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
	public long addLocation(final String label, final boolean allowed,
			final String keyword) throws SQLException {
		final ContentValues initialValues = new ContentValues();
		initialValues.put(Columns.LABEL, label);
		initialValues.put(Columns.ALLOWED, allowed ? 1 : 0);
		initialValues.put(Columns.KEYWORD, keyword);
		return mDb.insert(Tables.LOCATIONS, null, initialValues);
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
	public long addLocationLog(final long contact_id, final double lat,
			final double lon, final String time) throws SQLException {
		final ContentValues initialValues = new ContentValues();
		initialValues.put(Columns.CONTACTID, contact_id);
		initialValues.put(Columns.LAT, lat);
		initialValues.put(Columns.LON, lon);
		initialValues.put(Columns.TIME, time);
		return mDb.insert(Tables.LOCATION_LOG, null, initialValues);
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
	public boolean addLogEvent(final String type, final String message) {
		final ContentValues initialValues = new ContentValues();
		initialValues.put(Columns.TYPE, type);
		initialValues.put(Columns.MESSAGE, message);
		initialValues.put(Columns.TIME, Time.iso8601DateTime());
		return mDb.insert(Tables.LOG, null, initialValues) > -1;
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
	public void addMessagePending(final String number, final String message)
			throws SQLException {
		final ContentValues initialValues = new ContentValues();
		initialValues.put(Columns.NUMBER, number);
		initialValues.put(Columns.MESSAGE, message);
		initialValues.put(Columns.TIME, Time.iso8601DateTime());
		mDb.insert(Tables.PENDING, null, initialValues);
	}

	/**
	 * Adds call arounds at specified times, for the range of dates given, for
	 * the given house_id.
	 * 
	 * @param house_id
	 *            the house_id
	 * @param from
	 *            the date at which to start adding call arounds (inclusive)
	 * @param until
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
	public void addTravelCallarounds(final long house_id, final Calendar from,
			final Calendar until, final String firstTime,
			final String firstTimeEarliest, final String secondTime,
			final String secondTimeEarliest) throws SQLException {

		while (from.before(until) || from.equals(until)) {
			final String first = Time.iso8601Date(from.getTime()) + " "
					+ firstTime;
			final String firstEarliest = Time.iso8601Date(from.getTime()) + " "
					+ firstTimeEarliest;

			AlarmAdapter.setDailyAlarm(mContext,
					AlarmAdapter.Alerts.CALLAROUND_DUE,
					Time.iso8601DateTime(first));

			mDb.execSQL("insert or ignore into callarounds (house_id , dueby, duefrom) values ('"
					+ house_id + "','" + first + "','" + firstEarliest + "');");

			if (secondTime != null) {
				final String second = Time.iso8601Date(from.getTime()) + " "
						+ secondTime;
				final String secondEarliest = Time.iso8601Date(from.getTime())
						+ " " + secondTimeEarliest;

				AlarmAdapter.setDailyAlarm(mContext,
						AlarmAdapter.Alerts.CALLAROUND_DUE,
						Time.iso8601DateTime(second));

				mDb.execSQL("insert or ignore into callarounds (house_id , dueby, duefrom) values ('"
						+ house_id
						+ "','"
						+ second
						+ "','"
						+ secondEarliest
						+ "');");
			}
			from.add(Calendar.DAY_OF_MONTH, 1);
		}
	}

	/**
	 * Add a trip to the database.
	 * 
	 * @param contact_id
	 *            the contact id of the lead person
	 * @param with
	 *            a string indicating who is with that person
	 * @return the id of the newly created trip
	 */
	public long addTrip(final long contact_id, final String with) {
		final ContentValues initialValues = new ContentValues();
		initialValues.put(Columns.CONTACTID, contact_id);
		if (!with.isEmpty()) {
			initialValues.put(Columns.WITH, with);
		}
		return mDb.insert(Tables.TRIPS, null, initialValues);
	}

	/**
	 * Returns the result of the SQLite changes() function.
	 * 
	 * @return the the result of the SQLite changes() function
	 * @throws SQLException
	 *             a SQL exception
	 */
	private int changes() throws SQLException {
		final Cursor cur = mDb.rawQuery("select changes();", null);
		return cur.moveToFirst() ? cur.getInt(0) : 0;
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
	 * Deletes a record of an alarm from the database.
	 * 
	 * @param request_id
	 *            The request id of the alarm
	 * @return the number of rows deleted
	 * @throws SQLException
	 */
	public long deleteAlarm(final int request_id) throws SQLException {
		return mDb.delete(Tables.ALARMS, Columns.REQUESTID + "=?",
				new String[] { String.valueOf(request_id) });
	}

	/**
	 * Deletes alarms of the specified type from the database.
	 * 
	 * @param type
	 *            the type of alarm to delete
	 * @return the number of rows deleted
	 * @throws SQLException
	 */
	public long deleteAlarmsByType(final String type) throws SQLException {
		return mDb.delete(Tables.ALARMS, Columns.TYPE + "=?",
				new String[] { type });
	}

	/**
	 * Delete all information from database.
	 * 
	 * @throws SQLException
	 *             a SQL exception
	 */
	public void deleteAll() throws SQLException {
		mDb.delete(Tables.CHECKINS, null, null);
		mDb.delete(Tables.CALLAROUNDS, null, null);
		mDb.delete(Tables.CONTACTS, null, null);
		mDb.delete(Tables.CONTACTPHONES, null, null);
		mDb.delete(Tables.CONTACTEMAILS, null, null);
		mDb.delete(Tables.HOUSES, null, null);
		mDb.delete(Tables.HOUSEMEMBERS, null, null);
		mDb.delete(Tables.BLOCKEDNUMBERS, null, null);
		mDb.delete(Tables.LOG, null, null);
		mDb.delete(Tables.LOCATION_LOG, null, null);
		mDb.delete(Tables.PENDING, null, null);
		mDb.delete(Tables.GUARDS, null, null);
		mDb.delete(Tables.GUARD_CHECKINS, null, null);
		mDb.delete(Tables.ALARMS, null, null);
		mDb.delete(Tables.TRIPS, null, null);
		mDb.delete(Tables.TRIP_MEMBERS, null, null);
	}

	/**
	 * Delete all alarms from the database.
	 * 
	 * @return the number of rows deleted
	 * @throws SQLException
	 *             the SQL exception
	 */
	public long deleteAllAlarms() throws SQLException {
		return mDb.delete(Tables.ALARMS, null, null);
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
	public long deleteCallaround(final long rowId) throws SQLException {
		return mDb.delete(Tables.CALLAROUNDS, Columns.ROWID + "=?",
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
	public void deleteContact(final long contact_id) throws SQLException {
		mDb.delete(Tables.CONTACTS, Columns.ROWID + "=?",
				new String[] { String.valueOf(contact_id) });
		mDb.delete(Tables.CHECKINS, Columns.CONTACTID + "=?",
				new String[] { String.valueOf(contact_id) });
		mDb.delete(Tables.HOUSEMEMBERS, Columns.CONTACTID + "=?",
				new String[] { String.valueOf(contact_id) });
		mDb.delete(Tables.CONTACTPHONES, Columns.CONTACTID + "=?",
				new String[] { String.valueOf(contact_id) });
		mDb.delete(Tables.CONTACTEMAILS, Columns.CONTACTID + "=?",
				new String[] { String.valueOf(contact_id) });
		mDb.execSQL("delete from tripmembers where trip_id in (select _id from trips where contact_id='"
				+ contact_id + "');");
		mDb.delete(Tables.TRIPS, Columns.CONTACTID + "=?",
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
	 * Delete a guard from the database.
	 * 
	 * @param rowId
	 *            the row id
	 * @return the number of rows deleted.
	 * @throws SQLException
	 *             a SQL exception
	 */
	public long deleteGuard(final long rowId) throws SQLException {
		final ContentValues args = new ContentValues();
		for (int i = 0; i < 7; i++) {
			args.clear();

			final String typicalColumn = DbAdapter.getGuardScheduleColumnName(
					i, true);
			args.put(typicalColumn, -1);
			mDb.update(CreateStatements.HOUSES, args, typicalColumn + "=?",
					new String[] { String.valueOf(rowId) });

			args.clear();

			final String otherColumn = DbAdapter.getGuardScheduleColumnName(i,
					false);
			args.put(otherColumn, -1);
			mDb.update(CreateStatements.HOUSES, args, otherColumn + "=?",
					new String[] { String.valueOf(rowId) });

		}

		return mDb.delete(Tables.GUARDS, Columns.ROWID + "=" + rowId, null);
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
	public long deleteHouse(final long rowId) throws SQLException {
		mDb.delete(Tables.CALLAROUNDS, Columns.HOUSEID + "=?",
				new String[] { String.valueOf(rowId) });
		mDb.delete(Tables.HOUSEMEMBERS, Columns.HOUSEID + "=?",
				new String[] { String.valueOf(rowId) });
		return mDb.delete(Tables.HOUSES, Columns.ROWID + "=?",
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
	public long deleteLocation(final long rowId) throws SQLException {
		return mDb.delete(Tables.LOCATIONS, Columns.ROWID + "=" + rowId, null);
	}

	/**
	 * Deletes log events from before one week ago.
	 */
	public void deleteLogBeforeOneWeek() {
		mDb.execSQL("delete from log where time <= datetime('now','localtime','-7 days');");
	}

	/**
	 * Delete unresolved and unresolved call arounds that match today's date.
	 */
	private void deleteTodaysUnresolvedUndelayedCallarounds() {
		mDb.delete(
				Tables.CALLAROUNDS,
				"date(dueby) = date('now','localtime') and outstanding='1' and delayed = '0';",
				null);
	}

	/**
	 * Delete all unsent and undelivered messages from the database.
	 * 
	 * @throws SQLException
	 *             the sQL exception
	 */
	public void deleteUnsentUndelivered() throws SQLException {
		mDb.delete(Tables.PENDING, null, null);
	}

	/**
	 * Fetch contacts' numbers who are associated with houses receiving an
	 * active call around. Columns: Columns.NUMBER
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
	 * Return a cursor with a list of alarms of the specified type. Columns:
	 * Columns.REQUESTID
	 * 
	 * @param type
	 *            The type of alarm to return (defined in AlarmManager)
	 * @return the cursor
	 * @throws SQLException
	 */
	public Cursor fetchAlarmsForType(final String type) throws SQLException {
		return mDb.rawQuery("select request_id from alarms where type=?;",
				new String[] { type });
	}

	/**
	 * Return a cursor with a list of all check-ins. Columns: Columns.ROWID,
	 * Columns.LOCATION, Columns.KEYWORD, Columns.TIMEDUE, Columns.NAME,
	 * Columns.OUTSTANDING, Columns.WITH, Columns.TRIPRESOLVED
	 * 
	 * @return the cursor
	 * @throws SQLException
	 *             a SQL exception
	 */
	public Cursor fetchAllCheckins() throws SQLException {
		return mDb
				.rawQuery(
						"select checkins._id,location,keyword,timedue,name,outstanding,with,tripresolved from checkins,contacts,trips,tripmembers where checkins.contact_id=contacts._id and checkins._id=tripmembers.checkin_id and trips._id=tripmembers.trip_id order by name,tripresolved asc,outstanding desc,timedue desc;",
						null);
	}

	/**
	 * Fetch all contacts' numbers. Columns: Columns.NUMBER
	 * 
	 * @return the cursor
	 * @throws SQLException
	 *             a SQL exception
	 */
	public Cursor fetchAllContactNumbers() throws SQLException {
		return mDb.rawQuery("select number from contactphones;", null);
	}

	/**
	 * Return a cursor with all of the guards. Columns: Columns.ROWID,
	 * Columns.NAME, Columns.NUMBER
	 * 
	 * @return the cursor
	 * @throws SQLException
	 *             a SQL exception
	 */
	public Cursor fetchAllGuards() throws SQLException {
		return mDb.query(Tables.GUARDS, new String[] { Columns.ROWID,
				Columns.NAME, Columns.NUMBER }, null, null, null, null,
				Columns.NAME);
	}

	/**
	 * Return a cursor with all houses. Columns: Columns.ROWID, Columns.NAME,
	 * Columns.ACTIVE
	 * 
	 * @return the cursor
	 * @throws SQLException
	 *             a SQL exception
	 */
	public Cursor fetchAllHouses() throws SQLException {
		return mDb.query(Tables.HOUSES, new String[] { Columns.ROWID,
				Columns.NAME, Columns.ACTIVE }, null, null, null, null,
				Columns.NAME);
	}

	/**
	 * Return a cursor with all locations. Columns: Columns.ROWID,
	 * Columns.LABEL, Columns.ALLOWED, Columns.KEYWORD
	 * 
	 * @return the cursor
	 * @throws SQLException
	 *             a SQL exception
	 */
	public Cursor fetchAllLocations() throws SQLException {
		return mDb.query(Tables.LOCATIONS, new String[] { Columns.ROWID,
				Columns.LABEL, Columns.ALLOWED, Columns.KEYWORD }, null, null,
				null, null, Columns.LABEL);
	}

	/**
	 * Returns a cursor with the blocked numbers Columns: Columns.ROWID,
	 * Columns.NUMBER.
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
	 * Columns: Columns.ROWID,Columns.DUEBY,Columns.OUTSTANDING,Columns
	 * .RESOLVED
	 * 
	 * @param showFuture
	 *            the show future
	 * @return the cursor
	 * @throws SQLException
	 *             a SQL exception
	 */
	public Cursor fetchCallaroundReport(final boolean showFuture)
			throws SQLException {
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
	 * Returns a cursor with the call around report for one day. Columns.NAME is
	 * the name of the house. Columns.TIMERECEIVED is empty or null if the call
	 * around has not been resolved. Columns: Columns.ROWID, Columns.NAME,
	 * Columns.TIMERECEIVED, Columns.OUTSTANDING, Columns.DUEFROM,
	 * Columns.DUEBY, Columns.DELAYED
	 * 
	 * @param isoday
	 *            the requested day, in ISO 8601 format (2012-06-18)
	 * @return the cursor
	 * @throws SQLException
	 *             a SQL exception
	 */
	public Cursor fetchCallaroundReportForDay(final String isoday)
			throws SQLException {
		return mDb
				.rawQuery(
						"select callarounds._id as _id,name,timereceived,outstanding,duefrom,dueby,delayed from callarounds,houses where date(dueby)='"
								+ isoday
								+ "' and callarounds.house_id=houses._id order by outstanding asc;",
						null);
	}

	/**
	 * Returns a cursor with all checked in people. Columns.LABEL is the house
	 * name. Columns: Columns.ROWID, Columns.NAME, Columns.LABEL
	 * 
	 * @return the cursor
	 * @throws SQLException
	 *             a SQL exception
	 */
	public Cursor fetchCheckedInPeople() throws SQLException {
		return mDb
				.rawQuery(
						"select contacts._id,contacts.name as name, houses.name as label from contacts left join housemembers on housemembers.contact_id=contacts._id left join houses on houses._id=housemembers.house_id  where contacts._id not in (select contact_id from trips where tripresolved='0');",
						null);
	}

	/**
	 * Returns a cursor with all people not checked out. Columns.LABEL is the
	 * house name. Columns: Columns.ROWID, Columns.NAME, Columns.LABEL
	 * 
	 * @return the cursor
	 * @throws SQLException
	 *             a SQL exception
	 */
	public Cursor fetchCheckedOutPeople() throws SQLException {
		return mDb
				.rawQuery(
						"select contacts._id,contacts.name as name, houses.name as label from contacts left join housemembers on housemembers.contact_id=contacts._id left join houses on houses._id=housemembers.house_id  where contacts._id in (select contact_id from trips where tripresolved='0');",
						null);
	}

	/**
	 * Returns a cursor with names and phone numbers for a given house. Columns:
	 * Columns.ROWID, Columns.NAME, Columns.NUMBER
	 * 
	 * @param house_id
	 *            the house_id
	 * @return the cursor
	 * @throws SQLException
	 *             a SQL exception
	 */
	public Cursor fetchContactsForHouse(final long house_id)
			throws SQLException {
		return mDb
				.rawQuery(
						"select contactphones._id,name,number from contacts,housemembers,contactphones on contacts._id=contactphones.contact_id and contacts._id=housemembers.contact_id where house_id='"
								+ house_id + "';", null);
	}

	/**
	 * Get a report of a guard's check-ins that were due before the present
	 * time. Columns: Columns.ID, Columns.TIME, Columns.RESPONSE
	 * 
	 * @param guard_id
	 *            the guard_id
	 * @return a cursor with the report
	 * @throws SQLException
	 *             the sQL exception
	 */
	public Cursor fetchGuardCheckinReport(final long guard_id)
			throws SQLException {
		return mDb
				.rawQuery(
						"select _id,time,response from guardcheckins where guard_id=? order by time desc;",
						new String[] { String.valueOf(guard_id) });
	}

	/**
	 * Return a cursor with a list of all check-ins. Columns: Columns.ROWID,
	 * Columns.LOCATION, Columns.TIMEDUE, Columns.NAME, Columns.OUTSTANDING
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
	 * Return a cursor with a list of phone numbers associated with houses that
	 * have not done call around for today and are not delayed. Columns:
	 * Columns.NUMBER
	 * 
	 * @return the cursor
	 * @throws SQLException
	 */
	public Cursor fetchMissedCallaroundNumbers() throws SQLException {
		return mDb
				.rawQuery(
						"select number from callarounds left join housemembers,contactphones on housemembers.house_id=callarounds.house_id and housemembers.contact_id=contactphones.contact_id where outstanding='1' and delayed='0' and date(dueby)='"
								+ Time.iso8601Date() + "';", null);
	}

	/**
	 * Return a cursor with a list of all check-ins. Columns: Columns.ROWID,
	 * Columns.CONTACTID, Columns.TIMEDUE
	 * 
	 * @return the cursor
	 * @throws SQLException
	 *             a SQL exception
	 */
	public Cursor fetchUnresolvedCheckins() throws SQLException {
		return mDb
				.rawQuery(
						"select _id,contact_id,timedue from checkins where outstanding='1';",
						null);
	}

	/**
	 * Fetch unsent and undelivered messages. Columns Columns.ID,
	 * Columns.NUMBER, Columns.MESSAGE, Columns.TIME, Columns.SENT,
	 * Columns.DELIVERED
	 * 
	 * @return the cursor
	 */
	public Cursor fetchUnsentUndeliveredMessages() {
		return mDb
				.rawQuery(
						"select _id,number,message,time,sent,delivered from pending order by time desc;",
						null);
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
	public boolean getCallaroundActive(final long house_id) throws SQLException {
		final Cursor cur = mDb.query(Tables.HOUSES,
				new String[] { Columns.ROWID }, Columns.ROWID + "='" + house_id
						+ "' and " + Columns.ACTIVE + "='1'", null, null, null,
				null);
		final boolean ret = cur.moveToFirst();
		cur.close();
		return ret;
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
	public boolean getCallaroundOutstanding(final long house_id)
			throws SQLException {
		final Cursor cur = mDb.rawQuery(
				"select count(_id) as count from callarounds where house_id='"
						+ house_id + "' and outstanding='1';", null);

		boolean retVal;
		if (cur.moveToFirst()) {
			retVal = cur.getLong(0) > 0 ? true : false;
		} else {
			retVal = false;
		}
		return retVal;
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
	public boolean getCallaroundResolvedFromId(final long rowId)
			throws SQLException {
		final Cursor cur = mDb.query(Tables.CALLAROUNDS,
				new String[] { Columns.OUTSTANDING }, Columns.ROWID + "=?",
				new String[] { String.valueOf(rowId) }, null, null, null);
		boolean retVal;
		if (cur.moveToFirst()) {
			retVal = cur.getLong(0) > 0 ? false : true;
		} else {
			retVal = false;
		}
		return retVal;
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
	public String getCallaroundSummary(final Date date) throws SQLException {
		final Cursor cur = mDb
				.rawQuery(
						"select count(nullif(outstanding,0)) as outstanding,count(nullif(outstanding,1)) as resolved from callarounds where date(dueby)='"
								+ Time.iso8601Date(date) + "';", null);
		String retVal;
		if (cur.moveToFirst()) {
			final long outstanding = cur.getLong(0);
			final long resolved = cur.getLong(1);
			if (outstanding + resolved == 0) {
				retVal = mContext.getString(R.string.callaround_summary_none);
			} else if (outstanding == 0) {
				retVal = mContext.getString(R.string.callaround_summary_allin);
			} else {
				retVal = String.format(
						mContext.getString(R.string.callaround_summary),
						String.valueOf(resolved), String.valueOf(outstanding));
			}
		} else {
			retVal = "";
		}
		return retVal;
	}

	/**
	 * Returns true if a current call around would be eligible to be resolved at
	 * the current moment (whether it is active or not), otherwise false.
	 * 
	 * @param house_id
	 *            the house_id of the call around
	 * @return True if the call around is outstanding, otherwise false.
	 * @throws SQLException
	 *             a SQL exception
	 */
	public boolean getCallaroundTimely(final long house_id) throws SQLException {
		final String delayedDueTime = Time.iso8601DateTime(Time
				.nextDateFromPreferenceString(mContext,
						Preferences.CALLAROUND_DELAYED_TIME, "23:59"));
		final Cursor cur = mDb
				.rawQuery(
						"select _id from callarounds where house_id='"
								+ house_id
								+ "' and datetime('now','localtime') >= datetime(duefrom) and datetime('now','localtime') <= datetime('"
								+ delayedDueTime
								+ "') and date(dueby)=date('now','localtime');",
						null);
		return cur.moveToFirst();
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
	public boolean getCheckinOutstanding(final long checkin_id)
			throws SQLException {
		final Cursor cur = mDb.query(Tables.CHECKINS,
				new String[] { Columns.OUTSTANDING }, Columns.ROWID + "=?",
				new String[] { String.valueOf(checkin_id) }, null, null, null);
		boolean retVal;
		if (cur.moveToFirst()) {
			retVal = cur.getLong(0) == 1 ? true : false;
		} else {
			retVal = false;
		}
		return retVal;
	}

	/**
	 * Returns a formatted string with the number of outstanding checkins.
	 * Currently this is used in HomeActivity as an at-a-glance summary.
	 * 
	 * @return the checkin summary
	 * @throws SQLException
	 *             a SQL exception
	 */
	public String getCheckinSummary() throws SQLException {
		final Cursor cur = mDb
				.rawQuery(
						"select count(contact_id) from (select distinct contact_id from trips where tripresolved='0');",
						null);
		String retVal;
		if (cur.moveToFirst()) {
			final long outstanding = cur.getLong(0);
			if (outstanding == 0) {
				retVal = mContext.getString(R.string.checkin_summary_none);
			} else if (outstanding == 1) {
				retVal = mContext.getString(R.string.checkin_summary_singular);
			} else {
				retVal = String.format(
						mContext.getString(R.string.checkin_summary),
						String.valueOf(outstanding));
			}
		} else {
			retVal = "";
		}
		return retVal;
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
	public String getCheckinTime(final long checkin_id) throws SQLException {
		final Cursor cur = mDb.query(Tables.CHECKINS,
				new String[] { Columns.TIMEDUE }, Columns.ROWID + "=?",
				new String[] { String.valueOf(checkin_id) }, null, null, null);
		return cur.moveToFirst() ? cur.getString(0) : "";
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
	public String getContactEmail(final long contactId) throws SQLException {
		final Cursor cur = mDb.rawQuery(
				"select email from contactemails where contact_id=? limit 1;",
				new String[] { String.valueOf(contactId) });
		return cur.moveToFirst() ? cur.getString(0) : "";
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
	public boolean getContactHasCheckinOutstanding(final long contact_id)
			throws SQLException {
		final Cursor cur = mDb.query(Tables.CHECKINS,
				new String[] { Columns.ROWID }, Columns.CONTACTID + "='"
						+ contact_id + "' and " + Columns.OUTSTANDING + "='1'",
				null, null, null, null);
		final boolean ret = cur.moveToFirst();
		cur.close();
		return ret;
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
	public long getContactId(final String phoneNumber) throws SQLException {
		final Cursor cur = mDb.rawQuery(
				"select contact_id from contactphones where number=? limit 1;",
				new String[] { phoneNumber });
		return cur.moveToFirst() ? cur.getLong(0) : -1;
	}

	/**
	 * Returns the contact_id associated with a given checkin.
	 * 
	 * @param checkin_id
	 *            the checkin_id
	 * @return true if the check-in is outstanding, otherwise false.
	 * @throws SQLException
	 *             a SQL exception
	 */
	public long getContactIdForCheckin(final long checkin_id)
			throws SQLException {
		final Cursor cur = mDb.query(Tables.CHECKINS,
				new String[] { Columns.CONTACTID }, Columns.ROWID + "=?",
				new String[] { String.valueOf(checkin_id) }, null, null, null);
		return cur.moveToFirst() ? cur.getLong(0) : -1;
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
	public String getContactName(final long contactId) throws SQLException {
		final Cursor cur = mDb.rawQuery(
				"select name from contacts where _id=?;",
				new String[] { String.valueOf(contactId) });
		return cur.moveToFirst() ? cur.getString(0) : "";
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
	public String getContactNumber(final long contactId) throws SQLException {
		final Cursor cur = mDb.rawQuery(
				"select number from contactphones where contact_id=? limit 1;",
				new String[] { String.valueOf(contactId) });
		return cur.moveToFirst() ? cur.getString(0) : "";
	}

	/**
	 * Returns the specified contact's specified preference.
	 * 
	 * @param contact_id
	 *            the contact_id
	 * @param permissionId
	 *            the preference id
	 * @return the value of the preference
	 * @throws SQLException
	 *             a SQL exception
	 */
	public boolean getContactPermission(final long contact_id,
			final long permissionId) throws SQLException {
		final Cursor cur = mDb.query(Tables.CONTACTS,
				new String[] { Columns.PERMISSIONS }, Columns.ROWID + "=?",
				new String[] { String.valueOf(contact_id) }, null, null, null);
		cur.moveToFirst();
		final long result = cur.getLong(0);
		cur.close();
		return (permissionId & result) > 0;
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
	public boolean getContactPreference(final long contact_id,
			final long permissionId) throws SQLException {
		final Cursor cur = mDb.query(Tables.CONTACTS,
				new String[] { Columns.PREFERENCES }, Columns.ROWID + "=?",
				new String[] { String.valueOf(contact_id) }, null, null, null);
		cur.moveToFirst();
		final long result = cur.getLong(0);
		cur.close();
		return (permissionId & result) > 0;
	}

	/**
	 * Returns the id of an unresolved trip for a contact, or -1 if there is no
	 * unresolved trip.
	 * 
	 * @param contact_id
	 *            the id of the contact
	 * @return the id of the contact's unresolved trip, or -1 if there is no
	 *         unresolved trip
	 * @throws SQLException
	 */
	public long getContactUnresolvedTrip(final long contact_id)
			throws SQLException {
		final Cursor cur = mDb
				.query(Tables.TRIPS, new String[] { Columns.ROWID },
						Columns.CONTACTID + "='" + contact_id + "' and "
								+ Columns.TRIPRESOLVED + "='0'", null, null,
						null, null);
		return cur.moveToFirst() ? cur.getLong(0) : -1;
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
	public long getCurrentCheckinForContact(final long contact_id)
			throws SQLException {
		final Cursor cur = mDb.rawQuery(
				"select _id from checkins where contact_id='" + contact_id
						+ "' and outstanding='1';", null);
		return cur.moveToFirst() ? cur.getLong(0) : -1;
	}

	/**
	 * Returns the id of the guard currently assigned to the specified house for
	 * today.
	 * 
	 * @param house_id
	 *            the house_id
	 * @return the guard for house
	 */
	public long getCurrentGuardForHouse(final long house_id) {
		final Date checkinStartTime = Time.previousDateFromPreferenceString(
				mContext, Preferences.GUARD_CHECKIN_START, "22:00");

		final Calendar calendar = Calendar.getInstance();
		calendar.setTime(checkinStartTime);
		final int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);

		return getGuard(house_id, dayOfWeek);
	}

	/**
	 * Return a list of forbidden locations in a CSV string.
	 * 
	 * @return the string
	 * @throws SQLException
	 *             a SQL exception
	 */
	public String getForbiddenLocations() throws SQLException {
		final Cursor cursor = mDb.query(Tables.LOCATIONS, new String[] {
				Columns.ROWID, Columns.LABEL }, Columns.ALLOWED + "='0'", null,
				null, null, Columns.LABEL);
		String retVal;
		if (cursor.moveToFirst()) {
			final StringBuffer buffer = new StringBuffer();
			for (int i = 0; i < cursor.getCount(); i++) {
				buffer.append(cursor.getString(cursor
						.getColumnIndexOrThrow(DbAdapter.Columns.LABEL)));
				if (!cursor.isLast()) {
					buffer.append(", ");
				}
				cursor.moveToNext();
			}
			cursor.close();
			retVal = buffer.toString();
		} else {
			retVal = "";
		}
		return retVal;
	}

	/**
	 * Returns the ID of the guard given the house_id and the 'which' parameter.
	 * 
	 * @param house_id
	 *            the house_id
	 * @param day
	 *            the 0-index day of the week
	 * @return the guard
	 * @throws SQLException
	 *             the sQL exception
	 */
	public long getGuard(final long house_id, final int day)
			throws SQLException {
		final String typicalDay = DbAdapter.getGuardScheduleColumnName(day,
				true);
		final String otherDay = DbAdapter
				.getGuardScheduleColumnName(day, false);

		final Cursor cur = mDb.rawQuery("select case when " + otherDay
				+ "='-1' then " + typicalDay + " else " + otherDay
				+ " end from houses where _id=?;",
				new String[] { String.valueOf(house_id) });
		return cur.moveToFirst() ? cur.getLong(0) : -1;
	}

	/**
	 * Gets the guard checkin time.
	 * 
	 * @param guard_id
	 *            the guard_id
	 * @return the guard checkin time
	 */
	public String getGuardCheckinTime(final long guard_id) {
		// this query should select the most recent checkin time for the guard
		final Cursor cur = mDb
				.rawQuery(
						"select time from guardcheckins where guard_id=? order by time desc limit 1;",
						new String[] { String.valueOf(guard_id) });
		return cur.moveToFirst() ? cur.getString(0) : "";
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
	public long getGuardIdFromNumber(final String number) throws SQLException {
		final Cursor cur = mDb.rawQuery(
				"select _id from guards where number=?;",
				new String[] { number });
		return cur.moveToFirst() ? cur.getLong(0) : -1;
	}

	/**
	 * Returns the name of the guard.
	 * 
	 * @param guard_id
	 *            the guard id
	 * @return the guard name
	 * @throws SQLException
	 *             a SQL exception
	 */
	public String getGuardName(final long guard_id) throws SQLException {
		final Cursor cur = mDb.rawQuery("select name from guards where _id=?;",
				new String[] { String.valueOf(guard_id) });
		return cur.moveToFirst() ? cur.getString(0) : "";
	}

	/**
	 * Returns the number of the guard, or an empty string if there is no number
	 * for the supplied guardId.
	 * 
	 * @param guard_id
	 *            the guard id
	 * @return the guard number
	 * @throws SQLException
	 *             a SQL exception
	 */
	public String getGuardNumber(final long guard_id) throws SQLException {
		final Cursor cur = mDb.rawQuery(
				"select number from guards where _id=?;",
				new String[] { String.valueOf(guard_id) });
		return cur.moveToFirst() ? cur.getString(0) : "";
	}

	/**
	 * Gets the scheduled guard's phone number for the day of the week of the
	 * given date.
	 * 
	 * @param house_id
	 *            the house_id
	 * @param date
	 *            the date as an ISO 8601 date time string
	 * @return the guard number from date
	 */
	public String getGuardNumberFromDate(final long house_id, final String date) {
		final Calendar calendar = Calendar.getInstance();
		calendar.setTime(Time.iso8601DateTime(date));
		final int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);

		final long guardId = getGuard(house_id, dayOfWeek);
		return getGuardNumber(guardId);
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
	public long getHouseId(final long contact_id) throws SQLException {
		final Cursor cur = mDb.query(Tables.HOUSEMEMBERS,
				new String[] { Columns.HOUSEID }, Columns.CONTACTID + "= ?",
				new String[] { String.valueOf(contact_id) }, null, null, null);
		return cur.moveToFirst() ? cur.getInt(cur
				.getColumnIndex(Columns.HOUSEID)) : -1;
	}

	/**
	 * Returns the id of the house if it exists, otherwise -1.
	 * 
	 * @param house
	 * @return the id of the house if it exists, otherwise -1
	 * @throws SQLException
	 */
	public long getHouseId(final String house) throws SQLException {
		final Cursor cur = mDb.rawQuery(
				"select _id from houses where lower(name)=lower(?);",
				new String[] { house });
		return cur.moveToFirst() ? cur.getLong(0) : -1;
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
	public long getHouseIdFromCallaround(final long callaround_id)
			throws SQLException {
		final Cursor cur = mDb.query(Tables.CALLAROUNDS,
				new String[] { Columns.HOUSEID }, Columns.ROWID + "= ?",
				new String[] { String.valueOf(callaround_id) }, null, null,
				null);
		return cur.moveToFirst() ? cur.getInt(cur
				.getColumnIndex(Columns.HOUSEID)) : -1;
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
	public String getHouseName(final long rowId) throws SQLException {
		final Cursor cur = mDb.query(Tables.HOUSES,
				new String[] { Columns.NAME }, Columns.ROWID + "= ?",
				new String[] { String.valueOf(rowId) }, null, null, null);
		return cur.moveToFirst() ? cur.getString(0) : "";
	}

	/**
	 * Returns a comma separated list of houses.
	 * 
	 * @return the comma separated list of houses
	 * @throws SQLException
	 */
	public String getHouses() throws SQLException {
		final Cursor cursor = fetchAllHouses();
		String retVal;
		if (cursor.moveToFirst()) {
			final StringBuffer buffer = new StringBuffer();
			for (int i = 0; i < cursor.getCount(); i++) {
				buffer.append(cursor.getString(cursor
						.getColumnIndexOrThrow(DbAdapter.Columns.NAME)));
				if (!cursor.isLast()) {
					buffer.append(", ");
				}
				cursor.moveToNext();
			}
			cursor.close();
			retVal = buffer.toString();
		} else {
			retVal = "";
		}
		return retVal;
	}

	/**
	 * Returns the keyword of the location.
	 * 
	 * @param rowId
	 *            the row id
	 * @return the location name
	 * @throws SQLException
	 *             a SQL exception
	 */
	public String getLocationKeyword(final long rowId) throws SQLException {
		final Cursor cur = mDb.query(Tables.LOCATIONS,
				new String[] { Columns.KEYWORD }, Columns.ROWID + "= ?",
				new String[] { String.valueOf(rowId) }, null, null, null);
		return cur.moveToFirst() ? cur.getString(0) : "";
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
	public boolean getLocationKeywordExists(final String keyword)
			throws SQLException {
		final Cursor cur = mDb.rawQuery(
				"select _id from locations where lower(keyword)=lower(?);",
				new String[] { keyword });
		return cur.moveToFirst();
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
	public boolean getLocationKeywordPermitted(final String keyword)
			throws SQLException {
		final Cursor cur = mDb.rawQuery(
				"select allowed from locations where lower(keyword)=lower(?);",
				new String[] { keyword });
		boolean retVal;
		if (cur.moveToFirst()) {
			retVal = cur.getLong(0) == 1 ? true : false;
		} else {
			retVal = false;
		}
		return retVal;
	}

	/**
	 * Gets the location keywords.
	 * 
	 * @return the location keywords
	 * @throws SQLException
	 *             the sQL exception
	 */
	public String getLocationKeywords() throws SQLException {
		final Cursor cursor = mDb.query(Tables.LOCATIONS, new String[] {
				Columns.LABEL, Columns.KEYWORD }, null, null, null, null,
				Columns.KEYWORD);
		String retVal;
		if (cursor.moveToFirst()) {
			final StringBuffer buffer = new StringBuffer();
			for (int i = 0; i < cursor.getCount(); i++) {
				buffer.append(cursor.getString(cursor
						.getColumnIndexOrThrow(DbAdapter.Columns.KEYWORD)));
				buffer.append(" (");
				buffer.append(cursor.getString(cursor
						.getColumnIndexOrThrow(DbAdapter.Columns.LABEL)));
				buffer.append(')');
				if (!cursor.isLast()) {
					buffer.append(", ");
				}
				cursor.moveToNext();
			}
			cursor.close();
			retVal = buffer.toString();
		} else {
			retVal = "";
		}
		return retVal;
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
	public String getLocationName(final long rowId) throws SQLException {
		final Cursor cur = mDb.query(Tables.LOCATIONS,
				new String[] { Columns.LABEL }, Columns.ROWID + "= ?",
				new String[] { String.valueOf(rowId) }, null, null, null);
		return cur.moveToFirst() ? cur.getString(0) : "";
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
	public String getNumber(final long contactphoneId) throws SQLException {
		final Cursor cur = mDb.rawQuery(
				"select number from contactphones where _id=? limit 1;",
				new String[] { String.valueOf(contactphoneId) });
		return cur.moveToFirst() ? cur.getString(0) : "";
	}

	/**
	 * Returns the phone number associated with a check-in request.
	 * 
	 * @param checkin_id
	 *            the checkin id
	 * @return the number for checkin
	 * @throws SQLException
	 *             a SQL exception
	 */
	public String getNumberForCheckin(final long checkin_id)
			throws SQLException {
		final Cursor cur = mDb
				.rawQuery(
						"select number from contactphones,checkins where checkins.contact_id=contactphones.contact_id and checkins._id=? limit 1;",
						new String[] { String.valueOf(checkin_id) });
		return cur.moveToFirst() ? cur.getString(0) : "";
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
	public boolean getNumberIsBlocked(final String number) throws SQLException {
		final Cursor cur = mDb
				.rawQuery(
						"select count(_id) as count from blockednumbers where number=?;",
						new String[] { number });
		boolean retVal;
		if (cur.moveToFirst()) {
			retVal = cur.getLong(cur.getColumnIndex(Columns.COUNT)) > 0 ? true
					: false;
		} else {
			retVal = false;
		}
		return retVal;
	}

	/**
	 * Returns the number of call arounds that are due for today (including
	 * delayed callarounds). This is an odd method because it doesn't tell you
	 * whether the callarounds should have been made by now.
	 * 
	 * @return the number of due call arounds
	 * @throws SQLException
	 *             a SQL exception
	 */
	public long getNumberOfDueCallarounds() throws SQLException {
		final Cursor cur = mDb.rawQuery(
				"select count(_id) as count from callarounds where date(dueby)='"
						+ Time.iso8601Date()
						+ "' and datetime('now','localtime') >= datetime('"
						+ Time.iso8601Date(Time.todayAtPreferenceTime(mContext,
								Preferences.CALLAROUND_DELAYED_TIME, "23:59"))
						+ "') and outstanding='1';", null);
		return cur.moveToFirst() ? cur.getLong(cur
				.getColumnIndex(Columns.COUNT)) : 0;
	}

	/**
	 * Returns the number of call arounds that are due, excluding delayed
	 * callarounds.
	 * 
	 * @return the number of due call arounds
	 * @throws SQLException
	 *             a SQL exception
	 */
	public long getNumberOfDueCallaroundsNoDelayed() throws SQLException {
		final Cursor cur = mDb
				.rawQuery(
						"select count(_id) as count from callarounds where date(dueby)='"
								+ Time.iso8601Date()
								+ "' and datetime(dueby) <= datetime('now','localtime') and outstanding='1' and delayed='0';",
						null);
		return cur.moveToFirst() ? cur.getLong(cur
				.getColumnIndex(Columns.COUNT)) : 0;
	}

	/**
	 * Returns the number of check-ins that are due.
	 * 
	 * @return the number of due check-ins
	 * @throws SQLException
	 *             a SQL exception
	 */
	public long getNumberOfDueCheckins() throws SQLException {
		final Cursor cur = mDb
				.rawQuery(
						"select count(_id) as count from checkins where outstanding='1' and datetime(timedue) <= datetime('now','localtime');",
						null);
		return cur.moveToFirst() ? cur.getLong(cur
				.getColumnIndex(Columns.COUNT)) : 0;
	}

	/**
	 * Returns the combined number of unsent and undelivered messages in the
	 * database.
	 * 
	 * @return the combined number of unsent and undelivered messages in the
	 *         database
	 */
	public int getNumberOfMessageErrors() {
		final Cursor cur = mDb
				.rawQuery("select count(_id) from pending;", null);
		return cur.moveToFirst() ? cur.getInt(0) : 0;
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
		String checkin_people;
		String callaround_houses;
		final StringBuffer checkin_people_buffer = new StringBuffer();
		final StringBuffer callaround_houses_buffer = new StringBuffer();

		Cursor cur = mDb
				.rawQuery(
						"select name,location from trips left join contacts on contacts._id=trips.contact_id where tripresolved='0';",
						null);
		if (cur.moveToFirst()) {
			for (int i = 0; i < cur.getCount(); i++) {
				checkin_people_buffer.append(cur.getString(0) + " ("
						+ cur.getString(1) + ")");
				if (!cur.isLast()) {
					checkin_people_buffer.append(", ");
				}
				cur.moveToNext();
			}
			checkin_people = checkin_people_buffer.toString();
		} else {
			checkin_people = mContext.getString(R.string.none);
		}

		cur = mDb
				.rawQuery(
						"select name from callarounds left join houses on callarounds.house_id=houses._id where outstanding='1' and date(dueby)=date('now','localtime');",
						null);
		if (cur.moveToFirst()) {
			for (int i = 0; i < cur.getCount(); i++) {
				callaround_houses_buffer.append(cur.getString(0));
				if (!cur.isLast()) {
					callaround_houses_buffer.append(", ");
				}
				cur.moveToNext();
			}
			callaround_houses = callaround_houses_buffer.toString();
		} else {
			callaround_houses = mContext.getString(R.string.none);
		}

		final String checkin_report = String.format(
				mContext.getString(R.string.sms_report_checkins),
				checkin_people);
		final String callaround_report = String.format(
				mContext.getString(R.string.sms_report_callarounds),
				callaround_houses);
		final String report = String.format(
				mContext.getString(R.string.sms_report), checkin_report,
				callaround_report);

		return report;
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
	public boolean getTripResolvedFromCheckin(final long checkin_id)
			throws SQLException {
		final Cursor cur = mDb
				.rawQuery(
						"select tripresolved from trips where _id in (select trip_id from tripmembers where checkin_id="
								+ checkin_id + ");", null);

		boolean retVal;
		if (cur.moveToFirst()) {
			retVal = cur.getLong(0) == 1 ? true : false;
		} else {
			retVal = false;
		}
		return retVal;
	}

	/**
	 * Gets the typical guard for the given house and day.
	 * 
	 * @param house_id
	 *            the house id
	 * @param day
	 *            the 0-indexed day
	 * @return the id typical guard for the day
	 * @throws SQLException
	 *             the sQL exception
	 */
	public long getTypicalGuard(final long house_id, final int day)
			throws SQLException {
		final String typicalDay = DbAdapter.getGuardScheduleColumnName(day,
				true);
		final Cursor cur = mDb.rawQuery("select " + typicalDay
				+ " from houses where _id=?;",
				new String[] { String.valueOf(house_id) });
		return cur.moveToFirst() ? cur.getLong(0) : -1;
	}

	/**
	 * Returns the value of SQLite's last_insert_rowid() function.
	 * 
	 * @return the value of SQLite's last_insert_rowid()
	 * @throws SQLException
	 *             a SQL exception
	 */
	public int lastInsertId() throws SQLException {
		final Cursor cur = mDb.rawQuery("select last_insert_rowid();", null);
		return cur.moveToFirst() ? cur.getInt(0) : -1;
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
	 * Resets the guards' schedules for yesterday to whatever is set in the
	 * typical fields.
	 */
	public void resetGuardSchedule() {
		final String theDay = Time.dayOfWeek(
				Time.previousDateFromPreferenceString(mContext,
						Preferences.GUARD_CHECKIN_START, "22:00")).toLowerCase(
				Locale.US);
		mDb.execSQL("update houses set " + theDay + "_guard=typical_" + theDay
				+ "_guard;");
	}

	/**
	 * @param contact_id
	 * @return
	 */
	private int resolveExistingCheckins(final long contact_id) {
		final Cursor cur = mDb.rawQuery(
				"select count(_id) from checkins where outstanding='1' and contact_id='"
						+ contact_id + "';", null);
		cur.moveToFirst();
		final int count = cur.getInt(0);

		final ContentValues args = new ContentValues();
		args.put(Columns.OUTSTANDING, 0);
		mDb.update(Tables.CHECKINS, args, Columns.CONTACTID + "=" + contact_id,
				null);
		return count;
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
	public int setCallaroundActive(final long house_id, final boolean active)
			throws SQLException {
		final ContentValues args = new ContentValues();
		int retVal;
		args.put(Columns.ACTIVE, active ? 1 : 0);
		if (mDb.update(Tables.HOUSES, args, Columns.ROWID + "=" + house_id,
				null) > 0) {
			retVal = Notifications.SUCCESS;
		} else {
			retVal = Notifications.FAILURE;
		}

		// add today's, or remove it
		if (active) {
			addCallarounds();
		} else {
			mDb.delete(
					Tables.CALLAROUNDS,
					"datetime('now','localtime') >= datetime(duefrom) and datetime('now','localtime') <= datetime(dueby) and "
							+ Columns.HOUSEID
							+ "='"
							+ house_id
							+ "' and outstanding='1'", null);
		}
		return retVal;
	}

	/**
	 * Sets whether the callaround is delayed or not. If the call around is to
	 * be delayed, it is also made outstanding. This is so that if someone
	 * requests a delayed call around, a call around will be expected, even if
	 * it has already been resolved.
	 * 
	 * @param house_id
	 *            the house id
	 * @param delayed
	 *            whether the callaround should be delayed or... un-delayed
	 * @return either Notifications.NOTIFY_SUCCESS or
	 *         Notifications.NOTIFY_FAILURE
	 * @throws SQLException
	 *             the sQL exception
	 */
	public int setCallaroundDelayed(final long house_id, final boolean delayed)
			throws SQLException {
		final ContentValues args = new ContentValues();
		args.put(Columns.DELAYED, delayed ? 1 : 0);
		if (delayed) {
			args.put(Columns.OUTSTANDING, 1);
		}
		return mDb.update(Tables.CALLAROUNDS, args, Columns.HOUSEID + "="
				+ house_id + " and date(dueby)=date('now','localtime')", null) > 0 ? Notifications.SUCCESS
				: Notifications.FAILURE;
	}

	/**
	 * Sets whether an existing call around is resolved or not, for whatever
	 * call arounds can be resolved.
	 * 
	 * @param house_id
	 *            the house_id of the call around to update
	 * @param resolveCallaround
	 *            whether the call around is to be resolved or not
	 * @return Possible return values: Notifications.NOTIFY_SUCCESS,
	 *         Notifications.NOTIFY_FAILURE, Notifications.NOTIFY_ALREADY,
	 *         Notifications.NOTIFY_INACTIVE
	 * @throws SQLException
	 *             a SQL exception
	 */
	public int setCallaroundResolved(final long house_id,
			final boolean resolveCallaround) throws SQLException {
		final boolean outstanding = getCallaroundOutstanding(house_id);
		final boolean active = getCallaroundActive(house_id);
		final boolean timely = getCallaroundTimely(house_id);

		int retVal;

		// if we're not expecting a call around from the house, say so
		if (active) {
			// if it's not a timely call around
			if (timely) {
				// if this is already in effect
				if (!resolveCallaround || outstanding) {
					final String sOutstanding = resolveCallaround ? "0" : "1";
					final String delayedDueTime = Time.iso8601DateTime(Time
							.todayAtPreferenceTime(mContext,
									Preferences.CALLAROUND_DELAYED_TIME,
									"23:59"));

					mDb.execSQL("update callarounds set outstanding='"
							+ sOutstanding
							+ "',timereceived=datetime('now','localtime') where datetime('now','localtime') >= datetime(duefrom) and datetime('now','localtime') <= datetime('"
							+ delayedDueTime
							+ "') and date(dueby)=date('now','localtime') and house_id='"
							+ house_id + "';");

					if (changes() > 0) {
						retVal = Notifications.SUCCESS;
					} else {
						retVal = Notifications.FAILURE;
					}
				} else {
					retVal = Notifications.ALREADY;
				}
			} else {
				retVal = Notifications.UNTIMELY;
			}
		} else {
			retVal = Notifications.INACTIVE;
		}

		return retVal;
	}

	/**
	 * Sets whether an existing call around is resolved or not.
	 * 
	 * @param callaround_id
	 *            the callaround_id
	 * @param resolved
	 *            whether the call around is to be resolved or not
	 * @return Possible return values: Notifications.NOTIFY_SUCCESS,
	 *         Notifications.NOTIFY_FAILURE, Notifications.NOTIFY_ALREADY,
	 *         Notifications.NOTIFY_INACTIVE
	 * @throws SQLException
	 *             a SQL exception
	 */
	public int setCallaroundResolvedFromId(final long callaround_id,
			final boolean resolved) throws SQLException {
		final ContentValues args = new ContentValues();
		args.put(Columns.OUTSTANDING, resolved ? 0 : 1);
		args.put(Columns.TIMERECEIVED, resolved ? Time.iso8601DateTime() : "");

		return mDb.update(Tables.CALLAROUNDS, args, Columns.ROWID + "=?",
				new String[] { String.valueOf(callaround_id) }) > 0 ? Notifications.SUCCESS
				: Notifications.FAILURE;
	}

	/**
	 * Set the resolution status of any check-ins associated with a given
	 * contact.
	 * 
	 * @param contact_id
	 *            the contact_id
	 * @param resolved
	 *            the resolved
	 * @return Possible values: Notifications.NOTIFY_SUCCESS,
	 *         Notifications.NOTIFY_FAILURE
	 * @throws SQLException
	 *             a SQL exception
	 */
	public int setCheckinResolved(final long contact_id, final boolean resolved)
			throws SQLException {
		final ContentValues args = new ContentValues();
		args.put(Columns.OUTSTANDING, resolved ? 0 : 1);
		return mDb.update(Tables.CHECKINS, args, Columns.CONTACTID + "=?",
				new String[] { String.valueOf(contact_id) }) > 0 ? Notifications.SUCCESS
				: Notifications.FAILURE;
	}

	/**
	 * Resolve a check-in, given its _id.
	 * 
	 * @param checkin_id
	 *            the checkin_id
	 * @param resolved
	 *            the resolved
	 * @return Possible values: Notifications.NOTIFY_SUCCESS,
	 *         Notifications.NOTIFY_FAILURE
	 * @throws SQLException
	 *             a SQL exception
	 */
	public int setCheckinResolvedFromId(final long checkin_id,
			final boolean resolved) throws SQLException {
		int retVal;
		final ContentValues args = new ContentValues();
		args.put(Columns.OUTSTANDING, resolved ? 0 : 1);
		if (mDb.update(Tables.CHECKINS, args, Columns.ROWID + "=?",
				new String[] { String.valueOf(checkin_id) }) > 0) {
			retVal = Notifications.SUCCESS;
		} else {
			retVal = Notifications.FAILURE;
		}
		return retVal;
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
	public void setContactEmail(final long contact_id, final String newemail)
			throws SQLException {
		final ContentValues args = new ContentValues();
		args.put(Columns.EMAIL, newemail);
		mDb.update(Tables.CONTACTEMAILS, args, Columns.CONTACTID + "= ?",
				new String[] { String.valueOf(contact_id) });
		if (changes() == 0) {
			args.put(Columns.CONTACTID, contact_id);
			mDb.insert(Tables.CONTACTEMAILS, null, args);
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
	public void setContactName(final long contact_id, final String newname)
			throws SQLException {
		final ContentValues args = new ContentValues();
		args.put(Columns.NAME, newname);
		mDb.update(Tables.CONTACTS, args, Columns.ROWID + "= ?",
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
	 * @return Possible values: Notifications.NOTIFY_SUCCESS,
	 *         Notifications.NOTIFY_FAILURE, Notifications.NOTIFY_ALREADY
	 * @throws SQLException
	 *             a SQL exception
	 */
	public int setContactPermission(final long contact_id,
			final long permissionId, final boolean pref) throws SQLException {
		final Cursor cur = mDb.query(Tables.CONTACTS,
				new String[] { Columns.PERMISSIONS }, Columns.ROWID + "=?",
				new String[] { String.valueOf(contact_id) }, null, null, null);
		cur.moveToFirst();
		long result = cur.getLong(0);
		cur.close();

		int retVal;
		if (((result & permissionId) > 0) && pref) {
			retVal = Notifications.ALREADY;
		} else {
			if (((result & permissionId) == 0) && !pref) {
				retVal = Notifications.ALREADY;
			} else {

				if (pref) {
					result = result | permissionId;
				} else {
					result = result & (~permissionId);
				}

				final ContentValues args = new ContentValues();
				args.put(Columns.PERMISSIONS, result);
				final int nrow = mDb.update(Tables.CONTACTS, args,
						Columns.ROWID + "= ?",
						new String[] { String.valueOf(contact_id) });
				retVal = nrow > 0 ? Notifications.SUCCESS
						: Notifications.FAILURE;
			}
		}
		return retVal;
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
	public void setContactPhone(final long contact_id,
			final String newPhoneNumber) throws SQLException {
		final ContentValues args = new ContentValues();
		args.put(Columns.NUMBER, newPhoneNumber);
		mDb.update(Tables.CONTACTPHONES, args, Columns.CONTACTID + "= ?",
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
	 * @return Possible values: Notifications.NOTIFY_SUCCESS,
	 *         Notifications.NOTIFY_FAILURE, Notifications.NOTIFY_ALREADY
	 * @throws SQLException
	 *             a SQL exception
	 */
	public int setContactPreference(final long contact_id,
			final long preferenceId, final boolean pref) throws SQLException {
		final Cursor cur = mDb.query(Tables.CONTACTS,
				new String[] { Columns.PREFERENCES }, Columns.ROWID + "=?",
				new String[] { String.valueOf(contact_id) }, null, null, null);
		cur.moveToFirst();
		long result = cur.getLong(0);
		cur.close();

		int retVal;
		if (((result & preferenceId) > 0) && pref) {
			retVal = Notifications.ALREADY;
		} else {
			if (((result & preferenceId) == 0) && !pref) {
				retVal = Notifications.ALREADY;
			} else {

				if (pref) {
					result = result | preferenceId;
				} else {
					result = result & (~preferenceId);
				}

				final ContentValues args = new ContentValues();
				args.put(Columns.PREFERENCES, result);
				final int nrow = mDb.update(Tables.CONTACTS, args,
						Columns.ROWID + "= ?",
						new String[] { String.valueOf(contact_id) });

				retVal = nrow > 0 ? Notifications.SUCCESS
						: Notifications.FAILURE;
			}
		}
		return retVal;
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
	public int setGuardCheckinResolved(final Context context,
			final long guard_id) {
		final SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(mContext);
		final int window = Integer.parseInt(settings.getString(
				Preferences.GUARD_CHECKIN_WINDOW, "5"));

		int retVal;

		final String checkinTime = getGuardCheckinTime(guard_id);
		if (checkinTime.isEmpty()) {
			retVal = Notifications.FAILURE;
		} else {
			final ContentValues args = new ContentValues();
			args.put(Columns.RESPONSE, 1);

			if (mDb.update(
					Tables.GUARD_CHECKINS,
					args,
					"guard_id=? and datetime('now','localtime') >= time and datetime('now','localtime') <= datetime(time,'+"
							+ window + " minutes')",
					new String[] { String.valueOf(guard_id) }) > 0) {
				retVal = Notifications.SUCCESS;
			} else {
				retVal = Notifications.FAILURE;
			}
		}
		return retVal;
	}

	/**
	 * Sets the guard.
	 * 
	 * @param house_id
	 *            the id of the house to change
	 * @param guard_id
	 *            the id of the guard to assign
	 * @param day
	 *            the 0-indexed day of the week
	 * @throws SQLException
	 *             the SQL exception
	 */
	public void setGuardForDay(final long house_id, final long guard_id,
			final int day) throws SQLException {
		final String which = DbAdapter.getGuardScheduleColumnName(day, false);

		final long typicalGuard = getTypicalGuard(house_id, day);

		final ContentValues args = new ContentValues();
		if (typicalGuard == guard_id) {
			// in this case we just want to put -1 into the schedule for that
			// day, so that it later looks up the typical value
			args.put(which, -1);
		} else {
			// in this case we want to put the number into the specific day
			// column
			args.put(which, guard_id);
		}
		mDb.update(Tables.HOUSES, args, Columns.ROWID + "=?",
				new String[] { String.valueOf(house_id) });
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
	public void setGuardName(final long guard_id, final String newname)
			throws SQLException {
		final ContentValues args = new ContentValues();
		args.put(Columns.NAME, newname);
		mDb.update(Tables.GUARDS, args, Columns.ROWID + "= ?",
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
	public void setGuardNumber(final long guard_id, final String newnumber)
			throws SQLException {
		final ContentValues args = new ContentValues();
		args.put(Columns.NUMBER, newnumber);
		mDb.update(Tables.GUARDS, args, Columns.ROWID + "= ?",
				new String[] { String.valueOf(guard_id) });
	}

	/**
	 * Sets the house associated with a particular contact. If the specified
	 * house_id is -1, then the contact is dissociated from any house he is
	 * currently associated with.
	 * 
	 * @param contact_id
	 *            the contact_id
	 * @param house_id
	 *            the house_id
	 * @return Notifications.NOTIFY_HASHOUSE, Notifications.NOTIFY_NOHOUSE, or
	 *         Notifications.NOTIFY_FAILURE
	 * @throws SQLException
	 *             a SQL exception
	 */
	public int setHouse(final long contact_id, final long house_id)
			throws SQLException {
		mDb.delete(Tables.HOUSEMEMBERS, Columns.CONTACTID + "=" + contact_id,
				null);

		int retVal;
		if (house_id == -1) {
			retVal = Notifications.NOHOUSE;
		} else {
			final ContentValues initialValues = new ContentValues();
			initialValues.put(Columns.CONTACTID, contact_id);
			initialValues.put(Columns.HOUSEID, house_id);
			retVal = mDb.insert(Tables.HOUSEMEMBERS, null, initialValues) > 1 ? Notifications.HASHOUSE
					: Notifications.FAILURE;
		}
		return retVal;
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
	public void setHouseName(final long rowId, final String name)
			throws SQLException {
		final ContentValues args = new ContentValues();
		args.put(Columns.NAME, name);
		mDb.update(Tables.HOUSES, args, Columns.ROWID + "= ?",
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
	public boolean setLocationAllowed(final long rowId, final boolean allowed)
			throws SQLException {
		final ContentValues args = new ContentValues();
		args.put(Columns.ALLOWED, allowed ? 1 : 0);
		return mDb.update(Tables.LOCATIONS, args, Columns.ROWID + "=" + rowId,
				null) > 0;
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
	public void setLocationKeyword(final long rowId, final String keyword)
			throws SQLException {
		final ContentValues args = new ContentValues();
		args.put(Columns.KEYWORD, keyword);
		mDb.update(Tables.LOCATIONS, args, Columns.ROWID + "= ?",
				new String[] { String.valueOf(rowId) });
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
	public void setLocationName(final long rowId, final String name)
			throws SQLException {
		final ContentValues args = new ContentValues();
		args.put(Columns.LABEL, name);
		mDb.update(Tables.LOCATIONS, args, Columns.ROWID + "= ?",
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
	public void setNumberIsBlocked(final String number, final boolean blocked)
			throws SQLException {
		if (blocked) {
			final ContentValues initialValues = new ContentValues();
			initialValues.put(Columns.NUMBER, number);
			mDb.insert(Tables.BLOCKEDNUMBERS, null, initialValues);
		} else {
			mDb.delete(Tables.BLOCKEDNUMBERS, Columns.NUMBER + "=?",
					new String[] { number });
		}
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
	public void setPendingMessageDelivered(final String number,
			final String message) throws SQLException {
		final ContentValues args = new ContentValues();
		args.put(Columns.DELIVERED, 1);
		mDb.update(Tables.PENDING, args, Columns.NUMBER + "=? and "
				+ Columns.MESSAGE + "=?", new String[] { number, message });
		mDb.delete(Tables.PENDING, "delivered='1' and sent='1'", null);
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
	public void setPendingMessageSent(final String number, final String message)
			throws SQLException {
		final ContentValues args = new ContentValues();
		args.put(Columns.SENT, 1);
		mDb.update(Tables.PENDING, args, Columns.NUMBER + "=? and "
				+ Columns.MESSAGE + "=?", new String[] { number, message });
		mDb.delete(Tables.PENDING, "delivered='1' and sent='1'", null);
	}

	/**
	 * Resolves (or not) the trip associated with a given checkin id
	 * 
	 * @param checkin_id
	 *            the checkin id
	 * @param resolved
	 *            true if the trip should be resolved, false if it should be
	 *            unresolved
	 * @throws SQLException
	 */
	public void setTripResolvedFromCheckinId(final long checkin_id,
			final boolean resolved) throws SQLException {
		if (resolved) {
			resolveExistingCheckins(getContactIdForCheckin(checkin_id));
		}

		final long newValue = resolved ? 1 : 0;
		mDb.execSQL("update trips set tripresolved='"
				+ newValue
				+ "' where _id in (select trip_id from tripmembers where checkin_id="
				+ checkin_id + ");");
	}

	/**
	 * Resolves any trip associated with the contact.
	 * 
	 * @param contact_id
	 *            the contact_id
	 * @param resolved
	 *            the resolved
	 * @return Possible values: Notifications.NOTIFY_SUCCESS,
	 *         Notifications.NOTIFY_FAILURE
	 * @throws SQLException
	 *             a SQL exception
	 */
	public int setTripResolvedFromContact(final long contact_id)
			throws SQLException {
		resolveExistingCheckins(contact_id);

		int retVal;
		final ContentValues args = new ContentValues();
		args.put(Columns.TRIPRESOLVED, 1);
		if (mDb.update(Tables.TRIPS, args, Columns.CONTACTID + "=?",
				new String[] { String.valueOf(contact_id) }) > 0) {
			retVal = Notifications.SUCCESS;
		} else {
			retVal = Notifications.FAILURE;
		}
		return retVal;
	}

	/**
	 * Set the typical guard for the specified house, on the specified day.
	 * 
	 * @param house_id
	 *            The id of the house.
	 * @param guard_id
	 *            The id of the guard.
	 * @param day
	 *            The 0-indexed day of the week.
	 * @throws SQLException
	 */
	public void setTypicalGuard(final long house_id, final long guard_id,
			final int day) throws SQLException {
		final String which = DbAdapter.getGuardScheduleColumnName(day, true);

		final ContentValues args = new ContentValues();
		args.put(which, guard_id);
		mDb.update(Tables.HOUSES, args, Columns.ROWID + "= ?",
				new String[] { String.valueOf(house_id) });
	}

	/**
	 * If a call around scheduled for today has already been resolved or is
	 * delayed, update the due times.
	 * 
	 * @param dueby
	 *            the date the call around is due by
	 * @param duefrom
	 *            the date the call around is due from
	 */
	private void updateTimesOfResolvedDelayedCallarounds(final Date dueby,
			final Date duefrom) {
		final ContentValues args = new ContentValues();
		args.put(Columns.DUEBY, Time.iso8601DateTime(dueby));
		args.put(Columns.DUEFROM, Time.iso8601DateTime(duefrom));
		mDb.update(Tables.CALLAROUNDS, args,
				"date(dueby) = date('now','localtime') and ("
						+ Columns.OUTSTANDING + "='0' or " + Columns.DELAYED
						+ "='1');", null);
	}

}
