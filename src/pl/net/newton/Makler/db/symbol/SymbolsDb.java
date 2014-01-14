package pl.net.newton.Makler.db.symbol;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import pl.net.newton.Makler.R;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class SymbolsDb {
	private static final String TAG = "MaklerSymbolsDb";

	private final Context ctx;

	private final SQLiteDatabase sqlDb;

	public SymbolsDb(SQLiteDatabase sqlDb, Context ctx) {
		this.ctx = ctx;
		this.sqlDb = sqlDb;
	}

	public Boolean symbolsEmpty() {
		Cursor c = sqlDb.rawQuery(ctx.getString(R.string.symbolsEmpty), new String[] {});
		c.moveToFirst();
		Integer count = c.getInt(0);
		c.close();
		return count == 0;
	}

	public Integer getSymbolId(String symbol) {
		Cursor c = sqlDb.query("symbols", new String[] { "id" }, "symbols.symbol = ? COLLATE NOCASE",
				new String[] { symbol }, null, null, null);
		if (c.getCount() == 0) {
			return null;
		}
		c.moveToFirst();
		Integer symbolId = c.getInt(0);
		c.close();
		return symbolId;
	}

	public List<Symbol> getSymbols(String name) {
		ArrayList<Symbol> symbols = new ArrayList<Symbol>();
		Cursor c;
		if (StringUtils.isNoneBlank(name)) {
			String n = "%" + name + "%";
			c = sqlDb.query("symbols", null, "deleted = 0 AND (name LIKE ? OR symbol LIKE ?)", new String[] {
					n, n }, null, null, "symbol");
		} else {
			c = sqlDb.query("symbols", null, "deleted = 0", null, null, null, "symbol");
		}
		if (c.moveToFirst()) {
			do {
				symbols.add(new SymbolBuilder().setFromCursor(c).build());
			} while (c.moveToNext());
		}
		c.close();
		return symbols;
	}

	public Symbol getSymbolBySymbol(String symbol) {
		Cursor c;
		Symbol s = null;
		c = sqlDb.query("symbols", null, "deleted = 0 AND symbol = ?", new String[] { symbol }, null, null,
				null);
		if (c.moveToFirst()) {
			s = new SymbolBuilder().setFromCursor(c).build();
		}
		c.close();
		return s;
	}

	public Symbol getSymbolByName(String name) {
		Cursor c;
		Symbol s = null;
		c = sqlDb.query("symbols", null, "deleted = 0 AND name = ?", new String[] { name }, null, null, null);
		if (c.moveToFirst()) {
			s = new SymbolBuilder().setFromCursor(c).build();
		}
		c.close();
		return s;
	}

	public Symbol findSymbolByName(String name) {
		name = new StringBuilder("%").append(name).append("%").toString();
		Cursor c;
		Symbol s = null;
		c = sqlDb.query("symbols", null, "deleted = 0 AND name LIKE ?", new String[] { name }, null, null,
				null);
		if (c.moveToFirst()) {
			s = new SymbolBuilder().setFromCursor(c).build();
		}
		c.close();
		return s;
	}

	public void updateSymbol(Symbol s) {
		sqlDb.beginTransaction();
		Cursor c = sqlDb.query("symbols", new String[] { "id" }, "symbol = ?",
				new String[] { s.getSymbol() }, null, null, null);
		if (c.getCount() == 0) {
			sqlDb.insert("symbols", null, contentValues(s));
		} else {
			c.moveToFirst();
			Integer id = c.getInt(0);
			sqlDb.update("symbols", contentValues(s), "id = ?", new String[] { id.toString() });
		}
		c.close();
		sqlDb.setTransactionSuccessful();
		sqlDb.endTransaction();
	}

	public void updateSymbols(List<Symbol> symbols) {
		Log.d(TAG, "updating symbols - start");
		long start = System.currentTimeMillis();
		sqlDb.beginTransaction();
		ContentValues cv = new ContentValues();
		cv.put("deleted", 1);
		sqlDb.update("symbols", cv, null, null);
		Cursor c = sqlDb.query("symbols", new String[] { "id", "symbol" }, null, null, null, null, null);
		Map<String, Integer> symbolToId = new HashMap<String, Integer>();
		if (c.moveToFirst()) {
			do {
				symbolToId.put(c.getString(1), c.getInt(0));
			} while (c.moveToNext());
		}
		c.close();
		for (Symbol s : symbols) {
			if (symbolToId.containsKey(s.getSymbol())) {
				sqlDb.update("symbols", contentValues(s), "id = ?",
						new String[] { symbolToId.get(s.getSymbol()).toString() });
			} else {
				sqlDb.insert("symbols", null, contentValues(s));
			}
		}
		sqlDb.setTransactionSuccessful();
		sqlDb.endTransaction();

		// remove duplicates
		sqlDb.beginTransaction();
		String sql = ctx.getString(R.string.getSymbolDuplicates);
		c = sqlDb.rawQuery(sql, null);
		while (c.moveToNext()) {
			deleteSymbol(c.getInt(0), c.getInt(1));
		}
		c.close();

		// remove orphan quotes
		c = sqlDb.query("quotes", new String[] { "id" }, "from_wallet = 1", null, null, null, null);
		while (c.moveToNext()) {
			int quoteId = c.getInt(0);
			Cursor wallet = sqlDb.query("wallet_items", new String[] { "id" }, "quote_id = ?",
					new String[] { String.valueOf(quoteId) }, null, null, null);
			if (!wallet.moveToFirst()) {
				sqlDb.delete("quotes", "id = ?", new String[] { String.valueOf(quoteId) });
			}
			wallet.close();
		}
		c.close();

		sqlDb.setTransactionSuccessful();
		sqlDb.endTransaction();

		Log.d(TAG, "updating symbols took " + (System.currentTimeMillis() - start) / 1000 + " seconds");
	}

	private void deleteSymbol(int oldId, int newId) {
		Log.d(TAG, "oldId: " + oldId + ", newId: " + newId);
		String[] oldIdArgs = new String[] { String.valueOf(oldId) };
		sqlDb.delete("symbols", "id = ?", oldIdArgs);
		ContentValues cv = new ContentValues();
		cv.put("symbol_id", newId);
		sqlDb.update("quotes", cv, "symbol_id = ?", oldIdArgs);
		sqlDb.update("wallet_items", cv, "symbol_id = ?", oldIdArgs);
	}

	public ContentValues contentValues(Symbol symbol) {
		ContentValues cv = new ContentValues();
		cv.put("symbol", symbol.getSymbol());
		cv.put("name", symbol.getName());
		cv.put("code", symbol.getCode());
		cv.put("is_index", symbol.isIndex() ? 1 : 0);
		cv.put("deleted", symbol.getDeleted() ? 1 : 0);
		return cv;
	}
}
