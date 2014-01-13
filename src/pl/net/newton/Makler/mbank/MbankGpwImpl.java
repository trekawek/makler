package pl.net.newton.Makler.mbank;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import android.content.Context;
import android.util.Log;
import pl.net.newton.Makler.db.quote.Quote;
import pl.net.newton.Makler.db.symbol.Symbol;
import pl.net.newton.Makler.db.symbol.SymbolsDb;
import pl.net.newton.Makler.gpw.QuotesReceiver;
import pl.net.newton.Makler.gpw.Trades;
import pl.net.newton.Makler.gpw.ex.GpwException;
import pl.net.newton.Makler.gpw.ex.InvalidPasswordException;
import pl.net.newton.Makler.gpw.model.Finances;
import pl.net.newton.Makler.gpw.model.Order;
import pl.net.newton.Makler.gpw.model.OrderState;
import pl.net.newton.Makler.mbank.model.MbankPaper;
import pl.net.newton.Makler.common.Configuration;

public class MbankGpwImpl implements QuotesReceiver, Trades {
	private static final String TAG = "Makler";

	private MbankClient client;

	private Lock lock = new ReentrantLock();

	private void checkClient() throws GpwException, InvalidPasswordException {
		Log.d(TAG, "checkClient - start");
		lock.lock();
		Log.d(TAG, "checkClient - after lock()");
		if (!client.loggedIn()) {
			Log.d(TAG, "checkClient - not logged in");
			client.login();
		}
		Log.d(TAG, "checkClient - logged in");
	}

	public Quote getQuoteBySymbol(String symbol) throws GpwException, InvalidPasswordException {
		try {
			checkClient();
			return client.getQuotes(symbol).get(0).getDBQuote();
		} finally {
			lock.unlock();
		}
	}

	public List<Quote> getQuotesBySymbols(List<String> symbols) throws GpwException, InvalidPasswordException {
		try {
			checkClient();
			List<pl.net.newton.Makler.mbank.model.MbankQuote> quotes = client.getQuotesBySymbols(symbols);
			List<Quote> dbQuotes = new ArrayList<Quote>();
			for (pl.net.newton.Makler.mbank.model.MbankQuote q : quotes)
				if (q != null)
					dbQuotes.add(q.getDBQuote());
				else
					dbQuotes.add(null);
			return dbQuotes;
		} finally {
			lock.unlock();
		}
	}

	public List<Symbol> getSymbols() throws GpwException, InvalidPasswordException {
		try {
			checkClient();
			List<MbankPaper> papers = client.getPapers(true);
			List<Symbol> dbSymbols = new ArrayList<Symbol>();
			for (MbankPaper p : papers)
				dbSymbols.add(p.getDBSymbol());
			return dbSymbols;
		} finally {
			lock.unlock();
		}
	}

	public List<Symbol> getSymbols(String lastUpdate) throws GpwException, InvalidPasswordException {
		return getSymbols();
	}

	public void startSession(Context ctx, SymbolsDb symbolsDb) throws InvalidPasswordException, GpwException {
		lock.lock();
		Configuration config = new Configuration(ctx);
		try {
			client = new MbankClient(ctx, config.getDataSourceLogin(), config.getDataSourcePassword(), symbolsDb);
			client.login();
		} finally {
			lock.unlock();
		}
	}

	public void stopSession() {
		new Thread(new Runnable() {
			public void run() {
				lock.lock();
				try {
					if (client != null)
						client.logout();
				} catch (Exception e) {
					Log.e(TAG, "can't log out", e);
				} finally {
					lock.unlock();
				}
			}
		}).start();
	}

	public void cancel(String id) throws InvalidPasswordException, GpwException {
		try {
			checkClient();
			client.cancel(id);
		} finally {
			lock.unlock();
		}
	}

	public void changeOrder(String id, Order o) throws InvalidPasswordException, GpwException {
		try {
			checkClient();
			if (o.getSymbol() == null)
				throw new GpwException("Nieprawidłowy symbol waloru");
			MbankPaper p = client.getPaperBySymbol(o.getSymbol().getSymbol());
			if (p == null)
				throw new GpwException("Nieprawidłowy symbol waloru");
			client.change(id, pl.net.newton.Makler.mbank.model.MbankOrder.getFromDBOrder(o, p));
		} finally {
			lock.unlock();
		}
	}

	public Finances getFinances() throws InvalidPasswordException, GpwException {
		try {
			checkClient();
			return client.getFinances();
		} finally {
			lock.unlock();
		}
	}

	public OrderState getOrderState(String id) throws InvalidPasswordException, GpwException {
		try {
			checkClient();
			List<OrderState> states = client.getOrderStates();
			for (OrderState s : states)
				if (s.getId().equals(id))
					return s;
		} finally {
			lock.unlock();
		}
		return null;
	}

	public List<OrderState> getOrderStates() throws InvalidPasswordException, GpwException {
		try {
			checkClient();
			return client.getOrderStates();
		} finally {
			lock.unlock();
		}
	}

	public String trade(pl.net.newton.Makler.gpw.model.Order o) throws InvalidPasswordException, GpwException {
		try {
			checkClient();
			if (o.getSymbol() == null)
				throw new GpwException("Nieprawidłowy symbol waloru");
			MbankPaper p = client.getPaperBySymbol(o.getSymbol().getSymbol());
			if (p == null)
				throw new GpwException("Nieprawidłowy symbol waloru");
			client.trade(pl.net.newton.Makler.mbank.model.MbankOrder.getFromDBOrder(o, p));
			return "";
		} finally {
			lock.unlock();
		}
	}

	public boolean disablePassword() throws InvalidPasswordException, GpwException {
		try {
			checkClient();
			return client.disablePassword();
		} finally {
			lock.unlock();
		}
	}

	public boolean disablePassword(String code) throws InvalidPasswordException, GpwException {
		try {
			// checkClient();
			lock.lock();
			return client.disablePassword(code);
		} finally {
			lock.unlock();
		}
	}

	public boolean supportTrades() {
		return true;
	}

	public Trades getTrades() {
		return this;
	}
}
