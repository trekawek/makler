package pl.net.newton.Makler.gpw.model;

import java.math.BigDecimal;

import pl.net.newton.Makler.R;
import pl.net.newton.Makler.db.symbol.Symbol;
import pl.net.newton.Makler.db.symbol.SymbolBuilder;
import pl.net.newton.Makler.db.symbol.SymbolsDb;

import android.content.Context;

public class Order {
	private Character type; // K lub S

	private Symbol symbol;

	private Integer ilosc;

	private BigDecimal limit;

	private LimitType limitType;

	private String sesja;

	private Validity validity;

	private String doDnia;

	private BigDecimal limitAkt;

	private Integer iloscUjawn;

	private Integer iloscMin;

	private String wdc;

	public Order(Context ctx, Character type, String symbol, String ilosc, String limit, String limitType,
			String sesja, String validity, String doDnia, String limitAkt, String iloscUjawn,
			String iloscMin, SymbolsDb symbolsDb, String wdc) {

		this.type = type;
		this.ilosc = Integer.parseInt(ilosc);

		if (limit != null && !limit.equals("")) {
			this.limit = new BigDecimal(limit);
		}
		if (limitType != null && !limitType.equals("---")) {
			this.limitType = LimitType.valueOf(limitType);
		}

		this.sesja = sesja;

		if (validity != null)
			this.validity = validityFromString(ctx, validity);
		try {
			if (this.validity == null && validity != null)
				this.validity = Validity.valueOf(validity);
		} catch (Exception e) {
		}

		this.doDnia = doDnia;

		if (limitAkt != null && !limitAkt.equals("")) {
			this.limitAkt = new BigDecimal(limitAkt);
		}

		try {
			this.iloscUjawn = Integer.parseInt(iloscUjawn);
		} catch (Exception e) {
		}
		try {
			this.iloscMin = Integer.parseInt(iloscMin);
		} catch (Exception e) {
		}

		this.symbol = symbolsDb.getSymbolBySymbol(symbol);
		if (this.symbol == null)
			this.symbol = new SymbolBuilder().setName(symbol).setSymbol(symbol).build();
		
		this.wdc = wdc;
	}

	public static Validity validityFromString(Context ctx, String s) {
		String[] ar = ctx.getResources().getStringArray(R.array.order_form_waznosc);
		for (int i = 0; i < ar.length; i++)
			if (ar[i].equals(s))
				return Validity.values()[i];
		return null;
	}

	public static enum LimitType {
		PEG, PKC, PCR
	}

	public static enum Validity {
		DODN, WDC, WLA, WIA, WNF, WNZ
	}

	public Character getType() {
		return type;
	}

	public Symbol getSymbol() {
		return symbol;
	}

	public Integer getIlosc() {
		return ilosc;
	}

	public BigDecimal getLimit() {
		return limit;
	}

	public LimitType getLimitType() {
		return limitType;
	}

	public String getSesja() {
		return sesja;
	}

	public Validity getValidity() {
		return validity;
	}

	public String getDoDnia() {
		return doDnia;
	}

	public BigDecimal getLimitAkt() {
		return limitAkt;
	}

	public Integer getIloscUjawn() {
		return iloscUjawn;
	}

	public Integer getIloscMin() {
		return iloscMin;
	}

	public String getWdc() {
		return wdc;
	}
}
