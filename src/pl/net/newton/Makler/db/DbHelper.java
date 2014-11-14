package pl.net.newton.Makler.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import static pl.net.newton.Makler.db.Constants.ID_EQUALS;
import static pl.net.newton.Makler.db.Constants.POSITION;

public final class DbHelper {

	private DbHelper() {
	}

	public static void move(SQLiteDatabase sqlDb, String table, Integer id, boolean up) {
		sqlDb.beginTransaction();
		Cursor c = sqlDb.query(table, new String[] { "id", POSITION }, ID_EQUALS,
				new String[] { id.toString() }, null, null, null);
		c.moveToFirst();
		int currentPos = c.getInt(1);
		c.close();

		StringBuilder cond = new StringBuilder();
		String order;
		if (up) {
			cond.append("position < ?");
			order = "position DESC";
		} else {
			cond.append("position > ?");
			order = "position ASC";
		}
		if ("quotes".equals(table)) {
			cond.append(" AND from_wallet=0");
		}
		c = sqlDb.query(table, new String[] { "id", POSITION }, cond.toString(),
				new String[] { String.valueOf(currentPos) }, null, null, order);
		if (!c.moveToFirst()) {
			c.close();
			sqlDb.endTransaction();
			return;
		}
		int prevId = c.getInt(0);
		int prevPos = c.getInt(1);
		c.close();

		ContentValues cv = new ContentValues();
		cv.put(POSITION, currentPos);
		sqlDb.update(table, cv, ID_EQUALS, new String[] { String.valueOf(prevId) });

		cv = new ContentValues();
		cv.put(POSITION, prevPos);
		sqlDb.update(table, cv, ID_EQUALS, new String[] { id.toString() });
		sqlDb.setTransactionSuccessful();
		sqlDb.endTransaction();
	}

	public static void putToCv(ContentValues cv, String name, Object value) {
		if (value == null) {
			cv.putNull(name);
		} else {
			cv.put(name, value.toString());
		}
	}

	public static String[] _(String... args) {
		return args;
	}
}