package pl.net.newton.Makler.common;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import android.util.Log;

public final class DateFormatUtils {
	private static final String TAG = "MaklerDateFormatUtils";

	private static final String YYYY_MM_DD = "yyyy-MM-dd";

	private static final String HH_MM_SS = "HH:mm:ss";

	private DateFormatUtils() {
	}

	public static String formatCurrentDate() {
		return formatDate(new Date());
	}

	public static String formatCurrentTime() {
		return formatTime(Calendar.getInstance());
	}

	public static Date parseDate(String s) throws ParseException {
		return getDateFormat().parse(s);
	}

	public static String formatDate(Date date) {
		if (date == null) {
			return "-";
		} else {
			return getDateFormat().format(date);
		}
	}

	public static String formatTime(Calendar date) {
		if (date == null) {
			return "-";
		} else {
			return getTimeFormat().format(date.getTime());
		}
	}

	public static Calendar parseTime(String s) throws ParseException {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(getTimeFormat().parse(s));
		return calendar;
	}

	public static Calendar safeParseTime(String string) {
		if (string == null) {
			return null;
		}
		try {
			return parseTime(string);
		} catch (Exception e) {
			Log.e(TAG, "Can't parse time", e);
			return null;
		}
	}

	private static DateFormat getTimeFormat() {
		return new SimpleDateFormat(HH_MM_SS, LocaleUtils.LOCALE);
	}

	private static DateFormat getDateFormat() {
		return new SimpleDateFormat(YYYY_MM_DD, LocaleUtils.LOCALE);
	}
}
