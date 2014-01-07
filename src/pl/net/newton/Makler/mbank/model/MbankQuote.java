package pl.net.newton.Makler.mbank.model;

import pl.net.newton.Makler.common.DateFormatUtils;
import pl.net.newton.Makler.common.NumberFormatUtils;
import pl.net.newton.Makler.db.quote.QuoteBuilder;
import pl.net.newton.Makler.db.symbol.Symbol;

public class MbankQuote {
	private Symbol paper;

	private String[][] data;

	private String[] indexFields = { "nazwa", "wartodn", "wart", "zmiana", "czaspub1", "wartmaks",
			"czaspub2", "wartmin", "czaspub3", "wartobr" };

	private String[][] fields = { { "nazwa", "otwarte", "kursodn" }, { "kursotw", "kursmin", "kursmax" },
			{ "tko", "tkoprocent" }, { "zmianaprocent", "widelkimin", "widelkimax" }, { "kliczba" },
			{ "kwolumen" }, { "klimit" }, { "slimit" }, { "swolumen" }, { "sliczba" },
			{ "kursost1", "kursost2", "kursost3", "kursost4", "kursost5" },
			{ "wolumenost1", "wolumenost2", "wolumenost3", "wolumenost4", "wolumenost5" },
			{ "czasost1", "czasost2", "czasost3", "czasost4", "czasost5" },
			{ "wolumen1", "wolumen2", "wolumen3", "wolumen4", "wolumen5" },
			{ "obrot1", "obrot2", "obrot3", "obrot4", "obrot5" }, { "faza" } };

	public MbankQuote(String s, Symbol p) {
		String[] a = s.split("@");
		data = new String[a.length][];
		for (int i = 0; i < a.length; i++)
			data[i] = a[i].split("\\|");
		paper = p;
	}

	public String get(String name) {
		int i;
		if (paper.isIndex()) {
			String d = null;
			for (i = 0; i < indexFields.length; i++)
				if (indexFields[i].equals(name) && data.length > i) {
					d = data[i][0];
					break;
				}
			if (d != null && (name.equals("czaspub1") || name.equals("czaspub2") || name.equals("czaspub3"))) {
				String[] time = new String[3];
				time[0] = d.substring(0, 2);
				time[1] = d.substring(2, 4);
				time[2] = d.substring(4, 6);
				d = String.format("%s:%s:%s", time[0], time[1], time[2]);
			}
			return d;
		} else {
			for (i = 0; i < fields.length; i++)
				for (int j = 0; j < fields[i].length; j++)
					if (fields[i][j].equals(name))
						if (data.length > i && data[i].length > j)
							return data[i][j];
		}
		return null;
	}

	public Symbol getPaper() {
		return paper;
	}

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		if (paper.isIndex())
			return b.append(paper.getSymbol()).append(": ").append(get("wart")).toString();
		else
			return b.append(paper.getSymbol()).append(": ").append(getKurs()).toString();
	}

	public pl.net.newton.Makler.db.quote.Quote getDBQuote() {
		QuoteBuilder builder = new QuoteBuilder();
		builder.setSymbol(paper.getSymbol()).setName(paper.getName());

		if (paper.isIndex()) {
			builder.setUpdate(DateFormatUtils.parseHhMmSsOrNull(get("czaspub1")))
					.setKurs(NumberFormatUtils.parseOrNull(get("wart")))
					.setZmiana(NumberFormatUtils.parseOrNull(get("zmiana")))
					.setKursOdn(NumberFormatUtils.parseOrNull(get("wartodn")))
					.setKursMin(NumberFormatUtils.parseOrNull(get("wartmin")))
					.setKursMax(NumberFormatUtils.parseOrNull(get("wartmaks")))
					.setWartosc(NumberFormatUtils.parseOrNull(get("wartobr")));
		} else {
			builder.setUpdate(DateFormatUtils.parseHhMmSsOrNull(get("czasost1")))
					.setKurs(NumberFormatUtils.parseOrNull(get("kursost1")))
					.setZmiana(NumberFormatUtils.parseOrNull(get("zmianaprocent")))
					.setKursOdn(NumberFormatUtils.parseOrNull(get("kursodn")))
					.setKursOtw(NumberFormatUtils.parseOrNull(get("kursotw")))
					.setKursMin(NumberFormatUtils.parseOrNull(get("kursmin")))
					.setKursMax(NumberFormatUtils.parseOrNull(get("kursmax")))
					.setTko(NumberFormatUtils.parseOrNull(get("tko")))
					.setTkoProcent(NumberFormatUtils.parseOrNull(get("tkoprocent")))
					.setWolumen(NumberFormatUtils.parseIntOrNull(get("wolumen1")))
					.setWartosc(NumberFormatUtils.parseOrNull(get("obrot1")))
					.setkOfert(NumberFormatUtils.parseIntOrNull(get("kliczba")))
					.setkWol(NumberFormatUtils.parseIntOrNull(get("kwolumen")))
					.setkLim(NumberFormatUtils.parseOrNull(get("klimit")))
					.setsOfert(NumberFormatUtils.parseIntOrNull(get("sliczba")))
					.setsWol(NumberFormatUtils.parseIntOrNull(get("swolumen")))
					.setsLim(NumberFormatUtils.parseOrNull(get("slimit")));
		}

		return builder.build();
	}

	public String getKurs() {
		String kursost1 = get("kursost1");
		String kursotw = get("kursotw");
		if (kursost1 != null)
			return kursost1;
		if (kursotw != null)
			return kursotw;
		return null;
	}
}
