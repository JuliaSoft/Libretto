package com.juliasoft.libretto.unused;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBAdapter {
	private static final String TAG = DBAdapter.class.getName();
	public static final String DATABASE_NAME = "MyDB";
	public static final int DATABASE_VERSION = 1;

	private static final String CREATE_TABLE_DETAILS = "create table details (_id integer primary key autoincrement, " //$NON-NLS-1$
			+ DetailsDBAdapter.KEY_STUDENT_ID + " TEXT," //$NON-NLS-1$
			+ DetailsDBAdapter.KEY_NAME + " TEXT," //$NON-NLS-1$
			+ DetailsDBAdapter.KEY_TYPE_OF_COURSE + " TEXT," //$NON-NLS-1$
			+ DetailsDBAdapter.KEY_STUDENT_PROFILE + " TEXT," //$NON-NLS-1$
			+ DetailsDBAdapter.KEY_YEAR_OF_STUDY + " TEXT," //$NON-NLS-1$
			+ DetailsDBAdapter.KEY_DATE_OF_REGISTRATION + " TEXT," //$NON-NLS-1$
			+ DetailsDBAdapter.KEY_COURSE_OF_STUDY + " TEXT," //$NON-NLS-1$
			+ DetailsDBAdapter.KEY_ORDER + " TEXT,"
			+ DetailsDBAdapter.KEY_ROUTE_OF_STUDY + " TEXT" + ");"; //$NON-NLS-1$ //$NON-NLS-2$

	private final Context context;
	private static DatabaseHelper DBHelper;
	private static SQLiteDatabase db;

	public DBAdapter(Context context) {
		this.context = context;
	}

	/**
	 * open the db
	 * 
	 * @return this
	 * @throws SQLException
	 *             return type: DBAdapter
	 */
	public void open() {
		DBHelper = new DatabaseHelper(this.context);
		if (!isOpen()) {
			db = DBHelper.getWritableDatabase();
		}
	}

	public boolean isOpen() {
		return db != null && db.isOpen();
	}

	private void checkDbState() {
		if (db == null || !db.isOpen()) {
			throw new IllegalStateException("The database has not been opened");
		}
	}

	/**
	 * close the db return type: void
	 */
	public void close() {
		if (isOpen()) {
			db.close();
			db = null;
			if (DBHelper != null) {
				DBHelper.close();
				DBHelper = null;
			}
		}
	}
	
	public boolean delete(){
		return context.deleteDatabase(DATABASE_NAME);
	}

	private static class DatabaseHelper extends SQLiteOpenHelper {

		public DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(CREATE_TABLE_DETAILS);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			// TODO Auto-generated method stub

		}
	}
}
