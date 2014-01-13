package pl.net.newton.Makler.epromak.model;

import java.text.ParseException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import android.content.Context;
import pl.net.newton.Makler.common.DateFormatUtils;
import pl.net.newton.Makler.db.symbol.SymbolsDb;
import pl.net.newton.Makler.gpw.model.Order;
import pl.net.newton.Makler.gpw.model.Order.Validity;
import pl.net.newton.Makler.gpw.model.OrderState.State;

public class EpromakOrderState extends AbstractEpromakModel {
	public static final String[] fields = { "grupanotowan", "idzlecenia", "zewnSystem", "zewnId", "skrot",
			"oferta", "ilosczlc", "iloscrea", "limitceny", "data1sesji", "datawaznosci", "opcjazlc", "otp",
			"portfel", "opcjarea", "iloscodkryta", "iloscminimalna", "limitaktywacji", "blokada",
			"odroczenie", "stan", "czasrej", "kodpapieru", "blokow", "wezwanie", "rynekzlecen" };

	private EpromakPaper paper;

	public EpromakOrderState(Element e, EpromakPaper p) {
		super(e, fields);
		this.paper = p;
	}

	public EpromakPaper getPaper() {
		return paper;
	}

	public String getId() {
		return get("zewnId");
	}

	public Validity getValidity() {
		switch (getDataWaznosciTyp()) {
			case 'D':
				return Validity.DODN;
			default:
				return null;
		}
	}

	public Character getDataWaznosciTyp() {
		String dataW = get("datawaznosci");
		String data1 = get("data1sesji");
		String opcja = get("opcjarea");

		if (dataW == null || dataW.equals("")) {
			if (opcja.equals("------"))
				return 'M';
			else if (opcja.equals("--M---"))
				return 'M';
			else if (opcja.equals("-F----"))
				return 'I';
			else if (opcja.equals("----f-"))
				return 'U';
		} else if (dataW.equals(data1))
			return 'J';
		else
			try {
				if (DateFormatUtils.parseYyyyMmSs(dataW).after(DateFormatUtils.parseYyyyMmSs(data1)))
					return 'D';
			} catch (ParseException e) {
				e.printStackTrace();
			}
		return 'M';
	}

	public Element getDesc(Document doc) {
		Element e = doc.createElement("description");
		e.setAttribute("orgOfrt", get("oferta"));
		e.setAttribute("orgKodPW", get("kodpapieru"));
		e.setAttribute("orgIloscPW", get("ilosczlc"));
		e.setAttribute("orgLimTyp", get("opcjazlc"));
		e.setAttribute("orgLimit", get("limitceny"));
		e.setAttribute("orgDtSes", get("data1sesji"));
		e.setAttribute("orgDtWaz", get("datawaznosci"));
		e.setAttribute("orgOtp", get("otp"));
		Character dtWazTyp = getDataWaznosciTyp();
		if (dtWazTyp != null)
			e.setAttribute("orgDtWazTyp", dtWazTyp.toString());
		e.setAttribute("orgLimAkt", get("limitaktywacji"));
		e.setAttribute("orgIleOdk", get("iloscodkryta"));
		e.setAttribute("orgIleMin", get("iloscminimalna"));
		e.setAttribute("orgPortfel", get("portfel"));
		e.setAttribute("nrNik", "");
		return e;
	}

	public State getStan() {
		switch (get("stan").charAt(0)) {
			case 'M':
				return State.ZAMKN;
			case 'A':
			case 'O':
				return State.ANULOWANO;
			case 'F':
				return State.W_TR_ANUL;
			case 'R':
				return State.ZREAL;
			case 'Z':
			case 'C':
			case 'Y':
				return State.ZAKSIEG;
			case 'W':
				return State.WPROW;
			default:
				return null;
		}
	}

	public pl.net.newton.Makler.gpw.model.Order getGPWOrder(Context ctx, SymbolsDb symbolsDb) {
		String symbol;
		if (paper != null) {
			symbol = paper.getSymbol();
		} else {
			symbol = this.get("skrot");
		}

		return new Order(ctx, get("oferta").charAt(0), symbol, get("ilosczlc"), get("limitceny"),
				get("opcjazlc"), get("data1sesji"), getValidity().name(), get("datawaznosci"),
				get("limitaktywacji"), get("iloscodkryta"), get("iloscminimalna"), symbolsDb, null);
	}
}
