package pl.net.newton.Makler.epromak.model;

import java.util.Hashtable;

import pl.net.newton.Makler.db.symbol.Symbol;
import pl.net.newton.Makler.db.symbol.SymbolBuilder;

public class EpromakPaper extends AbstractEpromakModel {
	/*
	 * private static final String[] fields = {"kodpapieru", "skrot", "grupanotow", "rynkizlecen", "mjo",
	 * "ojo", "wartosc", "mnoznik", "datakonca", "standard", "datasesji", "zamkniecie", "skokceny", "waluta",
	 * "odsetki", "grupa_ws", "widelkiDolne", "widelkiGorne"};
	 */

	private String serializedString;

	public EpromakPaper(String s, String[] fields) {
		super(s, fields);
		serializedString = s;
	}

	public EpromakPaper(String[] a, String[] fields) {
		if (a == null || a.length == 0)
			return;
		data = new Hashtable<String, String>();
		for (int i = 0; i < a.length && i < fields.length; i++)
			data.put(fields[i], a[i]);

		StringBuilder b = new StringBuilder();
		for (int i = 0; i < a.length - 1; i++)
			b.append(a[i]).append(';');
		b.append(a[a.length - 1]);
		serializedString = b.toString();
	}

	public String getName() {
		//if (isIndex())
		//	return get("skrot");
		//else {
			return get("skrot").split(" ")[0];
		//}
	}

	public String getSymbol() {
		//if (isIndex())
		//	return get("skrot");
		//else {
			String[] a = get("skrot").split(" ");
			return a[a.length - 1];
		//}
	}

	public Boolean isIndex() {
		try {
			return get("grupa_ws").charAt(0) == 'I';
		} catch (Exception e) {
			return false;
		}
	}

	public String getCode() {
		return get("kodpapieru");
	}

	@Override
	public String toString() {
		return new StringBuilder(getSymbol()).append(" (").append(getCode()).append(")").toString();
	}

	public String getSerializedString() {
		return serializedString;
	}

	public Symbol getDBSymbol() {
		SymbolBuilder builder = new SymbolBuilder();
		builder.setSymbol(getSymbol()).setName(getName()).setIsIndex(isIndex()).setCode(getCode());
		return builder.build();
	}
}
