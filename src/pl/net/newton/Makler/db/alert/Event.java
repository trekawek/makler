package pl.net.newton.Makler.db.alert;

import java.math.BigDecimal;

import pl.net.newton.Makler.R;
import android.content.Context;
import android.content.res.Resources;

public enum Event {
	WZR_POW {
		@Override
		public boolean isAlarming(BigDecimal currentValue, AlertValue alertValue) {
			return currentValue.compareTo(alertValue.getValue()) > 0;
		}
	},
	WZR_DO {
		@Override
		public boolean isAlarming(BigDecimal currentValue, AlertValue alertValue) {
			return currentValue.compareTo(alertValue.getValue()) >= 0;
		}
	},
	WZR_O(true) {
		@Override
		public boolean isAlarming(BigDecimal currentValue, AlertValue alertValue) {
			BigDecimal baseValue = alertValue.getBaseValue();
			if (alertValue.isPercent()) {
				return currentValue.subtract(baseValue).compareTo(
						baseValue.multiply(alertValue.getValue()).divide(new BigDecimal(100))) >= 0;
			} else {
				return currentValue.subtract(baseValue).compareTo(alertValue.getValue()) >= 0;
			}
		}
	},
	SPA_O(true) {
		@Override
		public boolean isAlarming(BigDecimal currentValue, AlertValue alertValue) {
			BigDecimal baseValue = alertValue.getBaseValue();
			if (alertValue.isPercent()) {
				return baseValue.subtract(currentValue).compareTo(
						baseValue.multiply(alertValue.getValue()).divide(new BigDecimal(100))) >= 0;
			} else {
				return baseValue.subtract(currentValue).compareTo(alertValue.getValue()) >= 0;
			}
		}
	},
	SPA_DO {
		@Override
		public boolean isAlarming(BigDecimal currentValue, AlertValue alertValue) {
			return currentValue.compareTo(alertValue.getValue()) <= 0;
		}
	},
	SPA_PON {
		@Override
		public boolean isAlarming(BigDecimal currentValue, AlertValue alertValue) {
			return currentValue.compareTo(alertValue.getValue()) < 0;
		}
	};

	private boolean baseValueRequired;

	private Event() {
		this.baseValueRequired = false;
	}

	private Event(boolean baseValueRequired) {
		this.baseValueRequired = baseValueRequired;
	}

	public abstract boolean isAlarming(BigDecimal currentValue, AlertValue alertValue);

	public boolean isBaseValueRequired() {
		return baseValueRequired;
	}

	public static Event getFromLabel(Context context, String label) {
		Resources res = context.getResources();
		String[] codes = res.getStringArray(R.array.alert_events_quote_codes);
		String[] labels = res.getStringArray(R.array.alert_events_quote_strings);
		for (int i = 0; i < labels.length; i++) {
			if (labels[i].equals(label)) {
				return Event.valueOf(codes[i]);
			}
		}
		return null;
	}

	public String getLabel(Context context) {
		return getLabel(context, R.array.alert_events_quote_strings);
	}

	public String getLabel(Context context, int labelsId) {
		Resources res = context.getResources();
		String[] codes = res.getStringArray(R.array.alert_events_quote_codes);
		String[] labels = res.getStringArray(labelsId);
		for (int i = 0; i < codes.length; i++) {
			if (Event.valueOf(codes[i]) == this) {
				return labels[i];
			}
		}
		return null;
	}
}