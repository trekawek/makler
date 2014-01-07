package pl.net.newton.Makler.mbank.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import pl.net.newton.Makler.db.symbol.SymbolsDb;
import pl.net.newton.Makler.gpw.model.Order.LimitType;
import pl.net.newton.Makler.gpw.model.Order.Validity;
import android.content.Context;
import android.util.Log;

public class MbankOrder {
	private static final String TAG = "Makler";

	private Character type;

	private MbankPaper paper;

	private Integer quantity, wuj, wmin;

	private BigDecimal limit, limitAkt;

	private LimitType limitType;

	private String dataSes, dataWaz, wdc;

	private Validity validity;

	public MbankOrder(Character type, MbankPaper paper, Integer quantity, BigDecimal limit,
			LimitType limitType, BigDecimal limitAkt, String dataSes, String dataWaz, Validity validity,
			Integer wuj, Integer wmin, String wdc) {
		this.type = type;
		this.paper = paper;
		this.quantity = quantity;
		this.limit = limit;
		this.limitType = limitType;
		this.limitAkt = limitAkt;
		this.dataSes = dataSes;
		this.dataWaz = dataWaz;
		this.validity = validity;
		this.wuj = wuj;
		this.wmin = wmin;
		this.wdc = wdc;
		if (validity == null)
			this.validity = Validity.DODN;
	}

	public MbankPaper getPaper() {
		return paper;
	}

	public Integer getQuantity() {
		return quantity;
	}

	public BigDecimal getLimit() {
		return limit;
	}

	public BigDecimal getLimitAkt() {
		return limitAkt;
	}

	public LimitType getLimitType() {
		return limitType;
	}

	public String getDataSes() {
		return dataSes;
	}

	public String getDataWaz() {
		return dataWaz;
	}

	public List<NameValuePair> getChangeParams() {
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		String[] waz = dataWaz.split("-");
		params.add(new BasicNameValuePair("dtdValidityDate_day", waz[2]));
		params.add(new BasicNameValuePair("dtdValidityDate_month", waz[1]));
		params.add(new BasicNameValuePair("dtdValidityDate_year", waz[0]));
		params.add(new BasicNameValuePair("tbOrderAmount", String.valueOf(quantity)));

		if (limitType == null) {
			params.add(new BasicNameValuePair("PriceLimit", "rbPriceLimit"));
			params.add(new BasicNameValuePair("mPriceLimit", limit.toString().replace('.', ',')));
		} else {
			params.add(new BasicNameValuePair("mPriceLimit", ""));
			switch (limitType) {
				case PEG:
					params.add(new BasicNameValuePair("PriceLimit", "rbPEG"));
					break;

				case PKC:
					params.add(new BasicNameValuePair("PriceLimit", "rbPKC"));
					break;

				case PCR:
					params.add(new BasicNameValuePair("PriceLimit", "rbPCR"));
					break;
			}
		}
		if (limitAkt != null)
			params.add(new BasicNameValuePair("tbActLimit", limitAkt.toString().replace('.', ',')));
		else
			params.add(new BasicNameValuePair("tbActLimit", ""));
		return params;
	}

	public List<NameValuePair> getParams() {
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		if (type == 'K') {
			params.add(new BasicNameValuePair("tbacShareId", paper.getName()));
			params.add(new BasicNameValuePair("dtdOrderDate_year", "1"));
			params.add(new BasicNameValuePair("dtdOrderDate_month", "1"));
			params.add(new BasicNameValuePair("dtdOrderDate_day", "1"));
		}
		params.add(new BasicNameValuePair("tbAmount", String.valueOf(quantity)));

		if (limitType == null || limitType == LimitType.PEG) {
			params.add(new BasicNameValuePair("tbPriceLimit", limit.toString().replace('.', ',')));
		}
		if (limitType != null) {
			switch (limitType) {
				case PKC:
					params.add(new BasicNameValuePair("chbPKC", "on"));
					params.add(new BasicNameValuePair("tbPriceLimit", ""));
					break;

				case PCR:
					params.add(new BasicNameValuePair("chbPCR", "on"));
					params.add(new BasicNameValuePair("tbPriceLimit", ""));
					break;

				case PEG:
					params.add(new BasicNameValuePair("chbPEG", "on"));
					break;
			}
		}

		if (limitAkt != null)
			params.add(new BasicNameValuePair("tbActLimit", limitAkt.toString().replace('.', ',')));
		else
			params.add(new BasicNameValuePair("tbActLimit", ""));
		String[] ses = dataSes.split("-");
		params.add(new BasicNameValuePair("dtdSessionDate_day", ses[2]));
		params.add(new BasicNameValuePair("dtdSessionDate_month", ses[1]));
		params.add(new BasicNameValuePair("dtdSessionDate_year", ses[0]));

		switch (validity) {
			case WNZ:
				params.add(new BasicNameValuePair("chbWNZ", "on"));
				break;
			case WIA:
				params.add(new BasicNameValuePair("chbWIA", "on"));
				break;
			case WLA:
				params.add(new BasicNameValuePair("chbWLA", "on"));
				break;
			case WNF:
				params.add(new BasicNameValuePair("chbWNF", "on"));
				break;
			case WDC:
				params.add(new BasicNameValuePair("chbWDC", "on"));
				params.add(new BasicNameValuePair("tbPCRTime", wdc));
				Log.d(TAG, "wdc: " + wdc);
			case DODN:
			default:
				String[] waz = dataWaz.split("-");
				params.add(new BasicNameValuePair("dtdValidityDate_day", waz[2]));
				params.add(new BasicNameValuePair("dtdValidityDate_month", waz[1]));
				params.add(new BasicNameValuePair("dtdValidityDate_year", waz[0]));
				break;
		}

		/*
		 * else { params.add(new BasicNameValuePair("dtdValidityDate_day", ses[2])); params.add(new
		 * BasicNameValuePair("dtdValidityDate_month", ses[1])); params.add(new
		 * BasicNameValuePair("dtdValidityDate_year", ses[0])); }
		 */
		if (wuj != null)
			params.add(new BasicNameValuePair("tbWUJ", wuj.toString()));
		if (wmin != null)
			params.add(new BasicNameValuePair("tbWMin", wmin.toString()));
		else
			params.add(new BasicNameValuePair("tbWMin", ""));

		return params;
	}

	public pl.net.newton.Makler.gpw.model.Order getDBOrder(Context ctx, SymbolsDb symbolsDb) {
		return new pl.net.newton.Makler.gpw.model.Order(ctx, type, paper.getSymbol(),
				String.valueOf(quantity), limit == null ? null : limit.toString(), limitType == null ? null
						: limitType.toString(), dataSes, validity.toString(), dataWaz,
				limitAkt == null ? null : limitAkt.toString(), null, null, symbolsDb, wdc);
	}

	public Character getType() {
		return type;
	}

	public static MbankOrder getFromDBOrder(pl.net.newton.Makler.gpw.model.Order o, MbankPaper p) {
		return new MbankOrder(o.getType(), p, o.getIlosc(), o.getLimit(), o.getLimitType(), o.getLimitAkt(),
				o.getSesja(), o.getDoDnia(), o.getValidity(), o.getIloscUjawn(), o.getIloscMin(), o.getWdc());
	}

	public Integer getWuj() {
		return wuj;
	}

	public Integer getWmin() {
		return wmin;
	}

}
