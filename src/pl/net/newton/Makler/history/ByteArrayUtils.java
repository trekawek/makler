package pl.net.newton.Makler.history;

import java.util.Calendar;

public final class ByteArrayUtils {
	private ByteArrayUtils() {
	}

	public static int search(byte[] needle, byte[] haystack, int from, int to) {
		for (int i = from; i < to - needle.length; i++) {
			boolean ok = true;
			for (int j = 0; j < needle.length; j++) {
				if (needle[j] != haystack[i + j]) {
					ok = false;
					break;
				}
			}
			if (ok) {
				return i;
			}
		}
		return -1;
	}

	public static int nextLine(byte[] haystack, int offset) {
		return findNext((byte) '\n', haystack, offset) + 1;
	}

	public static int findNext(byte needle, byte[] haystack, int offset) {
		for (int i = offset; i < haystack.length; i++) {
			if (haystack[i] == needle) {
				return i;
			}
		}
		return -1;
	}

	public static long[] parseLong(byte[] array, int offset, long[] result) {
		return parseLong(array, offset, 0, result);
	}

	public static long[] parseLong(byte[] array, int offset, int multiplier, long[] result) {
		result[0] = 0;

		boolean decimal = false;
		int i;
		for (i = offset; i < array.length; i++) {
			if (array[i] == '.') {
				decimal = true;
				continue;
			} else if (array[i] < '0' || array[i] > '9') {
				break;
			}
			if (decimal == true && multiplier == 0) {
				break;
			}
			result[0] *= 10;
			result[0] += array[i] - '0';
			if (decimal) {
				multiplier--;
			}
		}
		for (int j = 0; j < multiplier; j++) {
			result[0] *= 10;
		}
		result[1] = i;
		return result;
	}

	public static void setDate(Calendar cal, long date) {
		int year = (int) (date / 10000);
		int month = (int) ((date / 100) % 100);
		int day = (int) (date % 100);
		cal.set(year, month - 1, day);
	}

	public static void setTime(Calendar cal, long time) {
		int hour = (int) (time / 10000);
		int minute = (int) ((time / 100) % 100);
		int second = (int) (time % 100);
		cal.set(Calendar.HOUR_OF_DAY, hour);
		cal.set(Calendar.MINUTE, minute);
		cal.set(Calendar.SECOND, second);
	}

}
