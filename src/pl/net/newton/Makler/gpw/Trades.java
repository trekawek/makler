package pl.net.newton.Makler.gpw;

import java.util.List;

import pl.net.newton.Makler.gpw.ex.GpwException;
import pl.net.newton.Makler.gpw.ex.InvalidPasswordException;
import pl.net.newton.Makler.gpw.model.Finances;
import pl.net.newton.Makler.gpw.model.Order;
import pl.net.newton.Makler.gpw.model.OrderState;

public interface Trades {
	public String trade(Order o) throws InvalidPasswordException, GpwException;

	public void cancel(String id) throws InvalidPasswordException, GpwException;

	public void changeOrder(String id, Order o) throws InvalidPasswordException, GpwException;

	public List<OrderState> getOrderStates() throws InvalidPasswordException, GpwException;

	public OrderState getOrderState(String id) throws InvalidPasswordException, GpwException;

	public Finances getFinances() throws InvalidPasswordException, GpwException;

	public boolean disablePassword() throws InvalidPasswordException, GpwException;

	public boolean disablePassword(String code) throws InvalidPasswordException, GpwException;
}
