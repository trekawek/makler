package pl.net.newton.Makler.gpw.service;

import pl.net.newton.Makler.gpw.ex.GpwException;
import pl.net.newton.Makler.gpw.ex.InvalidPasswordException;

public interface QuotesService {
	void updateQuotes() throws InvalidPasswordException, GpwException;

	void register(QuotesListener listener);

	void unregister(QuotesListener listnere);
	
	void setUpdates(boolean enabled);
	
	void setForeground(boolean enabled);
}
