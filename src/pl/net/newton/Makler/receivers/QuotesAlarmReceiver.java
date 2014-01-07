package pl.net.newton.Makler.receivers;

import pl.net.newton.Makler.common.Configuration;
import pl.net.newton.Makler.common.GpwUtils;
import pl.net.newton.Makler.gpw.service.impl.QuotesServiceImpl;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class QuotesAlarmReceiver extends BroadcastReceiver {
	private static final String TAG = "MaklerAlarm";

	public static final int REQUEST_CODE = 123;

	@Override
	public void onReceive(final Context ctx, Intent intent) {
		if (!GpwUtils.gpwActive()) {
			Log.d(TAG, "gpw is not active");
			return;
		}
		Log.d(TAG, "starting service");
		Intent quotesIntent = new Intent(ctx, QuotesServiceImpl.class);
		quotesIntent.putExtra(QuotesServiceImpl.StartIntent.class.getName(), QuotesServiceImpl.StartIntent.DOWNLOAD_QUOTES.name());
		ctx.startService(quotesIntent);
	}

	public static void setAlarm(Context context) {
		Configuration config = new Configuration(context);
		Integer freq = config.getFreqBackground();
		if (freq == 0) {
			return;
		}

		Log.d(TAG, "alarm frequency: " + freq.toString());
		Intent intent = new Intent(context, QuotesAlarmReceiver.class);
		PendingIntent alarmIntent = PendingIntent.getBroadcast(context, QuotesAlarmReceiver.REQUEST_CODE,
				intent, 0);
		AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + (freq * 1000),
				freq * 1000, alarmIntent);
	}
	
	public static void cancelAlarm(Context context) {
		Log.d(TAG, "cancelling alarms");
		Intent intent = new Intent(context, QuotesAlarmReceiver.class);
		PendingIntent alarmIntent = PendingIntent.getBroadcast(context, QuotesAlarmReceiver.REQUEST_CODE,
				intent, 0);
		AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		alarmManager.cancel(alarmIntent);
	}
}
