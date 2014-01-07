package pl.net.newton.Makler.common;

import pl.net.newton.Makler.epromak.EpromakGpwImpl;
import pl.net.newton.Makler.gpw.QuotesReceiver;
import pl.net.newton.Makler.gpw.DefaultQuotesReceiver;
import pl.net.newton.Makler.mbank.MbankGpwImpl;
import android.content.Context;

public enum DataSource {
	makler("", false, false) {
		@Override
		protected void init(Context ctx) {
			recv = new DefaultQuotesReceiver(ctx);
		}
	},
	pkobp("www.supermakler.pkobp.pl", true, true) {
		@Override
		protected void init(Context ctx) {
			recv = new EpromakGpwImpl();
		}
	},
	mbank("www.mbank.com.pl", false, true) {
		@Override
		protected void init(Context ctx) {
			recv = new MbankGpwImpl();
		}
	},
	millennium("epromak.millenniumdm.pl", true, true) {
		@Override
		protected void init(Context ctx) {
			recv = new EpromakGpwImpl();
		}
	},
	bre("www.brebrokers.pl", true, true) {
		@Override
		protected void init(Context ctx) {
			recv = new EpromakGpwImpl();
		}
	},
	bdm("e.bdm.com.pl", true, true) {
		@Override
		protected void init(Context ctx) {
			recv = new EpromakGpwImpl();
		}
	},
	dmbh("online.dmbh.pl", true, true) {
		@Override
		protected void init(Context ctx) {
			recv = new EpromakGpwImpl();
		}
	};

	protected QuotesReceiver recv;

	private boolean initialized = false;

	private boolean isEpromak;

	private boolean supportTrades = false;

	private String hostname;

	protected abstract void init(Context ctx);

	private DataSource(String hostname, boolean isEpromak, boolean supportTrades) {
		this.hostname = hostname;
		this.isEpromak = isEpromak;
		this.supportTrades = supportTrades;
	}

	public QuotesReceiver getQuotesImpl(Context ctx) {
		if (!initialized) {
			init(ctx);
			initialized = true;
		}
		return recv;
	}

	public String getHostname() {
		return hostname;
	}

	public boolean isEpromak() {
		return isEpromak;
	}

	public boolean isSupportTrades() {
		return supportTrades;
	}
}