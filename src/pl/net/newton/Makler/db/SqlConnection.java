package pl.net.newton.Makler.db;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import pl.net.newton.Makler.R;
import pl.net.newton.Makler.common.Configuration;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import static pl.net.newton.Makler.db.Constants.ID_EQUALS;
import static pl.net.newton.Makler.db.Constants.QUOTES;
import static pl.net.newton.Makler.db.Constants.SYMBOL_ID;

public class SqlConnection extends SQLiteOpenHelper {

	private static final String TAG = "MaklerSql";

	private static final int DATABASE_VERSION = 7;

	private static final String DATABASE_NAME = "makler.db";

	private static final boolean COPY_DB = true;

	private final File dataBaseFile;

	private Context context;

	public SqlConnection(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		this.context = context;
		this.dataBaseFile = context.getDatabasePath(DATABASE_NAME);
	}

	public SQLiteDatabase getDb() {
		try {
			if (!isDatabaseExist() && COPY_DB) {
				Log.d(TAG, "copying db");
				copyDatabase();

				Configuration config = new Configuration(context);
				config.setLastSymbolsUpdated("2011-11-03");
			}
			return getWritableDatabase();
		} catch (IOException e) {
			Log.e(TAG, "can't get database", e);
			return null;
		}
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		String[] createDB = context.getResources().getStringArray(pl.net.newton.Makler.R.array.createDB);
		try {
			for (String q : createDB) {
				db.execSQL(q);
			}
			onUpgrade(db, 1, DATABASE_VERSION);
		} catch (SQLException e) {
			Log.e(TAG, "can't create database", e);
		}
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int from, int to) {
		if (from <= 1 && to >= 2) {
			Log.w(TAG, "Akutalizacja do schematu 2.");
			String[] update = context.getResources().getStringArray(R.array.update_1_2);
			for (String q : update) {
				db.execSQL(q);
			}
		}
		if (from <= 2 && to >= 3) {
			Log.w(TAG, "Akutalizacja do schematu 3.");
			String[] update = context.getResources().getStringArray(R.array.update_2_3);
			for (String q : update) {
				db.execSQL(q);
			}

			int i = 1;
			db.beginTransaction();
			Cursor c = db.query(QUOTES, new String[] { "id" }, null, null, null, null, "id ASC");
			if (c.moveToFirst()) {
				do {
					ContentValues cv = new ContentValues();
					cv.put("position", i++);
					db.update(QUOTES, cv, ID_EQUALS, new String[] { String.valueOf(c.getInt(0)) });
				} while (c.moveToNext());
			}
			c.close();
			db.setTransactionSuccessful();
			db.endTransaction();
		}
		if (from <= 3 && to >= 4) {
			Log.w(TAG, "Akutalizacja do schematu 4.");
			String[] update = context.getResources().getStringArray(R.array.update_3_4);
			for (String q : update) {
				db.execSQL(q);
			}
		}
		if (from <= 4 && to >= 5) {
			Log.w(TAG, "Akutalizacja do schematu 5.");
			String[] update = context.getResources().getStringArray(R.array.update_4_5);
			for (String q : update) {
				db.execSQL(q);
			}

			db.beginTransaction();
			Cursor c = db.query("wallet_items", new String[] { "id", SYMBOL_ID }, null, null, null, null,
					"id ASC");
			if (c.moveToFirst()) {
				do {
					ContentValues cv = new ContentValues();
					cv.put(SYMBOL_ID, c.getInt(c.getColumnIndex(SYMBOL_ID)));
					cv.put("from_wallet", 1);
					long quoteId = db.insert(QUOTES, null, cv);

					cv = new ContentValues();
					cv.put("quote_id", quoteId);
					db.update("wallet_items", cv, "id = ?",
							new String[] { String.valueOf(c.getInt(c.getColumnIndex("id"))) });
				} while (c.moveToNext());
			}
			c.close();
			db.setTransactionSuccessful();
			db.endTransaction();
		}
		if (from <= 5 && to >= 6) {
			Log.w(TAG, "Akutalizacja do schematu 6.");
			String[] update = context.getResources().getStringArray(R.array.update_5_6);
			for (String q : update) {
				db.execSQL(q);
			}
		}
		if (from <= 6 && to >= 7) {
			Log.w(TAG, "Akutalizacja do schematu 7.");
			String[] update = context.getResources().getStringArray(R.array.update_6_7);
			for (String q : update) {
				db.execSQL(q);
			}
		}
	}

	private boolean isDatabaseExist() {
		return dataBaseFile.exists();
	}

	private void copyDatabase() throws IOException {
		File parent = dataBaseFile.getParentFile();
		if (!parent.exists() && !parent.mkdir()) {
			throw new IOException("can't create dir " + parent.getAbsolutePath());
		}

		InputStream is = null;
		OutputStream os = null;

		try {
			is = context.getAssets().open(DATABASE_NAME);
			os = new FileOutputStream(dataBaseFile);
			copy(is, os);
		} finally {
			if (is != null) {
				is.close();
			}
			if (os != null) {
				os.close();
			}
		}
	}

	private static int copy(InputStream input, OutputStream output) throws IOException {
		byte[] buffer = new byte[1024];
		int count = 0;
		int n = 0;
		while (-1 != (n = input.read(buffer))) {
			output.write(buffer, 0, n);
			count += n;
		}
		return count;
	}

}
