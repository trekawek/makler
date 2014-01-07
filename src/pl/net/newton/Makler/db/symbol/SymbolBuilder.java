package pl.net.newton.Makler.db.symbol;

import android.database.Cursor;

public class SymbolBuilder {
	private Integer id;

	private String symbol;

	private String name;

	private boolean isIndex;

	private boolean deleted;

	private String code;

	public Symbol build() {
		return new Symbol(this);
	}

	public SymbolBuilder setFromCursor(Cursor c) {
		this.id = c.getInt(c.getColumnIndex("id"));
		this.symbol = c.getString(c.getColumnIndex("symbol"));
		this.name = c.getString(c.getColumnIndex("name"));
		this.code = c.getString(c.getColumnIndex("code"));
		this.isIndex = c.getInt(c.getColumnIndex("is_index")) == 1 ? true : false;
		this.deleted = c.getInt(c.getColumnIndex("deleted")) == 1 ? true : false;
		return this;
	}

	public SymbolBuilder setId(Integer id) {
		this.id = id;
		return this;
	}

	public SymbolBuilder setSymbol(String symbol) {
		this.symbol = symbol;
		return this;
	}

	public SymbolBuilder setName(String name) {
		this.name = name;
		return this;
	}

	public SymbolBuilder setIsIndex(boolean isIndex) {
		this.isIndex = isIndex;
		return this;
	}

	public SymbolBuilder setDeleted(boolean deleted) {
		this.deleted = deleted;
		return this;
	}

	public SymbolBuilder setCode(String code) {
		this.code = code;
		return this;
	}

	Integer getId() {
		return id;
	}

	String getSymbol() {
		return symbol;
	}

	String getName() {
		return name;
	}

	
	boolean getIsIndex() {
		return isIndex;
	}

	boolean getDeleted() {
		return deleted;
	}

	String getCode() {
		return code;
	}
}
