package pl.net.newton.Makler.common;

import java.math.BigDecimal;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

public final class Configuration {
	private static final String FREQ_BACKGROUND = "frequencyBackground";

	private static final String FREQ_FOREGROUND = "frequencyForeground";

	private static final String AUTOSTART = "autoStartup";

	private static final String ALERT_RINGTONE_RISE = "alertRingtoneRise";

	private static final String ALERT_RINGTONE_FALL = "alertRingtoneFall";

	private static final String COMMISION = "commision";

	private static final String MIN_COMMISION = "minCommision";

	private static final String LAST_SYMBOLS_UPDATE = "lastSymbolsUpdated";

	private static final String WALLET_ACCOUNT = "walletAccount";

	private SharedPreferences pref;

	private Editor edit;

	public Configuration(Context ctx) {
		pref = PreferenceManager.getDefaultSharedPreferences(ctx);
		edit = pref.edit();
	}

	public int getFreqBackground() {
		return Integer.parseInt(pref.getString(FREQ_BACKGROUND, "300"));
	}

	public int getFreqForeground() {
		return Integer.parseInt(pref.getString(FREQ_FOREGROUND, "60"));
	}

	public boolean getAutostart() {
		return pref.getBoolean(AUTOSTART, false);
	}

	public String getAlertRingtoneRise() {
		return pref.getString(ALERT_RINGTONE_RISE, "");
	}

	public String getAlertRingtoneFall() {
		return pref.getString(ALERT_RINGTONE_FALL, "");
	}

	public BigDecimal getCommision() {
		try {
			return new BigDecimal(pref.getString(COMMISION, "0"));
		} catch (NumberFormatException e) {
			return BigDecimal.ZERO;
		}
	}

	public BigDecimal getMinCommision() {
		try {
			return new BigDecimal(pref.getString(MIN_COMMISION, "0"));
		} catch (NumberFormatException e) {
			return BigDecimal.ZERO;
		}
	}

	public String getLastSymbolsUpdated() {
		return pref.getString(LAST_SYMBOLS_UPDATE, "");
	}

	public void setLastSymbolsUpdated(String date) {
		edit.putString(LAST_SYMBOLS_UPDATE, date);
		edit.commit();
	}

	public void setWalletAccount(String account) {
		edit.putString(WALLET_ACCOUNT, account);
		edit.commit();
	}

	public BigDecimal getWalletAccount() {
		try {
			return new BigDecimal(pref.getString(WALLET_ACCOUNT, "0"));
		} catch (NumberFormatException e) {
			return BigDecimal.ZERO;
		}
	}

}
