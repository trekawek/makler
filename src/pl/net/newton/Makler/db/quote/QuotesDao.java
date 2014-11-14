package pl.net.newton.Makler.db.quote;

import java.util.ArrayList;
import java.util.List;

import pl.net.newton.Makler.R;
import pl.net.newton.Makler.db.DbHelper;
import pl.net.newton.Makler.db.symbol.SymbolsDb;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import static pl.net.newton.Makler.db.Constants.QUOTES;

public class QuotesDao {

	private final Context ctx;

	private final SQLiteDatabase sqlDb;

	private final SymbolsDb symbolsDb;

	public QuotesDao(SQLiteDatabase sqlDb, Context ctx) {
		this.ctx = ctx;
		this.sqlDb = sqlDb;
		this.symbolsDb = new SymbolsDb(sqlDb, ctx);
	}

	public List<Quote> getQuotes(boolean all) {
		List<Quote> quotes = new ArrayList<Quote>();
		String sql;
		if (all) {
			sql = ctx.getString(R.string.getAllQuotes);
		} else {
			sql = ctx.getString(R.string.getQuotes);
		}
		Cursor c = sqlDb.rawQuery(sql, null);
		if (c.moveToFirst()) {
			do {
				quotes.add(new Quote(c));
			} while (c.moveToNext());
		}
		c.close();
		return quotes;
	}

	public Quote getQuoteBySymbol(String symbol) {
		Cursor c = sqlDb.rawQuery(ctx.getString(R.string.quoteBySymbol), new String[] { symbol });
		if (!c.moveToFirst()) {
			return null;
		}
		Quote q = new Quote(c);
		c.close();
		return q;
	}

	public Quote getQuoteById(Integer id) {
		Cursor c = sqlDb.rawQuery(ctx.getString(R.string.quoteById), new String[] { id.toString() });
		try {
			if (!c.moveToFirst()) {
				return null;
			}
			return new Quote(c);
		} finally {
			c.close();
		}
	}

	public void addQuotes(String symbols) {
		String[] s = symbols.replaceAll(",", " ").split(" ");
		sqlDb.beginTransaction();
		Integer size = (int) sqlDb.compileStatement("SELECT COUNT(*) FROM quotes").simpleQueryForLong();
		Integer pos = size + 1;
		for (String symbol : s) {
			Integer symbolId = this.symbolsDb.getSymbolId(symbol);
			if (symbolId == null) {
				continue;
			}
			ContentValues cv = new ContentValues();
			cv.put("symbol_id", symbolId);
			cv.put("position", pos++);
			sqlDb.insert(QUOTES, null, cv);
		}
		sqlDb.setTransactionSuccessful();
		sqlDb.endTransaction();
	}

	public void updateQuote(Quote q) {
		sqlDb.beginTransaction();
		int symbolId = this.symbolsDb.getSymbolId(q.get(QuoteField.SYMBOL));
		ContentValues cv = q.getContentValue();
		sqlDb.update(QUOTES, cv, "symbol_id = ?", new String[] { Integer.toString(symbolId) });
		sqlDb.setTransactionSuccessful();
		sqlDb.endTransaction();
	}

	public void deleteQuote(Integer id) {
		sqlDb.beginTransaction();
		sqlDb.delete(QUOTES, "id = ?", new String[] { id.toString() });
		sqlDb.delete("alerts", "quote_id = ?", new String[] { id.toString() });
		sqlDb.setTransactionSuccessful();
		sqlDb.endTransaction();
	}

	public void move(int id, boolean up) {
		DbHelper.move(sqlDb, QUOTES, id, up);
	}
}
