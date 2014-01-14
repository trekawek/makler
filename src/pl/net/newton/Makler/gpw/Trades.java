package pl.net.newton.Makler.gpw;

import java.util.List;
import pl.net.newton.Makler.gpw.ex.GpwException;
import pl.net.newton.Makler.gpw.ex.InvalidPasswordException;
import pl.net.newton.Makler.gpw.model.Finances;
import pl.net.newton.Makler.gpw.model.Order;
import pl.net.newton.Makler.gpw.model.OrderState;

public interface Trades {
	String trade(Order o) throws InvalidPasswordException, GpwException;

	void cancel(String id) throws InvalidPasswordException, GpwException;

	void changeOrder(String id, Order o) throws InvalidPasswordException, GpwException;

	List<OrderState> getOrderStates() throws InvalidPasswordException, GpwException;

	OrderState getOrderState(String id) throws InvalidPasswordException, GpwException;

	Finances getFinances() throws InvalidPasswordException, GpwException;

	boolean disablePassword() throws InvalidPasswordException, GpwException;

	boolean disablePassword(String code) throws InvalidPasswordException, GpwException;
}
