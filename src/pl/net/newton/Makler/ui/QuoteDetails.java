package pl.net.newton.Makler.ui;

import java.math.BigDecimal;

import android.content.Intent;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import pl.net.newton.Makler.R;
import pl.net.newton.Makler.db.quote.Quote;
import pl.net.newton.Makler.db.quote.QuoteField;
import pl.net.newton.Makler.db.quote.QuotesDao;
import pl.net.newton.Makler.db.symbol.SymbolsDb;
import pl.net.newton.Makler.gpw.QuotesReceiver;
import pl.net.newton.Makler.gpw.ex.GpwException;
import pl.net.newton.Makler.gpw.service.QuotesListener;
import pl.net.newton.Makler.history.EntryListWithIndexes;
import pl.net.newton.Makler.history.service.HistoryListener;
import pl.net.newton.Makler.history.service.HistoryService;
import pl.net.newton.Makler.ui.adapter.QuotesAdapter;
import pl.net.newton.Makler.ui.graph.GraphView;
import pl.net.newton.Makler.common.DateFormatUtils;
import pl.net.newton.Makler.common.NumberFormatUtils;

public class QuoteDetails extends AbstractActivity implements QuotesListener, HistoryListener {

	private static final String SYMBOL = "symbol";

	private Quote quote;

	private String quoteSymbol;

	private HistoryService historyService;

	private Boolean index;

	private SparseArray<TextView> textViews;

	private GraphView graphView;

	private int graphRange = 0;

	private int graphType = 0;

	private LinearLayout graphLayout;

	private QuotesDao quotesDb;

	private SymbolsDb symbolsDb;

	private final Runnable mRefreshList = new Runnable() {
		public void run() {
			refresh();
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (savedInstanceState != null) {
			graphRange = savedInstanceState.getInt("graphRange", 0);
			graphType = savedInstanceState.getInt("graphType", 0);
		}

		textViews = new SparseArray<TextView>();
		quoteSymbol = getIntent().getStringExtra(SYMBOL);
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

	public void quotesUpdated() {
		mHandler.post(mRefreshList);
	}

	private void refresh() {
		if (quotesDb == null) {
			return;
		}

		quote = quotesDb.getQuoteBySymbol(quoteSymbol);
		if (quote == null || quote.get(QuoteField.SYMBOL) == null) {
			return;
		}

		if (quote.get(QuoteField.SYMBOL).length() > 5) {
			TextView symbol = (TextView) findViewById(R.id.quoteDetailSymbol);
			symbol.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
			symbol.setPadding(0, QuotesAdapter.dpToPx(this, 14), 0, 0);
		}

		setTextView(R.id.quoteDetailKurs, quote.chooseKurs());
		setTextView(R.id.quoteDetailZmiana, NumberFormatUtils.formatNumber(quote.chooseZmiana()) + "%");
		setTextView(R.id.quoteDetailKursMax, quote.getAsDecimal(QuoteField.MAX));
		setTextView(R.id.quoteDetailKursMin, quote.getAsDecimal(QuoteField.MIN));
		setTextView(R.id.quoteDetailKursOdn, quote.getAsDecimal(QuoteField.REFERENCE));
		setTextView(R.id.quoteDetailSymbol, quote.get(QuoteField.SYMBOL));
		if (!index) {
			setTextView(R.id.quoteDetailName, quote.get(QuoteField.NAME));
			setTextView(R.id.quoteDetailTko, quote.getAsDecimal(QuoteField.TKO));
			setTextView(R.id.quoteDetailTkoProcent, quote.getAsDecimal(QuoteField.TKO_PERCENT));
			setTextView(R.id.quoteDetailKursOtw, quote.getAsDecimal(QuoteField.OPEN));
			setTextView(R.id.quoteDetailWolumen, quote.getAsInt(QuoteField.VOL));

			setTextView(R.id.quoteDetailOfertK, quote.getAsInt(QuoteField.BID_OFFERS));
			setTextView(R.id.quoteDetailWolumenK, quote.getAsInt(QuoteField.BID_VOL));
			setTextView(R.id.quoteDetailLimitK, quote.getAsDecimal(QuoteField.BID));

			setTextView(R.id.quoteDetailLimitS, quote.getAsDecimal(QuoteField.ASK));
			setTextView(R.id.quoteDetailWolumenS, quote.getAsInt(QuoteField.ASK_VOL));
			setTextView(R.id.quoteDetailOfertS, quote.getAsInt(QuoteField.ASK_OFFERS));

		}
		setTextView(R.id.quoteDetailUpdate,
				DateFormatUtils.formatTime(quote.getAsCalendar(QuoteField.UPDATED)));
		setTextView(R.id.quoteDetailWartosc, quote.getAsDecimal(QuoteField.VALUE));

		Resources res = getResources();
		View zmiana = findViewById(R.id.quoteDetailZmiana);
		setBackground(res, zmiana);
	}

	@SuppressWarnings("deprecation")
	private void setBackground(Resources res, View zmiana) {
		if (quote.chooseZmiana() != null) {
			switch (quote.chooseZmiana().compareTo(BigDecimal.ZERO)) {
				case -1:
					zmiana.setBackgroundDrawable(res.getDrawable(R.drawable.redbox));
					break;
				case 1:
					zmiana.setBackgroundDrawable(res.getDrawable(R.drawable.greenbox));
					break;
				case 0:
				default:
					zmiana.setBackgroundDrawable(res.getDrawable(R.drawable.bluebox));
					break;
			}
		}
	}

	private void setTextView(int id, String text) {
		TextView t = textViews.get(id);
		if (t == null) {
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
				intent.putExtra(SYMBOL, quote.get(QuoteField.SYMBOL));
				startActivity(intent);
				break;

			case R.id.addToWallet:
				intent = new Intent(this, WalletForm.class);
				intent.putExtra(SYMBOL, quote.get(QuoteField.SYMBOL));
				intent.putExtra("quote", quote.chooseKurs());
				startActivity(intent);
				break;

			case R.id.graphRange:
				graphView.changeGraphRange();
				break;

			default:
				// do nothing
				break;
		}
		return super.onOptionsItemSelected(item);
	}

	private void updateQuotes() {
		perform(new ProcessPerformer() {
			public void showResults(boolean result) {
				refresh();
			}

			public boolean perform(QuotesReceiver quotesReceiver) throws GpwException {
				quotesService.updateQuotes();
				return true;
			}
		}, true);
	}

	public void gotEntries(EntryListWithIndexes entries) {
		graphView.gotEntries(entries);
	}

	@Override
	protected void initUi(SQLiteDatabase sqlDb, HistoryService historyService) {
		this.symbolsDb = new SymbolsDb(sqlDb, this);
		this.quotesDb = new QuotesDao(sqlDb, this);

		quote = quotesDb.getQuoteBySymbol(quoteSymbol);
		index = quote.getAsBoolean(QuoteField.IS_INDEX);
		if (index) {
			setContentView(R.layout.quote_details_index);
		} else {
			setContentView(R.layout.quote_details);
		}
		graphLayout = (LinearLayout) findViewById(R.id.graphParent);
		setTitle(quote.get(QuoteField.NAME));
		refresh();

		graphLayout.removeAllViews();
		graphView = new GraphView(this, quote, mHandler, historyService, symbolsDb);
		graphLayout.addView(graphView);
		graphView.setGraphRange(graphRange);
		graphView.setGraphType(graphType);
		graphView.refreshGraph(false);
	}
}
