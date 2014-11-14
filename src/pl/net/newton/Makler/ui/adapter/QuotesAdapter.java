package pl.net.newton.Makler.ui.adapter;

import java.math.BigDecimal;
import java.util.List;

import pl.net.newton.Makler.R;
import pl.net.newton.Makler.db.quote.Quote;
import pl.net.newton.Makler.db.quote.QuoteField;
import pl.net.newton.Makler.common.DateFormatUtils;
import pl.net.newton.Makler.common.NumberFormatUtils;
import android.content.Context;
import android.content.res.Resources;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class QuotesAdapter extends BaseAdapter {
	static class ViewHolder {
		TextView update;

		TextView symbol;

		TextView name;

		TextView kursMin;

		TextView kurs;

		TextView kursMax;

		TextView zmiana;
	}

	private LayoutInflater mInflater;

	private List<Quote> quotes;

	public QuotesAdapter(Context context, List<Quote> quotes) {
		mInflater = LayoutInflater.from(context);
		this.quotes = quotes;
	}

	public int getCount() {
		return quotes.size();
	}

	public Object getItem(int position) {
		return quotes.get(position);
	}

	public long getItemId(int position) {
		return position;
	}

	@SuppressWarnings("deprecation")
	public View getView(int position, View view, ViewGroup parent) {
		ViewHolder holder;
		View convertView = view;
		if (convertView == null) {
			convertView = mInflater.inflate(R.layout.quotes_item, null);
			holder = new ViewHolder();
			holder.update = (TextView) convertView.findViewById(R.id.quoteItemUpdate);
			holder.symbol = (TextView) convertView.findViewById(R.id.quoteItemSymbol);
			holder.name = (TextView) convertView.findViewById(R.id.quoteItemName);
			holder.kursMin = (TextView) convertView.findViewById(R.id.quoteItemKursMin);
			holder.kurs = (TextView) convertView.findViewById(R.id.quoteItemKurs);
			holder.kursMax = (TextView) convertView.findViewById(R.id.quoteItemKursMax);
			holder.zmiana = (TextView) convertView.findViewById(R.id.quoteItemZmiana);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}

		Quote quote = quotes.get(position);
		holder.update.setText(DateFormatUtils.formatTime(quote.getAsCalendar(QuoteField.UPDATED)));
		holder.symbol.setText(quote.get(QuoteField.SYMBOL));
		if (quote.get(QuoteField.SYMBOL).length() > 5) {
			holder.symbol.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
			holder.symbol.setPadding(0, dpToPx(convertView.getContext(), 14), 0, 0);
		}
		if (quote.getAsBoolean(QuoteField.IS_INDEX)) {
			holder.name.setVisibility(View.INVISIBLE);
		} else {
			holder.name.setVisibility(View.VISIBLE);
			holder.name.setText(quote.get(QuoteField.NAME));
		}
		holder.kursMin.setText(NumberFormatUtils.formatNumber(quote.getAsDecimal(QuoteField.MIN)));
		holder.kurs.setText(NumberFormatUtils.formatNumber(quote.chooseKurs()));
		holder.zmiana.setText(NumberFormatUtils.formatNumber(quote.chooseZmiana()) + "%");
		holder.kursMax.setText(NumberFormatUtils.formatNumber(quote.getAsDecimal(QuoteField.MAX)));

		Resources res = convertView.getResources();
		if (quote.chooseZmiana() != null) {
			switch (quote.chooseZmiana().compareTo(BigDecimal.ZERO)) {
				case 0:
					holder.zmiana.setBackgroundDrawable(res.getDrawable(R.drawable.bluebox));
					break;
				case -1:
					holder.zmiana.setBackgroundDrawable(res.getDrawable(R.drawable.redbox));
					break;
				case 1:
					holder.zmiana.setBackgroundDrawable(res.getDrawable(R.drawable.greenbox));
					break;
			}
		}

		return convertView;
	}

	public static int dpToPx(Context ctx, int paddingInDp) {
		final float scale = ctx.getResources().getDisplayMetrics().density;
		return (int) (paddingInDp * scale + 0.5f);
	}
}
