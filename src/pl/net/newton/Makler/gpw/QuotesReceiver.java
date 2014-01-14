package pl.net.newton.Makler.gpw;

import java.util.List;
import android.content.Context;
import pl.net.newton.Makler.db.quote.Quote;
import pl.net.newton.Makler.db.symbol.Symbol;
import pl.net.newton.Makler.db.symbol.SymbolsDb;
import pl.net.newton.Makler.gpw.ex.GpwException;
import pl.net.newton.Makler.gpw.ex.InvalidPasswordException;

public interface QuotesReceiver {
	List<Symbol> getSymbols() throws InvalidPasswordException, GpwException;

	List<Symbol> getSymbols(String lastUpdate) throws InvalidPasswordException, GpwException;

	Quote getQuoteBySymbol(String symbol) throws InvalidPasswordException, GpwException;

	List<Quote> getQuotesBySymbols(List<String> symbols) throws InvalidPasswordException, GpwException;

	void startSession(Context ctx, SymbolsDb symbolsDb) throws InvalidPasswordException, GpwException;

	void stopSession();

	boolean supportTrades();

	Trades getTrades();
}
