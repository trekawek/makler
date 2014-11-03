package pl.net.newton.Makler.history.service;

import java.util.ArrayList;
import java.util.List;
import pl.net.newton.Makler.db.symbol.Symbol;
import pl.net.newton.Makler.history.EntryListWithIndexes;
import pl.net.newton.Makler.history.HistoryFilter;
import pl.net.newton.Makler.history.BossaProvider;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class HistoryServiceImpl extends Service {
	private static final String TAG = "Makler";

	private HistoryFilter filter;

	private Binder binder;

	@Override
	public void onCreate() {
		super.onCreate();
		Log.d(TAG, this.getClass().getName() + " - onCreate");
		initComponents();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return START_NOT_STICKY;
	}

	private void initComponents() {
		this.filter = new HistoryFilter(new BossaProvider(this));
		this.binder = new LocalBinder();
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return binder;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.d(TAG, this.getClass().getName() + " - onDestroy");
	}

	private class LocalBinder extends Binder implements HistoryService {
		List<HistoryListener> listeners = new ArrayList<HistoryListener>();

		public synchronized void register(HistoryListener listener) {
			listeners.add(listener);
		}

		public synchronized void unregister(HistoryListener listener) {
			listeners.remove(listener);
		}

		public synchronized List<HistoryListener> getListeners() {
			return new ArrayList<HistoryListener>(listeners);
		}

		public void historyByInt(final int graphRange, final Symbol symbol, final boolean force) {
			new Thread(new Runnable() {
				public void run() {
					EntryListWithIndexes entries = null;
					try {
						entries = filter.historyByInt(graphRange, symbol, force);
					} catch (NullPointerException e) {
						Log.e(TAG, "npe during history getting", e);
					}
					for (HistoryListener l : getListeners()) {
						try {
							l.gotEntries(entries);
						} catch (Exception e) {
							Log.e(TAG, "Can't inform about entries", e);
						}
					}
				}
			}).start();
		}

		public boolean isRangeExist(final Symbol symbol, int graphRange) {
			return filter.isRangeExist(symbol, graphRange);
		}
	}

}
