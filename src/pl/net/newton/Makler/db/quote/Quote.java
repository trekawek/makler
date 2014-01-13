package pl.net.newton.Makler.db.quote;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Calendar;
import pl.net.newton.Makler.common.NumberFormatUtils;

public class Quote {
	private final Integer id;

	private final String symbol, name;

	private final BigDecimal kurs, zmiana, kursOdn, kursOtw, kursMin, kursMax, tko, tkoProcent, wartosc;

	private final BigDecimal kLim, sLim;

	private final Integer kOfert, kWol, sOfert, sWol, wolumen;

	private final Calendar update;

	private final Boolean index;

	Quote(QuoteBuilder builder) {
		this.id = builder.getId();
		this.symbol = builder.getSymbol();
		this.name = builder.getName();
		this.kurs = builder.getKurs();
		this.kursOdn = builder.getKursOdn();
		BigDecimal z = builder.getZmiana();
		if (z == null || z.compareTo(BigDecimal.ZERO) == 0) {
			if (kurs != null && kursOdn.compareTo(BigDecimal.ZERO) > 0) {
				z = (kurs.divide(kursOdn, 4, RoundingMode.HALF_UP).subtract(BigDecimal.ONE))
						.multiply(new BigDecimal(100));
			}
		}
		this.zmiana = z;
		this.kursOtw = builder.getKursOtw();
		this.kursMin = builder.getKursMin();
		this.kursMax = builder.getKursMax();
		this.tko = builder.getTko();
		this.tkoProcent = builder.getTkoProcent();
		this.wartosc = builder.getWartosc();
		this.kLim = builder.getkLim();
		this.sLim = builder.getsLim();
		this.kOfert = builder.getkOfert();
		this.kWol = builder.getkWol();
		this.sOfert = builder.getsOfert();
		this.sWol = builder.getsWol();
		this.wolumen = builder.getWolumen();
		this.update = Calendar.getInstance();
		this.update.setTime(builder.getUpdate());
		this.index = builder.getIndex();
	}

	public Integer getId() {
		return id;
	}

	public Boolean isIndex() {
		return index;
	}

	public String getSymbol() {
		return symbol;
	}

	public String getName() {
		return name;
	}

	public BigDecimal getKurs() {
		return kurs;
	}

	public BigDecimal getZmiana() {
		return zmiana;
	}

	public BigDecimal getKursOdn() {
		return kursOdn;
	}

	public BigDecimal getKursOtw() {
		return kursOtw;
	}

	public BigDecimal getKursMin() {
		return kursMin;
	}

	public BigDecimal getKursMax() {
		return kursMax;
	}

	public BigDecimal getTko() {
		return tko;
	}

	public BigDecimal getTkoProcent() {
		return tkoProcent;
	}

	public Integer getWolumen() {
		return wolumen;
	}

	public BigDecimal getWartosc() {
		return wartosc;
	}

	public Calendar getUpdate() {
		return update;
	}

	@Override
	public String toString() {
		return symbol + " " + kurs.toString();
	}

	public Integer getkOfert() {
		return kOfert;
	}

	public Integer getkWol() {
		return kWol;
	}

	public BigDecimal getkLim() {
		return kLim;
	}

	public Integer getsOfert() {
		return sOfert;
	}

	public Integer getsWol() {
		return sWol;
	}

	public BigDecimal getsLim() {
		return sLim;
	}

	public String getsLimString() {
		String name = limitString(sLim);
		if (name != null)
			return name;
		else
			return NumberFormatUtils.formatNumber(sLim);
	}

	public String getkLimString() {
		String name = limitString(kLim);
		if (name != null)
			return name;
		else
			return NumberFormatUtils.formatNumber(kLim);
	}

	public BigDecimal chooseKurs() {
		if (tko != null && update != null && update.get(Calendar.HOUR_OF_DAY) == 17
				&& update.get(Calendar.MINUTE) >= 20 && update.get(Calendar.MINUTE) < 30)
			return tko;
		else if (kurs != null)
			return kurs;
		else if (tko != null)
			return tko;
		else if (kursOtw != null)
			return kursOtw;
		else if (kursOdn != null)
			return kursOdn;
		else
			return null;
	}

	public BigDecimal chooseZmiana() {
		if (tkoProcent != null && update != null && update.get(Calendar.HOUR_OF_DAY) == 17
				&& update.get(Calendar.MINUTE) >= 20 && update.get(Calendar.MINUTE) < 30)
			return tkoProcent;
		else if (zmiana != null)
			return zmiana;
		else if (tkoProcent != null)
			return tkoProcent;
		else
			return null;
	}

	private String limitString(BigDecimal limit) {
		if (limit == null)
			return null;
		switch (limit.intValue()) {
			case -1:
				return "PCRO";
			case -2:
				return "PKC";
			default:
				return null;
		}
	}
}
