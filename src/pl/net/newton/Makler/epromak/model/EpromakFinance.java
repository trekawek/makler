package pl.net.newton.Makler.epromak.model;

import org.w3c.dom.Element;

public class EpromakFinance extends AbstractEpromakModel {
	public static final String[] FIELDS = { "standozlc", "srwolne", "srzablok", "naleznosci", "depozytwst",
			"depozytwla", "dlugotp", "blokotp", "odrniespl", "brakotp", "zangkredyt", "waluta", "odskarne",
			"blkudzwl", "zobowkrd" };
	
	public static final String[] FIELDS_MILLENNIUM = { "standozlc", "srwolne", "srzablok", "naleznosci", "depozytwst",
		"depozytwla", "dlugotp", "blokotp", "blokpno", "odrniespl", "brakotp", "zangkredyt", "waluta", "odskarne",
		"blkudzwl", "zobowkrd" };

	private boolean empty;
	
	public EpromakFinance(Element e) {
		super(e, FIELDS);
	}
	
	private EpromakFinance() {
	}
	
	public static EpromakFinance getEmpty() {
		EpromakFinance finance = new EpromakFinance();
		finance.empty = true;
		return finance;
	}
	
	@Override
	public String get(String name) {
		if(empty) {
			return "0";
		} else {
			return super.get(name);
		}
	}

}
