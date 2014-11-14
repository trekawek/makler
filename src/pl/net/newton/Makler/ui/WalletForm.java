package pl.net.newton.Makler.ui;

import java.math.BigDecimal;

import org.apache.commons.lang3.StringUtils;

import pl.net.newton.Makler.R;
import pl.net.newton.Makler.db.symbol.Symbol;
import pl.net.newton.Makler.db.symbol.SymbolsDb;
import pl.net.newton.Makler.db.wallet.WalletDb;
import pl.net.newton.Makler.db.wallet.WalletItem;
import pl.net.newton.Makler.history.service.HistoryService;
import android.app.Activity;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.RadioButton;

public class WalletForm extends AbstractActivity implements OnClickListener {

	private RadioButton kupno, sprzedaz;

	private EditText walor, ilosc, kurs;

	private SymbolsDb symbolsDb;

	private WalletDb walletDb;

	public static final int GET_SYMBOL_K = 200;

	public static final int GET_SYMBOL_S = 201;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.wallet_trans);

		kupno = (RadioButton) findViewById(R.id.transTypeK);
		sprzedaz = (RadioButton) findViewById(R.id.transTypeS);
		ilosc = (EditText) findViewById(R.id.transIlosc);
		walor = (EditText) findViewById(R.id.transWalor);
		kurs = (EditText) findViewById(R.id.transKurs);

		findViewById(R.id.transWalorBtn).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent(v.getContext(), Symbols.class);
				if (kupno.isChecked()) {
					startActivityForResult(intent, GET_SYMBOL_K);
				} else if (sprzedaz.isChecked()) {
					intent.putExtra("forWalletSell", true);
					startActivityForResult(intent, GET_SYMBOL_S);
				}
			}
		});

		findViewById(R.id.transDodaj).setOnClickListener(this);

		String symbol = getIntent().getStringExtra("symbol");
		String quote = getIntent().getStringExtra("quote");
		if (symbol != null) {
			this.walor.setText(symbol);
		}
		if (quote != null) {
			this.kurs.setText(quote.replace(',', '.').replaceAll("[^0-9\\.]", ""));
		}

	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if ((requestCode == GET_SYMBOL_K || requestCode == GET_SYMBOL_S) && resultCode == Activity.RESULT_OK) {
			String symbol = data.getStringExtra("symbol");
			walor.setText(symbol);
		}
	}

	public void onClick(View v) {
		Character type = null;
		if (kupno.isChecked()) {
			type = 'K';
		} else if (sprzedaz.isChecked()) {
			type = 'S';
		}
		final String name = this.walor.getText().toString();
		final String quantity = this.ilosc.getText().toString();
		final String quote = this.kurs.getText().toString();
		if (StringUtils.isAnyEmpty(name, quantity, quote) || type == null) {
			return;
		}
		if (addTransaction(quantity, quote, type, name)) {
			Intent resultIntent = new Intent();
			setResult(Activity.RESULT_OK, resultIntent);
			finish();
		}

	}

	private boolean addTransaction(String quantity, String quote, Character type, String symbolName) {
		Symbol s = symbolsDb.getSymbolBySymbol(symbolName);
		if (s == null) {
			return false;
		}
		if (s.isIndex()) {
			showMessage("Nie możesz przeprowadzić transakcji z indeksem");
			return false;
		}
		WalletItem item = walletDb.getWalletItem(s);
		String q = quantity;
		if (type == 'S' && item.getQuantity() < Integer.parseInt(quantity)) {
			q = item.getQuantity().toString();
		}

		BigDecimal commision = config.getCommision();
		BigDecimal minCommision = config.getMinCommision();
		BigDecimal account = config.getWalletAccount();
		BigDecimal value = new BigDecimal(quote).multiply(new BigDecimal(q));
		BigDecimal com = value.divide(new BigDecimal(100)).multiply(commision);
		if (com.compareTo(minCommision) < 0) {
			com = minCommision;
		}
		account = account.subtract(com);
		if (type == 'K') {
			account = account.subtract(value);
		} else {
			account = account.add(value);
		}

		item.addTrans(type, Integer.parseInt(quantity), new BigDecimal(quote.replace(" ", "")), com);
		walletDb.updateWalletItem(item);
		config.setWalletAccount(account.toString());
		return true;
	}

	@Override
	protected void initUi(SQLiteDatabase sqlDb, HistoryService historyService) {
		this.walletDb = new WalletDb(sqlDb, this);
		this.symbolsDb = new SymbolsDb(sqlDb, this);
	}
}
