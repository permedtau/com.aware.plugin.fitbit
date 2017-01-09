package com.aware.plugin.fitbit;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import android.support.annotation.Nullable;
import android.util.Log;

import com.aware.Aware;
import com.aware.utils.DatabaseHelper;

import java.util.HashMap;

public class Provider extends ContentProvider {

    public static String AUTHORITY = "com.aware.plugin.fitbit.provider.fitbit"; //change to package.provider.your_plugin_name
    public static final int DATABASE_VERSION = 3; //increase this if you make changes to the database structure, i.e., rename columns, etc.

    //    public static Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/plugin_fitbit");
    public static final String DATABASE_NAME = "plugin_fitbit.db"; //the database filename, use plugin_xxx for plugins.

    //Add here your database table names, as many as you need
    public static final String DB_TBL_FITBIT = "fitbit_data";
    public static final String DB_TBL_FITBIT_DEVICES = "fitbit_devices";

    //For each table, add two indexes: DIR and ITEM. The index needs to always increment. Next one is 3, and so on.
    private static final int FITBIT_DIR = 1;
    private static final int FITBIT_ONE_ITEM = 2;
    private static final int FITBIT_DEVICES_DIR = 3;
    private static final int FITBIT_DEVICES_ONE_ITEM = 4;

    //Put tables names in this array so AWARE knows what you have on the database
    public static final String[] DATABASE_TABLES = {
            DB_TBL_FITBIT,
            DB_TBL_FITBIT_DEVICES
    };

    //These are columns that we need to sync data, don't change this!
    public interface AWAREColumns extends BaseColumns {
        String _ID = "_id";
        String TIMESTAMP = "timestamp";
        String DEVICE_ID = "device_id";
    }

    /**
     * Create one of these per database table
     * In this example, we are adding example columns
     */
    public static final class Fitbit_Data implements AWAREColumns {
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + DB_TBL_FITBIT);
        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.com.aware.plugin.fitbit.provider.fitbit_data"; //modify me
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.com.aware.plugin.fitbit.provider.fitbit_data"; //modify me

