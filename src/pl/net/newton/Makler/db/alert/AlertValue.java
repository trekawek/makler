package pl.net.newton.Makler.db.alert;

import java.math.BigDecimal;

public class AlertValue {

	private final BigDecimal value;

	private final BigDecimal baseValue;

	private final boolean percent;

	public AlertValue(BigDecimal value, BigDecimal baseValue, boolean percent) {
		this.value = value;
		this.baseValue = baseValue;
		this.percent = percent;
	}

	public AlertValue(AlertBuilder builder) {
		this(builder.getValue(), builder.getBaseValue(), builder.getPercent());
	}

	public BigDecimal getValue() {
		return value;
	}

	public BigDecimal getBaseValue() {
		return baseValue;
	}

	public boolean isPercent() {
		return percent;
	}

}
