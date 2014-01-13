package pl.net.newton.Makler.db.alert;

import java.math.BigDecimal;

import pl.net.newton.Makler.R;
import pl.net.newton.Makler.db.quote.Quote;
import pl.net.newton.Makler.common.NumberFormatUtils;
import android.content.Context;

public class Alert {
	private int id;

	private Quote quote;

	private Subject subject;

	private Event event;

	private BigDecimal value;

	private boolean percent;

	private BigDecimal baseValue;

	private boolean used;

	Alert(AlertBuilder builder) {
		this.id = builder.getId();
		this.quote = builder.getQuote();
		this.subject = builder.getSubject();
		this.event = builder.getEvent();
		this.value = builder.getValue();
		this.percent = builder.getPercent();
		this.baseValue = builder.getBaseValue();
		this.used = builder.getUsed();
	}

	public Quote getQuote() {
		return quote;
	}

	public Subject getSubject() {
		return subject;
	}

	public Event getEvent() {
		return event;
	}

	public BigDecimal getValue() {
		return value;
	}

	public boolean getPercent() {
		return percent;
	}

	public int getId() {
		return id;
	}

	public boolean getUsed() {
		return used;
	}

	public void setUsed(boolean used) {
		this.used = used;
	}

	public BigDecimal getBaseValue() {
		return baseValue;
	}

	public String toString(Context context) {
		return subject.getLabel(context) + " " + event.getLabel(context) + " "
				+ NumberFormatUtils.formatNumber(value) + (percent ? "%" : "");
	}

	public String notification(Context context) {
		StringBuilder s = new StringBuilder();
		s.append("Alert: ");
		s.append(subject.getLabel(context));
		s.append(" ");
		s.append(quote.getName());
		s.append(" ");
		s.append(event.getLabel(context, R.array.alert_events_quote_strings2));
		s.append(" ");
		s.append(NumberFormatUtils.formatNumber(value));
		if (percent) {
			s.append("%");
		}
		String str = s.toString();
		if (subject == Subject.WARTOSC) {
			str = str.replace("spadł", "spadła");
		}
		return str;
	}

	public boolean isAlarming() {
		BigDecimal currentValue = subject.getValue(quote);

		if (currentValue == null)
			return false;

		switch (event) {
			case WZR_POW:
				return currentValue.compareTo(value) > 0;
			case SPA_PON:
				return currentValue.compareTo(value) < 0;
			case SPA_DO:
				return currentValue.compareTo(value) <= 0;
			case WZR_DO:
				return currentValue.compareTo(value) >= 0;
			case SPA_O:
				if (percent) {
					return baseValue.subtract(currentValue).compareTo(
							baseValue.multiply(value).divide(new BigDecimal(100))) >= 0;
				} else {
					return baseValue.subtract(currentValue).compareTo(value) >= 0;
				}
			case WZR_O:
				if (percent) {
					return currentValue.subtract(baseValue).compareTo(
							baseValue.multiply(value).divide(new BigDecimal(100))) >= 0;
				} else {
					return currentValue.subtract(baseValue).compareTo(value) >= 0;
				}
		}
		return false;
	}

}
