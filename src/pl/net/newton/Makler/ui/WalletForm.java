package pl.net.newton.Makler.ui;

import java.math.BigDecimal;

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
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.RadioButton;

public class WalletForm extends AbstractActivity implements OnClickListener {
	private static final String TAG = "Makler";

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
		String name, quantity, quote;
		Character type = null;
		if (kupno.isChecked()) {
			type = 'K';
		} else if (sprzedaz.isChecked()) {
			type = 'S';
		}
		name = this.walor.getText().toString();
		quantity = this.ilosc.getText().toString();
		quote = this.kurs.getText().toString();
		if (name.length() == 0 || quantity.length() == 0 || quote.length() == 0) {
			return;
		}
		if (type == null) {
			return;
		}
		Symbol s = symbolsDb.getSymbolBySymbol(name);
		if (s == null) {
			return;
		}

		if (s.isIndex()) {
			showMessage("Nie możesz przeprowadzić transakcji z indeksem");
			return;
		}

		WalletItem item = walletDb.getWalletItem(s);
		if (type == 'S' && item.getQuantity() < Integer.parseInt(quantity)) {
			quantity = item.getQuantity().toString();
		}

		BigDecimal commision = BigDecimal.ZERO;
		BigDecimal minCommision = BigDecimal.ZERO;
		BigDecimal account = BigDecimal.ZERO;
		try {
			commision = config.getCommision();
		} catch (Exception e) {
			Log.e(TAG, "Can't get commision", e);
		}
		try {
			minCommision = config.getMinCommision();
		} catch (Exception e) {
			Log.e(TAG, "Can't get min commision", e);
		}
		try {
			account = config.getWalletAccount();
		} catch (Exception e) {
			Log.e(TAG, "Can't get wallet account", e);
		}

		BigDecimal value = new BigDecimal(quote).multiply(new BigDecimal(quantity));
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

		Intent resultIntent = new Intent();
		setResult(Activity.RESULT_OK, resultIntent);
		finish();
	}

	@Override
	protected void initUi(SQLiteDatabase sqlDb, HistoryService historyService) {
		this.walletDb = new WalletDb(sqlDb, this);
		this.symbolsDb = new SymbolsDb(sqlDb, this);
	}
}
