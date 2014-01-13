package pl.net.newton.Makler.ui;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import pl.net.newton.Makler.common.Configuration;
import pl.net.newton.Makler.db.service.SqlProvider;
import pl.net.newton.Makler.gpw.DefaultQuotesReceiver;
import pl.net.newton.Makler.gpw.QuotesReceiver;
import pl.net.newton.Makler.gpw.Trades;
import pl.net.newton.Makler.gpw.ex.GpwException;
import pl.net.newton.Makler.gpw.ex.InvalidPasswordException;
import pl.net.newton.Makler.gpw.service.GpwProvider;
import pl.net.newton.Makler.gpw.service.QuotesListener;
import pl.net.newton.Makler.gpw.service.QuotesService;
import pl.net.newton.Makler.history.service.HistoryListener;
import pl.net.newton.Makler.history.service.HistoryService;
import pl.net.newton.Makler.service.ServiceManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.WindowManager.BadTokenException;

public abstract class AbstractActivity extends Activity {
	private static final String TAG = "Makler";

	protected Handler mHandler;

	protected Configuration config;

	protected boolean adsEnabled = ADS_ENABLED;

	protected GpwProvider gpwProviderService;

	protected QuotesService quotesService;

	private HistoryService historyService;

	protected ExecutorService executor = Executors.newCachedThreadPool();

	private static final boolean ADS_ENABLED = true;

	private ProgressDialog progressWindow;

	private AlertDialog excDialog;

	protected ServiceManager serviceManager;

	private SQLiteDatabase sqlDb;

