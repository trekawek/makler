package pl.net.newton.Makler.common;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public final class DateFormatUtils {
	private static final DateFormat YYYY_MM_DD = new SimpleDateFormat("yyyy-MM-dd", LocaleUtils.LOCALE);

	private static final DateFormat HH_MM_SS = new SimpleDateFormat("HH:mm:ss", LocaleUtils.LOCALE);

	private DateFormatUtils() {
	}

	public static String formatCurrentDate() {
		return formatDate(new Date());
	}

	public static Date parseDate(String s) throws ParseException {
		return YYYY_MM_DD.parse(s);
	}

	public static String formatDate(Date date) {
		if (date == null) {
			return "-";
		} else {
			return YYYY_MM_DD.format(date);
		}
	}

	public static String formatTime(Calendar date) {
		if (date == null) {
			return "-";
		} else {
			return HH_MM_SS.format(date.getTime());
		}
	}

	public static Date parseTime(String s) throws ParseException {
		return HH_MM_SS.parse(s);
	}

	public static Date safeParseTime(String string) {
		try {
			return parseTime(string);
		} catch (Exception e) {
			return null;
		}
	}
}
