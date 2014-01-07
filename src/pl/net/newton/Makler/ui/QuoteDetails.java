package pl.net.newton.Makler.ui;

import java.math.BigDecimal;
import java.util.HashMap;

import android.content.Intent;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.TypedValue;

import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import pl.net.newton.Makler.R;
import pl.net.newton.Makler.common.DateFormatUtils;
import pl.net.newton.Makler.common.NumberFormatUtils;
import pl.net.newton.Makler.db.quote.Quote;
import pl.net.newton.Makler.db.quote.QuotesDb;
import pl.net.newton.Makler.db.symbol.SymbolsDb;
import pl.net.newton.Makler.gpw.QuotesReceiver;
import pl.net.newton.Makler.gpw.Trades;
import pl.net.newton.Makler.gpw.ex.GpwException;
import pl.net.newton.Makler.gpw.ex.InvalidPasswordException;
import pl.net.newton.Makler.gpw.service.GpwProvider;
import pl.net.newton.Makler.gpw.service.QuotesListener;
import pl.net.newton.Makler.history.EntryListWithIndexes;
import pl.net.newton.Makler.history.service.HistoryListener;
import pl.net.newton.Makler.history.service.HistoryService;
import pl.net.newton.Makler.ui.adapter.QuotesAdapter;
import pl.net.newton.Makler.ui.graph.GraphView;

