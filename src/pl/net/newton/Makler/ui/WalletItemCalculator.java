package pl.net.newton.Makler.ui;

import java.math.BigDecimal;

import pl.net.newton.Makler.db.quote.Quote;
import pl.net.newton.Makler.db.quote.QuotesDb;
import pl.net.newton.Makler.db.symbol.SymbolsDb;
import pl.net.newton.Makler.db.wallet.WalletDb;
import pl.net.newton.Makler.db.wallet.WalletItem;
import pl.net.newton.Makler.history.service.HistoryService;
import pl.net.newton.Makler.ui.adapter.QuotesAdapter;
import pl.net.newton.Makler.R;
import pl.net.newton.Makler.common.Configuration;
import pl.net.newton.Makler.common.NumberFormatUtils;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

/**
 * @author Igor Andruszkiewicz
 * 
 */
public class WalletItemCalculator extends AbstractActivity implements TextWatcher, OnTouchListener {
	private static final int COUNTER_TIME_INTERVAL = 150; // ms

	private Quote quote;

	private WalletItem walletItem;

	private TextView walletItemZmiana;

	private TextView walletItemKurs;

	private TextView walletItemZysk;

	private TextView textViewZysk;

	BigDecimal commision = null;

	BigDecimal minCommision = null;

	private EditText editTextIlosc;

	private int quantity;

	private double kupno;

	private double sprzedaz;

	private TextView walletItemAvg;

	private TextView walletItemQuantity;

	private EditText editTextKupno;

	private EditText editTextSprzedaz;

	private Handler handler = new Handler();

	private String quoteSymbol;

	private WalletDb walletDb;

	private SymbolsDb symbolsDb;

	/**
	 * speed counter used to increase or decrease value step
	 */
	private int speed_counter = 0;

	private double mValueStep = 0.01;

	Runnable buttonPressedTask = new Runnable() {

		public void run() {
			buttonPressed();
			handler.postDelayed(this, COUNTER_TIME_INTERVAL);
			speed_counter += 1;

			if (speed_counter == 10) {
				mValueStep = 0.2;
			} else if (speed_counter == 10) {
				mValueStep = 0.5;
			} else if (speed_counter == 10) {
				mValueStep = 1.0;
			}
		}
	};

