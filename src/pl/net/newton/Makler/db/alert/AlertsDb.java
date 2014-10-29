package pl.net.newton.Makler.db.alert;

import java.util.ArrayList;
import java.util.List;

import pl.net.newton.Makler.db.quote.Quote;
import pl.net.newton.Makler.db.quote.QuotesDb;
import pl.net.newton.Makler.gpw.ex.GpwException;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class AlertsDb {
	private static final String TAG = "MaklerAlertsDb";

	private final SQLiteDatabase sqlDb;

	private final QuotesDb quotesDb;

	public AlertsDb(SQLiteDatabase sqlDb, Context ctx) {
		this.sqlDb = sqlDb;
		this.quotesDb = new QuotesDb(sqlDb, ctx);
	}

	public List<Alert> getAlerts() {
		List<Alert> alerts = new ArrayList<Alert>();
		Cursor c = sqlDb.query("alerts", null, null, null, null, null, null);
		if (c.moveToFirst()) {
			do {
				try {
					alerts.add(new AlertBuilder().setFromCursor(c, quotesDb).build());
				} catch (GpwException e) {
					Log.e(TAG, "Can't get alert", e);
				}
			} while (c.moveToNext());
		}
		c.close();
		return alerts;
	}

	public void markAlertAsUsed(Alert alert) {
		sqlDb.beginTransaction();
		ContentValues cv = new ContentValues();
		cv.put("used", 1);
		sqlDb.update("alerts", cv, "id = ?", new String[] { String.valueOf(alert.getId()) });
		sqlDb.setTransactionSuccessful();
		sqlDb.endTransaction();
	}

	public List<Alert> alertsByQuote(Quote quote) {
		ArrayList<Alert> alerts = new ArrayList<Alert>();
		Cursor c = sqlDb.query("alerts", null, "quote_id = ?", new String[] { quote.getId().toString() },
				null, null, null);
		if (c.moveToFirst()) {
			do {
				try {
					alerts.add(new AlertBuilder().setFromCursor(c, quotesDb).build());
				} catch (GpwException e) {
					Log.e(TAG, "Can't get alert", e);
				}
			} while (c.moveToNext());
		}
		c.close();
		return alerts;
	}

	public void deleteAlert(Integer id) {
		sqlDb.beginTransaction();
		sqlDb.delete("alerts", "id = ?", new String[] { id.toString() });
		sqlDb.setTransactionSuccessful();
		sqlDb.endTransaction();
	}

	public boolean addAlert(Alert a) {
		ContentValues cv = getContentValues(a);
		if (cv == null) {
			return false;
		} else {
			sqlDb.beginTransaction();
			sqlDb.insert("alerts", null, cv);
			sqlDb.setTransactionSuccessful();
			sqlDb.endTransaction();
			return true;
		}
	}

	public Alert getAlertById(int id) {
		Cursor c = sqlDb.query("alerts", null, "id=?", new String[] { String.valueOf(id) }, null, null, null);
		Alert alert = null;
		if (c.moveToFirst()) {
			try {
				alert = new AlertBuilder().setFromCursor(c, quotesDb).build();
			} catch (GpwException e) {
				Log.e(TAG, "Can't get alert", e);
			}
		}
		c.close();
		return alert;
	}

	public void updateAlert(Alert a) {
		ContentValues cv = getContentValues(a);
		if (cv != null) {
			sqlDb.beginTransaction();
			sqlDb.update("alerts", cv, "id=?", new String[] { String.valueOf(a.getId()) });
			sqlDb.setTransactionSuccessful();
			sqlDb.endTransaction();
		}
	}

	private ContentValues getContentValues(Alert a) {
		ContentValues cv = new ContentValues();
		cv.put("quote_id", a.getQuote().getId());
		cv.put("subject", a.getSubject().toString());
		cv.put("event", a.getEvent().toString());
		cv.put("value", a.getAlertValue().getValue().toString());
		cv.put("percent", a.getAlertValue().isPercent() ? 1 : 0);
		if (a.getAlertValue().getBaseValue() != null) {
			cv.put("base_value", a.getAlertValue().getBaseValue().toString());
		} else if (!a.getEvent().isBaseValueRequired()) {
			cv.put("base_value", "0");
		} else {
			return null;
		}
		cv.put("used", a.getUsed() ? 1 : 0);
		return cv;
	}
}
