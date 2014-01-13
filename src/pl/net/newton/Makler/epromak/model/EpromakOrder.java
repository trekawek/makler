package pl.net.newton.Makler.epromak.model;

import java.math.BigDecimal;
import java.util.Hashtable;
import java.util.Map;
import pl.net.newton.Makler.gpw.model.Order.LimitType;
import pl.net.newton.Makler.gpw.model.Order.Validity;

public class EpromakOrder extends AbstractEpromakModel {
	public static final String[] fields = { "ofrt", "kodPW", "rynekZlecen", "iloscPW", "limTyp", "limit",
			"dtSes", "dtWaz", "otp", "idZlec", "dtWazTyp", "limAkt", "ileOdk", "ileMin", "grpNotow",
			"krdEmisja" };

	public static final String[] updateFields = { "iloscPW", "limTyp", "limit", "dtWaz", "dtWazTyp",
			"limAkt", "ileOdk", "ileMin" };

	public static EpromakOrder getFromDBOrder(pl.net.newton.Makler.gpw.model.Order o, EpromakPaper p) {
		return new EpromakOrder(o.getType(), p, o.getIlosc(), o.getLimitType(), o.getLimit(), o.getSesja(),
				o.getDoDnia(), o.getValidity(), o.getLimitAkt(), o.getIloscUjawn(), o.getIloscMin());
	}

	public EpromakOrder(Map<String, String> data) {
		this.data = data;
		this.data.put("krdEmisja", "N");
		this.data.put("limAkt", "");
		this.data.put("ileOdk", "");
		this.data.put("ileMin", "");
		this.data.put("otp", "");
	}

	public EpromakOrder(Character ofrt, EpromakPaper paper, Integer iloscPW, LimitType limTyp, BigDecimal limit,
			String dtSes, String dtWaz, Validity dtWazTyp, BigDecimal limAkt, Integer ileOdk, Integer ileMin) {
		this.data = new Hashtable<String, String>();
		setOfrt(ofrt);
		setPaper(paper);
		data.put("iloscPW", iloscPW.toString());
		if (limTyp != null)
			data.put("limTyp", limTyp.name());
		if (limit != null)
			data.put("limit", limit.toString());
		data.put("dtSes", dtSes);
		if (dtWaz != null)
			data.put("dtWaz", dtWaz);
		else
			data.put("dtWaz", dtSes);
		switch (dtWazTyp) {
			case DODN:
				data.put("dtWazTyp", "D");
				break;
			default:
				data.remove("dtWaz");
				break;
		}
		if (limAkt != null)
			data.put("limAkt", limAkt.toString());
		if (ileOdk != null)
			data.put("ileOdk", ileOdk.toString());
		if (ileMin != null)
			data.put("ileMin", ileMin.toString());
		this.data.put("krdEmisja", "N");
	}

	public void setOfrt(Character t) {
		data.put("ofrt", t.toString());
		if (t == 'K')
			data.put("otp", "N");
		else
			data.put("otp", "-");
	}

	public void setPaper(EpromakPaper p) {
		data.put("kodPW", p.getCode());
		data.put("rynekZlecen", p.get("rynkizlecen").substring(0, 1));
		data.put("grpNotow", p.get("grupanotow"));
	}
}
