package pl.net.newton.Makler.ui;

import pl.net.newton.Makler.R;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.PreferenceActivity;
import android.text.method.DigitsKeyListener;
import android.widget.EditText;

public class Preferences extends PreferenceActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		setTitle("Ustawienia");

		Intent resultIntent = new Intent();
		resultIntent.putExtra("preferencesChanged", true);
		setResult(Activity.RESULT_OK, resultIntent);

		EditText commision = ((EditTextPreference) findPreference("commision")).getEditText();
		EditText minCommision = ((EditTextPreference) findPreference("minCommision")).getEditText();

		commision.setKeyListener(DigitsKeyListener.getInstance(false, true));
		minCommision.setKeyListener(DigitsKeyListener.getInstance(false, true));
	}
}