public class QuoteDetails extends AbstractActivity implements QuotesListener,
		HistoryListener {

	private Quote quote;

	private String quoteSymbol;

	private HistoryService historyService;

	private Boolean index;

	private HashMap<Integer, TextView> textViews;

	private GraphView graphView;

	private int graphRange = 0;

	private int graphType = 0;

	private LinearLayout graphLayout;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (savedInstanceState != null) {
			graphRange = savedInstanceState.getInt("graphRange", 0);
			graphType = savedInstanceState.getInt("graphType", 0);
		}

		textViews = new HashMap<Integer, TextView>();
		quoteSymbol = getIntent().getStringExtra("symbol");
		//isWalletItem = getIntent().getBooleanExtra("wallet_item", false);
	}

	@Override
	protected void onSaveInstanceState(Bundle state) {
		if (graphView != null) {
			state.putInt("graphRange", graphView.getGraphRange());
			state.putInt("graphType", graphView.getGraphType());
		}
		super.onSaveInstanceState(state);
	}

	@Override
	public void onResume() {
		super.onResume();
		if (historyService != null) {
			historyService.register(this);
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		if (historyService != null) {
			historyService.unregister(this);
		}
	}

	@Override
	public void onDestroy() {
		if (historyService != null) {
			historyService.unregister(this);
		}
		super.onDestroy();
	}

	private final Runnable mRefreshList = new Runnable() {
		public void run() {
			refresh();
		}
	};

	private QuotesDb quotesDb;

	private SymbolsDb symbolsDb;

	public void quotesUpdated() {
		mHandler.post(mRefreshList);
	}

	private void refresh() {
		if(quotesDb == null) {
			return;
		}
		
		quote = quotesDb.getQuoteBySymbol(quoteSymbol);
		if(quote == null || quote.getSymbol() == null) {
			return;
		}

		if (quote.getSymbol().length() > 5) {
			TextView symbol = (TextView) findViewById(R.id.quoteDetailSymbol);
			symbol.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
			symbol.setPadding(0, QuotesAdapter.dpToPx(this, 14), 0, 0);
		}

		setTextView(R.id.quoteDetailKurs, quote.chooseKurs());
		setTextView(R.id.quoteDetailZmiana, NumberFormatUtils.formatNumber(quote.chooseZmiana()) + "%");
		setTextView(R.id.quoteDetailKursMax, quote.getKursMax());
		setTextView(R.id.quoteDetailKursMin, quote.getKursMin());
		setTextView(R.id.quoteDetailKursOdn, quote.getKursOdn());
		setTextView(R.id.quoteDetailSymbol, quote.getSymbol());
		if (!index) {
			setTextView(R.id.quoteDetailName, quote.getName());
			setTextView(R.id.quoteDetailTko, quote.getTko());
			setTextView(R.id.quoteDetailTkoProcent, quote.getTkoProcent());
			setTextView(R.id.quoteDetailKursOtw, quote.getKursOtw());
			setTextView(R.id.quoteDetailWolumen, quote.getWolumen());

			setTextView(R.id.quoteDetailOfertK, quote.getkOfert());
			setTextView(R.id.quoteDetailWolumenK, quote.getkWol());
			setTextView(R.id.quoteDetailLimitK, quote.getkLimString());

			setTextView(R.id.quoteDetailLimitS, quote.getsLimString());
			setTextView(R.id.quoteDetailWolumenS, quote.getsWol());
			setTextView(R.id.quoteDetailOfertS, quote.getsOfert());

		}
		setTextView(R.id.quoteDetailUpdate, DateFormatUtils.formatHhMmSs(quote.getUpdate()));
		setTextView(R.id.quoteDetailWartosc, quote.getWartosc());

		Resources res = getResources();
		View zmiana = findViewById(R.id.quoteDetailZmiana);
		if (quote.chooseZmiana() != null)
			switch (quote.chooseZmiana().compareTo(BigDecimal.ZERO)) {
				case 0:
					zmiana.setBackgroundDrawable(res.getDrawable(R.drawable.bluebox));
					break;
				case -1:
					zmiana.setBackgroundDrawable(res.getDrawable(R.drawable.redbox));
					break;
				case 1:
					zmiana.setBackgroundDrawable(res.getDrawable(R.drawable.greenbox));
					break;
			}
	}

	private void setTextView(int id, String text) {
		TextView t;
		if (textViews.containsKey(id))
			t = textViews.get(id);
		else {
			t = (TextView) findViewById(id);
			textViews.put(id, t);
		}
		t.setText(text);
	}

	private void setTextView(int id, BigDecimal number) {
		setTextView(id, NumberFormatUtils.formatNumber(number));
	}

	private void setTextView(int id, Integer number) {
		setTextView(id, NumberFormatUtils.formatNumber(number));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.quote_details_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent intent;
		switch (item.getItemId()) {
			case R.id.refreshQuote:
				updateQuotes();
				graphView.refreshGraph(true);
				break;

			case R.id.showAlerts:
				intent = new Intent(this, Alerts.class);
				intent.putExtra("symbol", quote.getSymbol());
				startActivity(intent);
				break;

			case R.id.addToWallet:
				intent = new Intent(this, WalletForm.class);
				intent.putExtra("symbol", quote.getSymbol());
				intent.putExtra("quote", quote.chooseKurs());
				startActivity(intent);
				break;
			
			case R.id.graphRange:
				graphView.changeGraphRange();
				break;
		}
		return super.onOptionsItemSelected(item);
	}

	private void updateQuotes() {
		perform(new ProcessPerformer() {
			public void showResults(boolean result) {
				refresh();
			}

			public boolean perform(QuotesReceiver quotesReceiver, Trades trades) throws GpwException,
					InvalidPasswordException {
				quotesService.updateQuotes();
				return true;
			}
		}, true);
	}

	public void gotEntries(EntryListWithIndexes entries) {
		graphView.gotEntries(entries);
	}

	@Override
	protected void initUi(GpwProvider gpwProvider, SQLiteDatabase sqlDb, HistoryService historyService) {
		this.symbolsDb = new SymbolsDb(sqlDb, this);
		this.quotesDb = new QuotesDb(sqlDb, this);

		quote = quotesDb.getQuoteBySymbol(quoteSymbol);
		index = quote.isIndex();
		if (index) {
			setContentView(R.layout.quote_details_index);
		}
		else {
			setContentView(R.layout.quote_details);
		}
		graphLayout = (LinearLayout) findViewById(R.id.graphParent);
		setTitle(quote.getName());
		refresh();

		graphLayout.removeAllViews();
		graphView = new GraphView(this, quote, mHandler, historyService, symbolsDb);
		graphLayout.addView(graphView);
		graphView.setGraphRange(graphRange);
		graphView.setGraphType(graphType);
		graphView.refreshGraph(false);
	}
}
