package pl.net.newton.Makler.common;

import java.util.Calendar;
import java.util.TimeZone;

public class GpwUtils {
	private static final TimeZone WARSAW_TIMEZONE = TimeZone.getTimeZone("Europe/Warsaw");

	public static boolean gpwActive() {
		if(Configuration.DEBUG_UPDATES) {
			return true;
		}

		Calendar c = Calendar.getInstance();
		c.setTimeZone(WARSAW_TIMEZONE);

		int dayOfTheWeek = c.get(Calendar.DAY_OF_WEEK);
		int hour = c.get(Calendar.HOUR_OF_DAY);
		int minute = c.get(Calendar.MINUTE);
		if (dayOfTheWeek == Calendar.SATURDAY || dayOfTheWeek == Calendar.SUNDAY) {
			return false;
		}
		if (hour < 8 || hour > 17) {
			return false;
		}
		if (hour == 17 && minute > 50) {
			return false;
		}
		return true;
	}
}
