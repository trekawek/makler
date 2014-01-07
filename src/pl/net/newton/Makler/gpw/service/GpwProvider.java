package pl.net.newton.Makler.gpw.service;

import pl.net.newton.Makler.gpw.QuotesReceiver;
import pl.net.newton.Makler.gpw.ex.GpwException;
import pl.net.newton.Makler.gpw.ex.InvalidPasswordException;

public interface GpwProvider {
	public QuotesReceiver getQuotesImpl() throws GpwException, InvalidPasswordException;

	public void restart() throws GpwException, InvalidPasswordException;
}
