package pl.net.newton.Makler.common;

import java.util.Calendar;
import java.util.TimeZone;

public final class GpwUtils {
	private static final TimeZone WARSAW_TIMEZONE = TimeZone.getTimeZone("Europe/Warsaw");

	private GpwUtils() {
	}

	public static boolean gpwActive() {
		if (Configuration.DEBUG_UPDATES) {
			return true;
		}

		Calendar c = Calendar.getInstance();
		c.setTimeZone(WARSAW_TIMEZONE);
		return !isWeekend(c) && workingHours(c);
	}

	public static boolean isOvertime(Calendar calendar) {
		int hour = calendar.get(Calendar.HOUR_OF_DAY);
		int minute = calendar.get(Calendar.MINUTE);
		return hour == 17 && minute >= 20 && minute < 30;
	}

	private static boolean isWeekend(Calendar calendar) {
		int dayOfTheWeek = calendar.get(Calendar.DAY_OF_WEEK);
		return dayOfTheWeek == Calendar.SATURDAY || dayOfTheWeek == Calendar.SUNDAY;
	}

	private static boolean workingHours(Calendar calendar) {
		int hour = calendar.get(Calendar.HOUR_OF_DAY);
		int minute = calendar.get(Calendar.MINUTE);
		if (hour < 8 || hour > 17) {
			return false;
		}
		if (hour == 17 && minute > 50) {
			return false;
		}
		return true;
	}
}
