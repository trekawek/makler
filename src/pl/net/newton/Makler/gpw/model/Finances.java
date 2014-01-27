package pl.net.newton.Makler.gpw.model;

import java.math.BigDecimal;
import java.util.List;

public class Finances {
	private List<Paper> papers;

	private BigDecimal avail;

	private BigDecimal account;

	private BigDecimal charges;

	public Finances(List<Paper> papers, BigDecimal avail, BigDecimal account, BigDecimal charges) {
		this.papers = papers;
		this.avail = avail;
		this.account = account;
		this.charges = charges;
	}

	public List<Paper> getPapers() {
		return papers;
	}

	public BigDecimal getAvail() {
		return avail;
	}

	public BigDecimal getAccount() {
		return account;
	}

	public BigDecimal getCharges() {
		return charges;
	}

	public BigDecimal getValue() {
		BigDecimal v = BigDecimal.ZERO;
		for (Paper p : papers) {
			v = v.add(p.getValue());
		}
		return v;
	}

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append("avail:\t").append(avail).append('\n');
		b.append("account:\t").append(account).append('\n');
		b.append("charges:\t").append(charges).append('\n');
		b.append("papers:\n");
		for (Paper p : papers) {
			b.append(p.getSymbol().getSymbol()).append(" (").append(p.getSymbol().getName()).append(") ");
			b.append(p.getValue()).append('\n');
		}
		return b.toString();
	}
}
