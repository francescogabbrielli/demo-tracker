package com.dtracker;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.SystemClock;
import android.provider.BaseColumns;

/**
 * Utility class for DB operations
 */
public class PathContract {

    private PathContract() {
    }

    public static class Path implements BaseColumns {
        public static final String TABLE_NAME = "path";
        public static final String COLUMN_NAME_BEGIN = "begin";
        public static final String COLUMN_NAME_END = "end";
    }

    public static class PathPoints implements BaseColumns {
        public static final String TABLE_NAME = "path_points";
        public static final String COLUMN_NAME_PATH_ID = "path_id";
        public static final String COLUMN_NAME_LAT = "lat";
        public static final String COLUMN_NAME_LNG = "lng";
        public static final String COLUMN_NAME_DIST = "dist";
    }

    private static final String SQL_CREATE_ENTRIES[] = new String[]{
            "CREATE TABLE " + Path.TABLE_NAME + " (" +
                    Path._ID + " INTEGER PRIMARY KEY," +
                    Path.COLUMN_NAME_BEGIN + " INTEGER," +
                    Path.COLUMN_NAME_END + " INTEGER)",
            "CREATE TABLE " + PathPoints.TABLE_NAME + " (" +
                    PathPoints._ID + " INTEGER PRIMARY KEY," +
                    PathPoints.COLUMN_NAME_PATH_ID + " INTEGER," +
                    PathPoints.COLUMN_NAME_LAT + " REAL," +
                    PathPoints.COLUMN_NAME_LNG + " REAL," +
                    PathPoints.COLUMN_NAME_DIST + " REAL)"
    };

    private static final String[] SQL_DELETE_ENTRIES = new String[]{
            "DROP TABLE IF EXISTS " + PathPoints.TABLE_NAME,
            "DROP TABLE IF EXISTS " + Path.TABLE_NAME
    };

    public static DbHelper createHelper(Context context) {
        return new DbHelper(context);
    }

    public static class DbHelper extends SQLiteOpenHelper {

        public static final int DATABASE_VERSION = 1;
        public static final String DATABASE_NAME = "Paths.db";

        public DbHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        public void onCreate(SQLiteDatabase db) {
            for (String sql : SQL_CREATE_ENTRIES)
                db.execSQL(sql);
        }

        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // This database is only a cache for online data, so its upgrade policy is
            // to simply to discard the data and start over.
            // TODO: Change this to upgrade without destroying the data
            for (String sql : SQL_DELETE_ENTRIES)
                db.execSQL(sql);
            onCreate(db);
        }

        public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            onUpgrade(db, oldVersion, newVersion);
        }

        /**
         * Add a new path
         *
         * @return
         *      the path id
         */
        public long addPath() {

            SQLiteDatabase db = getWritableDatabase();

            // Create a new map of values, where column names are the keys
            ContentValues values = new ContentValues();
            values.put(Path.COLUMN_NAME_BEGIN, SystemClock.elapsedRealtime());

            db.beginTransaction();
            try {
                // Insert the new row, returning the primary key value of the new row
                long id = db.insert(Path.TABLE_NAME, null, values);
                db.setTransactionSuccessful();
                return id;
            } finally {
                db.endTransaction();
            }

        }

        /**
         * Add a new point to a path
         *
         * @param id
         *              the path id
         * @param lat
         *              point latitude
         * @param lng
         *              point longitude
         * @param dist
         *              cumulative distance
         * @return
         *      the point id
         */
        public long addPoint(long id, double lat, double lng, float dist) {

            SQLiteDatabase db = getWritableDatabase();

            // Create a new map of values, where column names are the keys
            ContentValues values = new ContentValues();
            values.put(PathPoints.COLUMN_NAME_PATH_ID, id);
            values.put(PathPoints.COLUMN_NAME_LAT, lat);
            values.put(PathPoints.COLUMN_NAME_LNG, lng);
            values.put(PathPoints.COLUMN_NAME_DIST, dist);

            // Insert the new row, returning the primary key value of the new row
            db.beginTransaction();
            try {
                long pid = db.insert(PathPoints.TABLE_NAME, null, values);
                db.setTransactionSuccessful();
                return pid;
            } finally {
                db.endTransaction();
            }

        }
    }
    
}