	private AtomicBoolean uiInitialized = new AtomicBoolean(false);

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, this.getClass().getName() + " - onCreate");
		
		serviceManager = new ServiceManager(this);
		
		bindService(serviceManager.getGpwProviderIntent(), gpwConnection, BIND_AUTO_CREATE);
		bindService(serviceManager.getQuotesServiceIntent(), quotesConnection, BIND_AUTO_CREATE);
		bindService(serviceManager.getSqlProviderIntent(), sqlConnection, BIND_AUTO_CREATE);
		bindService(serviceManager.getHistoryServiceIntent(), historyConnection, BIND_AUTO_CREATE);

		config = new Configuration(this);
		adsEnabled = !config.isUserRegistered();

		mHandler = new Handler();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.d(TAG, this.getClass().getName() + " - onDestroy");
		if (historyService != null && this instanceof HistoryListener) {
			historyService.unregister((HistoryListener) this);
		}
		unbindService(gpwConnection);
		unbindService(quotesConnection);
		unbindService(sqlConnection);
		unbindService(historyConnection);

		if (excDialog != null && excDialog.isShowing()) {
			excDialog.cancel();
		}
		if (progressWindow != null && progressWindow.isShowing()) {
			progressWindow.cancel();
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		Log.d(TAG, this.getClass().getName() + " - onPause");
		if(quotesService != null) {
			quotesService.setUpdates(true);
			quotesService.setForeground(false);
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		Log.d(TAG, this.getClass().getName() + " - onResume");
		if(quotesService != null) {
			quotesService.setUpdates(updatesEnabled());
			quotesService.setForeground(true);
		}
	}
	
	@Override
	public void onStart() {
		super.onStart();
		Log.d(TAG, this.getClass().getName() + " - onStart");
	}
	
	@Override
	public void onStop() {
		super.onStop();
		Log.d(TAG, this.getClass().getName() + " - onStop");
	}


	public void showProgressWindow() {
		mHandler.post(new Runnable() {
			public void run() {
				if (progressWindow != null && progressWindow.isShowing())
					return;
				Log.d(TAG, "pokazywanie okna - prosimy czekać");
				try {
					progressWindow = ProgressDialog.show(AbstractActivity.this, "Proszę czekać.",
							"Trwa komunikacja z serwerem.", true);
				} catch (BadTokenException e) {
					Log.e(TAG, "błąd przy wyświetlaniu ProgressDialog", e);
				}
			}
		});
	}

	public void hideProgressWindow() {
		mHandler.post(new Runnable() {
			public void run() {
				if (progressWindow != null && progressWindow.isShowing()) {
					Log.d(TAG, "ukrywanie okna - prosimy czekać");
					progressWindow.cancel();
					progressWindow = null;
				}
			}
		});
	}

	public void showMessage(final String msg) {
		mHandler.post(new Runnable() {
			public void run() {
				if (excDialog != null && excDialog.isShowing()) {
					Log.d(TAG, "Okienko jest już pokazywane");
					return;
				}
				try {
					AlertDialog.Builder builder = new AlertDialog.Builder(AbstractActivity.this);
					excDialog = builder.setMessage(msg).setCancelable(true).setTitle("Wystąpił błąd").show();
				} catch (Exception e) {
					Log.d(TAG, "Wyjątek przy pokazywaniu okienka", e);
				}
			}
		});
	}

	protected void checkIfRegistered() {
		new Thread(new Runnable() {
			public void run() {
				DefaultQuotesReceiver newton = new DefaultQuotesReceiver(AbstractActivity.this);
				if (newton.isRegistered()) {
					config.registerUser();
				}
			}
		}).start();
	}

	private ServiceConnection gpwConnection = new ServiceConnection() {
		public void onServiceDisconnected(ComponentName name) {
			gpwProviderService = null;
		}

		public void onServiceConnected(ComponentName name, IBinder service) {
			gpwProviderService = (GpwProvider) service;
			servicesInitialized();
		}
	};

	private ServiceConnection quotesConnection = new ServiceConnection() {
		public void onServiceDisconnected(ComponentName name) {
			quotesService = null;
		}

		public void onServiceConnected(ComponentName name, IBinder service) {
			quotesService = (QuotesService) service;
			if (AbstractActivity.this instanceof QuotesListener) {
				QuotesListener listener = (QuotesListener) AbstractActivity.this;
				quotesService.register(listener);
			}
			quotesService.setUpdates(updatesEnabled());
			quotesService.setForeground(true);
			servicesInitialized();
		}
	};

	protected boolean updatesEnabled() {
		return true;
	}

	private ServiceConnection sqlConnection = new ServiceConnection() {
		public void onServiceDisconnected(ComponentName name) {
			sqlDb = null;
		}

		public void onServiceConnected(ComponentName name, IBinder service) {
			SqlProvider sqlProvider = (SqlProvider) service;
			sqlDb = sqlProvider.getSql();
			servicesInitialized();
		}
	};

	private ServiceConnection historyConnection = new ServiceConnection() {
		public void onServiceDisconnected(ComponentName name) {
			historyService = null;
		}

		public void onServiceConnected(ComponentName name, IBinder service) {
			historyService = (HistoryService) service;
			if (AbstractActivity.this instanceof HistoryListener) {
				HistoryListener listener = (HistoryListener) AbstractActivity.this;
				historyService.register(listener);
			}
			servicesInitialized();
		}
	};

	synchronized private void servicesInitialized() {
		if (sqlDb != null && gpwProviderService != null && historyService != null
				&& !uiInitialized.getAndSet(true)) {
			mHandler.post(new Runnable() {
				public void run() {
					initUi(gpwProviderService, sqlDb, historyService);
				}
			});
		}
	}

	protected void handleException(Exception e) {
		String message = "Błąd: " + e.toString();
		if (e instanceof GpwException) {
			if (e.getMessage() != null) {
				message = e.getMessage();
			} else if (e.getCause() != null) {
				message = String.format("Wystąpił błąd z powodu " + e.getCause().toString());
			}
		} else if (e instanceof InvalidPasswordException) {
			message = "Nieprawidłowy login lub hasło";
		}
		showMessage(message);
		Log.e(TAG, "exception", e);
	}

	protected void perform(final ProcessPerformer performer, final boolean setGpwProvider) {
		executor.execute(new Runnable() {
			public void run() {
				boolean success = false;
				boolean result = false;
				showProgressWindow();
				try {
					QuotesReceiver receiver = null;
					Trades trades = null;
					if (setGpwProvider) {
						receiver = gpwProviderService.getQuotesImpl();
						if (receiver.supportTrades()) {
							trades = receiver.getTrades();
						}
					}
					result = performer.perform(receiver, trades);
					success = true;
				} catch (Exception e) {
					handleException(e);
				}
				hideProgressWindow();

				final boolean finalResult = result;
				if (success) {
					mHandler.post(new Runnable() {
						public void run() {
							performer.showResults(finalResult);
						}
					});
				}
			}
		});
	}

	protected static interface ProcessPerformer {
		public boolean perform(QuotesReceiver quotesReceiver, Trades trades) throws GpwException,
				InvalidPasswordException;

		public void showResults(boolean result);
	}

	public void onServiceDisconnected(ComponentName name) {
	}

	protected abstract void initUi(GpwProvider gpwProvider, SQLiteDatabase sqlDb,
			HistoryService historyService);
}
