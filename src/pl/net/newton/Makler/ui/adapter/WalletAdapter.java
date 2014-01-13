package pl.net.newton.Makler.ui.adapter;

import java.math.BigDecimal;
import java.util.List;

import pl.net.newton.Makler.R;
import pl.net.newton.Makler.common.NumberFormatUtils;
import pl.net.newton.Makler.db.wallet.WalletItem;
import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class WalletAdapter extends BaseAdapter {
	BigDecimal commision = BigDecimal.ZERO;

	BigDecimal minCommision = BigDecimal.ZERO;

	static class ViewHolder {
		TextView symbol;

		TextView name;

		TextView kurs;

		TextView zmiana;

		TextView avg;

		TextView gain;

		TextView quantity;
	}

	private LayoutInflater mInflater;

	private List<WalletItem> walletItems;

	public WalletAdapter(Context context, List<WalletItem> items, BigDecimal commision,
			BigDecimal minCommision) {
		mInflater = LayoutInflater.from(context);
		this.walletItems = items;
		this.commision = commision;
		this.minCommision = minCommision;
	}

	public int getCount() {
		return walletItems.size();
	}

	public Object getItem(int position) {
		return walletItems.get(position);
	}

	public long getItemId(int position) {
		return position;
	}

	@SuppressWarnings("deprecation")
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;
		if (convertView == null) {
			convertView = mInflater.inflate(R.layout.wallet_item, null);
			holder = new ViewHolder();
			holder.symbol = (TextView) convertView.findViewById(R.id.walletItemSymbol);
			holder.name = (TextView) convertView.findViewById(R.id.walletItemName);
			holder.kurs = (TextView) convertView.findViewById(R.id.walletItemKurs);
			holder.zmiana = (TextView) convertView.findViewById(R.id.walletItemZmiana);
			holder.gain = (TextView) convertView.findViewById(R.id.walletItemGain);
			holder.avg = (TextView) convertView.findViewById(R.id.walletItemAvg);
			holder.quantity = (TextView) convertView.findViewById(R.id.walletItemQuantity);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}

		WalletItem item = walletItems.get(position);
		holder.symbol.setText(item.getSymbol());
		holder.name.setText(item.getName());
		holder.kurs.setText(NumberFormatUtils.formatNumber(item.getQuote()));
		holder.zmiana.setText(NumberFormatUtils.formatNumber(item.getZmiana()) + "%");
		holder.avg.setText(NumberFormatUtils.formatNumber(item.getAvgBuy()));
		holder.gain.setText(NumberFormatUtils.formatNumber(item.gainWithCommision(commision, minCommision)));
		holder.quantity.setText(NumberFormatUtils.formatNumber(item.getQuantity()));

		Resources res = convertView.getResources();
		if (item.getZmiana() != null)
			switch (item.getZmiana().compareTo(BigDecimal.ZERO)) {
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
		return convertView;
	}
}