	private Button currentButtonPressed;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.wallet_item_calculator);

		quoteSymbol = getIntent().getStringExtra("symbol");

		Button buttonReset = (Button) findViewById(R.id.buttonReset);
		buttonReset.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				reset();
			}
		});

		Configuration config = new Configuration(this);
		commision = config.getCommision();
		minCommision = config.getMinCommision();

	}

	@Override
	public void onResume() {
		super.onResume();
	}

	private void reset() {
		walletItemQuantity.setText(NumberFormatUtils.formatNumber(walletItem.getQuantity()));
		walletItemKurs.setText(NumberFormatUtils.formatNumber(quote.getKurs()));
		walletItemZmiana.setText(NumberFormatUtils.formatNumber(quote.getZmiana()));
		walletItemAvg.setText(NumberFormatUtils.formatNumber(walletItem.getAvgBuy()));
		walletItemZysk.setText(NumberFormatUtils.formatNumber(walletItem.gain()));

		quantity = walletItem.getQuantity();
		kupno = walletItem.getAvgBuy().doubleValue();

		if (quote.getKurs() == null) {
			sprzedaz = kupno;
		} else {
			sprzedaz = quote.getKurs().doubleValue();
		}

		editTextIlosc.removeTextChangedListener(this);
		editTextKupno.removeTextChangedListener(this);
		editTextSprzedaz.removeTextChangedListener(this);

		// editTextIlosc.setText(walletItem.getQuantity().toString());
		editTextIlosc.setText(NumberFormatUtils.formatNumber(quantity));
		editTextKupno.setText(NumberFormatUtils.formatNumber(kupno));
		editTextSprzedaz.setText(NumberFormatUtils.formatNumber(sprzedaz));

		editTextIlosc.addTextChangedListener(this);
		editTextKupno.addTextChangedListener(this);
		editTextSprzedaz.addTextChangedListener(this);

		updateZysk();
	}

	private void setView() {
		String quoteSymbol = getIntent().getStringExtra("symbol");
		walletItem = walletDb.getWalletItem(symbolsDb.getSymbolBySymbol(quoteSymbol));

		if (quote.getSymbol().length() > 5) {
			TextView symbol = (TextView) findViewById(R.id.quoteDetailSymbol);
			symbol.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
			symbol.setPadding(0, QuotesAdapter.dpToPx(this, 14), 0, 0);
		}

		TextView walletItemSymbol = (TextView) findViewById(R.id.walletItemSymbol);
		TextView walletItemName = (TextView) findViewById(R.id.walletItemName);

		walletItemKurs = (TextView) findViewById(R.id.walletItemKurs); // aktualny
																		// kurs
																		// (ewentualnej
																		// sprzedaży)

		walletItemZmiana = (TextView) findViewById(R.id.walletItemZmiana);
		walletItemZysk = (TextView) findViewById(R.id.walletItemGain);
		walletItemAvg = (TextView) findViewById(R.id.walletItemAvg);
		walletItemQuantity = (TextView) findViewById(R.id.walletItemQuantity);

		textViewZysk = (TextView) findViewById(R.id.textViewZysk);

		walletItemSymbol.setText(quote.getSymbol());
		walletItemName.setText(quote.getName());
		if (quote.getKurs() == null) {
			walletItemKurs.setText(R.string.zero);
		} else {
			walletItemKurs.setText(quote.getKurs().toString());
		}
		walletItemZmiana.setText(String.valueOf(0));
		walletItemAvg.setText(NumberFormatUtils.formatNumber(walletItem.getAvgBuy()));
		walletItemZysk.setText(NumberFormatUtils.formatNumber(walletItem.gain()));
		walletItemQuantity.setText(String.valueOf(walletItem.getQuantity()));
		updateZmianaBackground(quote.chooseZmiana());

		editTextIlosc = (EditText) findViewById(R.id.editTextIlosc);
		editTextKupno = (EditText) findViewById(R.id.editTextKupno);
		editTextSprzedaz = (EditText) findViewById(R.id.editTextSprzedaz);

		Button button = (Button) findViewById(R.id.iloscPlus);
		button.setOnTouchListener(this);
		button = (Button) findViewById(R.id.iloscMinus);
		button.setOnTouchListener(this);

		button = (Button) findViewById(R.id.kupnoMinus);
		button.setOnTouchListener(this);
		button = (Button) findViewById(R.id.kupnoPlus);
		button.setOnTouchListener(this);

		button = (Button) findViewById(R.id.sprzedazMinus);
		button.setOnTouchListener(this);
		button = (Button) findViewById(R.id.sprzedazPlus);
		button.setOnTouchListener(this);

	}

	/**
	 * @param quote
	 */
	@SuppressWarnings("deprecation")
	public void updateZmianaBackground(BigDecimal value) {
		Resources res = getResources();
		View zmiana = findViewById(R.id.quoteDetailZmiana);

		if (value != null)
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

	private void updateZysk() {

		// Koszt KUPNA
		BigDecimal costKupno = new BigDecimal(kupno).multiply(new BigDecimal(quantity));
		BigDecimal commisionKupno = costKupno.multiply(commision).divide(BigDecimal.valueOf(100));
		if (commisionKupno.compareTo(minCommision) == -1) {
			commisionKupno = minCommision;
		}

		// Koszt SPRZEDAZY
		BigDecimal costSprzedaz = new BigDecimal(sprzedaz).multiply(new BigDecimal(quantity));
		BigDecimal commisionSprzedaz = costSprzedaz.multiply(commision).divide(BigDecimal.valueOf(100));
		if (commisionSprzedaz.compareTo(minCommision) == -1) {
			commisionSprzedaz = minCommision;
		}

		BigDecimal z = (costSprzedaz.subtract(costKupno).subtract(commisionSprzedaz).subtract(commisionKupno));

		int compare = z.compareTo(BigDecimal.ZERO);
		if (compare > 0) {
			textViewZysk.setTextColor(Color.parseColor("#ff229c22"));
		} else if (compare < 0) {
			textViewZysk.setTextColor(Color.parseColor("#ffff0303"));
		} else {
			textViewZysk.setTextColor(Color.GRAY);
		}

		textViewZysk.setText(String.format("%.02f", z.doubleValue()));

	}

	private void updateFields() {

		editTextIlosc.removeTextChangedListener(this);
		editTextKupno.removeTextChangedListener(this);
		editTextSprzedaz.removeTextChangedListener(this);

		editTextIlosc.setText(NumberFormatUtils.formatNumber(quantity));
		editTextKupno.setText(NumberFormatUtils.formatNumber(kupno));
		editTextSprzedaz.setText(NumberFormatUtils.formatNumber(sprzedaz));

		editTextIlosc.addTextChangedListener(this);
		editTextKupno.addTextChangedListener(this);
		editTextSprzedaz.addTextChangedListener(this);

		updateZysk();
	}

	public void buttonPressed() {
		switch (currentButtonPressed.getId()) {
			case R.id.iloscPlus:
				quantity += 1;
				break;
			case R.id.iloscMinus:
				quantity -= 1;
				break;
			case R.id.kupnoPlus:
				kupno += mValueStep;
				break;
			case R.id.kupnoMinus:
				kupno -= mValueStep;
				break;
			case R.id.sprzedazPlus:
				sprzedaz += mValueStep;
				break;
			case R.id.sprzedazMinus:
				sprzedaz -= mValueStep;
				break;
		}
		updateFields();
	}

	public boolean onTouch(View view, MotionEvent event) {

		boolean v = false;
		if ((event.getActionMasked() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_DOWN) {
			currentButtonPressed = (Button) view;
			currentButtonPressed.setPressed(true);
			handler.postDelayed(buttonPressedTask, 100);
			v = true;
		} else if ((event.getActionMasked() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP) {
			handler.removeCallbacks(buttonPressedTask);
			if (currentButtonPressed != null) {
				currentButtonPressed.setPressed(false);
			}
			currentButtonPressed = null;
			speed_counter = 0;
			mValueStep = 0.01;
			v = true;
		}
		return v;
	}

	public void afterTextChanged(Editable s) {
		if (s == editTextIlosc.getEditableText()) {
			quantity = NumberFormatUtils.parseIntOrZero(editTextIlosc.getText().toString());
		}

		if (s == editTextKupno.getEditableText()) {
			kupno = NumberFormatUtils.parseDoubleOrZero(editTextKupno.getText().toString());
		}

		if (s == editTextSprzedaz.getEditableText()) {
			sprzedaz = NumberFormatUtils.parseDoubleOrZero(editTextSprzedaz.getText().toString());
		}

		updateZysk();
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after) {
		// do nothing
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
		// do nothing
	}

	@Override
	protected void initUi(SQLiteDatabase sqlDb, HistoryService historyService) {
		this.walletDb = new WalletDb(sqlDb, this);
		this.symbolsDb = new SymbolsDb(sqlDb, this);
		QuotesDb quotesDb = new QuotesDb(sqlDb, this);
		quote = quotesDb.getQuoteBySymbol(quoteSymbol);
		setView();
		reset();
	}
}
