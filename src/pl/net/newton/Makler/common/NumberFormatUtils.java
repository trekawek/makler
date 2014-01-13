package pl.net.newton.Makler.common;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.ParseException;

import android.util.Log;

public final class NumberFormatUtils {
	private static final String TAG = "MaklerNumberFormat";
	
	private static final NumberFormat NF;

	static {
		NF = NumberFormat.getInstance(LocaleUtils.LOCALE);
		NF.setMaximumFractionDigits(2);
		NF.setGroupingUsed(true);
	}

	private NumberFormatUtils() {
	}

	public static String formatNumber(Double d) {
		if (d == null) {
			return "-";
		} else {
			return NF.format(d).replace('.', ',');
		}
	}

	public static String formatNumber(BigDecimal n) {
		if (n == null) {
			return "-";
		} else {
			return NF.format(n).replace('.', ',');
		}
	}

	public static String formatNumber(Integer n) {
		if (n == null) {
			return "-";
		} else {
			return NF.format(n).replace('.', ',');
		}
	}

	public static BigDecimal parse(String s) {
		String newString;
		newString = s.replaceAll("[^0-9.,-]", "");
		newString = newString.replace(',', '.');
		return new BigDecimal(newString);
	}

	public static BigDecimal parseOrNull(String s) {
		if (s == null) {
			return null;
		}
		try {
			return parse(s);
		} catch (NumberFormatException e) {
			Log.e(TAG, "Can't parse number", e);
			return null;
		}
	}

	public static BigDecimal parserOrZero(String s) {
		BigDecimal b = parseOrNull(s);
		if (b == null) {
			return BigDecimal.ZERO;
		} else {
			return b;
		}
	}

	public static int parseInt(String s) {
		String newString;
		newString = s.replace(" ", "");
		return Integer.parseInt(newString);
	}

	public static Integer parseIntOrNull(String s) {
		if (s == null) {
			return null;
		}
		try {
			return parseInt(s);
		} catch (NumberFormatException e) {
			Log.e(TAG, "Can't parse number", e);
			return null;
		}
	}

	public static int parseIntOrZero(String s) {
		Integer i = parseIntOrNull(s);
		if (i == null) {
			return 0;
		} else {
			return i;
		}
	}

	public static double parseDoubleOrZero(String s) {
		try {
			return NF.parse(s).doubleValue();
		} catch (ParseException e) {
			Log.e(TAG, "Can't parse number", e);
			return 0.0;
		}
	}
}
