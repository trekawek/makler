package pl.net.newton.Makler.db.wallet;

import java.util.ArrayList;
import java.util.List;

import pl.net.newton.Makler.R;
import pl.net.newton.Makler.db.DbHelper;
import pl.net.newton.Makler.db.quote.QuotesDb;
import pl.net.newton.Makler.db.symbol.Symbol;
import pl.net.newton.Makler.db.symbol.SymbolsDb;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class WalletDb {
	private final Context ctx;

	private final SQLiteDatabase sqlDb;

	private final SymbolsDb symbolsDb;

	private final QuotesDb quotesDb;

	public WalletDb(SQLiteDatabase sqlDb, Context ctx) {
		this.ctx = ctx;
		this.sqlDb = sqlDb;
		this.symbolsDb = new SymbolsDb(sqlDb, ctx);
		this.quotesDb = new QuotesDb(sqlDb, ctx);
	}

	public List<WalletItem> getWalletItems() {
		List<WalletItem> items = new ArrayList<WalletItem>();
		Cursor c = sqlDb.rawQuery(ctx.getString(R.string.getWalletItems), null);
		if (c.moveToFirst()) {
			do {
				items.add(new WalletItemBuilder().setFromCursor(c, quotesDb).build());
			} while (c.moveToNext());
		}
		c.close();
		return items;
	}

	public WalletItem getWalletItem(Symbol s) {
		WalletItem w = null;
		Cursor c = sqlDb.rawQuery(ctx.getString(R.string.getWalletItemBySymbol), new String[] { s.getId()
				.toString() });
		if (!c.moveToFirst()) {
			w = new WalletItemBuilder().setFromSymbol(s).build();
		} else {
			w = new WalletItemBuilder().setFromCursor(c, quotesDb).build();
		}
		c.close();
		return w;
	}

	public void updateWalletItem(WalletItem w) {
		sqlDb.beginTransaction();
		if (w.getId() == null) {
			Symbol s = symbolsDb.getSymbolBySymbol(w.getSymbol());

			ContentValues cv = new ContentValues();
			cv.put("symbol_id", s.getId());
			cv.put("from_wallet", 1);
			long quoteId = sqlDb.insert("quotes", null, cv);

			Integer size = (int) sqlDb.compileStatement("SELECT COUNT(*) FROM wallet_items")
					.simpleQueryForLong();
			cv = getContentValues(w);
			cv.put("position", size + 1);
			// cv.put("symbol_id", s.getId());
			cv.put("quote_id", quoteId);
			Integer id = (int) sqlDb.insert("wallet_items", null, cv);
			w.setId(id);
		} else
			sqlDb.update("wallet_items", getContentValues(w), "id = ?", new String[] { w.getId().toString() });

		if (w.getQuantity() == 0)
			sqlDb.delete("wallet_items", "id = ?", new String[] { w.getId().toString() });

		sqlDb.setTransactionSuccessful();
		sqlDb.endTransaction();
	}

	public void deleteWalletItem(Integer id) {
		sqlDb.beginTransaction();
		Cursor c = sqlDb.query("wallet_items", new String[] { "quote_id" }, "id = ?",
				new String[] { id.toString() }, null, null, null);
		c.moveToFirst();
		int quoteId = c.getInt(0);
		sqlDb.delete("quotes", "id = ?", new String[] { String.valueOf(quoteId) });
		sqlDb.delete("wallet_items", "id = ?", new String[] { id.toString() });
		sqlDb.setTransactionSuccessful();
		sqlDb.endTransaction();
	}

	public void move(int id, boolean up) {
		DbHelper.move(sqlDb, "wallet_items", id, up);
	}

	public ContentValues getContentValues(WalletItem w) {
		ContentValues cv = new ContentValues();
		if (w.getId() != null) {
			DbHelper.putToCv(cv, "id", w.getId());
		}
		DbHelper.putToCv(cv, "quantity", w.getQuantity());
		DbHelper.putToCv(cv, "avg_buy", w.getAvgBuy());
		DbHelper.putToCv(cv, "quote", w.getQuote());
		DbHelper.putToCv(cv, "total_commision", w.getTotalCommision());
		return cv;
	}
}
