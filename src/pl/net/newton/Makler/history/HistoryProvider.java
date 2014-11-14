package pl.net.newton.Makler.history;

import pl.net.newton.Makler.db.symbol.Symbol;

public interface HistoryProvider {
	EntryListWithIndexes getHistory(Symbol symbol, boolean force);

	EntryListWithIndexes getIntraday(Symbol symbol, boolean force);

	boolean rangeExist(final Symbol symbol, int range);
}
