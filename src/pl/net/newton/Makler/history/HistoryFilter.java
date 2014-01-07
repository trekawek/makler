package pl.net.newton.Makler.history;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import pl.net.newton.Makler.db.symbol.Symbol;

public class HistoryFilter {
	private HistoryProvider provider;

	public final static int MINUTES_IN_DAY = (17 - 9) * 60 + 35;

	public HistoryFilter(HistoryProvider provider) {
		this.provider = provider;
	}

	public EntryListWithIndexes historyByInt(int graphRange, Symbol symbol, boolean force) {
		switch (graphRange) {
			case 1:
				return intraday5Days(symbol, force);
			case 2:
				return history1Month(symbol, force);
			case 3:
				return history3Month(symbol, force);
			case 4:
				return history1Year(symbol, force);
			case 5:
				return history2Years(symbol, force);
			case 6:
				return history(symbol, force);
			case 0:
			default:
				return intradayToday(symbol, force);
		}
	}

	public EntryListWithIndexes intradayToday(Symbol symbol, boolean force) {
		EntryListWithIndexes entries = provider.getIntraday(symbol, force);
		entries = lastDays(entries, 1, 2);
		return entries;
	}

	public EntryListWithIndexes intraday5Days(Symbol symbol, boolean force) {
		EntryListWithIndexes entries = provider.getIntraday(symbol, force);
		return lastDays(entries, 5, 15);
	}

	public EntryListWithIndexes history1Month(Symbol symbol, boolean force) {
		EntryListWithIndexes entries = provider.getIntraday(symbol, force);
		return lastDays(entries, 30, 30);
	}

	public EntryListWithIndexes history3Month(Symbol symbol, boolean force) {
		EntryListWithIndexes entries = provider.getHistory(symbol, force);
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.MONTH, -3);
		return sinceDate(entries, cal.getTime(), 1);
	}

	public EntryListWithIndexes history1Year(Symbol symbol, boolean force) {
		EntryListWithIndexes entries = provider.getHistory(symbol, force);
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.YEAR, -1);
		return sinceDate(entries, cal.getTime(), 2);
	}

	public EntryListWithIndexes history2Years(Symbol symbol, boolean force) {
		EntryListWithIndexes entries = provider.getHistory(symbol, force);
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.YEAR, -2);
		return sinceDate(entries, cal.getTime(), 2);
	}

	public EntryListWithIndexes history(Symbol symbol, boolean force) {
		EntryListWithIndexes entries = provider.getHistory(symbol, force);
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.YEAR, 1990);
		return sinceDate(entries, cal.getTime(), 5);
	}

	private EntryListWithIndexes sinceDate(EntryListWithIndexes entries, Date since, int resolutionIdDays) {
		if (entries == null)
			return null;

		int i, l = entries.getLength();
		for (i = 0; i < l; i++)
			if (!new Date(entries.getDate(i)).before(since))
				break;
		return setGraphIndex(entries.subList(i, l), resolutionIdDays * MINUTES_IN_DAY);
	}

	private EntryListWithIndexes lastDays(EntryListWithIndexes entriesWithIndexes, int days,
			int resolutionInMinutes) {
		int d, l = entriesWithIndexes.getLength(), i = l - 1;
		if (l > 0) {
			Calendar cal = Calendar.getInstance();
			cal.setTimeInMillis(entriesWithIndexes.getDate(l - 1));
			int lastDay = cal.get(Calendar.DAY_OF_MONTH);
			for (d = 0, i = l - 1; i >= 0; i--) {
				cal.setTimeInMillis(entriesWithIndexes.getDate(i));
				int day = cal.get(Calendar.DAY_OF_MONTH);
				if (day != lastDay) {
					d++;
					lastDay = day;
				}
				if (d == days)
					break;
			}
		}
		entriesWithIndexes = entriesWithIndexes.subList(i + 1, l);
		return setGraphIndex(entriesWithIndexes, resolutionInMinutes);
	}

	private EntryListWithIndexes setGraphIndex(EntryListWithIndexes entriesWithIndexes,
			int resolutionInMinutes) {
		int day = -1;
		int lastDay = -1;
		int lastMinute = -1;
		long dayBeginning = 0;

		List<Integer> indexes = new ArrayList<Integer>();
		EntryList entries = entriesWithIndexes.getEntryList();

		// 9:00 - 17:35
		for (Integer i : entriesWithIndexes.getIndexes()) {
			Calendar cal = Calendar.getInstance();
			cal.setTimeInMillis(entries.getDate(i));

			int currentDay = cal.get(Calendar.DAY_OF_MONTH);
			if (lastDay != currentDay) {
				day++;
				lastDay = currentDay;

				cal.set(Calendar.HOUR_OF_DAY, 9);
				cal.set(Calendar.MINUTE, 0);
				cal.set(Calendar.SECOND, 0);
				dayBeginning = cal.getTimeInMillis();
			}
			int minuteOfTheDay = (int) (entries.getDate(i) - dayBeginning) / (60 * 1000);
			if (minuteOfTheDay < 0)
				minuteOfTheDay = 0;
			int currentMinute = day * MINUTES_IN_DAY + minuteOfTheDay;
			if (lastMinute == -1 || (currentMinute - lastMinute) >= resolutionInMinutes) {
				lastMinute = currentMinute;
				entries.setGraphIndex(i, currentMinute);
				indexes.add(i);
			}
		}
		return new EntryListWithIndexes(entries, indexes);
	}

	public boolean isRangeExist(Symbol symbol, int graphRange) {
		// TODO Auto-generated method stub
		return provider.isRangeExist(symbol, graphRange);
	}
}
