package pl.net.newton.Makler.db.alert;

import pl.net.newton.Makler.R;
import android.content.Context;
import android.content.res.Resources;

public enum Event {
	WZR_POW, WZR_DO, WZR_O(true), SPA_O(true), SPA_DO, SPA_PON;

	private boolean baseValueRequired;

	private Event() {
		this.baseValueRequired = false;
	}

	private Event(boolean baseValueRequired) {
		this.baseValueRequired = baseValueRequired;
	}

	public boolean isBaseValueRequired() {
		return baseValueRequired;
	}

	public static Event getFromLabel(Context context, String label) {
		Resources res = context.getResources();
		String[] codes = res.getStringArray(R.array.alert_events_quote_codes);
		String[] labels = res.getStringArray(R.array.alert_events_quote_strings);
		for (int i = 0; i < labels.length; i++)
			if (labels[i].equals(label))
				return Event.valueOf(codes[i]);
		return null;
	}

	public String getLabel(Context context) {
		return getLabel(context, R.array.alert_events_quote_strings);
	}

	public String getLabel(Context context, int labelsId) {
		Resources res = context.getResources();
		String[] codes = res.getStringArray(R.array.alert_events_quote_codes);
		String[] labels = res.getStringArray(labelsId);
		for (int i = 0; i < codes.length; i++)
			if (Event.valueOf(codes[i]) == this)
				return labels[i];
		return null;
	}
}