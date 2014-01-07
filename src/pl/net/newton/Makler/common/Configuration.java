package pl.net.newton.Makler.common;

import java.math.BigDecimal;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

public class Configuration {
	private static final String FREQ_BACKGROUND = "frequencyBackground";

	private static final String FREQ_FOREGROUND = "frequencyForeground";

	private static final String AUTOSTART = "autoStartup";

	private static final String ALERT_RINGTONE_RISE = "alertRingtoneRise";

	private static final String ALERT_RINGTONE_FALL = "alertRingtoneFall";

	private static final String COMMISION = "commision";

	private static final String MIN_COMMISION = "minCommision";

	private static final String OWN_DATA_SOURCE = "ownDataSource";

	private static final String DATA_SOURCE_TYPE = "dataSourceType";

	private static final String DATA_SOURCE_LOGIN = "dataSourceLogin";

	private static final String DATA_SOURCE_PASSWORD = "dataSourcePassword";

	private static final String REGISTERED = "userRegistered";

	private static final String LAST_SYMBOLS_UPDATE = "lastSymbolsUpdated";

	private static final String WALLET_ACCOUNT = "walletAccount";

	public static boolean DEBUG_UPDATES = false;

	private SharedPreferences pref;

	private Editor edit;

	private boolean cachedOwnDataSource;

	private DataSource cachedType;

	private String cachedLogin, cachedPassword;

	public Configuration(Context ctx) {
		pref = PreferenceManager.getDefaultSharedPreferences(ctx);
		edit = pref.edit();
		cachedOwnDataSource = getOwnDataSource();
		cachedType = getDataSourceType();
		cachedLogin = getDataSourceLogin();
		cachedPassword = getDataSourcePassword();
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
		return new BigDecimal(pref.getString(COMMISION, "0"));
	}

	public BigDecimal getMinCommision() {
		return new BigDecimal(pref.getString(MIN_COMMISION, "0"));
	}

	public boolean dataSourceChanged() {
		boolean newOwnDataSource = getOwnDataSource();
		DataSource newType = getDataSourceType();
		String newLogin = getDataSourceLogin();
		String newPassword = getDataSourcePassword();

		boolean changed = false;
		changed = changed || (newOwnDataSource != cachedOwnDataSource);
		changed = changed || (newType != cachedType);
		changed = changed || !newLogin.equals(cachedLogin);
		changed = changed || !newPassword.equals(cachedPassword);

		cachedOwnDataSource = newOwnDataSource;
		cachedType = newType;
		cachedLogin = newLogin;
		cachedPassword = newPassword;

		return changed;
	}

	public boolean getOwnDataSource() {
		return pref.getBoolean(OWN_DATA_SOURCE, false);
	}

	public void disableOwnDataSource() {
		edit.putBoolean(OWN_DATA_SOURCE, false);
		edit.commit();
	}

	public DataSource getDataSourceType() {
		String type = pref.getString(DATA_SOURCE_TYPE, null);
		if (type == null || !getOwnDataSource()) {
			return DataSource.makler;
		} else {
			try {
				return DataSource.valueOf(type);
			} catch (IllegalArgumentException e) {
				edit.putString(DATA_SOURCE_TYPE, null);
				edit.putBoolean(OWN_DATA_SOURCE, false);
				edit.commit();
				return DataSource.makler;
			}
		}
	}

	public String getDataSourceLogin() {
		return pref.getString(DATA_SOURCE_LOGIN, "");
	}

	public String getDataSourcePassword() {
		return pref.getString(DATA_SOURCE_PASSWORD, "");
	}

	public void registerUser() {
		edit.putBoolean(REGISTERED, true);
		edit.commit();
	}

	public boolean isUserRegistered() {
		return pref.getBoolean(REGISTERED, false);
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
		return new BigDecimal(pref.getString(WALLET_ACCOUNT, "0"));
	}

}
