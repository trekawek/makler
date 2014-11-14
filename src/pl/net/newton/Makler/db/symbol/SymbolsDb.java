package pl.net.newton.Makler.db.symbol;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import pl.net.newton.Makler.R;
import pl.net.newton.Makler.db.Constants;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import static pl.net.newton.Makler.db.Constants.ID_EQUALS;
import static pl.net.newton.Makler.db.Constants.QUOTES;
import static pl.net.newton.Makler.db.Constants.SYMBOLS;
import static pl.net.newton.Makler.db.Constants.SYMBOL;

public class SymbolsDb {
	private static final String TAG = "MaklerSymbolsDb";

	private final Context ctx;

	private final SQLiteDatabase sqlDb;

	public SymbolsDb(SQLiteDatabase sqlDb, Context ctx) {
		this.ctx = ctx;
		this.sqlDb = sqlDb;
	}

	public boolean symbolsEmpty() {
		final boolean isEmpty;
		final Cursor c = sqlDb.query(Constants.SYMBOLS, null, null, null, null, null, null);
		isEmpty = c.moveToFirst();
		c.close();
		return isEmpty;
	}

	public Integer getSymbolId(String symbol) {
		Cursor c = sqlDb.query(SYMBOLS, new String[] { "id" }, "symbols.symbol = ? COLLATE NOCASE",
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
		List<Symbol> symbols = new ArrayList<Symbol>();
		Cursor c;
		if (StringUtils.isNoneBlank(name)) {
			String n = "%" + name + "%";
			c = sqlDb.query(SYMBOLS, null, "deleted = 0 AND (name LIKE ? OR symbol LIKE ?)", new String[] {
					n, n }, null, null, SYMBOL);
		} else {
			c = sqlDb.query(SYMBOLS, null, "deleted = 0", null, null, null, SYMBOL);
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
		c = sqlDb.query(SYMBOLS, null, "deleted = 0 AND symbol = ?", new String[] { symbol }, null, null,
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
		c = sqlDb.query(SYMBOLS, null, "deleted = 0 AND name = ?", new String[] { name }, null, null, null);
		if (c.moveToFirst()) {
			s = new SymbolBuilder().setFromCursor(c).build();
		}
		c.close();
		return s;
	}

	public Symbol findSymbolByName(String name) {
		final String wildcardName = new StringBuilder("%").append(name).append("%").toString();
		Cursor c;
		Symbol s = null;
		c = sqlDb.query(SYMBOLS, null, "deleted = 0 AND name LIKE ?", new String[] { wildcardName }, null,
				null, null);
		if (c.moveToFirst()) {
			s = new SymbolBuilder().setFromCursor(c).build();
		}
		c.close();
		return s;
	}

	public void updateSymbol(Symbol s) {
		sqlDb.beginTransaction();
		Cursor c = sqlDb.query(SYMBOLS, new String[] { "id" }, "symbol = ?", new String[] { s.getSymbol() },
				null, null, null);
		if (c.getCount() == 0) {
			sqlDb.insert(SYMBOLS, null, contentValues(s));
		} else {
			c.moveToFirst();
			Integer id = c.getInt(0);
			sqlDb.update(SYMBOLS, contentValues(s), ID_EQUALS, new String[] { id.toString() });
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
		sqlDb.update(SYMBOLS, cv, null, null);
		Cursor c = sqlDb.query(SYMBOLS, new String[] { "id", SYMBOL }, null, null, null, null, null);
		Map<String, Integer> symbolToId = new HashMap<String, Integer>();
		if (c.moveToFirst()) {
			do {
				symbolToId.put(c.getString(1), c.getInt(0));
			} while (c.moveToNext());
		}
		c.close();
		for (Symbol s : symbols) {
			if (symbolToId.containsKey(s.getSymbol())) {
				sqlDb.update(SYMBOLS, contentValues(s), ID_EQUALS,
						new String[] { symbolToId.get(s.getSymbol()).toString() });
			} else {
				sqlDb.insert(SYMBOLS, null, contentValues(s));
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
		c = sqlDb.query(QUOTES, new String[] { "id" }, "from_wallet = 1", null, null, null, null);
		while (c.moveToNext()) {
			int quoteId = c.getInt(0);
			Cursor wallet = sqlDb.query("wallet_items", new String[] { "id" }, "quote_id = ?",
					new String[] { String.valueOf(quoteId) }, null, null, null);
			if (!wallet.moveToFirst()) {
				sqlDb.delete(QUOTES, ID_EQUALS, new String[] { String.valueOf(quoteId) });
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
		sqlDb.delete(SYMBOLS, "id = ?", oldIdArgs);
		ContentValues cv = new ContentValues();
		cv.put("symbol_id", newId);
		sqlDb.update(QUOTES, cv, "symbol_id = ?", oldIdArgs);
		sqlDb.update("wallet_items", cv, "symbol_id = ?", oldIdArgs);
	}

	public ContentValues contentValues(Symbol symbol) {
		ContentValues cv = new ContentValues();
		cv.put(SYMBOL, symbol.getSymbol());
		cv.put("name", symbol.getName());
		cv.put("code", symbol.getCode());
		cv.put("is_index", symbol.isIndex() ? 1 : 0);
		cv.put("deleted", symbol.getDeleted() ? 1 : 0);
		return cv;
	}
}
