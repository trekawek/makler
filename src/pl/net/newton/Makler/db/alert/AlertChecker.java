package pl.net.newton.Makler.db.alert;

import java.util.List;

import pl.net.newton.Makler.R;
import pl.net.newton.Makler.ui.QuoteDetails;
import pl.net.newton.Makler.common.Configuration;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class AlertChecker {
	private Context ctx;

	private AlertsDb alertsDb;

	private Configuration config;

	private NotificationManager notifManager;

	public AlertChecker(Context ctx, AlertsDb alertsDb, Configuration config) {
		this.ctx = ctx;
		this.alertsDb = alertsDb;
		this.config = config;
		this.notifManager = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
	}

	public void checkAlerts() {
		List<Alert> alerts = alertsDb.getAlerts();
		for (Alert a : alerts) {
			if (!a.getUsed() && a.isAlarming()) {
				launchAlert(a);
			}
		}
	}

	@SuppressWarnings("deprecation")
	private void launchAlert(Alert a) {
		alertsDb.markAlertAsUsed(a);
		int icon;
		String ringTone;
		if (a.getEvent() == Event.WZR_DO || a.getEvent() == Event.WZR_O || a.getEvent() == Event.WZR_POW) {
			icon = R.drawable.stock_up;
			ringTone = config.getAlertRingtoneRise();
		} else {
			icon = R.drawable.stock_down;
			ringTone = config.getAlertRingtoneFall();
		}

		Intent intent = new Intent(ctx, QuoteDetails.class);
		intent.putExtra("symbol", a.getQuote().getSymbol());
		intent.putExtra("fromAlert", true);
		PendingIntent pendingIntent = PendingIntent.getActivity(ctx, 0, intent,
				PendingIntent.FLAG_CANCEL_CURRENT);

		Notification notification = new Notification(icon, "Alert", System.currentTimeMillis());
		notification.flags = Notification.FLAG_AUTO_CANCEL;
		notification.setLatestEventInfo(ctx, "Alert maklera", a.notification(ctx), pendingIntent);

		if (!"".equals(ringTone)) {
			notification.sound = Uri.parse(ringTone);
		}
		notifManager.notify(0, notification);
	}
}
