package pl.net.newton.Makler.gpw.model;

import java.math.BigDecimal;

import pl.net.newton.Makler.db.symbol.Symbol;

public class Paper {
	private Symbol symbol;

	private Integer quantity;

	private Integer ownership;

	private BigDecimal quote;

	private String name;

	public Paper(String name, Integer quantity, Integer ownership, BigDecimal quote) {
		this.name = name;
		this.quantity = quantity;
		this.ownership = ownership;
		this.quote = quote;
	}

	public Paper(Symbol symbol, Integer quantity, Integer ownership, BigDecimal quote) {
		this.symbol = symbol;
		this.quantity = quantity;
		this.ownership = ownership;
		this.quote = quote;
	}

	public BigDecimal getQuote() {
		return quote;
	}

	public Symbol getSymbol() {
		return symbol;
	}

	public String getName() {
		return name;
	}

	public Integer getQuantity() {
		return quantity;
	}

	public Integer getOwnership() {
		return ownership;
	}

	public BigDecimal getValue() {
		return quote.multiply(new BigDecimal(quantity));
	}
}
