package pl.net.newton.Makler.ui.adapter;

import java.util.List;

import pl.net.newton.Makler.db.alert.Alert;
import android.content.Context;
import android.text.Spannable;
import android.text.Spanned;
import android.text.style.StrikethroughSpan;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class AlertsAdapter extends BaseAdapter {
	private List<Alert> alerts;

	private Context context;

	public AlertsAdapter(Context context, List<Alert> alerts) {
		this.alerts = alerts;
		this.context = context;
	}

	public int getCount() {
		return alerts.size();
	}

	public Object getItem(int position) {
		return alerts.get(position);
	}

	public long getItemId(int position) {
		return position;
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		TextView v = new TextView(context);
		Alert a = alerts.get(position);
		v.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 24);
		v.setText(a.toString(parent.getContext()), TextView.BufferType.SPANNABLE);
		if (a.getUsed()) {
			Spannable str = (Spannable) v.getText();
			str.setSpan(new StrikethroughSpan(), 0, str.length() - 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		}
		return v;
	}
}
