package pl.net.newton.Makler.gpw;

import java.io.BufferedReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import android.content.Context;
import android.provider.Settings.Secure;
import android.util.Log;
import pl.net.newton.Makler.common.Configuration;
import pl.net.newton.Makler.common.DateFormatUtils;
import pl.net.newton.Makler.common.NumberFormatUtils;
import pl.net.newton.Makler.db.quote.Quote;
import pl.net.newton.Makler.db.quote.QuoteBuilder;
import pl.net.newton.Makler.db.symbol.Symbol;
import pl.net.newton.Makler.db.symbol.SymbolBuilder;
import pl.net.newton.Makler.db.symbol.SymbolsDb;
import pl.net.newton.Makler.httpClient.Connector;

public class DefaultQuotesReceiver implements QuotesReceiver {
	private static final String TAG = "Makler";

	private static final String URL = "http://makler.newton.net.pl:8080/";

	private Context context;

	private Connector connector;

	public DefaultQuotesReceiver(Context ctx) {
		context = ctx;
		connector = new Connector("makler.newton.net.pl", 8080);
	}

	public boolean isRegistered() {
		String url = DefaultQuotesReceiver.URL + "registered_user/" + androidId();
		try {
			BufferedReader buffer = connector.readUrl(url);
			String line = buffer.readLine();
			if (line != null) {
				return line.trim().equals("ok");
			}
		} catch (Exception e) {
			return false;
		}
		return false;
	}

	public List<Quote> getQuotesBySymbols(List<String> symbols) {
		List<Quote> quotes = new ArrayList<Quote>();
		if (symbols.size() == 0)
			return quotes;
		String url = DefaultQuotesReceiver.URL + "quote/";
		for (int i = 0; i < symbols.size() - 1; i++)
			url += symbols.get(i) + ",";
		url += symbols.get(symbols.size() - 1);
		url += "/" + androidId();

		try {
			BufferedReader buffer = connector.readUrl(url);
			String line;
			while ((line = buffer.readLine()) != null) {
				Quote q = null;
				try {
					q = quoteFromLine(line);
				} catch (Exception e) {
					Log.e(TAG, "error parsing quote: " + q, e);
				}
				quotes.add(q);
			}

		} catch (Exception e) {
			e.printStackTrace();
			return quotes;
		}
		return quotes;
	}

	public Quote getQuoteBySymbol(String symbol) {
		List<String> symbols = new ArrayList<String>();
		symbols.add(symbol);
		return getQuotesBySymbols(symbols).get(0);
	}

	public List<Symbol> getSymbols() {
		return getSymbols("0000-00-00");
	}

	public List<Symbol> getSymbols(String lastUpdate) {
		List<Symbol> symbols = new ArrayList<Symbol>();
		String url = DefaultQuotesReceiver.URL + "symbols";
		url += "/" + lastUpdate;
		url += "/" + androidId();

		try {
			BufferedReader buffer = connector.readUrl(url);
			String line;
			while ((line = buffer.readLine()) != null) {
				String[] a = line.split("\\|");

				SymbolBuilder builder = new SymbolBuilder();
				builder.setSymbol(a[0]).setName(a[1]).setIsIndex(a[2].equals("1")).setDeleted(a[3].equals("1"))
						.setCode(a[4]);
				symbols.add(builder.build());
			}

		} catch (Exception e) {
			Log.e(TAG, "can't get symbols", e);
			return symbols;
		}
		return symbols;
	}

	private String androidId() {
		String id = Secure.getString(context.getContentResolver(), Secure.ANDROID_ID);
		if (id == null)
			return "2fc89b9c332df4a6";
		else
			return id;
	}

	public void startSession(Context ctx, SymbolsDb symbolsDb) {
	}

	public void stopSession() {
	}

	private Quote quoteFromLine(String line) {
		String[] a = line.split("\\|");
		QuoteBuilder builder = new QuoteBuilder();

		builder.setSymbol(a[0]).setName(a[1]);
		try {
			builder.setUpdate(DateFormatUtils.parseTime(a[2]));
		} catch (ParseException e) {
		}
		try {
			builder.setKurs(NumberFormatUtils.parseOrNull(a[3]))
					.setZmiana(NumberFormatUtils.parseOrNull(a[4]))
					.setKursOdn(NumberFormatUtils.parseOrNull(a[5]))
					.setKursOtw(NumberFormatUtils.parseOrNull(a[6]))
					.setKursMin(NumberFormatUtils.parseOrNull(a[7]))
					.setKursMax(NumberFormatUtils.parseOrNull(a[8]))
					.setTko(NumberFormatUtils.parseOrNull(a[9]))
					.setTkoProcent(NumberFormatUtils.parseOrNull(a[10]))
					.setWolumen(NumberFormatUtils.parseIntOrNull(a[11]))
					.setWartosc(NumberFormatUtils.parseOrNull(a[12]));
			if (a.length > 13) {
				builder.setkOfert(NumberFormatUtils.parseIntOrNull(a[13]))
						.setkWol(NumberFormatUtils.parseIntOrNull(a[14]))
						.setkLim(NumberFormatUtils.parseOrNull(a[15]))
						.setsLim(NumberFormatUtils.parseOrNull(a[16]))
						.setsWol(NumberFormatUtils.parseIntOrNull(a[17]))
						.setsOfert(NumberFormatUtils.parseIntOrNull(a[18]));
			}
		} catch (ArrayIndexOutOfBoundsException e) {
		}

		if (Configuration.DEBUG_UPDATES) {
			builder.setUpdate(new Date());
		}
		return builder.build();
	}

	public boolean supportTrades() {
		return false;
	}

	public Trades getTrades() {
		return null;
	}
}
