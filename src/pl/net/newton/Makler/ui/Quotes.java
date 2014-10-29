package pl.net.newton.Makler.ui;

import java.util.List;

import pl.net.newton.Makler.R;
import pl.net.newton.Makler.db.quote.Quote;
import pl.net.newton.Makler.db.quote.QuotesDb;
import pl.net.newton.Makler.db.symbol.Symbol;
import pl.net.newton.Makler.db.symbol.SymbolsDb;
import pl.net.newton.Makler.gpw.DefaultQuotesReceiver;
import pl.net.newton.Makler.gpw.QuotesReceiver;
import pl.net.newton.Makler.gpw.ex.GpwException;
import pl.net.newton.Makler.gpw.service.QuotesListener;
import pl.net.newton.Makler.history.service.HistoryService;
import pl.net.newton.Makler.receivers.QuotesAlarmReceiver;
import pl.net.newton.Makler.ui.adapter.QuotesAdapter;
import pl.net.newton.Makler.common.DateFormatUtils;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

public class Quotes extends AbstractActivity implements QuotesListener, OnItemClickListener {
	private static final String SYMBOL = "symbol";

	private static final String TAG = "Makler";

	static final int GET_SYMBOL_FROM_LIST = 1, SHOW_PREFERENCES = 2;

	private List<Quote> quotes;

	private ListView quotesList;

	private EditText addQuoteSymbols = null;

	private View noQuotes;

	private QuotesDb quotesDb;

	private SymbolsDb symbolsDb;

	private final Runnable mRefreshList = new Runnable() {
		public void run() {
			refreshList();
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "Quotes - onCreate");
		serviceManager.startServices();

		setContentView(R.layout.quotes);

		noQuotes = findViewById(R.id.noQuotes);
		quotesList = (ListView) findViewById(R.id.quotesListView);
		quotesList.setOnCreateContextMenuListener(this);
		quotesList.setOnItemClickListener(this);
		registerForContextMenu(quotesList);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		serviceManager.stopServices();
	}

	@Override
	public void onResume() {
		super.onResume();
		QuotesAlarmReceiver.cancelAlarm(this);
		refreshList();
	}

	@Override
	public void onPause() {
		super.onPause();
		QuotesAlarmReceiver.setAlarm(this);
	}

