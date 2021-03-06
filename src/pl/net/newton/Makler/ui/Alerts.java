package pl.net.newton.Makler.ui;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;
import pl.net.newton.Makler.R;
import pl.net.newton.Makler.db.alert.Alert;
import pl.net.newton.Makler.db.alert.AlertBuilder;
import pl.net.newton.Makler.db.alert.AlertsDao;
import pl.net.newton.Makler.db.alert.Event;
import pl.net.newton.Makler.db.alert.Subject;
import pl.net.newton.Makler.db.quote.Quote;
import pl.net.newton.Makler.db.quote.QuoteField;
import pl.net.newton.Makler.db.quote.QuotesDao;
import pl.net.newton.Makler.history.service.HistoryService;
import pl.net.newton.Makler.ui.adapter.AlertsAdapter;
import pl.net.newton.Makler.ui.adapter.QuotesAdapter;

public class Alerts extends AbstractActivity implements OnClickListener, OnItemSelectedListener {

	private static final int EDIT_RESULT = 100;

	private ListView alertList;

	private List<Alert> alerts;

	private Quote quote;

	private AlertsDao alertsDb;

	private QuotesDao quotesDb;

	private String symbol;

	private int alertId;

	private Spinner subjectSpinner;

	private Spinner eventSpinner;

	private EditText valueEditText;

	private CheckBox percentCheckBox;

	private ArrayAdapter<CharSequence> eventQuoteAdapter, eventNonQuoteAdapter;

	private boolean eventInitialized;

	private Alert alert;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		symbol = getIntent().getStringExtra("symbol");
		alertId = getIntent().getIntExtra("alertId", 0);

		setContentView(R.layout.alerts);

		Button btn = (Button) findViewById(R.id.addAlert);
		btn.setOnClickListener(this);
		if (alertId != 0) {
			btn.setText("Modyfikuj");
		}
		alertList = (ListView) findViewById(R.id.alertList);
		alertList.setOnCreateContextMenuListener(this);

		subjectSpinner = (Spinner) findViewById(R.id.alertSubject);
		eventSpinner = (Spinner) findViewById(R.id.alertEvent);
		valueEditText = (EditText) findViewById(R.id.alertValue);
		percentCheckBox = (CheckBox) findViewById(R.id.alertPercent);

		eventQuoteAdapter = ArrayAdapter.createFromResource(this, R.array.alert_events_quote_strings,
				android.R.layout.simple_spinner_item);
		eventQuoteAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		eventNonQuoteAdapter = ArrayAdapter.createFromResource(this, R.array.alert_events_non_quote_strings,
				android.R.layout.simple_spinner_item);
		eventNonQuoteAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

		subjectSpinner.setOnItemSelectedListener(this);
		eventSpinner.setOnItemSelectedListener(this);

