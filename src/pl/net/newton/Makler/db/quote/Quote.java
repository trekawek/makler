package pl.net.newton.Makler.db.quote;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Calendar;
import java.util.EnumMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import android.content.ContentValues;
import android.database.Cursor;
import pl.net.newton.Makler.common.DateFormatUtils;
import pl.net.newton.Makler.common.GpwUtils;
import pl.net.newton.Makler.common.NumberFormatUtils;
import static pl.net.newton.Makler.db.quote.QuoteField.*;

public class Quote {
	private final Map<QuoteField, String> map = new EnumMap<QuoteField, String>(QuoteField.class);

	public Quote(Cursor cursor) {
		for (QuoteField f : QuoteField.values()) {
			final String columnName = f.getDatabaseField();
			final int columnIndex = cursor.getColumnIndex(columnName);
			if (columnIndex != -1) {
				map.put(f, cursor.getString(columnIndex));
			}
		}
	}

	public Quote(String line) {
		final String[] split = StringUtils.split(line, '|');
		for (int i = 0; i < split.length; i++) {
			map.put(QuoteField.values()[i], split[i]);
		}
	}

	public String get(QuoteField field) {
		return map.get(field);
	}

	public BigDecimal getAsDecimal(QuoteField field) {
		return NumberFormatUtils.parseOrNull(get(field));
	}

	public int getAsInt(QuoteField field) {
		return NumberFormatUtils.parseIntOrZero(get(field));
	}

	public Calendar getAsCalendar(QuoteField field) {
		return DateFormatUtils.safeParseTime(get(UPDATED));
	}

	public boolean getAsBoolean(QuoteField field) {
		return Boolean.parseBoolean(get(field));
	}

	public BigDecimal chooseKurs() {
		if (get(TKO) != null && GpwUtils.isOvertime(getAsCalendar(UPDATED))) {
			return getAsDecimal(TKO);
		}
		for (QuoteField field : Arrays.asList(QUOTE, TKO, OPEN, REFERENCE)) {
			final BigDecimal v = getAsDecimal(field);
			if (v != null) {
				return v;
			}
		}
		return null;
	}

	public BigDecimal chooseZmiana() {
		if (get(TKO_PERCENT) != null && GpwUtils.isOvertime(getAsCalendar(UPDATED))) {
			return getAsDecimal(TKO_PERCENT);
		}
		for (QuoteField field : Arrays.asList(CHANGE, TKO_PERCENT)) {
			final BigDecimal v = getAsDecimal(field);
			if (v != null) {
				return v;
			}
		}
		return null;
	}

	public String formatAskBid(QuoteField field) {
		final BigDecimal value = getAsDecimal(field);
		if (value != null) {
			switch (value.intValue()) {
				case -1:
					return "PCRO";
				case -2:
					return "PKC";
				default:
					break;
			}
		}
		return NumberFormatUtils.formatNumber(value);
	}

	public ContentValues getContentValue() {
		final ContentValues cv = new ContentValues();
		for (QuoteField f : QuoteField.values()) {
			if (!f.isIncludeInCv()) {
				continue;
			}
			final String v = get(f);
			if (v == null) {
				cv.putNull(f.getDatabaseField());
			} else {
				cv.put(f.getDatabaseField(), v);
			}
		}
		if (get(UPDATED) != null) {
			cv.put(UPDATED.getDatabaseField(), DateFormatUtils.formatTime(getAsCalendar(UPDATED)));
		}
		return cv;
	}

	@Override
	public String toString() {
		return String.format("%s %s", get(SYMBOL), get(QUOTE));
	}

}
