package pl.net.newton.Makler.db.wallet;

import java.math.BigDecimal;

import android.database.Cursor;
import pl.net.newton.Makler.db.quote.Quote;
import pl.net.newton.Makler.db.quote.QuotesDb;
import pl.net.newton.Makler.db.symbol.Symbol;
import pl.net.newton.Makler.common.NumberFormatUtils;

public class WalletItemBuilder {
	private Integer id;

	private Integer quantity;

	private String symbol, name;

	// commision of already made transactions
	private BigDecimal avgBuy, quote, totalCommision;

	private Quote quoteRef;

	public WalletItem build() {
		return new WalletItem(this);
	}

	public WalletItemBuilder setFromCursor(Cursor c, QuotesDb db) {
		this.id = c.getInt(c.getColumnIndex("id"));
		this.quantity = c.getInt(c.getColumnIndex("quantity"));
		this.symbol = c.getString(c.getColumnIndex("symbol"));
		this.name = c.getString(c.getColumnIndex("name"));
		this.avgBuy = NumberFormatUtils.parseOrNull(c.getString(c.getColumnIndex("avg_buy")));
		this.totalCommision = NumberFormatUtils.parseOrNull(c.getString(c.getColumnIndex("total_commision")));
		if (totalCommision == null) {
			totalCommision = BigDecimal.ZERO;
		}
		this.quoteRef = db.getQuoteById(c.getInt(c.getColumnIndex("quote_id")));
		if (this.quoteRef != null) {
			this.quote = this.quoteRef.chooseKurs();
		}
		if (this.quote == null) {
			this.quote = BigDecimal.ZERO;
		}
		return this;
	}

	public WalletItemBuilder setFromSymbol(Symbol s) {
		this.id = null;
		this.symbol = s.getSymbol();
		this.name = s.getName();
		this.avgBuy = BigDecimal.ZERO;
		this.quote = BigDecimal.ZERO;
		this.totalCommision = BigDecimal.ZERO;
		this.quantity = 0;
		return this;
	}

	public WalletItemBuilder setId(Integer id) {
		this.id = id;
		return this;
	}

	public WalletItemBuilder setQuantity(Integer quantity) {
		this.quantity = quantity;
		return this;
	}

	public WalletItemBuilder setSymbol(String symbol) {
		this.symbol = symbol;
		return this;
	}

	public WalletItemBuilder setName(String name) {
		this.name = name;
		return this;
	}

	public WalletItemBuilder setAvgBuy(BigDecimal avgBuy) {
		this.avgBuy = avgBuy;
		return this;
	}

	public WalletItemBuilder setQuote(BigDecimal quote) {
		this.quote = quote;
		return this;
	}

	public WalletItemBuilder setTotalCommision(BigDecimal totalCommision) {
		this.totalCommision = totalCommision;
		return this;
	}

	public WalletItemBuilder setQuoteRef(Quote quoteRef) {
		this.quoteRef = quoteRef;
		return this;
	}

	Integer getId() {
		return id;
	}

	Integer getQuantity() {
		return quantity;
	}

	String getSymbol() {
		return symbol;
	}

	String getName() {
		return name;
	}

	BigDecimal getAvgBuy() {
		return avgBuy;
	}

	BigDecimal getQuote() {
		return quote;
	}

	BigDecimal getTotalCommision() {
		return totalCommision;
	}

	Quote getQuoteRef() {
		return quoteRef;
	}

}
