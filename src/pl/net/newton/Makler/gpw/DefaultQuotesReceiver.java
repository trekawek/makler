package pl.net.newton.Makler.gpw;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import android.content.Context;
import android.provider.Settings.Secure;
import android.util.Log;
import pl.net.newton.Makler.db.quote.Quote;
import pl.net.newton.Makler.db.quote.QuoteBuilder;
import pl.net.newton.Makler.db.symbol.Symbol;
import pl.net.newton.Makler.db.symbol.SymbolBuilder;
import pl.net.newton.Makler.httpClient.Connector;
import pl.net.newton.Makler.common.Configuration;
import pl.net.newton.Makler.common.DateFormatUtils;
import pl.net.newton.Makler.common.NumberFormatUtils;

public class DefaultQuotesReceiver implements QuotesReceiver {
	private static final String TAG = "MaklerDefaultQuotesReceiver";

	private Context context;

	private Connector connector;

	public DefaultQuotesReceiver(Context ctx) {
		context = ctx;
		connector = new Connector("makler.newton.net.pl", 8080);
	}

	public boolean isRegistered() {
		String url = "/registered_user/" + androidId();
		BufferedReader reader = null;
		boolean result = false;
		try {
			reader = get(url);
			String line = reader.readLine();
			result = "ok".equals(StringUtils.trim(line));
			reader.close();
		} catch (IOException e) {
			Log.e(TAG, "Can't check if user is registered", e);
		}
		return result;
	}

	public List<Quote> getQuotesBySymbols(List<String> symbols) {
		List<Quote> quotes = new ArrayList<Quote>();
		if (symbols.isEmpty()) {
			return quotes;
		}
		StringBuilder url = new StringBuilder("/quote/");
		for (int i = 0; i < symbols.size() - 1; i++) {
			url.append(symbols.get(i));
			url.append(',');
		}
		url.append(symbols.get(symbols.size() - 1));
		url.append("/").append(androidId());

		BufferedReader reader = null;
		try {
			reader = get(url.toString());
			String line;
			while ((line = reader.readLine()) != null) {
				Quote q = quoteFromLine(line);
				if (q != null) {
					quotes.add(q);
				}
			}
			reader.close();
		} catch (IOException e) {
			Log.e(TAG, "can't read quotes", e);
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
		StringBuilder url = new StringBuilder("/symbols");
		url.append("/").append(lastUpdate);
		url.append("/").append(androidId());

		BufferedReader reader = null;
		try {
			reader = get(url.toString());
			String line;
			while ((line = reader.readLine()) != null) {
				String[] a = StringUtils.split(line, '|');
				SymbolBuilder builder = new SymbolBuilder();
				builder.setSymbol(a[0]).setName(a[1]).setIsIndex("1".equals(a[2]))
						.setDeleted("1".equals(a[3])).setCode(a[4]);
				symbols.add(builder.build());
			}
			reader.close();
		} catch (IOException e) {
			Log.e(TAG, "can't get symbols", e);
		}
		return symbols;
	}

	private String androidId() {
		String id = Secure.getString(context.getContentResolver(), Secure.ANDROID_ID);
		if (id == null) {
			return "2fc89b9c332df4a6";
		} else {
			return id;
		}
	}

	private Quote quoteFromLine(String line) {
		String[] a = line.split("\\|");
		QuoteBuilder builder = new QuoteBuilder();

		builder.setSymbol(a[0]).setName(a[1]);
		builder.setUpdate(a[2]);
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
			Log.e(TAG, "Can't parse quote", e);
		}

		if (Configuration.DEBUG_UPDATES) {
			builder.setUpdate(DateFormatUtils.formatCurrentTime());
		}
		return builder.build();
	}

	private BufferedReader get(String path) throws IOException {
		final InputStream is = connector.get(path, null);
		return new BufferedReader(new InputStreamReader(is));
	}

}
