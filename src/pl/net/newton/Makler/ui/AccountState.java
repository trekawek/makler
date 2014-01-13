package pl.net.newton.Makler.ui;

import pl.net.newton.Makler.R;
import pl.net.newton.Makler.gpw.QuotesReceiver;
import pl.net.newton.Makler.gpw.Trades;
import pl.net.newton.Makler.gpw.ex.GpwException;
import pl.net.newton.Makler.gpw.ex.InvalidPasswordException;
import pl.net.newton.Makler.gpw.model.Finances;
import pl.net.newton.Makler.gpw.model.Paper;
import pl.net.newton.Makler.gpw.service.GpwProvider;
import pl.net.newton.Makler.history.service.HistoryService;
import pl.net.newton.Makler.ui.adapter.PapersAdapter;
import pl.net.newton.Makler.common.NumberFormatUtils;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

public class AccountState extends AbstractActivity {
	private Finances finances;

	private ListView papers;

	private TextView avail, money, charges, value;

	private View table;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.account_state);

		papers = (ListView) findViewById(R.id.accountStatePapers);
		avail = (TextView) findViewById(R.id.accountStateAvail);
		money = (TextView) findViewById(R.id.accountStateMoney);
		charges = (TextView) findViewById(R.id.accountStateCharges);
		value = (TextView) findViewById(R.id.accountStatePaperValue);
		table = findViewById(R.id.accountStateTable);
		table.setVisibility(android.view.View.INVISIBLE);
		setTitle("Stan konta");
		registerForContextMenu(papers);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.account_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.refreshAccount:
				getFinances();
				break;
		}
		return super.onOptionsItemSelected(item);
	}

	private static class ContextMenuItem {
		final static int SELL = 0;
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		menu.add(0, ContextMenuItem.SELL, 0, "Sprzedaj");
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
		Paper paper;
		try {
			paper = finances.getPapers().get(info.position);
		} catch (NullPointerException e) {
			return false;
		}

		Intent intent;

		switch (item.getItemId()) {
			case ContextMenuItem.SELL:
				intent = new Intent(this, OrderForm.class);
				if (paper.getSymbol() != null)
					intent.putExtra("symbol", paper.getSymbol().getSymbol());
				else
					intent.putExtra("symbol", paper.getName());
				intent.putExtra("type", 'S');
				intent.putExtra("quantity", paper.getQuantity());
				startActivity(intent);
				break;
		}
		return true;
	}

	private void refreshList() {
		mHandler.post(new Runnable() {
			public void run() {
				if (finances == null) {
					return;
				}
				if (finances.getPapers().size() == 0) {
					findViewById(R.id.noPapers).setVisibility(View.VISIBLE);
				} else {
					findViewById(R.id.noPapers).setVisibility(View.GONE);
				}

				papers.setAdapter(new PapersAdapter(AccountState.this, finances));

				avail.setText(NumberFormatUtils.formatNumber(finances.getAvail()));
				money.setText(NumberFormatUtils.formatNumber(finances.getAccount()));
				charges.setText(NumberFormatUtils.formatNumber(finances.getCharges()));
				value.setText(NumberFormatUtils.formatNumber(finances.getValue()));
				table.setVisibility(android.view.View.VISIBLE);
			}
		});
	}

	private void getFinances() {
		perform(new ProcessPerformer() {
			public void showResults(boolean result) {
				refreshList();
			}

			public boolean perform(QuotesReceiver quotesReceiver, Trades trades) throws GpwException,
					InvalidPasswordException {
				finances = trades.getFinances();
				return true;
			}
		}, true);
	}

	@Override
	protected void initUi(GpwProvider gpwProvider, SQLiteDatabase sqlDb, HistoryService historyService) {
		getFinances();
	}
}
