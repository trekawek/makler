package pl.net.newton.Makler.gpw;

import java.util.List;
import pl.net.newton.Makler.gpw.ex.GpwException;
import pl.net.newton.Makler.gpw.model.Finances;
import pl.net.newton.Makler.gpw.model.Order;
import pl.net.newton.Makler.gpw.model.OrderState;

public interface Trades {
	String trade(Order o) throws GpwException;

	void cancel(String id) throws GpwException;

	void changeOrder(String id, Order o) throws GpwException;

	List<OrderState> getOrderStates() throws GpwException;

	OrderState getOrderState(String id) throws GpwException;

	Finances getFinances() throws GpwException;

	boolean disablePassword() throws GpwException;

	boolean disablePassword(String code) throws GpwException;
}
