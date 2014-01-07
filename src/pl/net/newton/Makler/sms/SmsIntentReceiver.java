package pl.net.newton.Makler.sms;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

public class SmsIntentReceiver extends BroadcastReceiver {
	private static final String TAG = "Makler";

	private static final SimpleDateFormat SMS_DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy");

	private static final String SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";

	private static final String FROM_NUMBER = "3388";

	private static final String PDUS = "pdus";

	private static final Pattern CODE_PATTERN = Pattern.compile("haslo: (\\d+)");

	private static final String NO_PASSWORD = "eMAKLER bez uzycia hasla";

	private CodeListener listener;

	public SmsIntentReceiver(CodeListener listener) {
		this.listener = listener;
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		if (listener == null) {
			return;
		}
		if (!intent.getAction().equals(SMS_RECEIVED)) {
			return;
		}
		Bundle bundle = intent.getExtras();
		Object[] pdusObj = (Object[]) bundle.get(PDUS);
		SmsMessage[] messages = new SmsMessage[pdusObj.length];
		for (int i = 0; i < pdusObj.length; i++)
			messages[i] = SmsMessage.createFromPdu((byte[]) pdusObj[i]);
		String date = SMS_DATE_FORMAT.format(new Date());

		for (SmsMessage m : messages) {
			String from = m.getOriginatingAddress();
			String body = m.getMessageBody();
			Log.d(TAG, "from: " + from);
			Log.d(TAG, "body: " + body);
			if (from == null || !from.equals(FROM_NUMBER)) {
				continue;
			}
			if (body == null || !body.contains(NO_PASSWORD) || !body.contains(date)) {
				continue;
			}
			Matcher match = CODE_PATTERN.matcher(body);
			if (!match.find()) {
				continue;
			}
			String code = match.group(1);
			listener.setCode(code);
		}
	}
}
