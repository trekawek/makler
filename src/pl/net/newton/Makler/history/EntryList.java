package pl.net.newton.Makler.history;

import java.io.Serializable;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class EntryList implements Serializable {
	private static final long serialVersionUID = -8335210038807291253L;

	private long[] date;

	private int[] open;

	private int[] close;

	private int[] low;

	private int[] high;

	private int[] graphIndex;

	private long[] vol;

	private boolean intraday;

	private int length;

	private int i;

	private static final DateFormat DATE_WITH_FORMAT = new SimpleDateFormat("yyyyMMdd_HHmmss");

	private static final DateFormat DATE_WITHOUT_FORMAT = new SimpleDateFormat("yyyyMMdd");

	private static final BigDecimal ONE_HUNDRED = new BigDecimal(100);

	private static final Calendar cal = Calendar.getInstance();

	public EntryList(int length, boolean intraday) {
		date = new long[length];
		// open = new int[length];
		close = new int[length];
		// low = new int[length];
		// high = new int[length];
		graphIndex = new int[length];
		vol = new long[length];
		this.intraday = intraday;
		this.i = 0;
		this.length = length;
	}

	public int addEntry(byte[] array, int from) {
		if (i >= length) {
			throw new IndexOutOfBoundsException();
		}

		long[] result;
		if (intraday) {
			result = ByteArrayUtils.parseLong(array, from);
			if (result[1] == from) {
				return ByteArrayUtils.nextLine(array, (int) result[1]);
			}
			ByteArrayUtils.setDate(cal, result[0]);
			result = ByteArrayUtils.parseLong(array, (int) result[1] + 1);
			ByteArrayUtils.setTime(cal, result[0]);
			date[i] = cal.getTimeInMillis();
			result = ByteArrayUtils.parseLong(array, (int) result[1] + 1, 2);
			// open[i] = (int) result[0];
			result = ByteArrayUtils.parseLong(array, (int) result[1] + 1, 2);
			// high[i] = (int) result[0];
			result = ByteArrayUtils.parseLong(array, (int) result[1] + 1, 2);
			// low[i] = (int) result[0];
			result = ByteArrayUtils.parseLong(array, (int) result[1] + 1, 2);
			close[i] = (int) result[0];
			result = ByteArrayUtils.parseLong(array, (int) result[1] + 1);
			if (result[0] <= Integer.MAX_VALUE)
				vol[i] = (int) result[0];
			else
				vol[i] = Integer.MAX_VALUE;
		} else {
			result = ByteArrayUtils.parseLong(array, from);
			if (result[1] == from) {
				return ByteArrayUtils.nextLine(array, (int) result[1]);
			}
			ByteArrayUtils.setDate(cal, result[0]);
			date[i] = cal.getTimeInMillis();
			result = ByteArrayUtils.parseLong(array, (int) result[1] + 1, 2);
			// open[i] = (int) result[0];
			result = ByteArrayUtils.parseLong(array, (int) result[1] + 1, 2);
			// high[i] = (int) result[0];
			result = ByteArrayUtils.parseLong(array, (int) result[1] + 1, 2);
			// low[i] = (int) result[0];
			result = ByteArrayUtils.parseLong(array, (int) result[1] + 1, 2);
			close[i] = (int) result[0];
			result = ByteArrayUtils.parseLong(array, (int) result[1] + 1);
			if (result[0] <= Integer.MAX_VALUE)
				vol[i] = (int) result[0];
			else
				vol[i] = Integer.MAX_VALUE;
		}
		i++;
		return ByteArrayUtils.nextLine(array, (int) result[1]);
	}

	public int addEntry(String row) throws ParseException {
		if (i >= length) {
			throw new IndexOutOfBoundsException();
		}

		String s[] = row.split(",");
		if (intraday) {
			date[i] = DATE_WITH_FORMAT.parse(s[0] + "_" + s[1]).getTime();
			// open[i] = new BigDecimal(s[2]).multiply(ONE_HUNDRED).intValue();
			// high[i] = new BigDecimal(s[3]).multiply(ONE_HUNDRED).intValue();
			// low[i] = new BigDecimal(s[4]).multiply(ONE_HUNDRED).intValue();
			close[i] = new BigDecimal(s[5]).multiply(ONE_HUNDRED).intValue();
			// vol[i] = new BigDecimal(s[6]).longValue();
		} else {
			date[i] = DATE_WITHOUT_FORMAT.parse(s[0]).getTime();
			// open[i] = new BigDecimal(s[1]).multiply(ONE_HUNDRED).intValue();
			// high[i] = new BigDecimal(s[2]).multiply(ONE_HUNDRED).intValue();
			// low[i] = new BigDecimal(s[3]).multiply(ONE_HUNDRED).intValue();
			close[i] = new BigDecimal(s[4]).multiply(ONE_HUNDRED).intValue();
			// vol[i] = new BigDecimal(s[5]).longValue();
		}
		return i++;
	}

	public int getGraphIndex(int i) {
		return graphIndex[i];
	}

	public void setGraphIndex(int i, int graphIndex) {
		this.graphIndex[i] = graphIndex;
	}

	public long getDate(int i) {
		return date[i];
	}

	public int getOpen(int i) {
		return open[i];
	}

	public int getClose(int i) {
		return close[i];
	}

	public int getLow(int i) {
		return low[i];
	}

	public int getHigh(int i) {
		return high[i];
	}

	public long getVol(int i) {
		return vol[i];
	}

	public boolean isIntraday(int i) {
		return intraday;
	}

	public int getLength() {
		return i;
	}
}
