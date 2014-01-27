package pl.net.newton.Makler.gpw.service;

import pl.net.newton.Makler.gpw.QuotesReceiver;
import pl.net.newton.Makler.gpw.ex.GpwException;

public interface GpwProvider {
	QuotesReceiver getQuotesImpl() throws GpwException;

	void restart() throws GpwException;
}
