package pl.net.newton.Makler.db.service.impl;

import pl.net.newton.Makler.db.SQLConnection;
import pl.net.newton.Makler.db.service.SqlProvider;
import android.app.Service;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class SqlProviderImpl extends Service {
	private static final String TAG = "Makler";

	private final Binder binder = new LocalBinder();

	private SQLiteDatabase sql;

	@Override
	public void onCreate() {
		super.onCreate();
		Log.d(TAG, this.getClass().getName() + " - onCreate");
		sql = new SQLConnection(this).getDb();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return START_NOT_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		Log.d(TAG, this.getClass().getName() + " - onBind");
		return binder;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.d(TAG, this.getClass().getName() + " - onDestroy");
		sql.close();
	}

	private class LocalBinder extends Binder implements SqlProvider {
		public SQLiteDatabase getSql() {
			return sql;
		}
	}
}