		registerForContextMenu(alertList);
		populateSubjectSpinner();
		populateEventsSpinner();
		setTitle("Alerty");
	}

	private void populateSubjectSpinner() {
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
				R.array.alert_subjects_strings, android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		subjectSpinner.setAdapter(adapter);
	}

	private void populateEventsSpinner() {
		Subject subject = Subject.getFromLabel(this, (String) subjectSpinner.getSelectedItem());
		if (subject == Subject.KURS) {
			eventSpinner.setAdapter(eventQuoteAdapter);
		} else {
			eventSpinner.setAdapter(eventNonQuoteAdapter);
		}
	}

	public void onClick(View button) {
		Subject subject = Subject.getFromLabel(this, (String) subjectSpinner.getSelectedItem());
		Event event = Event.getFromLabel(this, (String) eventSpinner.getSelectedItem());
		String value = valueEditText.getText().toString();
		Boolean percent = percentCheckBox.isChecked();

		if (button.getId() == R.id.addAlert) {
			if (value.length() == 0) {
				return;
			}

			AlertBuilder builder = new AlertBuilder();
			builder.setQuote(quote).setSubject(subject).setEvent(event).setValue(new BigDecimal(value))
					.setPercent(percent);
			if (event.isBaseValueRequired()) {
				builder.setBaseValue(subject.getValue(quote));
			}
			if (this.alertId != 0) {
				builder.setId(alertId).setUsed(false);
				Alert newAlert = builder.build();
				alertsDb.update(newAlert);
				finish();
				return;
			} else {
				Alert newAlert = builder.build();
				if (alertsDb.create(newAlert)) {
					subjectSpinner.setSelection(0);
					eventSpinner.setSelection(0);
					valueEditText.setText("");
					percentCheckBox.setChecked(false);
				} else {
					showMessage("Nie udało się dodać alertu");
				}
			}
			refreshList();
		}
	}

	public void refreshList() {
		alerts = alertsDb.getByQuote(quote);
		alertList.setAdapter(new AlertsAdapter(this, alerts));
		View label = findViewById(R.id.alertListLabel);
		if (alerts.isEmpty()) {
			label.setVisibility(View.INVISIBLE);
		} else {
			label.setVisibility(View.VISIBLE);
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		menu.add(0, ContextMenuItem.EDIT, 0, "Edytuj");
		menu.add(0, ContextMenuItem.DELETE, 0, "Usuń");
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item
				.getMenuInfo();
		final Alert selectedAlert = alerts.get(info.position);
		final int id = item.getItemId();

		if (id == ContextMenuItem.EDIT) {
			Intent intent = new Intent(this, getClass());
			intent.putExtra("alertId", selectedAlert.getId());
			startActivityForResult(intent, EDIT_RESULT);
		} else if (id == ContextMenuItem.DELETE) {
			alertsDb.delete(selectedAlert.getId());
			refreshList();
		}
		return true;
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == EDIT_RESULT) {
			refreshList();
		}
	}

	private static final class ContextMenuItem {
		static final int DELETE = 0;

		static final int EDIT = 1;

		private ContextMenuItem() {
		}
	}

	public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
		if (parent.getId() == R.id.alertSubject) {
			populateEventsSpinner();

			if (alertId != 0 && !eventInitialized) {
				int eventPos;
				if (alert.getSubject() == Subject.KURS) {
					eventPos = eventQuoteAdapter.getPosition(alert.getEvent().getLabel(this));
				} else {
					eventPos = eventNonQuoteAdapter.getPosition(alert.getEvent().getLabel(this));
				}
				eventSpinner.setSelection(eventPos);
				eventInitialized = true;
			}
		}
		setCheckboxEnabled();
	}

	private void setCheckboxEnabled() {
		Event event = Event.getFromLabel(this, (String) eventSpinner.getSelectedItem());
		if (event == Event.SPA_O || event == Event.WZR_O) {
			percentCheckBox.setEnabled(true);
		} else {
			percentCheckBox.setEnabled(false);
		}
	}

	@Override
	public void onNothingSelected(AdapterView<?> view) {
		// do nothing
	}

	private void populateQuoteItem() {
		List<Quote> quotes = new ArrayList<Quote>();
		quotes.add(quote);
		QuotesAdapter adapter = new QuotesAdapter(this, quotes);
		ViewGroup layout = (ViewGroup) findViewById(R.id.alertQuoteItem);
		layout.addView(adapter.getView(0, null, null), 0);
	}

	@Override
	protected void initUi(SQLiteDatabase sqlDb, HistoryService historyService) {
		this.quotesDb = new QuotesDao(sqlDb, this);
		this.alertsDb = new AlertsDao(sqlDb, this);
		if (alertId != 0) {
			setAlertData(alertId);
		} else {
			setSymbolData(symbol);
		}
		populateQuoteItem();
		if (alertId == 0) {
			refreshList();
		} else {
			findViewById(R.id.alertListLabel).setVisibility(View.GONE);
		}
	}

	private void setSymbolData(String symbol) {
		this.quote = quotesDb.getQuoteBySymbol(symbol);
		TextView txt = (TextView) findViewById(R.id.alertQuoteName);
		txt.setText(quote.get(QuoteField.NAME));
	}

	private void setAlertData(int alertId) {
		alert = alertsDb.getById(alertId);
		this.quote = alert.getQuote();

		TextView txt = (TextView) findViewById(R.id.alertQuoteName);
		txt.setText(quote.get(QuoteField.NAME));

		eventInitialized = false;
		subjectSpinner.setSelection(alert.getSubject().ordinal());
		valueEditText.setText(alert.getAlertValue().getValue().toString());
		percentCheckBox.setChecked(alert.getAlertValue().isPercent());
		setCheckboxEnabled();
	}
}
