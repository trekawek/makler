package pl.net.newton.Makler.ui;

import pl.net.newton.Makler.R;
import pl.net.newton.Makler.gpw.service.GpwProvider;
import pl.net.newton.Makler.history.service.HistoryService;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;

public class About extends AbstractActivity implements OnClickListener {
	private ImageView taxity;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Window w = getWindow();
		w.requestFeature(Window.FEATURE_LEFT_ICON);

		setContentView(R.layout.about);

		w.setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, android.R.drawable.ic_dialog_info);

		TextView text = (TextView) this.findViewById(R.id.app_version);
		taxity = (ImageView) this.findViewById(R.id.taxity);
		ComponentName comp = new ComponentName(this, Quotes.class);
		try {
			text.setText(getPackageManager().getPackageInfo(comp.getPackageName(), 0).versionName);
		} catch (NameNotFoundException e) {
			text.setText("1.0");
			e.printStackTrace();
		}

		taxity.setOnClickListener(this);

	}

	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.taxity:
				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setData(Uri.parse("market://details?id=com.taxity"));
				startActivity(intent);
				break;
		}
	}

	@Override
	protected void initUi(GpwProvider gpwProvider, SQLiteDatabase sqlDb, HistoryService historyService) {
		// do nothing
	}
}
