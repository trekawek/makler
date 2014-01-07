package pl.net.newton.Makler.epromak;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import android.content.Context;
import android.util.Log;
import pl.net.newton.Makler.common.Configuration;
import pl.net.newton.Makler.db.quote.Quote;
import pl.net.newton.Makler.db.symbol.Symbol;
import pl.net.newton.Makler.db.symbol.SymbolBuilder;
import pl.net.newton.Makler.db.symbol.SymbolsDb;
import pl.net.newton.Makler.gpw.QuotesReceiver;
import pl.net.newton.Makler.gpw.Trades;
import pl.net.newton.Makler.gpw.ex.GpwException;
import pl.net.newton.Makler.gpw.ex.InvalidPasswordException;
import pl.net.newton.Makler.gpw.model.Finances;
import pl.net.newton.Makler.gpw.model.Order;
import pl.net.newton.Makler.gpw.model.OrderState;
import pl.net.newton.Makler.gpw.model.Paper;
import pl.net.newton.Makler.epromak.model.EpromakFinance;
import pl.net.newton.Makler.epromak.model.EpromakOrder;
import pl.net.newton.Makler.epromak.model.EpromakOrderState;
import pl.net.newton.Makler.epromak.model.EpromakOwnership;
import pl.net.newton.Makler.epromak.model.EpromakPaper;
import pl.net.newton.Makler.epromak.model.EpromakQuote;

public class EpromakGpwImpl implements QuotesReceiver, Trades {
	private static final String TAG = "Makler";

	private EpromakClient client;

	private Lock lock = new ReentrantLock();

	private SymbolsDb symbolsDb;

	private Context ctx;

