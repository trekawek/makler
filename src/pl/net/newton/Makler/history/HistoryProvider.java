package pl.net.newton.Makler.history;

import pl.net.newton.Makler.db.symbol.Symbol;

public interface HistoryProvider {
	public abstract EntryListWithIndexes getHistory(Symbol symbol, boolean force);

	public abstract EntryListWithIndexes getIntraday(Symbol symbol, boolean force);

	public abstract boolean isRangeExist(final Symbol symbol, int range);
}
