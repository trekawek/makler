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
import pl.net.newton.Makler.db.symbol.Symbol;
import pl.net.newton.Makler.db.symbol.SymbolBuilder;
import pl.net.newton.Makler.httpClient.Connector;

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
				if (StringUtils.trimToNull(line) == null) {
					continue;
				}
				Quote q = new Quote(line);
				quotes.add(q);
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

	private BufferedReader get(String path) throws IOException {
		final InputStream is = connector.get(path, null);
		return new BufferedReader(new InputStreamReader(is));
	}

}
