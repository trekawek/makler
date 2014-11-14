package pl.net.newton.Makler.history;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import android.content.Context;
import android.util.Log;
import pl.net.newton.Makler.db.symbol.Symbol;
import pl.net.newton.Makler.httpClient.Connector;

public class BossaProvider implements HistoryProvider {
	private static final String TAG = "Makler";

	private static final String HOST = "bossa.pl";

	private static final String INTRADAY_PATH = "/notowania/wykresy/data/n_int5.txt";

	private static final String HISTORY_PATH = "/notowania/wykresy/data/n.txt";

	private static final String QUERY = "id=%s";

	private static final String HISTORY_FILE_PREFIX = "history_cache_v2_";

	private static final String INTRADAY_FILE_PREFIX = "intraday_cache_v2_";

	private final Connector conn;

	private final Cache intradayCache;

	private final Cache historyCache;

	public BossaProvider(Context ctx) {
		conn = new Connector(HOST, 80);
		intradayCache = new Cache(INTRADAY_FILE_PREFIX, 5, 15 * 60, ctx);
		historyCache = new Cache(HISTORY_FILE_PREFIX, 5, 24 * 60 * 60, ctx);
	}

	public EntryListWithIndexes getIntraday(Symbol symbol, boolean force) {
		final String symbolName = symbol.getSymbol();
		if (intradayCache.hasKey(symbolName) && !force) {
			return new EntryListWithIndexes(intradayCache.getEntry(symbolName));
		}

		final String query = String.format(QUERY, symbolName);
		final EntryList entries = getEntries(INTRADAY_PATH, query);
		intradayCache.addEntry(symbolName, entries);
		return new EntryListWithIndexes(entries);
	}

	public EntryListWithIndexes getHistory(Symbol symbol, boolean force) {
		final String symbolName = symbol.getSymbol();
		if (historyCache.hasKey(symbolName) && !force) {
			return new EntryListWithIndexes(historyCache.getEntry(symbolName));
		}

		final String query = String.format(QUERY, symbolName);
		final EntryList entries = getEntries(HISTORY_PATH, query);
		historyCache.addEntry(symbolName, entries);
		return new EntryListWithIndexes(entries);
	}

	private EntryList getEntries(String path, String query) {
		boolean withTime = false;
		byte[] byteArray;
		EntryList list = new EntryList(0, false);
		int i = 0;
		try {
			Log.d(TAG, path);
			InputStream is = conn.get(path, query);
			ZipInputStream zis = new ZipInputStream(is);
			ByteArrayOutputStream bos = new ByteArrayOutputStream(150000);
			Log.d(TAG, "history loaded");
			ZipEntry entry = zis.getNextEntry();
			Log.d(TAG, "entry name: " + entry.getName());
			ChannelTools.copy(zis, bos);
			Log.d(TAG, "copied to ByteArray");

			byteArray = bos.toByteArray();
			ByteArrayInputStream bis = new ByteArrayInputStream(byteArray);
			int lines = count(bis);
			Log.d(TAG, "lines counted: " + lines);

			i = ByteArrayUtils.nextLine(byteArray, 0);
			withTime = ByteArrayUtils.search("<TIME>".getBytes("UTF-8"), byteArray, 0, i) != -1;
			list = new EntryList(lines, withTime);
		} catch (Exception e) {
			Log.e(TAG, "can't get entries", e);
			return list;
		}

		Log.d(TAG, "parsing lines");
		while (i < byteArray.length) {
			try {
				i = list.addEntry(byteArray, i);
			} catch (IndexOutOfBoundsException e) {
				Log.e(TAG, "index out of bounds", e);
				break;
			}
		}
		Log.d(TAG, "lines parse");
		return list;
	}

	public int count(InputStream is) throws IOException {
		byte[] c = new byte[1024];
		int count = 0;
		int readChars = 0;
		while ((readChars = is.read(c)) != -1) {
			for (int i = 0; i < readChars; ++i) {
				if (c[i] == '\n') {
					++count;
				}
			}
		}
		return count;
	}

	public boolean rangeExist(final Symbol symbol, int range) {
		String symbolName = symbol.getSymbol();

		if (range >= 0 && range <= 2 && !intradayCache.hasKey(symbolName)) {
			return false;
		}

		if (range >= 3 && range <= 6 && !historyCache.hasKey(symbolName)) {
			return false;
		}

		return true;
	}
}
