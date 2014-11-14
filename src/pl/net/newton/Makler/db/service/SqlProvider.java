package pl.net.newton.Makler.db.service;

import pl.net.newton.Makler.db.SqlConnection;
import android.app.Service;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Binder;
import android.os.IBinder;

public class SqlProvider extends Service {

	private final Binder binder = new LocalBinder();

	private SQLiteDatabase sql;

	@Override
	public void onCreate() {
		super.onCreate();
		sql = new SqlConnection(this).getDb();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return START_NOT_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		sql.close();
	}

	public SQLiteDatabase getSql() {
		return sql;
	}

	public class LocalBinder extends Binder {
		public SqlProvider getService() {
			return SqlProvider.this;
		}
	}
}