package pl.net.newton.Makler.gpw.service;

import pl.net.newton.Makler.gpw.ex.GpwException;

public interface QuotesService {
	void updateQuotes() throws GpwException;

	void register(QuotesListener listener);

	void unregister(QuotesListener listnere);

	void setUpdates(boolean enabled);

	void setForeground(boolean enabled);
}
