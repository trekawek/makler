package pl.net.newton.Makler.db.quote;

import java.util.ArrayList;
import java.util.List;

import pl.net.newton.Makler.R;
import pl.net.newton.Makler.common.DateFormatUtils;
import pl.net.newton.Makler.db.DbHelper;
import pl.net.newton.Makler.db.symbol.SymbolsDb;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class QuotesDb {
	private final Context ctx;

	private final SQLiteDatabase sqlDb;

	private final SymbolsDb symbolsDb;

	public QuotesDb(SQLiteDatabase sqlDb, Context ctx) {
		this.ctx = ctx;
		this.sqlDb = sqlDb;
		this.symbolsDb = new SymbolsDb(sqlDb, ctx);
	}

	public List<Quote> getQuotes(boolean all) {
		ArrayList<Quote> quotes = new ArrayList<Quote>();
		String sql;
		if (all) {
			sql = ctx.getString(R.string.getAllQuotes);
		} else {
			sql = ctx.getString(R.string.getQuotes);
		}
		Cursor c = sqlDb.rawQuery(sql, null);
		if (c.moveToFirst()) {
			do
				quotes.add(new QuoteBuilder().setFromCursor(c).build());
			while (c.moveToNext());
		}
		c.close();
		return quotes;
	}

	public Quote getQuoteBySymbol(String symbol) {
		Cursor c = sqlDb.rawQuery(ctx.getString(R.string.quoteBySymbol), new String[] { symbol });
		if (!c.moveToFirst()) {
			return null;
		}
		Quote q = new QuoteBuilder().setFromCursor(c).build();
		c.close();
		return q;
	}

	public Quote getQuoteById(Integer id) {
		Cursor c = sqlDb.rawQuery(ctx.getString(R.string.quoteById), new String[] { id.toString() });
		if (!c.moveToFirst())
			return null;
		Quote q = null;
		try {
			q = new QuoteBuilder().setFromCursor(c).build();
		} finally {
			c.close();
		}
		return q;
	}

	public void addQuotes(String symbols) {
		String[] s = symbols.replaceAll(",", " ").split(" ");
		sqlDb.beginTransaction();
		Integer size = (int) sqlDb.compileStatement("SELECT COUNT(*) FROM quotes").simpleQueryForLong();
		Integer pos = size + 1;
		for (String symbol : s) {
			Integer symbolId = this.symbolsDb.getSymbolId(symbol);
			if (symbolId == null)
				continue;
			ContentValues cv = new ContentValues();
			cv.put("symbol_id", symbolId);
			cv.put("position", pos++);
			sqlDb.insert("quotes", null, cv);
		}
		sqlDb.setTransactionSuccessful();
		sqlDb.endTransaction();
	}

	public void updateQuote(Quote q) {
		sqlDb.beginTransaction();
		Integer symbolId = this.symbolsDb.getSymbolId(q.getSymbol());
		ContentValues cv = getContentValues(q);
		sqlDb.update("quotes", cv, "symbol_id = ?", new String[] { symbolId.toString() });
		sqlDb.setTransactionSuccessful();
		sqlDb.endTransaction();
	}

	public void deleteQuote(Integer id) {
		sqlDb.beginTransaction();
		sqlDb.delete("quotes", "id = ?", new String[] { id.toString() });
		sqlDb.delete("alerts", "quote_id = ?", new String[] { id.toString() });
		sqlDb.setTransactionSuccessful();
		sqlDb.endTransaction();
	}

	public void move(int id, boolean up) {
		DbHelper.move(sqlDb, "quotes", id, up);
	}

	private ContentValues getContentValues(Quote quote) {
		ContentValues cv = new ContentValues();
		DbHelper.putToCv(cv, "kurs", quote.getKurs());
		DbHelper.putToCv(cv, "zmiana", quote.getZmiana());
		DbHelper.putToCv(cv, "kurs_odn", quote.getKursOdn());
		DbHelper.putToCv(cv, "kurs_min", quote.getKursMin());
		DbHelper.putToCv(cv, "kurs_max", quote.getKursMax());
		DbHelper.putToCv(cv, "wartosc", quote.getWartosc());
		if (quote.getUpdate() != null)
			DbHelper.putToCv(cv, "`update`", DateFormatUtils.formatHhMmSs(quote.getUpdate()));
		else
			DbHelper.putToCv(cv, "`update`", null);
		DbHelper.putToCv(cv, "kurs_otw", quote.getKursOtw());
		DbHelper.putToCv(cv, "tko", quote.getTko());
		DbHelper.putToCv(cv, "tko_procent", quote.getTkoProcent());
		DbHelper.putToCv(cv, "wolumen", quote.getWolumen());

		DbHelper.putToCv(cv, "k_ofert", quote.getkOfert());
		DbHelper.putToCv(cv, "k_wol", quote.getkWol());
		DbHelper.putToCv(cv, "k_lim", quote.getkLim());
		DbHelper.putToCv(cv, "s_lim", quote.getsLim());
		DbHelper.putToCv(cv, "s_wol", quote.getsWol());
		DbHelper.putToCv(cv, "s_ofert", quote.getsOfert());
		return cv;
	}

}
