package pl.net.newton.Makler.gpw.model;

import java.util.EnumSet;

import android.content.Context;
import pl.net.newton.Makler.R;
import pl.net.newton.Makler.common.LocaleUtils;

public class OrderState {
	private String id;

	private Order order;

	private State state;

	private String customState = null;

	private Integer zrealizowano;

	public OrderState(Order order, String id, State state, Integer zrealizowano) {
		this.order = order;
		this.id = id;
		this.state = state;
		this.zrealizowano = zrealizowano;
	}

	public OrderState(Order order, String id, String state, Integer zrealizowano) {
		this.order = order;
		this.id = id;
		this.state = null;
		this.customState = formatState(state);
		this.zrealizowano = zrealizowano;
	}

	public enum State {
		WPROW, ZAKSIEG, ZREAL, ZAMKN, ANULOWANO, W_TR_ANUL, MODYF, REDU
	}

	public boolean canBeModified() {
		if (state != null) {
			return EnumSet.of(State.WPROW, State.ZAKSIEG).contains(state);
		} else if (customState != null) {
			return "przyjęte".equals(customState.toLowerCase(LocaleUtils.LOCALE));
		} else {
			return false;
		}
	}

	public String getStateString(Context ctx) {
		if (customState != null) {
			return customState;
		}

		String[] states = ctx.getResources().getStringArray(R.array.order_states);
		State[] statesEnum = State.values();
		for (int i = 0; i < states.length; i++) {
			if (statesEnum[i] == state) {
				return states[i];
			}
		}
		return null;
	}

	public String getId() {
		return id;
	}

	public Order getOrder() {
		return order;
	}

	public State getState() {
		return state;
	}

	public Integer getZrealizowano() {
		return zrealizowano;
	}

	private String formatState(String state) {
		return state.toLowerCase(LocaleUtils.LOCALE);
	}
}
