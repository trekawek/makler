package pl.net.newton.Makler.history.service;

import pl.net.newton.Makler.db.symbol.Symbol;

public interface HistoryService {
	public void register(HistoryListener listener);

	public void unregister(HistoryListener listener);

	public void historyByInt(final int graphRange, final Symbol symbol, final boolean force);

	public boolean isRangeExist(final Symbol symbol, int graphRange);
}
