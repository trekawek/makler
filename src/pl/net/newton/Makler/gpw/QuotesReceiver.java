package pl.net.newton.Makler.gpw;

import java.util.List;

import android.content.Context;

import pl.net.newton.Makler.db.quote.Quote;
import pl.net.newton.Makler.db.symbol.Symbol;
import pl.net.newton.Makler.db.symbol.SymbolsDb;
import pl.net.newton.Makler.gpw.ex.GpwException;
import pl.net.newton.Makler.gpw.ex.InvalidPasswordException;

public interface QuotesReceiver {
	public List<Symbol> getSymbols() throws InvalidPasswordException, GpwException;

	public List<Symbol> getSymbols(String lastUpdate) throws InvalidPasswordException, GpwException;

	public Quote getQuoteBySymbol(String symbol) throws InvalidPasswordException, GpwException;

	public List<Quote> getQuotesBySymbols(List<String> symbols) throws InvalidPasswordException, GpwException;

	public void startSession(Context ctx, SymbolsDb symbolsDb) throws InvalidPasswordException, GpwException;

	public void stopSession();

	public boolean supportTrades();

	public Trades getTrades();
}
