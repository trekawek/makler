package pl.net.newton.Makler.db.alert;

import java.util.ArrayList;
import java.util.List;

import pl.net.newton.Makler.db.quote.Quote;
import pl.net.newton.Makler.db.quote.QuoteField;
import pl.net.newton.Makler.db.quote.QuotesDao;
import pl.net.newton.Makler.gpw.ex.GpwException;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import static pl.net.newton.Makler.db.Constants.ALERTS;
import static pl.net.newton.Makler.db.Constants.ID_EQUALS;
import static pl.net.newton.Makler.db.DbHelper._;

public class AlertsDao {
	private static final String CAN_T_GET_ALERT = "Can't get alert";

	private static final String TAG = "MaklerAlertsDb";

	private final SQLiteDatabase sqlDb;

	private final QuotesDao quotesDb;

	public AlertsDao(SQLiteDatabase sqlDb, Context ctx) {
		this.sqlDb = sqlDb;
		this.quotesDb = new QuotesDao(sqlDb, ctx);
	}

	public boolean create(Alert a) {
		ContentValues cv = getContentValues(a);
		if (cv == null) {
			return false;
		} else {
			sqlDb.beginTransaction();
			sqlDb.insert(ALERTS, null, cv);
			sqlDb.setTransactionSuccessful();
			sqlDb.endTransaction();
			return true;
		}
	}

	public List<Alert> getAll() {
		List<Alert> alerts = new ArrayList<Alert>();
		Cursor c = sqlDb.query(ALERTS, null, null, null, null, null, null);
		if (c.moveToFirst()) {
			do {
				try {
					alerts.add(new AlertBuilder().setFromCursor(c, quotesDb).build());
				} catch (GpwException e) {
					Log.e(TAG, CAN_T_GET_ALERT, e);
				}
			} while (c.moveToNext());
		}
		c.close();
		return alerts;
	}

	public List<Alert> getByQuote(Quote quote) {
		List<Alert> alerts = new ArrayList<Alert>();
		Cursor c = sqlDb.query(ALERTS, null, "quote_id = ?", _(quote.get(QuoteField.ID)), null, null, null);
		if (c.moveToFirst()) {
			do {
				try {
					alerts.add(new AlertBuilder().setFromCursor(c, quotesDb).build());
				} catch (GpwException e) {
					Log.e(TAG, CAN_T_GET_ALERT, e);
				}
			} while (c.moveToNext());
		}
		c.close();
		return alerts;
	}

	public Alert getById(int id) {
		Cursor c = sqlDb.query(ALERTS, null, ID_EQUALS, _(String.valueOf(id)), null, null, null);
		Alert alert = null;
		if (c.moveToFirst()) {
			try {
				alert = new AlertBuilder().setFromCursor(c, quotesDb).build();
			} catch (GpwException e) {
				Log.e(TAG, CAN_T_GET_ALERT, e);
			}
		}
		c.close();
		return alert;
	}

	public void update(Alert a) {
		ContentValues cv = getContentValues(a);
		if (cv != null) {
			sqlDb.beginTransaction();
			sqlDb.update(ALERTS, cv, ID_EQUALS, new String[] { String.valueOf(a.getId()) });
			sqlDb.setTransactionSuccessful();
			sqlDb.endTransaction();
		}
	}

	public void markAsUsed(Alert alert) {
		sqlDb.beginTransaction();
		ContentValues cv = new ContentValues();
		cv.put("used", 1);
		sqlDb.update(ALERTS, cv, "id = ?", _(String.valueOf(alert.getId())));
		sqlDb.setTransactionSuccessful();
		sqlDb.endTransaction();
	}

	public void delete(int id) {
		sqlDb.beginTransaction();
		sqlDb.delete(ALERTS, ID_EQUALS, _(String.valueOf(id)));
		sqlDb.setTransactionSuccessful();
		sqlDb.endTransaction();
	}

	private ContentValues getContentValues(Alert a) {
		ContentValues cv = new ContentValues();
		cv.put("quote_id", a.getQuote().get(QuoteField.ID));
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
