package pl.net.newton.Makler.history;

import java.io.Serializable;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class EntryList implements Serializable {
	private static final long serialVersionUID = -8335210038807291253L;

	private static final BigDecimal ONE_HUNDRED = new BigDecimal(100);

	private long[] date;

	private int[] close;

	private int[] graphIndex;

	private long[] vol;

	private long[] tmp;

	private boolean intraday;

	private int length;

	private int i;

	private final DateFormat dateWithFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);

	private final DateFormat dateWithoutFormat = new SimpleDateFormat("yyyyMMdd", Locale.US);

	private final Calendar now = Calendar.getInstance();

	public EntryList(int length, boolean intraday) {
		date = new long[length];
		close = new int[length];
		graphIndex = new int[length];
		vol = new long[length];
		tmp = new long[2];
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
			result = ByteArrayUtils.parseLong(array, from, tmp);
			if (result[1] == from) {
				return ByteArrayUtils.nextLine(array, (int) result[1]);
			}
			ByteArrayUtils.setDate(now, result[0]);
			result = ByteArrayUtils.parseLong(array, (int) result[1] + 1, tmp);
			ByteArrayUtils.setTime(now, result[0]);
			date[i] = now.getTimeInMillis();
			result = ByteArrayUtils.parseLong(array, (int) result[1] + 1, 2, tmp);
			result = ByteArrayUtils.parseLong(array, (int) result[1] + 1, 2, tmp);
			result = ByteArrayUtils.parseLong(array, (int) result[1] + 1, 2, tmp);
			result = ByteArrayUtils.parseLong(array, (int) result[1] + 1, 2, tmp);
			close[i] = (int) result[0];
			result = ByteArrayUtils.parseLong(array, (int) result[1] + 1, tmp);
			if (result[0] <= Integer.MAX_VALUE) {
				vol[i] = (int) result[0];
			} else {
				vol[i] = Integer.MAX_VALUE;
			}
		} else {
			result = ByteArrayUtils.parseLong(array, from, tmp);
			if (result[1] == from) {
				return ByteArrayUtils.nextLine(array, (int) result[1]);
			}
			ByteArrayUtils.setDate(now, result[0]);
			date[i] = now.getTimeInMillis();
			result = ByteArrayUtils.parseLong(array, (int) result[1] + 1, 2, tmp);
			result = ByteArrayUtils.parseLong(array, (int) result[1] + 1, 2, tmp);
			result = ByteArrayUtils.parseLong(array, (int) result[1] + 1, 2, tmp);
			result = ByteArrayUtils.parseLong(array, (int) result[1] + 1, 2, tmp);
			close[i] = (int) result[0];
			result = ByteArrayUtils.parseLong(array, (int) result[1] + 1, tmp);
			if (result[0] <= Integer.MAX_VALUE) {
				vol[i] = (int) result[0];
			} else {
				vol[i] = Integer.MAX_VALUE;
			}
		}
		i++;
		return ByteArrayUtils.nextLine(array, (int) result[1]);
	}

	public int addEntry(String row) throws ParseException {
		if (i >= length) {
			throw new IndexOutOfBoundsException();
		}

		String[] s = row.split(",");
		if (intraday) {
			date[i] = dateWithFormat.parse(s[0] + "_" + s[1]).getTime();
			close[i] = new BigDecimal(s[5]).multiply(ONE_HUNDRED).intValue();
		} else {
			date[i] = dateWithoutFormat.parse(s[0]).getTime();
			close[i] = new BigDecimal(s[4]).multiply(ONE_HUNDRED).intValue();
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

	public int getClose(int i) {
		return close[i];
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
