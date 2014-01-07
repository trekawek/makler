package pl.net.newton.Makler.ui.adapter;

import java.util.List;

import pl.net.newton.Makler.R;
import pl.net.newton.Makler.common.NumberFormatUtils;
import pl.net.newton.Makler.gpw.model.Order;
import pl.net.newton.Makler.gpw.model.OrderState;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class OrdersAdapter extends BaseAdapter {
	private LayoutInflater mInflater;

	private List<OrderState> orders;

	private Context ctx;

	public OrdersAdapter(Context context, List<OrderState> orders) {
		this.ctx = context;
		this.orders = orders;
		mInflater = LayoutInflater.from(context);
	}

	public int getCount() {
		return orders.size();
	}

	public Object getItem(int position) {
		return orders.get(position);
	}

	public long getItemId(int position) {
		return position;
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;
		if (convertView == null) {
			convertView = mInflater.inflate(R.layout.order_item, null);
			holder = new ViewHolder();
			holder.symbol = (TextView) convertView.findViewById(R.id.orderItemSymbol);
			holder.name = (TextView) convertView.findViewById(R.id.orderItemName);
			holder.typ = (TextView) convertView.findViewById(R.id.orderItemTyp);
			holder.limit = (TextView) convertView.findViewById(R.id.orderItemLimit);
			holder.stan = (TextView) convertView.findViewById(R.id.orderItemStan);
			holder.dataSesji = (TextView) convertView.findViewById(R.id.orderItemDataSesji);
			holder.ilosc = (TextView) convertView.findViewById(R.id.orderItemIlosc);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}

		OrderState os = orders.get(position);
		Order o = os.getOrder();

		holder.symbol.setText(o.getSymbol().getSymbol());
		holder.name.setText(o.getSymbol().getName());
		holder.typ.setText(o.getType() == 'K' ? "kup" : "sprzedaj");
		if (o.getLimitType() != null)
			holder.limit.setText(o.getLimitType().toString());
		else if (o.getLimit() != null)
			holder.limit.setText(NumberFormatUtils.formatNumber(o.getLimit()));
		holder.stan.setText(os.getStateString(ctx));
		holder.dataSesji.setText(o.getSesja());
		holder.ilosc.setText(new StringBuilder().append(NumberFormatUtils.formatNumber(os.getZrealizowano()))
				.append("/").append(NumberFormatUtils.formatNumber(o.getIlosc())).toString());
		return convertView;
	}

	static class ViewHolder {
		TextView symbol;

		TextView name;

		TextView typ;

		TextView limit;

		TextView stan;

		TextView dataSesji;

		TextView ilosc;
	}
}
