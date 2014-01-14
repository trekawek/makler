package pl.net.newton.Makler.db.wallet;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class WalletItem {
	private Integer id;

	private Integer quantity;

	private String symbol, name;

	// commision of already made transactions
	private BigDecimal avgBuy, quote, totalCommision;

	WalletItem(WalletItemBuilder builder) {
		this.id = builder.getId();
		this.quantity = builder.getQuantity();
		this.symbol = builder.getSymbol();
		this.name = builder.getName();
		this.avgBuy = builder.getAvgBuy();
		this.quote = builder.getQuote();
		this.totalCommision = builder.getTotalCommision();
	}

	public Integer getId() {
		return id;
	}

	public Integer getQuantity() {
		return quantity;
	}

	public String getSymbol() {
		return symbol;
	}

	public String getName() {
		return name;
	}

	public BigDecimal getAvgBuy() {
		return avgBuy;
	}

	public BigDecimal getQuote() {
		return quote;
	}

	public BigDecimal getTotalCommision() {
		return totalCommision;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public void addTrans(Character type, Integer quantity, BigDecimal kurs, BigDecimal commison) {
		if (quantity == 0) {
			return;
		}
		if (type.equals('K')) {
			BigDecimal currentCost = this.avgBuy.multiply(new BigDecimal(this.quantity));
			BigDecimal transCost = kurs.multiply(new BigDecimal(quantity));
			BigDecimal avgCost = transCost.add(currentCost).divide(new BigDecimal(quantity + this.quantity),
					3, RoundingMode.HALF_UP);
			this.avgBuy = avgCost;
			this.quantity += quantity;
		} else if (type.equals('S')) {
			this.quantity -= quantity;
		}
		this.totalCommision = this.totalCommision.add(commison);
	}

	public BigDecimal gain() {
		return quote.multiply(new BigDecimal(quantity)).subtract(avgBuy.multiply(new BigDecimal(quantity)));
	}

	public BigDecimal gainWithCommision(BigDecimal commision, BigDecimal minCommision) {
		BigDecimal transValue = quote.multiply(new BigDecimal(quantity));
		BigDecimal comm = transValue.multiply(commision).divide(BigDecimal.valueOf(100));
		if (comm.compareTo(minCommision) == -1) {
			comm = minCommision;
		}
		return transValue.subtract(avgBuy.multiply(new BigDecimal(quantity))).subtract(comm)
				.subtract(totalCommision);
	}

	public BigDecimal getZmiana() {
		if (quote == null || avgBuy == null) {
			return null;
		}
		return quote.subtract(avgBuy).multiply(new BigDecimal(100)).divide(avgBuy, 3, RoundingMode.HALF_UP);
	}
}
