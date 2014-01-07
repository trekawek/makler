package pl.net.newton.Makler.common;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DateFormatUtils {
	private static final DateFormat YYYY_MM_DD = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

	private static final DateFormat HH_MM_SS = new SimpleDateFormat("HH:mm:ss", Locale.US);

	public static String formatYyyyMmDd() {
		return formatYyyyMmDd(new Date());
	}

	public static Date parseYyyyMmSs(String s) throws ParseException {
		return YYYY_MM_DD.parse(s);
	}

	public static String formatYyyyMmDd(Date date) {
		if (date == null) {
			return "-";
		} else {
			return YYYY_MM_DD.format(date);
		}
	}

	public static String formatHhMmSs(Calendar date) {
		if (date == null) {
			return "-";
		} else {
			return HH_MM_SS.format(date);
		}
	}

	public static Date parseHhMmSs(String s) throws ParseException {
		return HH_MM_SS.parse(s);
	}

	public static Date parseHhMmSsOrNull(String string) {
		try {
			return parseHhMmSs(string);
		} catch (Exception e) {
			return null;
		}
	}
}
