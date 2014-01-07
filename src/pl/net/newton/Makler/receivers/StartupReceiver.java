package pl.net.newton.Makler.receivers;

import pl.net.newton.Makler.common.Configuration;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class StartupReceiver extends BroadcastReceiver {
	private static final String TAG = "Makler";

	@Override
	public void onReceive(Context context, Intent intent) {
		if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
			Configuration config = new Configuration(context);
			if (config.getAutostart()) {
				Log.d(TAG, "on boot");
				QuotesAlarmReceiver.setAlarm(context);
			}
		}
	}

}
