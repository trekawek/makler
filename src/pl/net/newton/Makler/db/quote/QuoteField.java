package pl.net.newton.Makler.db.quote;

public enum QuoteField {
	SYMBOL("symbol", false), NAME("name", false), UPDATED("`update`"), QUOTE("kurs"), CHANGE("zmiana"), REFERENCE(
			"kurs_odn"), OPEN("kurs_otw"), MIN("kurs_min"), MAX("kurs_max"), TKO("tko"), TKO_PERCENT(
			"tko_procent"), VOL("wolumen"), VALUE("wartosc"), BID_OFFERS("k_ofert"), BID_VOL("k_wol"), BID(
			"k_lim"), ASK("s_lim"), ASK_VOL("s_wol"), ASK_OFFERS("s_ofert"), IS_INDEX("is_index", false), ID("id");

	private final String databaseField;

	private final boolean includeInCv;

	private QuoteField(String databaseField) {
		this(databaseField, true);
	}

	private QuoteField(String databaseField, boolean includeInCv) {
		this.databaseField = databaseField;
		this.includeInCv = includeInCv;
	}

	public String getDatabaseField() {
		return databaseField;
	}

	public boolean isIncludeInCv() {
		return includeInCv;
	}
}
