package pl.net.newton.Makler.ui;

import java.util.ArrayList;
import java.util.List;

import pl.net.newton.Makler.R;
import pl.net.newton.Makler.db.symbol.Symbol;
import pl.net.newton.Makler.db.symbol.SymbolsDb;
import pl.net.newton.Makler.db.wallet.WalletDb;
import pl.net.newton.Makler.db.wallet.WalletItem;
import pl.net.newton.Makler.gpw.QuotesReceiver;
import pl.net.newton.Makler.gpw.ex.GpwException;
import pl.net.newton.Makler.history.service.HistoryService;
import pl.net.newton.Makler.ui.adapter.SymbolsAdapter;
import pl.net.newton.Makler.common.DateFormatUtils;
import android.app.Activity;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

public class Symbols extends AbstractActivity implements OnItemClickListener {
	private List<Symbol> symbols;

	private ListView symbolsList;

	private boolean forWalletSell = false;

	private boolean forceUpdate;

	private boolean refreshList;

	private WalletDb walletDb;

	private SymbolsDb symbolsDb;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.symbols);
		symbolsList = (ListView) findViewById(R.id.symbolsListView);
		symbolsList.setOnItemClickListener(this);

		if (getIntent().getBooleanExtra("update", false)) {
			forceUpdate = true;
		} else if (getIntent().getBooleanExtra("forWalletSell", false)) {
			forWalletSell = true;
			findViewById(R.id.symbolsSearchRow).setVisibility(View.GONE);
		} else {
			refreshList = true;
		}

		EditText findSymbolName = (EditText) findViewById(R.id.findSymbolName);
		findSymbolName.addTextChangedListener(new TextWatcher() {

			public void onTextChanged(CharSequence s, int start, int before, int count) {
				refreshList();
			}

			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			public void afterTextChanged(Editable s) {
			}
		});

		setTitle("Walory");
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.symbols_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.refreshSymbols) {
			updateSymbols();
		}

		return super.onOptionsItemSelected(item);
	}

	public void refreshList() {
		if (walletDb == null || symbolsDb == null) {
			return;
		}

		EditText findSymbolName = (EditText) findViewById(R.id.findSymbolName);
		String name = findSymbolName.getText().toString();

		if (forWalletSell) {
			symbols = new ArrayList<Symbol>();
			for (WalletItem item : walletDb.getWalletItems()) {
				symbols.add(symbolsDb.getSymbolBySymbol(item.getSymbol()));
			}
		} else {
			symbols = symbolsDb.getSymbols(name);
		}

		symbolsList.setAdapter(new SymbolsAdapter(this, symbols));
	}

	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		Intent resultIntent = new Intent();
		resultIntent.putExtra("symbol", symbols.get(position).getSymbol());
		setResult(Activity.RESULT_OK, resultIntent);
		finish();
	}

	private void updateSymbols() {
		perform(new ProcessPerformer() {
			public void showResults(boolean result) {
				refreshList();
			}

			public boolean perform(QuotesReceiver quotesReceiver) throws GpwException {
				List<Symbol> symbols;
				symbols = quotesReceiver.getSymbols();
				if (symbols != null) {
					config.setLastSymbolsUpdated(DateFormatUtils.formatCurrentDate());
					symbolsDb.updateSymbols(symbols);
					return true;
				} else {
					return false;
				}
			}
		}, true);
	}

	@Override
	protected void initUi(SQLiteDatabase sqlDb, HistoryService historyService) {
		this.symbolsDb = new SymbolsDb(sqlDb, this);
		this.walletDb = new WalletDb(sqlDb, this);

		if (forceUpdate) {
			updateSymbols();
		}
		if (refreshList || forWalletSell) {
			refreshList();
		}
	}

}
