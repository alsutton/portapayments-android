package com.portapayments.android.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public final class DBHelper 
	extends SQLiteOpenHelper {

	/**
	 * The name of the payments table
	 */
	
	public static final String PAYMENTS_TABLE_NAME = "payments";

	/**
	 * The current schema version number
	 */
	
	private static final int SCHEMA_VERSION = 1;

	/**
	 * Create SQL Clause
	 */
	
	private static final String CREATE_STATEMENT = 
		"CREATE TABLE IF NOT EXISTS payments( _id integer primary key autoincrement, timestamp INT(8), scannedData TEXT, payKey TEXT )";
	
	
	/**
	 * The SQL to create the required indexes.
	 */
	
	private static final String INDEX_STATEMENT =
		"CREATE INDEX payKey_idx ON payments(payKey)";

	 /**
	  * Constructor.
	  */
	
	 public DBHelper(final Context context) {
		 super(context, "PortaPayments", null, DBHelper.SCHEMA_VERSION);
	 }
	 
	@Override
	public void onCreate(final SQLiteDatabase database) {
		database.execSQL(CREATE_STATEMENT);
		database.execSQL(INDEX_STATEMENT);
	}

	@Override
	public void onUpgrade(final SQLiteDatabase database, final int oldVersion, final int newVersion) {
		onCreate(database);
	}	 
}
