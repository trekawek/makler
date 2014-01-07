package pl.net.newton.Makler.epromak.model;

import org.w3c.dom.Element;

public class EpromakOwnership extends AbstractEpromakModel {
	private static final String[] fields = { "blokDpz", "blokKrdEmisja", "grupaNotowan", "iloscPWBlokOther",
			"kodGieldy", "kodpapieru", "portfel", "prawaWlas", "skrot", "stanPWBlok", "stanPWPodst",
			"stanPWZast", "typPW" };

	private EpromakPaper paper;

	public EpromakOwnership(Element e, EpromakPaper p) {
		super(e, fields);
		paper = p;
	}

	public EpromakPaper getPaper() {
		return paper;
	}

	public String getSymbol() {
		String[] a = get("skrot").split(" ");
		return a[a.length - 1];
	}

	public String getName() {
		return get("skrot").split(" ")[0];
	}
}
