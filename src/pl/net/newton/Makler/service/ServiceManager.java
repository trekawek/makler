package pl.net.newton.Makler.service;

import pl.net.newton.Makler.db.service.impl.SqlProviderImpl;
import pl.net.newton.Makler.gpw.service.impl.GpwProviderImpl;
import pl.net.newton.Makler.gpw.service.impl.QuotesServiceImpl;
import pl.net.newton.Makler.history.service.HistoryServiceImpl;
import android.content.Context;
import android.content.Intent;

public class ServiceManager {
	private Intent gpwProviderIntent;

	private Intent quotesServiceIntent;

	private Intent sqlProviderIntent;

	private Intent historyServiceIntent;
	
	private Context context;
	
	public ServiceManager(Context context) {
		gpwProviderIntent = new Intent(context, GpwProviderImpl.class);
		quotesServiceIntent = new Intent(context, QuotesServiceImpl.class);
		quotesServiceIntent.putExtra(QuotesServiceImpl.StartIntent.class.getName(), QuotesServiceImpl.StartIntent.START_UPDATER_THREAD.name());
		sqlProviderIntent = new Intent(context, SqlProviderImpl.class);
		historyServiceIntent = new Intent(context, HistoryServiceImpl.class);
		this.context = context;
	}
	
	public void startServices() {
		context.startService(gpwProviderIntent);
		context.startService(quotesServiceIntent);
		context.startService(sqlProviderIntent);
	}

	public void stopServices() {
		context.stopService(gpwProviderIntent);
		context.stopService(quotesServiceIntent);
		context.stopService(sqlProviderIntent);
	}

	public Intent getGpwProviderIntent() {
		return gpwProviderIntent;
	}

	public Intent getQuotesServiceIntent() {
		return quotesServiceIntent;
	}

	public Intent getSqlProviderIntent() {
		return sqlProviderIntent;
	}

	public Intent getHistoryServiceIntent() {
		return historyServiceIntent;
	}
}
