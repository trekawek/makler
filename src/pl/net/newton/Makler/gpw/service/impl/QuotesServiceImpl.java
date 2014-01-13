package pl.net.newton.Makler.gpw.service.impl;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import pl.net.newton.Makler.common.Configuration;
import pl.net.newton.Makler.common.GpwUtils;
import pl.net.newton.Makler.db.alert.AlertChecker;
import pl.net.newton.Makler.db.alert.AlertsDb;
import pl.net.newton.Makler.db.quote.Quote;
import pl.net.newton.Makler.db.quote.QuotesDb;
import pl.net.newton.Makler.db.service.SqlProvider;
import pl.net.newton.Makler.db.service.impl.SqlProviderImpl;
import pl.net.newton.Makler.gpw.ex.GpwException;
import pl.net.newton.Makler.gpw.ex.InvalidPasswordException;
import pl.net.newton.Makler.gpw.service.GpwProvider;
import pl.net.newton.Makler.gpw.service.QuotesListener;
import pl.net.newton.Makler.gpw.service.QuotesService;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.sqlite.SQLiteDatabase;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class QuotesServiceImpl extends Service {
	public static enum StartIntent {
		DOWNLOAD_QUOTES, START_UPDATER_THREAD
	}

	private static final String TAG = "Makler";

	private final LocalBinder binder = new LocalBinder();

	private List<QuotesListener> listeners = Collections.synchronizedList(new ArrayList<QuotesListener>());

	private Configuration config;

	private GpwProvider gpw;

	private UpdatingThread updatingThread = new UpdatingThread();

	private SQLiteDatabase sql;

	private QuotesDb quotesDb;

	private AlertChecker alertChecker;

	private StartIntent startIntent;

	private Executor executor = Executors.newFixedThreadPool(1);

	private volatile boolean updatesEnabled = true;

	private volatile boolean foreground = false;

	@Override
	public void onCreate() {
		super.onCreate();
		Log.d(TAG, this.getClass().getName() + " - onCreate");
		config = new Configuration(this);

		bindService(new Intent(this, GpwProviderImpl.class), gpwConnection, BIND_AUTO_CREATE);
		bindService(new Intent(this, SqlProviderImpl.class), sqlConnection, BIND_AUTO_CREATE);
	}

	@Override
	synchronized public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(TAG, this.getClass().getName() + " - onStart");
		if (intent != null) {
			String stringIntent = intent.getStringExtra(StartIntent.class.getName());
			if (stringIntent != null) {
				startIntent = StartIntent.valueOf(stringIntent);
			} else {
				startIntent = null;
			}

			if (sql != null && gpw != null) {
				utilizeIntent();
			}
		}
		return START_NOT_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.d(TAG, this.getClass().getName() + " - onDestroy");
		try {
			updatingThread.stopThread();
			if (gpw != null) {
				gpw.getQuotesImpl().stopSession();
			}
		} catch (Exception e) {
			Log.e(TAG, "error in destroying QuotesService", e);
		}
		unbindService(gpwConnection);
		unbindService(sqlConnection);
	}

	private class UpdatingThread implements Runnable {
		private volatile Thread runner;

		private Calendar lastUpdate;

		public void run() {
			while (Thread.currentThread() == runner) {
				lastUpdate = Calendar.getInstance();
				int freq = config.getFreqForeground();
				if (GpwUtils.gpwActive() && freq != 0 && foreground) {
					try {
						binder.updateQuotes();
						for (QuotesListener listener : listeners) {
							listener.quotesUpdated();
						}
					} catch (Exception e) {
						Log.e(TAG, "can't update quotes", e);
					}
				}
				Log.d(TAG, "waiting for " + freq + "s");
				try {
					Thread.sleep(1000 * freq);
				} catch (InterruptedException e) {
					return;
				}
			}
		}

		public synchronized void startThread() {
			if (!isAlive()) {
				runner = new Thread(this);
				runner.start();
			}
		}

		public synchronized void stopThread() {
			if (runner != null) {
				Thread moribund = runner;
				runner = null;
				moribund.interrupt();
			}
		}

		private boolean isAlive() {
			if (runner == null) {
				return false;
			}
			if (!runner.isAlive()) {
				return false;
			}
			Calendar now = Calendar.getInstance();
			now.add(Calendar.SECOND, -(config.getFreqForeground() + 30));
			return lastUpdate.after(now);
		}
	}

	private class LocalBinder extends Binder implements QuotesService {
		public void register(QuotesListener listener) {
			listeners.add(listener);
		}

		public void unregister(QuotesListener listener) {
			listeners.remove(listener);
		}

		public void updateQuotes() throws InvalidPasswordException, GpwException {
			if (gpw == null || !updatesEnabled) {
				return;
			}
			synchronized (gpw) {
				Log.d(TAG, "updating quotes");
				List<Quote> quotes = quotesDb.getQuotes(true);
				List<String> symbols = new ArrayList<String>();
				for (Quote q : quotes) {
					symbols.add(q.getSymbol());
				}

				List<Quote> newQuotes = gpw.getQuotesImpl().getQuotesBySymbols(symbols);
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

		public void setForeground(boolean enabled) {
			foreground = enabled;
		}
	}

	private ServiceConnection sqlConnection = new ServiceConnection() {
		public void onServiceDisconnected(ComponentName name) {
			sql = null;
		}

		public void onServiceConnected(ComponentName name, IBinder service) {
			synchronized (QuotesServiceImpl.this) {
				if (service instanceof SqlProvider) {
					sql = ((SqlProvider) service).getSql();
					quotesDb = new QuotesDb(sql, QuotesServiceImpl.this);
					alertChecker = new AlertChecker(QuotesServiceImpl.this, new AlertsDb(sql,
							QuotesServiceImpl.this), config);
				}
				servicesInitialized();
			}
		}
	};

	private ServiceConnection gpwConnection = new ServiceConnection() {
		public void onServiceDisconnected(ComponentName name) {
			gpw = null;
		}

		public void onServiceConnected(ComponentName name, IBinder service) {
			synchronized (QuotesServiceImpl.this) {
				if (service instanceof GpwProvider) {
					gpw = (GpwProvider) service;
				}
				servicesInitialized();
			}
		}
	};

	private void servicesInitialized() {
		if (sql != null && gpw != null) {
			utilizeIntent();
		}
	}

	private void downloadQuotesAndReturn() {
		executor.execute(new Runnable() {
			public void run() {
				try {
					QuotesServiceImpl.this.binder.updateQuotes();
					stopSelf();
				} catch (Exception e) {
					Log.e(TAG, "Can't update quotes", e);
				}
			}
		});
	}

	synchronized private void utilizeIntent() {
		StartIntent intent = startIntent;
		startIntent = null;
		if (intent != null) {
			switch (intent) {
				case DOWNLOAD_QUOTES:
					downloadQuotesAndReturn();
					break;

				case START_UPDATER_THREAD:
					updatingThread.startThread();
					break;
			}
		}
	}
}
