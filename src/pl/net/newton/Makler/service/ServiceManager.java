package pl.net.newton.Makler.service;

import pl.net.newton.Makler.db.service.SqlProvider;
import pl.net.newton.Makler.gpw.service.QuotesService;
import pl.net.newton.Makler.history.service.HistoryService;
import android.content.Context;
import android.content.Intent;

public class ServiceManager {
	private Intent quotesServiceIntent;

	private Intent sqlProviderIntent;

	private Intent historyServiceIntent;

	private Context context;

	public ServiceManager(Context context) {
		quotesServiceIntent = new Intent(context, QuotesService.class);
		quotesServiceIntent.putExtra(QuotesService.StartIntent.class.getName(),
				QuotesService.StartIntent.START_UPDATER_THREAD.name());
		sqlProviderIntent = new Intent(context, SqlProvider.class);
		historyServiceIntent = new Intent(context, HistoryService.class);
		this.context = context;
	}

	public void startServices() {
		context.startService(quotesServiceIntent);
		context.startService(sqlProviderIntent);
	}

	public void stopServices() {
		context.stopService(quotesServiceIntent);
		context.stopService(sqlProviderIntent);
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
