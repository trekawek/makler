package pl.net.newton.Makler.ui;

import java.math.BigDecimal;
import java.util.List;

import pl.net.newton.Makler.R;
import pl.net.newton.Makler.db.wallet.WalletDb;
import pl.net.newton.Makler.db.wallet.WalletItem;
import pl.net.newton.Makler.gpw.QuotesReceiver;
import pl.net.newton.Makler.gpw.ex.GpwException;
import pl.net.newton.Makler.gpw.service.QuotesListener;
import pl.net.newton.Makler.history.service.HistoryService;
import pl.net.newton.Makler.ui.adapter.WalletAdapter;
import pl.net.newton.Makler.common.NumberFormatUtils;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.method.DigitsKeyListener;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

public class Wallet extends AbstractActivity implements QuotesListener, OnItemClickListener {
	private static final String SYMBOL = "symbol";

	private static final int NEW_TRANS = 400;

	private static final String TAG = "Makler";

	private List<WalletItem> items;

	private ListView listView;

	private TextView walletAccount, walletGain;

	BigDecimal commision = BigDecimal.ZERO;

	BigDecimal minCommision = BigDecimal.ZERO;

	private WalletDb walletDb;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		try {
			commision = config.getCommision();
		} catch (Exception e) {
			Log.e(TAG, "Can't get comission", e);
		}
		try {
			minCommision = config.getMinCommision();
		} catch (Exception e) {
			Log.e(TAG, "Can't get min comission", e);
		}

		setContentView(R.layout.wallet);
		walletAccount = (TextView) findViewById(R.id.walletAccount);
		walletGain = (TextView) findViewById(R.id.walletGain);

		listView = (ListView) findViewById(R.id.walletListView);

		listView.setOnCreateContextMenuListener(this);
		listView.setOnItemClickListener(this);
		registerForContextMenu(listView);

		Button btn = (Button) findViewById(R.id.addTransButton);
		btn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent(v.getContext(), WalletForm.class);
				startActivityForResult(intent, NEW_TRANS);
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.wallet_menu, menu);
		return true;
	}

	// @Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		WalletItem item = items.get(position);
		showDetails(item);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.refreshWalletItems:
				updateQuotes();
				break;

			case R.id.setWalletState:
				setAccount();
				break;

			default:
				// do nothing
				break;
		}
		return super.onOptionsItemSelected(item);
	}

	private static final class ContextMenuItem {
		static final int TRANSACTION = 0, DELETE = 1, UP = 2, DOWN = 3, DETAILS = 4, CALCULATOR = 5;

		private ContextMenuItem() {
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		menu.add(0, ContextMenuItem.CALCULATOR, 0, "Kalkulator");
		menu.add(0, ContextMenuItem.DETAILS, 0, "Szczegóły");
		menu.add(0, ContextMenuItem.TRANSACTION, 0, "Dodaj transakcję");
		menu.add(0, ContextMenuItem.UP, 0, "W górę");
		menu.add(0, ContextMenuItem.DOWN, 0, "W dół");
		menu.add(0, ContextMenuItem.DELETE, 0, "Usuń");
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
		WalletItem walletItem = items.get(info.position);

		Intent intent;

		switch (item.getItemId()) {
			case ContextMenuItem.CALCULATOR:
				showCalculator(walletItem);
				break;

			case ContextMenuItem.DETAILS:
				showDetails(walletItem);
				break;

			case ContextMenuItem.TRANSACTION:
				intent = new Intent(this, WalletForm.class);
				intent.putExtra(SYMBOL, walletItem.getSymbol());
				intent.putExtra("quote", NumberFormatUtils.formatNumber(walletItem.getQuote()));
				startActivityForResult(intent, NEW_TRANS);
				break;

			case ContextMenuItem.UP:
				walletDb.move(walletItem.getId(), true);
				refreshList();
				break;

			case ContextMenuItem.DOWN:
				walletDb.move(walletItem.getId(), false);
				refreshList();
				break;

			case ContextMenuItem.DELETE:
				walletDb.deleteWalletItem(walletItem.getId());
				refreshList();
				break;
		}
		return true;
	}

	private void showCalculator(WalletItem walletItem) {
		Intent intent = new Intent(this, WalletItemCalculator.class);
		intent.putExtra(SYMBOL, walletItem.getSymbol());
		startActivity(intent);
	}

	private void showDetails(WalletItem item) {
		Intent intent = new Intent(this, QuoteDetails.class);
		intent.putExtra(SYMBOL, item.getSymbol());
		intent.putExtra("wallet_item", true);
		startActivity(intent);
	}

	public void refreshList() {
		if (walletDb == null) {
			return;
		}

		items = walletDb.getWalletItems();
		listView.setAdapter(new WalletAdapter(this, items, commision, minCommision));

		BigDecimal acc = BigDecimal.ZERO;
		try {
			acc = config.getWalletAccount();
		} catch (Exception e) {
			Log.e(TAG, "Can't get wallet", e);
		}
		BigDecimal gain = BigDecimal.ZERO;

		for (WalletItem i : items) {
			gain = gain.add(i.gainWithCommision(commision, minCommision));
		}
		walletAccount.setText(NumberFormatUtils.formatNumber(acc));
		walletGain.setText(NumberFormatUtils.formatNumber(gain));
	}

	public void quotesUpdated() {
		mHandler.post(new Runnable() {
			public void run() {
				refreshList();
			}
		});
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == NEW_TRANS && resultCode == Activity.RESULT_OK && quotesService != null) {
			updateQuotes();
		}
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

	private void setAccount() {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);

		alert.setTitle("Wartość konta");
		alert.setMessage("Wprowadź wartość konta");

		// Set an EditText view to get user input
		final EditText input = new EditText(this);
		input.setKeyListener(DigitsKeyListener.getInstance(false, true));
		BigDecimal newWalletAccount = config.getWalletAccount();
		newWalletAccount = newWalletAccount.setScale(2, BigDecimal.ROUND_HALF_UP);
		input.setText(newWalletAccount.toString());

		alert.setView(input);

		alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				String value = input.getText().toString();
				if (value.length() == 0) {
					value = getString(R.string.zero);
				}
				config.setWalletAccount(value);
				refreshList();
			}
		});

		alert.setNegativeButton("Anuluj", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				// Canceled.
			}
		});

		alert.show();
	}

	@Override
	protected void initUi(SQLiteDatabase sqlDb, HistoryService historyService) {
		this.walletDb = new WalletDb(sqlDb, this);
		refreshList();
	}
}
