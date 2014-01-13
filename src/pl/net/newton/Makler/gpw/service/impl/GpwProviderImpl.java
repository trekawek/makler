package pl.net.newton.Makler.gpw.service.impl;

import java.util.concurrent.Executors;
import pl.net.newton.Makler.common.Configuration;
import pl.net.newton.Makler.common.DataSource;
import pl.net.newton.Makler.db.service.SqlProvider;
import pl.net.newton.Makler.db.service.impl.SqlProviderImpl;
import pl.net.newton.Makler.db.symbol.SymbolsDb;
import pl.net.newton.Makler.gpw.QuotesReceiver;
import pl.net.newton.Makler.gpw.ex.GpwException;
import pl.net.newton.Makler.gpw.ex.InvalidPasswordException;
import pl.net.newton.Makler.gpw.service.GpwProvider;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.sqlite.SQLiteDatabase;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class GpwProviderImpl extends Service implements ServiceConnection {
	private static final String TAG = "Makler";

	private final Binder binder = new LocalBinder();

	private QuotesReceiver quotesRecv;

	private SymbolsDb symbolsDb;

	private Configuration config;

	private boolean initialized;

	@Override
	public void onCreate() {
		super.onCreate();
		Log.d(TAG, this.getClass().getName() + " - onCreate");
		config = new Configuration(this);
		setImpl();
		bindService(new Intent(this, SqlProviderImpl.class), this, BIND_AUTO_CREATE);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return START_NOT_STICKY;
	}

	synchronized private void setImpl() {
		DataSource dataSource = config.getDataSourceType();
		quotesRecv = dataSource.getQuotesImpl(this);
		initialized = false;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.d(TAG, this.getClass().getName() + " - onDestroy");
		if (quotesRecv != null) {
			quotesRecv.stopSession();
			quotesRecv = null;
		}
		unbindService(this);
	}

	synchronized private void startSession() throws GpwException, InvalidPasswordException {
		if(initialized) {
			return;
		}
		try {
			if (quotesRecv != null) {
				quotesRecv.startSession(GpwProviderImpl.this, symbolsDb);
				initialized = true;
			}
		} catch (InvalidPasswordException e) {
			config.disableOwnDataSource();
			setImpl();
			throw e;
		}
	}

	private class LocalBinder extends Binder implements GpwProvider {
		public QuotesReceiver getQuotesImpl() throws GpwException, InvalidPasswordException {
			startSession();
			return quotesRecv;
		}

		public void restart() throws GpwException, InvalidPasswordException {
			if(symbolsDb != null) {
				setImpl();
				startSession();
			}
		}
	}

	public void onServiceConnected(ComponentName service, IBinder binder) {
		if(binder instanceof SqlProvider) {
			SqlProvider sqlProvider = (SqlProvider) binder;
			SQLiteDatabase sql = sqlProvider.getSql();
			symbolsDb = new SymbolsDb(sql, this);
			Executors.newSingleThreadExecutor().execute(new Runnable() {
				public void run() {
					try {
						startSession();
					} catch (Exception e) {
						Log.e(TAG, "can't start session", e);
					}
				}
			});
		}
	}

	public void onServiceDisconnected(ComponentName service) {
	}
}
