package com.portapayments.android.database;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

public final class PaymentsProvider extends ContentProvider {

	/**
	 * The URL the categories provider covers
	 */
	
	private static final String CONTENT_URI_STRING = "content://com.portapayments.payments/";
	
	/**
	 * The type used for lists of applications
	 */
	
	private static final String MULTIPLE_ROWS_TYPE = "vnd.android.cursor.dir/vnd.com.portapayments.payments";
	
	/**
	 * The type used for a single of application
	 */
	
	private static final String SINGLE_ROW_TYPE = "vnd.android.cursor.item/vnd.com.portapayments.payments";
	
	/**
	 * The Uri object for the content URI.
	 */
	
	public static final Uri CONTENT_URI = Uri.parse(CONTENT_URI_STRING);

	/**
	 * Contstants for different URI request types
	 */
	
	private static final int	URI_ALL_ROWS = 1,
							 	URI_SINGLE_ROW = 2;
	
	/**
	 * The URI matcher to determine the URI request type.
	 */
	
	private static final UriMatcher URI_MATCHER;
	
	static {
		URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
		URI_MATCHER.addURI("com.portapayments.payments", null, URI_ALL_ROWS);
		URI_MATCHER.addURI("com.portapayments.payments", "#", URI_SINGLE_ROW);
	}
	
	/**
	 * The where clause for selecting by ID clause.
	 */
	
	private static final String SELECT_BY_ID_CLAUSE_PREFIX = "_id = ?";
	
	/**
	 * Access to the database
	 */

	private SQLiteDatabase database;
	
	/**
	 * Creation not implemented.
	 */
	
	@Override
	public boolean onCreate() {
		database = new DBHelper(getContext()).getWritableDatabase();
		return true;
	}

	@Override
	public String getType(final Uri uri) {
		switch(URI_MATCHER.match(uri)) {
		case URI_ALL_ROWS:
			return MULTIPLE_ROWS_TYPE;
		case URI_SINGLE_ROW:
			return SINGLE_ROW_TYPE;
		default:
			throw new IllegalArgumentException("Unsupported URI: "+uri);
		}
	}

	@Override
	public int delete(final Uri uri, final String clause, final String[] args) {
		final MyWhereClause whereClause = buildWhereClause(uri, clause, args);
		return database.delete(DBHelper.PAYMENTS_TABLE_NAME, whereClause.clause, whereClause.args);
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		switch(URI_MATCHER.match(uri)) {
		case URI_ALL_ROWS: 
		{
			long id = database.insert(DBHelper.PAYMENTS_TABLE_NAME, null, values);
			if(values.containsKey("_id")) {
				id = values.getAsInteger("_id");
			}
			Uri entryUri = ContentUris.withAppendedId(CONTENT_URI, id);
			getContext().getContentResolver().notifyChange(entryUri, null);
			return entryUri;
		}
		case URI_SINGLE_ROW:
		{
			int id = Integer.valueOf(uri.getPathSegments().get(0));
			values.put("_id", id);
			database.insert(DBHelper.PAYMENTS_TABLE_NAME, null, values);
			Uri entryUri = ContentUris.withAppendedId(CONTENT_URI, id);
			getContext().getContentResolver().notifyChange(entryUri, null);
			return entryUri;
		}
		default:
			throw new IllegalArgumentException("Unsupported URI: "+uri);
		}
	}

	@Override
	public Cursor query(final Uri uri, final String[] projection, final String clause,
			final String[] args, final String sortOrder) {
		final MyWhereClause whereClause = buildWhereClause(uri, clause, args);
		return database.query(DBHelper.PAYMENTS_TABLE_NAME, projection, 
				whereClause.clause, whereClause.args, null, null, sortOrder);
	}

	@Override
	public int update(final Uri uri, final ContentValues values, final String clause,
			final String[] args) {
		final MyWhereClause whereClause = buildWhereClause(uri, clause, args);
		int count = database.update(DBHelper.PAYMENTS_TABLE_NAME, values, 
				whereClause.clause, whereClause.args);
		getContext().getContentResolver().notifyChange(uri, null);		
		return count;
	}

	/**
	 * Build the where clause, adding in the ID if neccessary
	 */
	
	private MyWhereClause buildWhereClause(final Uri uri, final String clause, final String[] args) {
		switch(URI_MATCHER.match(uri)) {
		case URI_ALL_ROWS:
			return new MyWhereClause(clause, args);
		case URI_SINGLE_ROW:
			String newWhereClause;
			if( clause == null) {
				newWhereClause = SELECT_BY_ID_CLAUSE_PREFIX;
			} else {
				final StringBuilder newWhereClauseBuilder = 
					new StringBuilder(clause.length() + SELECT_BY_ID_CLAUSE_PREFIX.length() + 7);
				newWhereClauseBuilder.append(SELECT_BY_ID_CLAUSE_PREFIX);
				newWhereClauseBuilder.append(" AND (");
				newWhereClauseBuilder.append(clause);
				newWhereClauseBuilder.append(')');
				newWhereClause = newWhereClauseBuilder.toString();
			}
			
			String[] newArgs;
			if(args == null) {
				newArgs = new String[1];
				newArgs[0] = uri.getPathSegments().get(0);
			} else {
				newArgs = new String[args.length];
				newArgs[0] = uri.getPathSegments().get(0);
				for(int i = 0 ; i < args.length ; i++) {
					newArgs[i+1] = args[i];
				}				
			}
			
			return new MyWhereClause(newWhereClause, newArgs);
		default:
			throw new IllegalArgumentException("Unsupported URI: "+uri);
		}
	}
	
	/**
	 * Class holding details of the where clause
	 */
	private static final class MyWhereClause {
		private String clause;
		private String[] args;
		
		private MyWhereClause(final String clause, final String[] args) {
			this.clause = clause;
			this.args = args;
		}
	}
}
