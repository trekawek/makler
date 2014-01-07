package pl.net.newton.Makler.db.symbol;

public class Symbol {
	private final Integer id;

	private final String symbol;

	private final String name;

	private final Boolean isIndex;

	private boolean deleted;

	private final String code;

	/*
	 * public Symbol(String symbol, String name, Boolean isIndex, String code) { this.symbol = symbol;
	 * this.name = name; this.isIndex = isIndex; this.deleted = false; this.code = code; }
	 * 
	 * public Symbol(String s) { String[] a = s.split("\\|"); this.symbol = a[0]; this.name = a[1];
	 * this.isIndex = a[2].equals("1"); this.deleted = a[3].equals("1"); this.code = a[4]; }
	 */

	Symbol(SymbolBuilder builder) {
		this.id = builder.getId();
		this.symbol = builder.getSymbol();
		this.name = builder.getName();
		this.isIndex = builder.getIsIndex();
		this.deleted = builder.getDeleted();
		this.code = builder.getCode();
	}

	public Integer getId() {
		return id;
	}

	public String getSymbol() {
		return symbol;
	}

	public String getName() {
		return name;
	}

	public Boolean isIndex() {
		return isIndex;
	}

	public boolean getDeleted() {
		return deleted;
	}

	public void setDeleted(Boolean deleted) {
		this.deleted = deleted;
	}

	@Override
	public String toString() {
		return symbol + " " + name + " " + (isIndex ? "(index)" : "");
	}

	public String getCode() {
		return code;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof Symbol) {
			Symbol s = (Symbol) o;
			try {
				return s.getDeleted() == getDeleted() && s.isIndex().equals(isIndex())
						&& s.getCode().equals(getCode()) && s.getName().equals(getName())
						&& s.getSymbol().equals(getSymbol());
			} catch (NullPointerException e) {
				return false;
			}
		} else {
			return false;
		}
	}
}
