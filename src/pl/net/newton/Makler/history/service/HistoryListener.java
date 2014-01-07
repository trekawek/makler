package pl.net.newton.Makler.history.service;

import pl.net.newton.Makler.history.EntryListWithIndexes;

public interface HistoryListener {

	void gotEntries(EntryListWithIndexes entries);

}
