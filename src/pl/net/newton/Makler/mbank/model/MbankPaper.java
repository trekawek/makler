package pl.net.newton.Makler.mbank.model;

import pl.net.newton.Makler.db.symbol.Symbol;
import pl.net.newton.Makler.db.symbol.SymbolBuilder;

public class MbankPaper {
	private String symbol;

	private String name;

	private Boolean isIndex;

	private String code;

	public MbankPaper(String symbol, String name, Boolean isIndex, String code) {
		this.symbol = symbol;
		this.name = name;
		this.isIndex = isIndex;
		this.code = code;
	}

	public MbankPaper(String s) {
		String[] a = s.split("\\|");
		this.symbol = a[0];
		this.name = a[1];
		this.isIndex = a[2].equals("1");
		this.code = a[3];
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

	@Override
	public String toString() {
		return symbol + " " + name + " " + (isIndex ? "(index)" : "");
	}

	public String getCode() {
		return code;
	}

	public String getSerializedString() {
		return new StringBuilder(symbol).append('|').append(name).append('|').append(isIndex ? '1' : '0')
				.append('|').append(code).toString();
	}

	public Symbol getDBSymbol() {
		SymbolBuilder builder = new SymbolBuilder();
		builder.setSymbol(symbol).setName(name).setIsIndex(isIndex).setCode(code);
		return builder.build();
	}
}
