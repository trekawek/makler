package pl.net.newton.Makler.ui;

import java.util.ArrayList;
import java.util.List;

import pl.net.newton.Makler.R;
import pl.net.newton.Makler.common.DateFormatUtils;
import pl.net.newton.Makler.db.symbol.Symbol;
import pl.net.newton.Makler.db.symbol.SymbolsDb;
import pl.net.newton.Makler.db.wallet.WalletDb;
import pl.net.newton.Makler.db.wallet.WalletItem;
import pl.net.newton.Makler.gpw.QuotesReceiver;
import pl.net.newton.Makler.gpw.Trades;
import pl.net.newton.Makler.gpw.ex.GpwException;
import pl.net.newton.Makler.gpw.ex.InvalidPasswordException;
import pl.net.newton.Makler.gpw.model.Finances;
import pl.net.newton.Makler.gpw.model.Paper;
import pl.net.newton.Makler.gpw.service.GpwProvider;
import pl.net.newton.Makler.history.service.HistoryService;
import pl.net.newton.Makler.ui.adapter.SymbolsAdapter;
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

	private boolean forSell = false, forWalletSell = false;
	
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
		} else if (getIntent().getBooleanExtra("forSell", false)) {
			forSell = true;
			findViewById(R.id.symbolsSearchRow).setVisibility(View.GONE);
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
		if (!forSell) {
			MenuInflater inflater = getMenuInflater();
			inflater.inflate(R.menu.symbols_menu, menu);
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.refreshSymbols:
				if (forSell) {
					getFinances();
				} else {
					updateSymbols();
				}

			default:
				return super.onOptionsItemSelected(item);
		}
	}

	public void refreshList() {
		if(walletDb == null || symbolsDb == null) {
			return;
		}
		
		EditText findSymbolName = (EditText) findViewById(R.id.findSymbolName);
		String name = findSymbolName.getText().toString();

		if (forWalletSell) {
			symbols = new ArrayList<Symbol>();
			for (WalletItem item : walletDb.getWalletItems()) {
				symbols.add(symbolsDb.getSymbolBySymbol(item.getSymbol()));
			}
		} else if (!forSell) {
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

			public boolean perform(QuotesReceiver quotesReceiver, Trades trades) throws GpwException,
					InvalidPasswordException {
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

	private void getFinances() {
		perform(new ProcessPerformer() {
			public void showResults(boolean result) {
				refreshList();
			}

			public boolean perform(QuotesReceiver quotesReceiver, Trades trades) throws GpwException,
					InvalidPasswordException {
				Finances finances = trades.getFinances();
				symbols = new ArrayList<Symbol>();
				for (Paper p : finances.getPapers())
					symbols.add(p.getSymbol());
				return true;
			}
		}, true);
	}

	@Override
	protected void initUi(GpwProvider gpwProvider, SQLiteDatabase sqlDb, HistoryService historyService) {
		this.symbolsDb = new SymbolsDb(sqlDb, this);
		this.walletDb = new WalletDb(sqlDb, this);
		
		if(forceUpdate) {
			updateSymbols();
		}
		if(refreshList || forWalletSell) {
			refreshList();
		}		
		if (forSell) {
			getFinances();
		}
	}

}
