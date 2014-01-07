package pl.net.newton.Makler.epromak.model;

import java.math.BigDecimal;

public class EpromakRoundRule extends AbstractEpromakModel {
	// public static final String[] fields =
	// {"gielda","grupanotow","rynekzlecen","cenamin","cenamax","dokladnosc"};
	private BigDecimal cenamin, cenamax, dokladnosc;

	public EpromakRoundRule(String s, String[] fields) {
		super(s, fields);
		this.cenamin = new BigDecimal(get("cenamin"));
		this.cenamax = new BigDecimal(get("cenamax"));
		this.dokladnosc = new BigDecimal(get("dokladnosc"));
	}

	public Boolean match(EpromakOrder o) {
		Boolean m = false;
		if (o.get("limit") != null)
			m = m || match(o, new BigDecimal(o.get("limit")));
		if (o.get("limAkt") != null)
			m = m || match(o, new BigDecimal(o.get("limAkt")));
		return m;
	}

	private Boolean match(EpromakOrder o, BigDecimal limit) {
		return get("grupanotow").equals(o.get("grpNotow")) && get("rynekzlecen").equals(o.get("rynekZlecen"))
				&& limit.compareTo(cenamin) >= 0 && limit.compareTo(cenamax) <= 0;
	}

	public void roundOrder(EpromakOrder o) {
		if (o.get("limit") != null)
			o.data.put("limit", round(o.get("limit")));
		if (o.get("limAkt") != null)
			o.data.put("limAkt", round(o.get("limAkt")));
	}

	private String round(String limit) {
		BigDecimal l = new BigDecimal(limit);
		l = l.divide(dokladnosc);
		l = new BigDecimal(l.intValue());
		l = l.multiply(dokladnosc);
		return l.toString();
	}

	@Override
	public String toString() {
		return new StringBuilder(get("grupanotow")).append(' ').append(get("rynekzlecen")).append(' ')
				.append(get("cenamin")).append('-').append(get("cenamax")).append(':')
				.append(get("dokladnosc")).toString();
	}
}
