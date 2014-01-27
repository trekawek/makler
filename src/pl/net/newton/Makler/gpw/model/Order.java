package pl.net.newton.Makler.gpw.model;

import java.math.BigDecimal;

import org.apache.commons.lang3.StringUtils;

import pl.net.newton.Makler.R;
import pl.net.newton.Makler.common.NumberFormatUtils;
import pl.net.newton.Makler.db.symbol.Symbol;
import pl.net.newton.Makler.db.symbol.SymbolBuilder;
import pl.net.newton.Makler.db.symbol.SymbolsDb;
import android.content.Context;
import android.util.Log;

public class Order {
	private static final String TAG = "MaklerOrder";

	public static enum LimitType {
		PEG, PKC, PCR
	}

	public static enum Validity {
		DODN, WDC, WLA, WIA, WNF, WNZ
	}

	// K lub S
	private Character type;

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
		this.limit = NumberFormatUtils.parseOrNull(limit);
		if (limitType != null && !limitType.equals("---")) {
			this.limitType = LimitType.valueOf(limitType);
		}
		this.sesja = sesja;
		this.validity = validityFromString(ctx, validity);
		this.doDnia = doDnia;
		this.limitAkt = NumberFormatUtils.parseOrNull(limitAkt);
		this.iloscUjawn = NumberFormatUtils.parseIntOrNull(iloscUjawn);
		this.iloscMin = NumberFormatUtils.parseIntOrNull(iloscMin);
		this.symbol = symbolsDb.getSymbolBySymbol(symbol);
		if (this.symbol == null) {
			this.symbol = new SymbolBuilder().setName(symbol).setSymbol(symbol).build();
		}
		this.wdc = wdc;
	}

	public static Validity validityFromString(Context ctx, String s) {
		if (StringUtils.isBlank(s)) {
			return null;
		}

		String[] ar = ctx.getResources().getStringArray(R.array.order_form_waznosc);
		for (int i = 0; i < ar.length; i++) {
			if (ar[i].equals(s)) {
				return Validity.values()[i];
			}
		}
		if (s != null) {
			try {
				return Validity.valueOf(s);
			} catch (IllegalArgumentException e) {
				Log.e(TAG, "Illegal value for validity: " + s, e);
			}
		}
		return null;
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
