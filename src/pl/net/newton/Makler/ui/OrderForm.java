package pl.net.newton.Makler.ui;

import java.util.Calendar;

import pl.net.newton.Makler.R;
import pl.net.newton.Makler.common.DataSource;
import pl.net.newton.Makler.db.symbol.SymbolsDb;
import pl.net.newton.Makler.gpw.QuotesReceiver;
import pl.net.newton.Makler.gpw.Trades;
import pl.net.newton.Makler.gpw.ex.GpwException;
import pl.net.newton.Makler.gpw.ex.InvalidPasswordException;
import pl.net.newton.Makler.gpw.model.Order;
import pl.net.newton.Makler.gpw.model.OrderState;
import pl.net.newton.Makler.gpw.model.Order.LimitType;
import pl.net.newton.Makler.gpw.model.Order.Validity;
import pl.net.newton.Makler.gpw.service.GpwProvider;
import pl.net.newton.Makler.history.service.HistoryService;
import pl.net.newton.Makler.sms.CodeListener;
import pl.net.newton.Makler.sms.SmsIntentReceiver;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TimePicker;

public class OrderForm extends AbstractActivity implements OnCheckedChangeListener,
		OnClickListener, OnItemSelectedListener, CodeListener {
	public static final int GET_SYMBOL_K = 100;

	public static final int GET_SYMBOL_S = 101;

	private RadioButton peg, pkc, pcr, kupno, sprzedaż;

	private RadioGroup limitType;

	private String limitTypeStr;

	private EditText limit, limitAkt, iloscUjawn, iloscMin, walor, ilosc, smsPass;

	private Button sesja, doDnia, wdc, listaWal, dodaj;

	private static final int SESJA_DATE_DIALOG_ID = 0, DODNIA_DATE_DIALOG_ID = 1, WDC_DIALOG_ID = 2;

	private int sesjaYear, sesjaMonth, sesjaDay;

	private int doDniaYear, doDniaMonth, doDniaDay;

	private int wdcHour = 17, wdcMinute = 0;

	private Spinner waznosc;

	private String orderId;

	private OrderState orderState;

	private boolean smsPassDisabled = false;

	private SmsIntentReceiver smsRecv;

	private DataSource dataSource;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.order_form);

		dataSource = config.getDataSourceType();
		registerReceiver(smsRecv = new SmsIntentReceiver(this), new IntentFilter(
				"android.provider.Telephony.SMS_RECEIVED"));

		orderId = getIntent().getStringExtra("orderId");

		kupno = (RadioButton) findViewById(R.id.orderFormTypeK);
		sprzedaż = (RadioButton) findViewById(R.id.orderFormTypeS);
		ilosc = (EditText) findViewById(R.id.orderFormIlosc);
		limitType = (RadioGroup) findViewById(R.id.orderFormLimitType);
		peg = (RadioButton) findViewById(R.id.orderFormPEG);
		pkc = (RadioButton) findViewById(R.id.orderFormPKC);
		pcr = (RadioButton) findViewById(R.id.orderFormPCR);
		limit = (EditText) findViewById(R.id.orderFormLimit);
		limitAkt = (EditText) findViewById(R.id.orderFormLimitAkt);
		limitAkt.setOnFocusChangeListener(new OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {
					iloscMin.setText("");
				}
			}
		});
		iloscUjawn = (EditText) findViewById(R.id.orderFormIloscUjawn);
		iloscMin = (EditText) findViewById(R.id.orderFormIloscMin);
		walor = (EditText) findViewById(R.id.orderFormWalor);
		sesja = (Button) findViewById(R.id.orderFormSesja);
		doDnia = (Button) findViewById(R.id.orderFormDoDnia);
		wdc = (Button) findViewById(R.id.orderFormWdc);
		waznosc = (Spinner) findViewById(R.id.orderFormWaznosc);
		waznosc.setOnItemSelectedListener(this);
		listaWal = (Button) findViewById(R.id.orderFormWalorBtn);
		dodaj = (Button) findViewById(R.id.orderFormDodaj);
		smsPass = (EditText) findViewById(R.id.orderFormSmsPassword);

		limitType.setOnCheckedChangeListener(this);
		limit.setOnFocusChangeListener(new OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus && limitTypeStr != null && !limitTypeStr.equals("PEG")) {
					limitType.clearCheck();
				}
			}
		});
		sesja.setOnClickListener(this);
		doDnia.setOnClickListener(this);
		wdc.setOnClickListener(this);
		listaWal.setOnClickListener(this);
		dodaj.setOnClickListener(this);

		final Calendar c = Calendar.getInstance();
		doDniaYear = sesjaYear = c.get(Calendar.YEAR); /* TODO - wyznaczyć lepsze daty */
		doDniaMonth = sesjaMonth = c.get(Calendar.MONTH) + 1;
		doDniaDay = sesjaDay = c.get(Calendar.DAY_OF_MONTH);

		iloscUjawn.setEnabled(false);
		iloscMin.setEnabled(true);
		iloscMin.setOnFocusChangeListener(new OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {
					limitAkt.setText("");
					if (!"PCR".equals(limitTypeStr)) {
						limitType.clearCheck();
					}
				}
			}
		});
		doDnia.setEnabled(false);
		wdc.setEnabled(false);

		updateSesja();
		populateWaznosc();
		updateDoDnia();

		String symbol = getIntent().getStringExtra("symbol");
		Character type = getIntent().getCharExtra("type", '-');
		Integer quantity = getIntent().getIntExtra("quantity", -1);
		if (symbol != null)
			walor.setText(symbol);
		if (quantity > -1)
			ilosc.setText(quantity.toString());
		kupno.setChecked(type == 'K');
		sprzedaż.setChecked(type == 'S');
		setTitle("Zlecenie");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		unregisterReceiver(smsRecv);
	}

	private void createOrder() {
		if (!smsPassDisabled) {
			if (smsPass.getText().toString().length() == 0) {
				showMessage("Wpisz hasło SMS-owe");
				return;
			}
			disablePassword(smsPass.getText().toString());
			return;
		}
		EditText ilosc = (EditText) findViewById(R.id.orderFormIlosc);
		try {
			Order o = new Order(this, kupno.isChecked() ? 'K' : 'S', walor.getText().toString(), ilosc
					.getText().toString(), limit.getText().toString(), limitTypeStr, sesja.getText()
					.toString(), (String) waznosc.getSelectedItem(), doDnia.getText().toString(), limitAkt
					.getText().toString(), iloscUjawn.getText().toString(), iloscMin.getText().toString(),
					symbolsDb, wdc.getText().toString() + ":00");
			if (orderId == null) {
				trade(o);
			} else {
				updateOrder(orderId, o);
			}
		} catch (Exception e) {
			showMessage("Błąd podczas wysłania zlecenia.");
		}
	}

	private void trade(final Order o) {
		perform(new ProcessPerformer() {
			public void showResults(boolean result) {
				if (result) {
					Intent resultIntent = new Intent();
					setResult(Activity.RESULT_OK, resultIntent);
					finish();
				} else {
					showMessage("Błąd podczas wysłania zlecenia.");
				}
			}

			public boolean perform(QuotesReceiver quotesReceiver, Trades trades) throws GpwException,
					InvalidPasswordException {
				return trades.trade(o) != null;
			}
		}, true);
	}

	private void updateOrder(final String orderId, final Order o) {
		perform(new ProcessPerformer() {
			public void showResults(boolean result) {
				Intent resultIntent = new Intent();
				setResult(Activity.RESULT_OK, resultIntent);
				finish();
			}

			public boolean perform(QuotesReceiver quotesReceiver, Trades trades) throws GpwException,
					InvalidPasswordException {
				trades.changeOrder(orderId, o);
				return true;
			}
		}, true);
	}

	private void updateSesja() {
		// if(isEpromak())
		// sesja.setText(String.format("%d-%02d-%02d", sesjaYear, sesjaMonth+1, sesjaDay));
		// else
		sesja.setText(String.format("%d-%02d-%02d", sesjaYear, sesjaMonth, sesjaDay));
	}

	private void updateDoDnia() {
		Validity validity = Order.validityFromString(this, (String) waznosc.getSelectedItem());
		doDnia.setEnabled(false);
		wdc.setEnabled(false);
		if (validity == Validity.DODN || validity == Validity.WDC) {
			doDnia.setEnabled(true);
			doDnia.setText(String.format("%d-%02d-%02d", doDniaYear, doDniaMonth, doDniaDay));
		}
		if (validity == Validity.WDC) {
			wdc.setEnabled(true);
			wdc.setText(String.format("%02d:%02d", wdcHour, wdcMinute));
		}
	}

	private void populateWaznosc() {
		ArrayAdapter<?> adapter;
		if (!waznosc.isEnabled())
			return;
		adapter = ArrayAdapter.createFromResource(this, R.array.order_form_waznosc,
				android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		waznosc.setAdapter(adapter);
	}

	public void onCheckedChanged(RadioGroup arg0, int arg1) {
		if (peg.isChecked()) {
			limitTypeStr = "PEG";
		} else if (pkc.isChecked()) {
			limit.setText("");
			limitTypeStr = "PKC";
		} else if (pcr.isChecked()) {
			limit.setText("");
			limitTypeStr = "PCR";
		} else {
			limitTypeStr = null;
		}
		if (dataSource == DataSource.mbank && orderId != null) {
			iloscMin.setEnabled(false);
		}
		if (limitTypeStr != null && !limitTypeStr.equals("PCR")) {
			iloscMin.setText("");
		}
	}

	@SuppressWarnings("deprecation")
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.orderFormSesja:
				showDialog(SESJA_DATE_DIALOG_ID);
				break;
			case R.id.orderFormDoDnia:
				showDialog(DODNIA_DATE_DIALOG_ID);
				break;
			case R.id.orderFormWdc:
				showDialog(WDC_DIALOG_ID);
				break;
			case R.id.orderFormWalorBtn:
				Intent intent = new Intent(this, Symbols.class);
				if (kupno.isChecked())
					startActivityForResult(intent, GET_SYMBOL_K);
				else if (sprzedaż.isChecked()) {
					intent.putExtra("forSell", true);
					startActivityForResult(intent, GET_SYMBOL_S);
				}
				break;
			case R.id.orderFormDodaj:
				createOrder();
				break;
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
			case SESJA_DATE_DIALOG_ID:
				return new DatePickerDialog(this, sesjaDateListener, sesjaYear, sesjaMonth - 1, sesjaDay);

			case DODNIA_DATE_DIALOG_ID:
				return new DatePickerDialog(this, doDniaDateListener, doDniaYear, doDniaMonth - 1, doDniaDay);

			case WDC_DIALOG_ID:
				return new TimePickerDialog(this, wdcTimeListener, 17, 0, true);
		}
		return null;
	}

	private DatePickerDialog.OnDateSetListener sesjaDateListener = new DatePickerDialog.OnDateSetListener() {
		public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
			sesjaYear = year;
			sesjaMonth = monthOfYear + 1;
			sesjaDay = dayOfMonth;
			updateSesja();
		}
	};

	private DatePickerDialog.OnDateSetListener doDniaDateListener = new DatePickerDialog.OnDateSetListener() {
		public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
			doDniaYear = year;
			doDniaMonth = monthOfYear + 1;
			doDniaDay = dayOfMonth;
			updateDoDnia();
		}
	};

	private TimePickerDialog.OnTimeSetListener wdcTimeListener = new TimePickerDialog.OnTimeSetListener() {
		@Override
		public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
			wdcHour = hourOfDay;
			wdcMinute = minute;
			updateDoDnia();
		}
	};

	private SymbolsDb symbolsDb;

	public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		updateDoDnia();
	}

	public void onNothingSelected(AdapterView<?> arg0) {
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
			case GET_SYMBOL_K:
			case GET_SYMBOL_S:
				if (resultCode == Activity.RESULT_OK) {
					String symbol = data.getStringExtra("symbol");
					walor.setText(symbol);
				}
		}
	}

	public void setOrderState(final OrderState s) {
		if (s == null)
			return;
		Order o = s.getOrder();
		kupno.setChecked(o.getType() == 'K');
		kupno.setEnabled(false);
		sprzedaż.setChecked(o.getType() == 'S');
		sprzedaż.setEnabled(false);
		walor.setText(o.getSymbol().getSymbol());
		walor.setEnabled(false);
		listaWal.setEnabled(false);
		ilosc.setText(o.getIlosc().toString());
		if (o.getLimit() != null)
			limit.setText(o.getLimit().toString());
		peg.setChecked(o.getLimitType() == LimitType.PEG);
		pkc.setChecked(o.getLimitType() == LimitType.PKC);
		pcr.setChecked(o.getLimitType() == LimitType.PCR);
		peg.setEnabled(false);
		if (o.getLimitType() == LimitType.PEG) {
			pkc.setEnabled(false);
			pcr.setEnabled(false);
		}
		sesja.setText(o.getSesja());
		sesja.setEnabled(false);
		populateWaznosc();
		int waznoscCount = waznosc.getAdapter().getCount();
		for (int i = 0; i < waznoscCount; i++)
			if (Order.validityFromString(getApplicationContext(), (String) waznosc.getAdapter().getItem(i)) == o
					.getValidity()) {
				waznosc.setSelection(i);
				break;
			}
		if (o.getLimitAkt() != null)
			limitAkt.setText(o.getLimitAkt().toString());
		if (o.getIloscUjawn() != null)
			iloscUjawn.setText(o.getIloscUjawn().toString());
		if (o.getIloscMin() != null)
			iloscMin.setText(o.getIloscMin().toString());
		dodaj.setText("Modyfikuj");

		if (dataSource == DataSource.mbank) {
			String[] waz = null;
			if (o.getDoDnia() != null)
				waz = o.getDoDnia().split(getString(R.string.minus));
			else
				waz = o.getSesja().split(getString(R.string.minus));
			doDniaYear = Integer.valueOf(waz[0]);
			doDniaMonth = Integer.valueOf(waz[1]);
			doDniaDay = Integer.valueOf(waz[2]);
			updateDoDnia();
			waznosc.setEnabled(false);
			iloscMin.setEnabled(false);
			iloscUjawn.setEnabled(false);
		}
	}

	private void disablePassword(final String code) {
		perform(new ProcessPerformer() {
			public void showResults(boolean result) {
				if (result) {
					createOrder();
				} else {
					showMessage("Nieprawidłowe hasło SMS");
				}
			}

			public boolean perform(QuotesReceiver quotesReceiver, Trades trades) throws GpwException,
					InvalidPasswordException {
				return smsPassDisabled = trades.disablePassword(code);
			}
		}, true);
	}

	private void tryToDisablePassword() {
		perform(new ProcessPerformer() {
			public void showResults(boolean result) {
				if (result) {
					findViewById(R.id.orderFormSmsPasswordLbl).setVisibility(View.GONE);
					smsPass.setVisibility(View.GONE);
				} else {
					findViewById(R.id.orderFormSmsPasswordLbl).setVisibility(View.VISIBLE);
					smsPass.setVisibility(View.VISIBLE);
				}
			}

			public boolean perform(QuotesReceiver quotesReceiver, Trades trades) throws GpwException,
					InvalidPasswordException {
				return smsPassDisabled = trades.disablePassword();
			}
		}, true);
	}

	public void setCode(final String code) {
		mHandler.post(new Runnable() {
			public void run() {
				smsPass.setText(code);
			}
		});
	}

	@Override
	protected void initUi(GpwProvider gpwProvider, SQLiteDatabase sqlDb, HistoryService historyService) {
		this.symbolsDb = new SymbolsDb(sqlDb, this);
		tryToDisablePassword();
		if (orderId != null) {
			perform(new ProcessPerformer() {
				public void showResults(boolean result) {
					if (result) {
						setOrderState(orderState);
					}
				}

				public boolean perform(QuotesReceiver quotesReceiver, Trades trades) throws GpwException,
						InvalidPasswordException {
					orderState = trades.getOrderState(orderId);
					return orderState != null;
				}
			}, true);
		}
	}

	@Override
	protected boolean updatesEnabled() {
		return false;
	}
}