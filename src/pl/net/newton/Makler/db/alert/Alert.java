package pl.net.newton.Makler.db.alert;

import java.math.BigDecimal;

import pl.net.newton.Makler.R;
import pl.net.newton.Makler.db.quote.Quote;
import pl.net.newton.Makler.common.LocaleUtils;
import pl.net.newton.Makler.common.NumberFormatUtils;
import android.content.Context;

public class Alert {
	private final int id;

	private final Quote quote;

	private final Subject subject;

	private final Event event;

	private boolean used;

	private final AlertValue alertValue;

	Alert(AlertBuilder builder) {
		this.id = builder.getId();
		this.quote = builder.getQuote();
		this.subject = builder.getSubject();
		this.event = builder.getEvent();
		this.used = builder.getUsed();
		this.alertValue = new AlertValue(builder);
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

	public int getId() {
		return id;
	}

	public boolean getUsed() {
		return used;
	}

	public AlertValue getAlertValue() {
		return alertValue;
	}

	public void setUsed(boolean used) {
		this.used = used;
	}

	public String toString(Context context) {
		return String.format(LocaleUtils.LOCALE, "%s %s %s%s", subject.getLabel(context),
				event.getLabel(context), NumberFormatUtils.formatNumber(alertValue.getValue()),
				alertValue.isPercent() ? "%" : "");
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
		s.append(NumberFormatUtils.formatNumber(alertValue.getValue()));
		if (alertValue.isPercent()) {
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
		if (currentValue == null) {
			return false;
		}
		return event.isAlarming(currentValue, alertValue);
	}

}
