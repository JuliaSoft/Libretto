package com.juliasoft.libretto.unused;

import java.util.Arrays;
import java.util.Collection;

import org.jsoup.helper.StringUtil;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DetailsDBAdapter {
	/*-------------------- USER DETAILS --------------------*/
	public static final String KEY_ROWID = "_id";
	public static final String KEY_STUDENT_ID = "freshman";
	public static final String KEY_NAME = "name";
	public static final String KEY_TYPE_OF_COURSE = "type";
	public static final String KEY_STUDENT_PROFILE = "profile";
	public static final String KEY_YEAR_OF_STUDY = "year";
	public static final String KEY_DATE_OF_REGISTRATION = "date";
	public static final String KEY_COURSE_OF_STUDY = "course";
	public static final String KEY_ORDER = "ordinament";
	public static final String KEY_ROUTE_OF_STUDY = "route";

	private static final String DATABASE_TABLE = "Details";

	private DatabaseHelper mDbHelper;
	private SQLiteDatabase mDb;

	private final Context mCtx;

	private static class DatabaseHelper extends SQLiteOpenHelper {

		public DatabaseHelper(Context context) {
			super(context, DBAdapter.DATABASE_NAME, null,
					DBAdapter.DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase arg0) {
		}

		@Override
		public void onUpgrade(SQLiteDatabase arg0, int arg1, int arg2) {
		}

	}

	/**
	 * Constructor - takes the context to allow the database to be
	 * opened/created
	 * 
	 * @param ctx
	 *            the Context within which to work
	 */
	public DetailsDBAdapter(Context context) {
		this.mCtx = context;
	}

	/**
	 * Open the details database. If it cannot be opened, try to create a new
	 * instance of the database. If it cannot be created, throw an exception to
	 * signal the failure
	 * 
	 * @return this (self reference, allowing this to be chained in an
	 *         initialization call)
	 * @throws SQLException
	 *             if the database could be neither opened or created
	 */
	public DetailsDBAdapter open() throws SQLException {
		this.mDbHelper = new DatabaseHelper(this.mCtx);
		this.mDb = this.mDbHelper.getWritableDatabase();
		return this;
	}

	/**
	 * close return type: void
	 */
	public void close() {
		this.mDbHelper.close();
	}

	public long createDetail(Object[] values) {
		
		if (values.length == 9) {
			return createDetail(values[0].toString(), values[1].toString(), values[2].toString(), values[3].toString(),
					values[4].toString(), values[5].toString(), values[6].toString(), values[7].toString(), values[8].toString());
		}
		return -1;
	}

	/**
	 * Create a new detail. If the detail is successfully created return the new
	 * rowId for that detail, otherwise return a -1 to indicate failure.
	 * 
	 * @param freshman
	 *            Student id
	 * @param name
	 *            Student name
	 * @param type
	 * @param profile
	 * @param year
	 * @param date
	 * @param course
	 * @param route
	 * @return rowId or -1 if failed
	 */
	public long createDetail(String freshman, 
							 String name, 
							 String type,
							 String profile, 
							 String year, 
							 String date, 
							 String course,
							 String order,
							 String route) {
		
		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_STUDENT_ID, freshman);
		initialValues.put(KEY_NAME, name);
		initialValues.put(KEY_TYPE_OF_COURSE, type);
		initialValues.put(KEY_STUDENT_PROFILE, profile);
		initialValues.put(KEY_YEAR_OF_STUDY, year);
		initialValues.put(KEY_DATE_OF_REGISTRATION, date);
		initialValues.put(KEY_COURSE_OF_STUDY, course);
		initialValues.put(KEY_ORDER, route);
		initialValues.put(KEY_ROUTE_OF_STUDY, route);
		return this.mDb.insert(DATABASE_TABLE, null, initialValues);
	}
	
	 /**
     * Delete the detail with the given rowId
     * 
     * @param rowId
     * @return true if deleted, false otherwise
     */
    public boolean deleteCar(long rowId) {
        return this.mDb.delete(DATABASE_TABLE, KEY_ROWID + "=" + rowId, null) > 0; //$NON-NLS-1$
    }
    
    /**
     * Return a Cursor over the list of all detail in the database
     * 
     * @return Cursor over all details
     */
    public Cursor getAllDetails() {

        return this.mDb.query(DATABASE_TABLE, new String[] { 
        		KEY_ROWID,
                KEY_STUDENT_ID, 
                KEY_NAME, 
                KEY_TYPE_OF_COURSE, 
                KEY_STUDENT_PROFILE,
                KEY_YEAR_OF_STUDY, 
                KEY_DATE_OF_REGISTRATION, 
                KEY_COURSE_OF_STUDY, 
                KEY_ORDER,
                KEY_ROUTE_OF_STUDY},
                null, null, null, null, null);
    }
    
    /**
     * Update the detail.
     * 
     * @param rowId
     * @param freshman Student id
	 * @param name Student name
	 * @param type
	 * @param profile
	 * @param year
	 * @param date
	 * @param course
	 * @param route
     * @return true if the note was successfully updated, false otherwise
     */
    public boolean updateCar(long rowId, 
    						 String freshman, 
    						 String name, 
    						 String type,
    						 String profile, 
    						 String year, 
    						 String date, 
    						 String course,
    						 String order,
    						 String route){
    	
        ContentValues args = new ContentValues();
        args.put(KEY_STUDENT_ID, freshman);
		args.put(KEY_NAME, name);
		args.put(KEY_TYPE_OF_COURSE, type);
		args.put(KEY_STUDENT_PROFILE, profile);
		args.put(KEY_YEAR_OF_STUDY, year);
		args.put(KEY_DATE_OF_REGISTRATION, date);
		args.put(KEY_COURSE_OF_STUDY, course);
		args.put(KEY_ORDER, course);
		args.put(KEY_ROUTE_OF_STUDY, route);

        return this.mDb.update(DATABASE_TABLE, args, KEY_ROWID + "=" + rowId, null) >0; 
    }
}
