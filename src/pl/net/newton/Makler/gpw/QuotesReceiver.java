package pl.net.newton.Makler.gpw;

import java.util.List;
import pl.net.newton.Makler.db.quote.Quote;
import pl.net.newton.Makler.db.symbol.Symbol;
import pl.net.newton.Makler.gpw.ex.GpwException;

public interface QuotesReceiver {
	List<Symbol> getSymbols() throws GpwException;

	List<Symbol> getSymbols(String lastUpdate) throws GpwException;

	Quote getQuoteBySymbol(String symbol) throws GpwException;

	List<Quote> getQuotesBySymbols(List<String> symbols) throws GpwException;
}
