package pl.net.newton.Makler.ui;

import pl.net.newton.Makler.R;
import pl.net.newton.Makler.db.quote.Quote;
import pl.net.newton.Makler.db.quote.QuotesDb;
import pl.net.newton.Makler.db.symbol.SymbolsDb;
import pl.net.newton.Makler.history.EntryListWithIndexes;
import pl.net.newton.Makler.history.service.HistoryListener;
import pl.net.newton.Makler.history.service.HistoryService;
import pl.net.newton.Makler.ui.graph.GraphView;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.LinearLayout;

public class FullScreenGraph extends AbstractActivity implements HistoryListener {
	private static final String GRAPH_TYPE = "graphType";

	private static final String GRAPH_RANGE = "graphRange";

	private Quote quote;

	private String quoteSymbol;

	private HistoryService historyService;

	private GraphView graphView;

	private int graphType, graphRange;

	private SymbolsDb symbolsDb;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		graphRange = getIntent().getIntExtra(GRAPH_RANGE, 0);
		graphType = getIntent().getIntExtra(GRAPH_TYPE, 0);
		if (savedInstanceState != null) {
			graphRange = savedInstanceState.getInt(GRAPH_RANGE, graphRange);
			graphType = savedInstanceState.getInt(GRAPH_TYPE, graphType);
		}

		quoteSymbol = getIntent().getStringExtra("symbol");
		setContentView(R.layout.full_screen_graph);
	}

	@Override
	protected void onSaveInstanceState(Bundle state) {
		if (graphView != null) {
			state.putInt(GRAPH_RANGE, graphView.getGraphRange());
			state.putInt(GRAPH_TYPE, graphView.getGraphType());
		}
		super.onSaveInstanceState(state);
	}

	@Override
	public void onDestroy() {
		if (historyService != null) {
			historyService.unregister(this);
		}
		super.onDestroy();
	}

	public void gotEntries(EntryListWithIndexes entries) {
		graphView.gotEntries(entries);
	}

	@Override
	protected void initUi(SQLiteDatabase sqlDb, HistoryService historyService) {
		QuotesDb quotesDb = new QuotesDb(sqlDb, this);
		this.symbolsDb = new SymbolsDb(sqlDb, this);
		quote = quotesDb.getQuoteBySymbol(quoteSymbol);
		setTitle(quote.getName());

		graphView = new GraphView(this, quote, mHandler, historyService, symbolsDb);
		graphView.setGraphRange(graphRange);
		graphView.setGraphType(graphType);
		graphView.refreshGraph(false);

		LinearLayout mainLayout = (LinearLayout) findViewById(R.id.graphParent);
		mainLayout.addView(graphView);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.full_screen_graph, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.graphRange) {
			graphView.changeGraphRange();
		}
		return super.onOptionsItemSelected(item);
	}

}
