package pl.net.newton.Makler.db.alert;

import java.math.BigDecimal;

import android.content.Context;
import android.content.res.Resources;

import pl.net.newton.Makler.R;
import pl.net.newton.Makler.db.quote.Quote;

public enum Subject {
	KURS {
		@Override
		public BigDecimal getValue(Quote q) {
			return q.getKurs();
		}
	},
	WOLUMEN {
		@Override
		public BigDecimal getValue(Quote q) {
			if (q.getWolumen() == null) {
				return null;
			} else {
				return new BigDecimal(q.getWolumen());
			}
		}
	},
	WARTOSC {
		@Override
		public BigDecimal getValue(Quote q) {
			return q.getWartosc();
		}
	};

	public abstract BigDecimal getValue(Quote q);

	public static Subject getFromLabel(Context context, String label) {
		Resources res = context.getResources();
		String[] codes = res.getStringArray(R.array.alert_subjects_codes);
		String[] labels = res.getStringArray(R.array.alert_subjects_strings);
		for (int i = 0; i < labels.length; i++) {
			if (labels[i].equals(label)) {
				return Subject.valueOf(codes[i]);
			}
		}
		return null;
	}

	public String getLabel(Context context) {
		Resources res = context.getResources();
		String[] codes = res.getStringArray(R.array.alert_subjects_codes);
		String[] labels = res.getStringArray(R.array.alert_subjects_strings);
		for (int i = 0; i < codes.length; i++)
			if (Subject.valueOf(codes[i]) == this)
				return labels[i];
		return null;
	}
}