	private void checkClient() throws GpwException, InvalidPasswordException {
		StackTraceElement[] st = Thread.currentThread().getStackTrace();
		Log.d(TAG, "checkClient called by " + st[3].toString());
		Log.d(TAG, "checkClient called by " + st[4].toString());

		Log.d(TAG, "checkClient - start");
		lock.lock();
		/*
		 * try { lock.tryLock(30, TimeUnit.SECONDS); } catch(InterruptedException e) { lock = new
		 * ReentrantLock(); lock.lock(); }
		 */
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

			List<EpromakQuote> quotes = client.getQuotesFromSymbols(symbols);
			List<Quote> dbQuotes = new ArrayList<Quote>();
			for (EpromakQuote q : quotes)
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
			List<EpromakPaper> papers = client.getPapers(true);
			List<Symbol> dbSymbols = new ArrayList<Symbol>();
			for (EpromakPaper p : papers) {
				Symbol s = p.getDBSymbol();
				dbSymbols.add(s);
			}
			return dbSymbols;
		} finally {
			lock.unlock();
		}
	}

	public List<Symbol> getSymbols(String lastUpdate) throws GpwException, InvalidPasswordException {
		return getSymbols();
	}

	public void startSession(Context ctx, SymbolsDb symbolsDb) throws InvalidPasswordException, GpwException {
		this.ctx = ctx;
		this.symbolsDb = symbolsDb;
		Log.d(TAG, "startSession - lock enter");
		lock.lock();
		Configuration config = new Configuration(ctx);
		try {
			client = new EpromakClient(ctx, config.getDataSourceLogin(), config.getDataSourcePassword(),
					config.getDataSourceType());
			client.login();
		} finally {
			lock.unlock();
			Log.d(TAG, "startSession - unlocked");
		}
	}

	public void stopSession() {
		new Thread(new Runnable() {
			public void run() {
				Log.d(TAG, "stopSession - lock enter");
				lock.lock();
				try {
					if (client != null)
						client.logout();
				} catch (Exception e) {
					Log.e(TAG, "Can't log out", e);
				} finally {
					lock.unlock();
					Log.d(TAG, "stopSession - unlocked");
				}
			}
		}).start();
	}

	public void cancel(String id) throws GpwException, InvalidPasswordException {
		try {
			checkClient();
			EpromakOrderState s = client.getOrderStateById(id);
			if (s != null)
				client.cancelOrder(s);
		} finally {
			lock.unlock();
		}
	}

	public void changeOrder(String id, Order o) throws GpwException, InvalidPasswordException {
		try {
			checkClient();
			EpromakOrderState s = client.getOrderStateById(id);
			EpromakPaper p = client.getPaperBySymbol(o.getSymbol().getSymbol());
			if (s != null)
				client.updateOrder(s, EpromakOrder.getFromDBOrder(o, p));
		} finally {
			lock.unlock();
		}
	}

	public Finances getFinances() throws GpwException, InvalidPasswordException {
		try {
			checkClient();
			EpromakFinance f = client.getFinance();
			List<EpromakOwnership> oList = client.getOwnerships();
			List<EpromakPaper> p = new ArrayList<EpromakPaper>();
			for (EpromakOwnership o : oList)
				if (o.getPaper() != null)
					p.add(o.getPaper());
			List<EpromakQuote> quotes = client.getQuotes(p);
			List<Paper> papers = new ArrayList<Paper>();
			for (int i = 0, j = 0, l = oList.size(); i < l; i++) {
				EpromakOwnership o = oList.get(i);
				if (o.getPaper() != null)
					papers.add(new Paper(symbolsDb.getSymbolBySymbol(o.getPaper()
							.getSymbol()), new Integer(o.get("stanPWPodst")),
							new Integer(o.get("prawaWlas")), new BigDecimal(quotes.get(j++).getKurs())));
				else {
					Symbol s = symbolsDb.getSymbolBySymbol(o.getSymbol());
					if (s == null)
						s = new SymbolBuilder().setSymbol(o.getSymbol()).setName(o.getName()).build();
					papers.add(new Paper(s, new Integer(o.get("stanPWPodst")),
							new Integer(o.get("prawaWlas")), BigDecimal.ZERO));
				}
			}

			return new Finances(papers, new BigDecimal(f.get("standozlc")), new BigDecimal(f.get("srwolne")),
					new BigDecimal(f.get("naleznosci")));
		} finally {
			lock.unlock();
		}
	}

	public OrderState getOrderState(String id) throws GpwException, InvalidPasswordException {
		try {
			checkClient();
			EpromakOrderState s = client.getOrderStateById(id);
			Order o = s.getGPWOrder(ctx, symbolsDb);
			return new OrderState(o, id, s.getStan(), new Integer(s.get("iloscrea")));
		} finally {
			lock.unlock();
		}
	}

	public List<OrderState> getOrderStates() throws GpwException, InvalidPasswordException {
		try {
			checkClient();
			List<EpromakOrderState> states = client.getOrderStates();
			List<OrderState> list = new ArrayList<OrderState>();
			for (EpromakOrderState s : states) {
				Order o = s.getGPWOrder(ctx, symbolsDb);
				Log.d("Makler - zrealizowano", s.get("iloscrea"));
				list.add(new OrderState(o, s.getId(), s.getStan(), new Integer(s.get("iloscrea"))));
			}
			return list;
		} finally {
			lock.unlock();
		}
	}

	public String trade(Order o) throws GpwException, InvalidPasswordException {
		try {
			checkClient();
			if (o.getSymbol() == null)
				throw new GpwException("Nieprawidłowy symbol waloru");
			EpromakPaper p = client.getPaperBySymbol(o.getSymbol().getSymbol());
			return client.submitOrder(pl.net.newton.Makler.epromak.model.EpromakOrder.getFromDBOrder(o, p));
		} finally {
			lock.unlock();
		}
	}

	public boolean disablePassword() {
		return true;
	}

	public boolean disablePassword(String code) {
		return true;
	}

	public boolean supportTrades() {
		return true;
	}

	public Trades getTrades() {
		return this;
	}
}