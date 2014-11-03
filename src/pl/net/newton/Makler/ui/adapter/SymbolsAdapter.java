package pl.net.newton.Makler.ui.adapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pl.net.newton.Makler.R;
import pl.net.newton.Makler.db.symbol.Symbol;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.SectionIndexer;
import android.widget.TextView;

public class SymbolsAdapter extends BaseAdapter implements SectionIndexer {
	private LayoutInflater mInflater;

	private List<Symbol> symbols;

	private Map<Character, Integer> sectionBegins;

	private List<Character> sections;

	public SymbolsAdapter(Context context, List<Symbol> symbols) {
		mInflater = LayoutInflater.from(context);
		this.symbols = symbols;
		sectionBegins = new HashMap<Character, Integer>();
		sections = new ArrayList<Character>();
		Character c = null;
		for (int i = 0; i < symbols.size(); i++) {
			Symbol s = symbols.get(i);
			if (s.getSymbol().length() == 0) {
				continue;
			}
			Character d = s.getSymbol().charAt(0);
			if (d != c) {
				sectionBegins.put(d, i);
				sections.add(d);
				c = d;
			}
		}
	}

	public int getCount() {
		return symbols.size();
	}

	public Object getItem(int position) {
		return symbols.get(position);
	}

	public long getItemId(int position) {
		return position;
	}

	public View getView(int position, View view, ViewGroup parent) {
		ViewHolder holder;
		View convertView = view;
		if (convertView == null) {
			convertView = mInflater.inflate(R.layout.symbol_item, null);
			holder = new ViewHolder();
			holder.symbol = (TextView) convertView.findViewById(R.id.symbolItemSymbol);
			holder.name = (TextView) convertView.findViewById(R.id.symbolItemName);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}

		Symbol symbol = symbols.get(position);
		holder.symbol.setText(symbol.getSymbol());
		if (symbol.isIndex()) {
			holder.name.setText("");
		} else {
			holder.name.setText(symbol.getName());
		}
		return convertView;
	}

	static class ViewHolder {
		TextView symbol;

		TextView name;
	}

	public int getPositionForSection(int i) {
		return sectionBegins.get(sections.get(i));
	}

	public int getSectionForPosition(int i) {
		for (int j = 0; j < sections.size(); j++) {
			int pos = getPositionForSection(j);
			if (pos > i) {
				return j - 1;
			}
		}
		return sections.size() - 1;
	}

	public Object[] getSections() {
		return sections.toArray();
	}
}