	private void addSymbol() {
		LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		View layout = inflater.inflate(R.layout.add_symbol, null);
		Button btn = (Button) layout.findViewById(R.id.addQuoteShow);
		addQuoteSymbols = (EditText) layout.findViewById(R.id.addQuoteSymbols);
		btn.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				Intent intent = new Intent(view.getContext(), Symbols.class);
				startActivityForResult(intent, GET_SYMBOL_FROM_LIST);
			}
		});

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setView(layout).setCancelable(false)
				.setPositiveButton("Dodaj", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						AlertDialog ad = (AlertDialog) dialog;
						EditText v = (EditText) ad.findViewById(R.id.addQuoteSymbols);
						quotesDb.addQuotes(v.getText().toString());
						updateQuotes();
						addQuoteSymbols = null;
					}
				}).setNegativeButton("Anuluj", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
						addQuoteSymbols = null;
					}
				});

		AlertDialog alertDialog = builder.create();
		alertDialog.show();
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
			case GET_SYMBOL_FROM_LIST:
				if (resultCode == Activity.RESULT_OK) {
					String symbol = data.getStringExtra(SYMBOL);
					if (addQuoteSymbols == null) {
						return;
					}
					String symbols = addQuoteSymbols.getText().toString();
					if (symbols.endsWith(" ") || symbols.length() == 0) {
						addQuoteSymbols.append(symbol);
					} else {
						addQuoteSymbols.append(" " + symbol);
					}
					break;
				}
				break;
			case SHOW_PREFERENCES:
				Log.d(TAG, "preferences set");
				break;
			default:
				// do nothing
				break;
		}
	}

	private final static class ContextMenuItem {
		static final int DETAILS = 0, ALERTS = 1, WALLET = 5, DELETE = 2, UP = 3, DOWN = 4;

		private ContextMenuItem() {
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		menu.add(0, ContextMenuItem.DETAILS, 0, "Szczegóły");
		menu.add(0, ContextMenuItem.ALERTS, 0, "Alerty");
		menu.add(0, ContextMenuItem.WALLET, 0, "Dodaj do portfela");
		menu.add(0, ContextMenuItem.UP, 0, "W górę");
		menu.add(0, ContextMenuItem.DOWN, 0, "W dół");
		menu.add(0, ContextMenuItem.DELETE, 0, "Usuń");
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
		Quote quote = quotes.get(info.position);

		Intent intent;

		switch (item.getItemId()) {
			case ContextMenuItem.DETAILS:
				intent = new Intent(this, QuoteDetails.class);
				intent.putExtra(SYMBOL, quote.getSymbol());
				startActivity(intent);
				break;

			case ContextMenuItem.ALERTS:
				intent = new Intent(this, Alerts.class);
				intent.putExtra(SYMBOL, quote.getSymbol());
				startActivity(intent);
				break;

			case ContextMenuItem.WALLET:
				intent = new Intent(this, WalletForm.class);
				intent.putExtra(SYMBOL, quote.getSymbol());
				intent.putExtra("quote", quote.chooseKurs());
				startActivity(intent);
				break;

			case ContextMenuItem.UP:
				quotesDb.move(quote.getId(), true);
				refreshList();
				break;

			case ContextMenuItem.DOWN:
				quotesDb.move(quote.getId(), false);
				refreshList();
				break;

			case ContextMenuItem.DELETE:
				quotesDb.deleteQuote(quote.getId());
				refreshList();
				break;

			default:
				// do nothing
				break;
		}
		return true;
	}

	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		Quote quote = quotes.get(position);

		Intent intent = new Intent(quotesList.getContext(), QuoteDetails.class);
		intent.putExtra(SYMBOL, quote.getSymbol());
		startActivity(intent);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.quotes_menu_with_trades, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent intent;
		switch (item.getItemId()) {
			case R.id.addQuote:
				addSymbol();
				break;

			case R.id.refreshQuotes:
				updateQuotes();
				break;

			case R.id.showSettings:
				intent = new Intent(this, Preferences.class);
				startActivityForResult(intent, SHOW_PREFERENCES);
				break;

			case R.id.showAbout:
				intent = new Intent(this, About.class);
				startActivity(intent);
				break;

			case R.id.showWallet:
				intent = new Intent(quotesList.getContext(), Wallet.class);
				startActivity(intent);
				break;

			default:
				// do nothing
				break;
		}
		return super.onOptionsItemSelected(item);
	}

	private void refreshList() {
		if (quotesDb == null) {
			return;
		}
		quotes = quotesDb.getQuotes(false);
		if (quotes.isEmpty()) {
			noQuotes.setVisibility(View.VISIBLE);
		} else {
			noQuotes.setVisibility(View.GONE);
		}
		quotesList.setAdapter(new QuotesAdapter(this, quotes));
	}

	private void updateQuotes() {
		perform(new ProcessPerformer() {
			public void showResults(boolean result) {
				refreshList();
			}

			public boolean perform(QuotesReceiver quotesReceiver) throws GpwException {
				quotesService.updateQuotes();
				return true;
			}
		}, true);
	}

	private void areSymbolsUpToDate() {
		boolean doUpdate = false;

		final String lastSymbolsUpdated = config.getLastSymbolsUpdated();
		final String date = DateFormatUtils.formatCurrentDate();
		String sinceDate = "";
		if (!date.equals(lastSymbolsUpdated)) {
			doUpdate = true;
			sinceDate = lastSymbolsUpdated;
		}

		Symbol s = symbolsDb.getSymbolBySymbol("PKN");
		if (s == null || s.getCode() == null) {
			doUpdate = true;
			sinceDate = "";
		}

		if (!doUpdate) {
			return;
		}

		final String finalSinceDate = sinceDate;
		perform(new ProcessPerformer() {
			public void showResults(boolean result) {
				refreshList();
			}

			public boolean perform(QuotesReceiver quotesReceiver) throws GpwException {
				DefaultQuotesReceiver q = new DefaultQuotesReceiver(Quotes.this);
				List<Symbol> symbols;
				if ("".equals(finalSinceDate)) {
					symbols = q.getSymbols();
				} else {
					symbols = q.getSymbols(finalSinceDate);
				}

				if (symbols.isEmpty()) {
					Log.d(TAG, "brak nowych papierów od " + finalSinceDate);
					config.setLastSymbolsUpdated(date);
				} else {
					updateSymbols(quotesReceiver);
				}
				return true;
			}
		}, true);
	}

	private void updateSymbols(QuotesReceiver quotesReceiver) throws GpwException {
		List<Symbol> symbols;
		symbols = quotesReceiver.getSymbols();
		if (symbols != null) {
			config.setLastSymbolsUpdated(DateFormatUtils.formatCurrentDate());
			symbolsDb.updateSymbols(symbols);
		}
	}

	public void quotesUpdated() {
		Log.d(TAG, "Aktualizacja listy notowań");
		mHandler.post(mRefreshList);
	}

	@Override
	protected void initUi(SQLiteDatabase sqlDb, HistoryService historyService) {
		this.quotesDb = new QuotesDb(sqlDb, this);
		this.symbolsDb = new SymbolsDb(sqlDb, this);

		areSymbolsUpToDate();
		refreshList();
	}
}