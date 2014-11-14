package pl.net.newton.Makler.gpw.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import pl.net.newton.Makler.db.alert.AlertChecker;
import pl.net.newton.Makler.db.alert.AlertsDao;
import pl.net.newton.Makler.db.quote.Quote;
import pl.net.newton.Makler.db.quote.QuoteField;
import pl.net.newton.Makler.db.quote.QuotesDao;
import pl.net.newton.Makler.db.service.SqlProvider;
import pl.net.newton.Makler.gpw.DefaultQuotesReceiver;
import pl.net.newton.Makler.gpw.QuotesReceiver;
import pl.net.newton.Makler.gpw.ex.GpwException;
import pl.net.newton.Makler.common.Configuration;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.sqlite.SQLiteDatabase;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class QuotesService extends Service {
	public static enum StartIntent {
		DOWNLOAD_QUOTES, START_UPDATER_THREAD
	}

	static final String TAG = "MaklerQuotesServiceImpl";

	private final UpdatingThread updatingThread = new UpdatingThread(this);

	private final LocalBinder binder = new LocalBinder();

	private QuotesReceiver quotesReceiver;

	List<QuotesListener> listeners = Collections.synchronizedList(new ArrayList<QuotesListener>());

	Configuration config;

	private SQLiteDatabase sql;

	private StartIntent startIntent;

	private Executor executor = Executors.newFixedThreadPool(1);

	private QuotesDao quotesDb;

	private AlertChecker alertChecker;

	private volatile boolean updatesEnabled = true;

	volatile boolean foreground = false;

	private ServiceConnection sqlConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName name, IBinder service) {
			gotSql(((SqlProvider.LocalBinder) service).getService().getSql());
		}

		public void onServiceDisconnected(ComponentName name) {
			sql = null;
		}
	};

	@Override
	public void onCreate() {
		super.onCreate();
		Log.d(TAG, this.getClass().getName() + " - onCreate");
		config = new Configuration(this);
		quotesReceiver = new DefaultQuotesReceiver(this);
		bindService(new Intent(this, SqlProvider.class), sqlConnection, BIND_AUTO_CREATE);
	}

	private void gotSql(SQLiteDatabase sql) {
		this.sql = sql;
		this.quotesDb = new QuotesDao(sql, this);
		this.alertChecker = new AlertChecker(this, new AlertsDao(sql, this), config);
		utilizeIntent();
	}

	@Override
	public synchronized int onStartCommand(Intent intent, int flags, int startId) {
		if (intent == null) {
			return START_NOT_STICKY;
		}
		String stringIntent = intent.getStringExtra(StartIntent.class.getName());
		if (stringIntent != null) {
			startIntent = StartIntent.valueOf(stringIntent);
		} else {
			startIntent = null;
		}
		if (sql != null) {
			utilizeIntent();
		}
		return START_NOT_STICKY;
	}

	private synchronized void utilizeIntent() {
		StartIntent intent = startIntent;
		startIntent = null;
		if (intent == null) {
			return;
		}
		switch (intent) {
			case DOWNLOAD_QUOTES:
				downloadQuotesAndReturn();
				break;

			case START_UPDATER_THREAD:
				updatingThread.startThread();
				break;

			default:
				Log.e(TAG, "Invalid intent value: " + intent);
				break;
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		try {
			updatingThread.stopThread();
		} catch (Exception e) {
			Log.e(TAG, "error in destroying QuotesService", e);
		}
		unbindService(sqlConnection);
	}

	public void register(QuotesListener listener) {
		listeners.add(listener);
	}

	public void unregister(QuotesListener listener) {
		listeners.remove(listener);
	}

	public void updateQuotes() throws GpwException {
		if (!updatesEnabled) {
			return;
		}
		synchronized (quotesReceiver) {
			Log.d(TAG, "updating quotes");
			List<Quote> quotes = quotesDb.getQuotes(true);
			List<String> symbols = new ArrayList<String>();
			for (Quote q : quotes) {
				symbols.add(q.get(QuoteField.SYMBOL));
			}

			List<Quote> newQuotes = quotesReceiver.getQuotesBySymbols(symbols);
			if (newQuotes != null) {
				int qSize = quotes.size();
				int nSize = newQuotes.size();
				for (int i = 0; i < qSize && i < nSize; i++) {
					if (newQuotes.get(i) != null) {
						quotesDb.updateQuote(newQuotes.get(i));
					}
				}
				alertChecker.checkAlerts();
			}
		}
	}

	public void setUpdates(boolean enabled) {
		updatesEnabled = enabled;
	}

	public void setIsInForeground(boolean enabled) {
		foreground = enabled;
	}

	private void downloadQuotesAndReturn() {
		executor.execute(new Runnable() {
			public void run() {
				try {
					updateQuotes();
					stopSelf();
				} catch (Exception e) {
					Log.e(TAG, "Can't update quotes", e);
				}
			}
		});
	}

	public class LocalBinder extends Binder {
		public QuotesService getService() {
			return QuotesService.this;
		}
	}

}
