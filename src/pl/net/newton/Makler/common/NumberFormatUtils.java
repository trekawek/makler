package pl.net.newton.Makler.common;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.ParseException;

public final class NumberFormatUtils {
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
		try {
			return parse(s);
		} catch (NullPointerException e) {
			return null;
		} catch (NumberFormatException e) {
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
		try {
			return parseInt(s);
		} catch (NullPointerException e) {
			return null;
		} catch (NumberFormatException e) {
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
			return 0.0;
		}
	}
}
