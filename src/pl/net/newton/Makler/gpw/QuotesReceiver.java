package pl.net.newton.Makler.gpw;

import java.util.List;
import android.content.Context;
import pl.net.newton.Makler.db.quote.Quote;
import pl.net.newton.Makler.db.symbol.Symbol;
import pl.net.newton.Makler.db.symbol.SymbolsDb;
import pl.net.newton.Makler.gpw.ex.GpwException;

public interface QuotesReceiver {
	List<Symbol> getSymbols() throws GpwException;

	List<Symbol> getSymbols(String lastUpdate) throws GpwException;

	Quote getQuoteBySymbol(String symbol) throws GpwException;

	List<Quote> getQuotesBySymbols(List<String> symbols) throws GpwException;

	void startSession(Context ctx, SymbolsDb symbolsDb) throws GpwException;

	void stopSession();

	boolean supportTrades();

	Trades getTrades();
}
