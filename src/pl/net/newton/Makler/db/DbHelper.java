package pl.net.newton.Makler.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class DbHelper {
	public static void move(SQLiteDatabase sqlDb, String table, Integer id, boolean up) {
		sqlDb.beginTransaction();
		Cursor c = sqlDb.query(table, new String[] { "id", "position" }, "id = ?",
				new String[] { id.toString() }, null, null, null);
		c.moveToFirst();
		Integer currentPos = c.getInt(1);

		String cond = "";
		if (table.equals("quotes")) {
			cond = " AND from_wallet=0";
		}
		if (up) {
			c = sqlDb.query(table, new String[] { "id", "position" }, "position < ?" + cond,
					new String[] { currentPos.toString() }, null, null, "position DESC");
		} else {
			c = sqlDb.query(table, new String[] { "id", "position" }, "position > ?" + cond,
					new String[] { currentPos.toString() }, null, null, "position ASC");
		}
		if (!c.moveToFirst()) {
			c.close();
			sqlDb.endTransaction();
			return;
		}
		Integer prevId = c.getInt(0);
		Integer prevPos = c.getInt(1);
		c.close();

		ContentValues cv = new ContentValues();
		cv.put("position", currentPos);
		sqlDb.update(table, cv, "id = ?", new String[] { prevId.toString() });

		cv = new ContentValues();
		cv.put("position", prevPos);
		sqlDb.update(table, cv, "id = ?", new String[] { id.toString() });
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
}