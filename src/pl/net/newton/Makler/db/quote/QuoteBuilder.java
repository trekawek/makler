package pl.net.newton.Makler.db.quote;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.Date;
import pl.net.newton.Makler.common.DateFormatUtils;
import pl.net.newton.Makler.common.NumberFormatUtils;
import android.database.Cursor;

public class QuoteBuilder {
	private Integer id;

	private String symbol, name;

	private BigDecimal kurs, zmiana, kursOdn, kursOtw, kursMin, kursMax, tko, tkoProcent, wartosc;

	private BigDecimal kLim, sLim;

	private Integer kOfert, kWol, sOfert, sWol, wolumen;

	private Date update;

	private Boolean index;

	public QuoteBuilder setFromCursor(Cursor c) {
		this.id = c.getInt(c.getColumnIndex("id"));
		this.symbol = c.getString(c.getColumnIndex("symbol"));
		this.name = c.getString(c.getColumnIndex("name"));
		this.kurs = NumberFormatUtils.parseOrNull(c.getString(c.getColumnIndex("kurs")));
		this.zmiana = NumberFormatUtils.parseOrNull(c.getString(c.getColumnIndex("zmiana")));
		this.kursOdn = NumberFormatUtils.parseOrNull(c.getString(c.getColumnIndex("kurs_odn")));
		this.kursOtw = NumberFormatUtils.parseOrNull(c.getString(c.getColumnIndex("kurs_otw")));
		this.kursMin = NumberFormatUtils.parseOrNull(c.getString(c.getColumnIndex("kurs_min")));
		this.kursMax = NumberFormatUtils.parseOrNull(c.getString(c.getColumnIndex("kurs_max")));
		this.index = c.getInt(c.getColumnIndex("is_index")) == 1 ? Boolean.TRUE : Boolean.FALSE;
		if (!index) {
			this.tko = NumberFormatUtils.parseOrNull(c.getString(c.getColumnIndex("tko")));
			this.tkoProcent = NumberFormatUtils.parseOrNull(c.getString(c.getColumnIndex("tko_procent")));
		}
		this.wolumen = NumberFormatUtils.parseIntOrNull(c.getString(c.getColumnIndex("wolumen")));
		this.wartosc = NumberFormatUtils.parseOrNull(c.getString(c.getColumnIndex("wartosc")));

		this.kOfert = NumberFormatUtils.parseIntOrNull(c.getString(c.getColumnIndex("k_ofert")));
		this.kWol = NumberFormatUtils.parseIntOrNull(c.getString(c.getColumnIndex("k_wol")));
		this.kLim = NumberFormatUtils.parseOrNull(c.getString(c.getColumnIndex("k_lim")));
		this.sLim = NumberFormatUtils.parseOrNull(c.getString(c.getColumnIndex("s_lim")));
		this.sWol = NumberFormatUtils.parseIntOrNull(c.getString(c.getColumnIndex("s_wol")));
		this.sOfert = NumberFormatUtils.parseIntOrNull(c.getString(c.getColumnIndex("s_ofert")));

		try {
			this.update = DateFormatUtils.parseTime(c.getString(c.getColumnIndex("update")));
		} catch (NullPointerException e) {
			this.update = null;
		} catch (ParseException e) {
			this.update = null;
		}
		return this;
	}

	public Quote build() {
		return new Quote(this);
	}

	Integer getId() {
		return id;
	}

	public QuoteBuilder setId(Integer id) {
		this.id = id;
		return this;
	}

	String getSymbol() {
		return symbol;
	}

	public QuoteBuilder setSymbol(String symbol) {
		this.symbol = symbol;
		return this;
	}

	String getName() {
		return name;
	}

	public QuoteBuilder setName(String name) {
		this.name = name;
		return this;
	}

	BigDecimal getKurs() {
		return kurs;
	}

	public QuoteBuilder setKurs(BigDecimal kurs) {
		this.kurs = kurs;
		return this;
	}

	BigDecimal getZmiana() {
		return zmiana;
	}

	public QuoteBuilder setZmiana(BigDecimal zmiana) {
		this.zmiana = zmiana;
		return this;
	}

	BigDecimal getKursOdn() {
		return kursOdn;
	}

	public QuoteBuilder setKursOdn(BigDecimal kursOdn) {
		this.kursOdn = kursOdn;
		return this;
	}

	BigDecimal getKursOtw() {
		return kursOtw;
	}

	public QuoteBuilder setKursOtw(BigDecimal kursOtw) {
		this.kursOtw = kursOtw;
		return this;
	}

	BigDecimal getKursMin() {
		return kursMin;
	}

	public QuoteBuilder setKursMin(BigDecimal kursMin) {
		this.kursMin = kursMin;
		return this;
	}

	BigDecimal getKursMax() {
		return kursMax;
	}

	public QuoteBuilder setKursMax(BigDecimal kursMax) {
		this.kursMax = kursMax;
		return this;
	}

	BigDecimal getTko() {
		return tko;
	}

	public QuoteBuilder setTko(BigDecimal tko) {
		this.tko = tko;
		return this;
	}

	BigDecimal getTkoProcent() {
		return tkoProcent;
	}

	public QuoteBuilder setTkoProcent(BigDecimal tkoProcent) {
		this.tkoProcent = tkoProcent;
		return this;
	}

	BigDecimal getWartosc() {
		return wartosc;
	}

	public QuoteBuilder setWartosc(BigDecimal wartosc) {
		this.wartosc = wartosc;
		return this;
	}

	BigDecimal getkLim() {
		return kLim;
	}

	public QuoteBuilder setkLim(BigDecimal kLim) {
		this.kLim = kLim;
		return this;
	}

	BigDecimal getsLim() {
		return sLim;
	}

	public QuoteBuilder setsLim(BigDecimal sLim) {
		this.sLim = sLim;
		return this;
	}

	Integer getkOfert() {
		return kOfert;
	}

	public QuoteBuilder setkOfert(Integer kOfert) {
		this.kOfert = kOfert;
		return this;
	}

	Integer getkWol() {
		return kWol;
	}

	public QuoteBuilder setkWol(Integer kWol) {
		this.kWol = kWol;
		return this;
	}

	Integer getsOfert() {
		return sOfert;
	}

	public QuoteBuilder setsOfert(Integer sOfert) {
		this.sOfert = sOfert;
		return this;
	}

	Integer getsWol() {
		return sWol;
	}

	public QuoteBuilder setsWol(Integer sWol) {
		this.sWol = sWol;
		return this;
	}

	Integer getWolumen() {
		return wolumen;
	}

	public QuoteBuilder setWolumen(Integer wolumen) {
		this.wolumen = wolumen;
		return this;
	}

	Date getUpdate() {
		return update;
	}

	public QuoteBuilder setUpdate(Date update) {
		this.update = update;
		return this;
	}

	Boolean getIndex() {
		return index;
	}

	public QuoteBuilder setIndex(Boolean index) {
		this.index = index;
		return this;
	}
}
