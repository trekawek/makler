package pl.net.newton.Makler.ui.adapter;

import pl.net.newton.Makler.R;
import pl.net.newton.Makler.common.NumberFormatUtils;
import pl.net.newton.Makler.gpw.model.Finances;
import pl.net.newton.Makler.gpw.model.Paper;
import android.content.Context;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class PapersAdapter extends BaseAdapter {
	private LayoutInflater mInflater;

	private Finances finances;

	public PapersAdapter(Context context, Finances finances) {
		this.finances = finances;
		mInflater = LayoutInflater.from(context);
	}

	public int getCount() {
		return finances.getPapers().size();
	}

	public Object getItem(int position) {
		return finances.getPapers().get(position);
	}

	public long getItemId(int position) {
		return position;
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;
		if (convertView == null) {
			convertView = mInflater.inflate(R.layout.paper_item, null);
			holder = new ViewHolder();
			holder.symbol = (TextView) convertView.findViewById(R.id.paperItemSymbol);
			holder.name = (TextView) convertView.findViewById(R.id.paperItemName);
			holder.ownership = (TextView) convertView.findViewById(R.id.paperItemOwnership);
			holder.quote = (TextView) convertView.findViewById(R.id.paperItemQuote);
			holder.value = (TextView) convertView.findViewById(R.id.paperItemValue);
			holder.quantity = (TextView) convertView.findViewById(R.id.paperItemQuantity);

			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}

		Paper p = finances.getPapers().get(position);
		if (p.getSymbol() != null) {
			holder.symbol.setText(p.getSymbol().getSymbol());
			holder.name.setText(p.getSymbol().getName());

			if (p.getSymbol().getSymbol().length() > 5) {
				holder.symbol.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
				holder.symbol.setPadding(0, QuotesAdapter.dpToPx(convertView.getContext(), 14), 0, 0);
			}
		} else {
			holder.symbol.setText("???");
			holder.name.setText("???");
		}
		holder.ownership.setText(NumberFormatUtils.formatNumber(p.getOwnership()));
		holder.quote.setText(NumberFormatUtils.formatNumber(p.getQuote()));
		holder.value.setText(NumberFormatUtils.formatNumber(p.getValue()));
		holder.quantity.setText(NumberFormatUtils.formatNumber(p.getQuantity()));

		return convertView;
	}

	static class ViewHolder {
		TextView symbol;

		TextView name;

		TextView ownership;

		TextView quote;

		TextView value;

		TextView quantity;
	}
}