        public static final String FITBIT_ID = "fitbit_id";
        public static final String FITBIT_JSON = "fitbit_data";
        public static final String DATA_TYPE = "fitbit_data_type";
    }

    public static final class Fitbit_Devices implements AWAREColumns {
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + DB_TBL_FITBIT_DEVICES);
        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.com.aware.plugin.fitbit.provider.fitbit_devices"; //modify me
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.com.aware.plugin.fitbit.provider.fitbit_devices"; //modify me

        public static final String FITBIT_ID = "fitbit_id";
        public static final String FITBIT_VERSION = "fitbit_version";
        public static final String FITBIT_BATTERY = "fitbit_battery";
        public static final String LAST_SYNC = "fitbit_last_sync";
    }

    //Define each database table fields
    private static final String DB_TBL_FITBIT_DATA_FIELDS =
            Fitbit_Data._ID + " integer primary key autoincrement," +
                    Fitbit_Data.TIMESTAMP + " real default 0," +
                    Fitbit_Data.DEVICE_ID + " text default ''," +
                    Fitbit_Data.FITBIT_ID + " text default ''," +
                    Fitbit_Data.DATA_TYPE + " text default 0," +
                    Fitbit_Data.FITBIT_JSON + " text default ''";

    private static final String DB_TBL_FITBIT_DEVICES_FIELDS =
            Fitbit_Devices._ID + " integer primary key autoincrement," +
                    Fitbit_Devices.TIMESTAMP + " real default 0," +
                    Fitbit_Devices.DEVICE_ID + " text default ''," +
                    Fitbit_Devices.FITBIT_ID + " text default ''," +
                    Fitbit_Devices.FITBIT_VERSION + " text default 0," +
                    Fitbit_Devices.FITBIT_BATTERY + " text default 0," +
                    Fitbit_Devices.LAST_SYNC + " text default ''";

    /**
     * Share the fields with AWARE so we can replicate the table schema on the server
     */
    public static final String[] TABLES_FIELDS = {
            DB_TBL_FITBIT_DATA_FIELDS,
            DB_TBL_FITBIT_DEVICES_FIELDS
    };

    //Helper variables for ContentProvider - don't change me
    private static UriMatcher sUriMatcher;
    private static DatabaseHelper databaseHelper;
    private static SQLiteDatabase database;

    //For each table, create a hashmap needed for database queries
    private static HashMap<String, String> fitbitHash;
    private static HashMap<String, String> fitbitDevicesHash;

    /**
     * Initialise database: create the database file, update if needed, etc. DO NOT CHANGE ME
     *
     * @return
     */
    public boolean initializeDB() {
        if (databaseHelper == null) {
            databaseHelper = new DatabaseHelper(getContext(), DATABASE_NAME, null, DATABASE_VERSION, DATABASE_TABLES, TABLES_FIELDS);
        }
        if (database == null || !database.isOpen()) {
            database = databaseHelper.getWritableDatabase();
        }
        return (database != null && databaseHelper != null);
    }

    @Override
    public boolean onCreate() {
        //This is a hack to allow providers to be reusable in any application/plugin by making the authority dynamic using the package name of the parent app
        AUTHORITY = getContext().getPackageName() + ".provider.fitbit"; //make sure xxx matches the first string in this class
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

        //For each table, add indexes DIR and ITEM
        sUriMatcher.addURI(AUTHORITY, DATABASE_TABLES[0], FITBIT_DIR);
        sUriMatcher.addURI(AUTHORITY, DATABASE_TABLES[0] + "/#", FITBIT_ONE_ITEM);
        sUriMatcher.addURI(AUTHORITY, DATABASE_TABLES[1], FITBIT_DEVICES_DIR);
        sUriMatcher.addURI(AUTHORITY, DATABASE_TABLES[1] + "/#", FITBIT_DEVICES_ONE_ITEM);

        //Create each table hashmap so Android knows how to insert data to the database. Put ALL table fields.
        fitbitHash = new HashMap<>();
        fitbitHash.put(Fitbit_Data._ID, Fitbit_Data._ID);
        fitbitHash.put(Fitbit_Data.TIMESTAMP, Fitbit_Data.TIMESTAMP);
        fitbitHash.put(Fitbit_Data.DEVICE_ID, Fitbit_Data.DEVICE_ID);
        fitbitHash.put(Fitbit_Data.FITBIT_ID, Fitbit_Data.FITBIT_ID);
        fitbitHash.put(Fitbit_Data.DATA_TYPE, Fitbit_Data.DATA_TYPE);
        fitbitHash.put(Fitbit_Data.FITBIT_JSON, Fitbit_Data.FITBIT_JSON);

        fitbitDevicesHash = new HashMap<>();
        fitbitDevicesHash.put(Fitbit_Devices._ID, Fitbit_Devices._ID);
        fitbitDevicesHash.put(Fitbit_Devices.TIMESTAMP, Fitbit_Devices.TIMESTAMP);
        fitbitDevicesHash.put(Fitbit_Devices.DEVICE_ID, Fitbit_Devices.DEVICE_ID);
        fitbitDevicesHash.put(Fitbit_Devices.FITBIT_ID, Fitbit_Devices.FITBIT_ID);
        fitbitDevicesHash.put(Fitbit_Devices.FITBIT_VERSION, Fitbit_Devices.FITBIT_VERSION);
        fitbitDevicesHash.put(Fitbit_Devices.FITBIT_BATTERY, Fitbit_Devices.FITBIT_BATTERY);
        fitbitDevicesHash.put(Fitbit_Devices.LAST_SYNC, Fitbit_Devices.LAST_SYNC);

        return true;
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        if (!initializeDB()) {
            Log.w("", "Database unavailable...");
            return null;
        }

        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        switch (sUriMatcher.match(uri)) {

            //Add all tables' DIR entries, with the right table index
            case FITBIT_DIR:
                qb.setTables(DATABASE_TABLES[0]);
                qb.setProjectionMap(fitbitHash); //the hashmap of the table
                break;

            case FITBIT_DEVICES_DIR:
                qb.setTables(DATABASE_TABLES[1]);
                qb.setProjectionMap(fitbitDevicesHash); //the hashmap of the table
                break;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        //Don't change me
        try {
            Cursor c = qb.query(database, projection, selection, selectionArgs,
                    null, null, sortOrder);
            c.setNotificationUri(getContext().getContentResolver(), uri);
            return c;
        } catch (IllegalStateException e) {
            if (Aware.DEBUG)
                Log.e(Aware.TAG, e.getMessage());
            return null;
        }
    }

    @Nullable
    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {

            //Add each table indexes DIR and ITEM
            case FITBIT_DIR:
                return Fitbit_Data.CONTENT_TYPE;
            case FITBIT_ONE_ITEM:
                return Fitbit_Data.CONTENT_ITEM_TYPE;

            case FITBIT_DEVICES_DIR:
                return Fitbit_Devices.CONTENT_TYPE;
            case FITBIT_DEVICES_ONE_ITEM:
                return Fitbit_Devices.CONTENT_ITEM_TYPE;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues new_values) {
        if (!initializeDB()) {
            Log.w("", "Database unavailable...");
            return null;
        }

        ContentValues values = (new_values != null) ? new ContentValues(new_values) : new ContentValues();
        long _id;

        switch (sUriMatcher.match(uri)) {

            //Add each table DIR case
            case FITBIT_DIR:
                _id = database.insert(DATABASE_TABLES[0], Fitbit_Data.DEVICE_ID, values);
                if (_id > 0) {
                    Uri dataUri = ContentUris.withAppendedId(Fitbit_Data.CONTENT_URI, _id);
                    getContext().getContentResolver().notifyChange(dataUri, null);
                    return dataUri;
                }
                throw new SQLException("Failed to insert row into " + uri);

            case FITBIT_DEVICES_DIR:
                _id = database.insert(DATABASE_TABLES[1], Fitbit_Devices.DEVICE_ID, values);
                if (_id > 0) {
                    Uri dataUri = ContentUris.withAppendedId(Fitbit_Devices.CONTENT_URI, _id);
                    getContext().getContentResolver().notifyChange(dataUri, null);
                    return dataUri;
                }
                throw new SQLException("Failed to insert row into " + uri);

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        if (!initializeDB()) {
            Log.w("", "Database unavailable...");
            return 0;
        }

        int count;
        switch (sUriMatcher.match(uri)) {

            //Add each table DIR case
            case FITBIT_DIR:
                count = database.delete(DATABASE_TABLES[0], selection, selectionArgs);
                break;

            case FITBIT_DEVICES_DIR:
                count = database.delete(DATABASE_TABLES[1], selection, selectionArgs);
                break;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        if (!initializeDB()) {
            Log.w("", "Database unavailable...");
            return 0;
        }

        int count;
        switch (sUriMatcher.match(uri)) {

            //Add each table DIR case
            case FITBIT_DIR:
                count = database.update(DATABASE_TABLES[0], values, selection, selectionArgs);
                break;

            case FITBIT_DEVICES_DIR:
                count = database.update(DATABASE_TABLES[1], values, selection, selectionArgs);
                break;

            default:
                database.close();
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }
}