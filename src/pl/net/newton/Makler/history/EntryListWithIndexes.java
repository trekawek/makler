package pl.net.newton.Makler.history;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class EntryListWithIndexes implements Serializable {
	private static final long serialVersionUID = 4945948104655442906L;

	private final EntryList entryList;

	private final List<Integer> indexes;

	public EntryListWithIndexes(EntryList entryList, List<Integer> indexes) {
		this.entryList = entryList;
		this.indexes = indexes;
	}

	public EntryListWithIndexes(EntryList entryList) {
		this.entryList = entryList;
		this.indexes = new ArrayList<Integer>(entryList.getLength());
		for (int i = 0; i < entryList.getLength(); i++)
			this.indexes.add(i);
	}

	public EntryList getEntryList() {
		return entryList;
	}

	public List<Integer> getIndexes() {
		return indexes;
	}

	public int getLength() {
		return indexes.size();
	}

	public int getGraphIndex(int i) {
		return entryList.getGraphIndex(indexes.get(i));
	}

	public void setGraphIndex(int i, int graphIndex) {
		entryList.setGraphIndex(indexes.get(i), graphIndex);
	}

	public long getDate(int i) {
		return entryList.getDate(indexes.get(i));
	}

	public int getOpen(int i) {
		return entryList.getOpen(indexes.get(i));
	}

	public int getClose(int i) {
		return entryList.getClose(indexes.get(i));
	}

	public int getLow(int i) {
		return entryList.getLow(indexes.get(i));
	}

	public int getHigh(int i) {
		return entryList.getHigh(indexes.get(i));
	}

	public long getVol(int i) {
		return entryList.getVol(indexes.get(i));
	}

	public boolean isIntraday(int i) {
		return entryList.isIntraday(i);
	}

	public EntryListWithIndexes subList(int start, int end) {
		return new EntryListWithIndexes(entryList, indexes.subList(start, end));
	}
}
