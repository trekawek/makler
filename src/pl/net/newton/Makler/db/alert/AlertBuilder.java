package pl.net.newton.Makler.db.alert;

import java.math.BigDecimal;
import android.database.Cursor;
import pl.net.newton.Makler.db.quote.Quote;
import pl.net.newton.Makler.db.quote.QuotesDb;

public class AlertBuilder {
	private Integer id;

	private Quote quote;

	private Subject subject;

	private Event event;

	private BigDecimal value;

	private boolean percent;

	private BigDecimal baseValue;

	private boolean used;

	public AlertBuilder setFromCursor(Cursor c, QuotesDb db) {
		this.id = c.getInt(c.getColumnIndex("id"));
		this.quote = db.getQuoteById(c.getInt(c.getColumnIndex("quote_id")));
		if (quote == null)
			throw new RuntimeException("Nieznany walor");
		this.subject = Subject.valueOf(c.getString(c.getColumnIndex("subject")));
		this.event = Event.valueOf(c.getString(c.getColumnIndex("event")));
		this.value = new BigDecimal(c.getString(c.getColumnIndex("value")));
		this.percent = c.getInt(c.getColumnIndex("percent")) == 1;
		this.baseValue = new BigDecimal(c.getString(c.getColumnIndex("base_value")));
		this.used = c.getInt(c.getColumnIndex("used")) == 1;
		return this;
	}

	public Alert build() {
		return new Alert(this);
	}

	Integer getId() {
		return id;
	}

	public AlertBuilder setId(Integer id) {
		this.id = id;
		return this;
	}

	Quote getQuote() {
		return quote;
	}

	public AlertBuilder setQuote(Quote quote) {
		this.quote = quote;
		return this;
	}

	Subject getSubject() {
		return subject;
	}

	public AlertBuilder setSubject(Subject subject) {
		this.subject = subject;
		return this;
	}

	Event getEvent() {
		return event;
	}

	public AlertBuilder setEvent(Event event) {
		this.event = event;
		return this;
	}

	BigDecimal getValue() {
		return value;
	}

	public AlertBuilder setValue(BigDecimal value) {
		this.value = value;
		return this;
	}

	boolean getPercent() {
		return percent;
	}

	public AlertBuilder setPercent(boolean percent) {
		this.percent = percent;
		return this;
	}

	BigDecimal getBaseValue() {
		return baseValue;
	}

	public AlertBuilder setBaseValue(BigDecimal baseValue) {
		this.baseValue = baseValue;
		return this;
	}

	boolean getUsed() {
		return used;
	}

	public AlertBuilder setUsed(boolean used) {
		this.used = used;
		return this;
	}

}
