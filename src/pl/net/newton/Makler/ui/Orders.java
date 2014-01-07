package pl.net.newton.Makler.ui;

import java.util.List;

import pl.net.newton.Makler.R;
import pl.net.newton.Makler.gpw.QuotesReceiver;
import pl.net.newton.Makler.gpw.Trades;
import pl.net.newton.Makler.gpw.ex.GpwException;
import pl.net.newton.Makler.gpw.ex.InvalidPasswordException;
import pl.net.newton.Makler.gpw.model.OrderState;
import pl.net.newton.Makler.gpw.service.GpwProvider;
import pl.net.newton.Makler.history.service.HistoryService;
import pl.net.newton.Makler.ui.adapter.OrdersAdapter;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

public class Orders extends AbstractActivity {
	private ListView ordersList;

	private List<OrderState> orders;

	private Button newOrder;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.orders);
		ordersList = (ListView) findViewById(R.id.ordersListView);
		newOrder = (Button) findViewById(R.id.newOrderBtn);
		newOrder.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent(getApplicationContext(), OrderForm.class);
				startActivity(intent);
			}
		});
		registerForContextMenu(ordersList);
		setTitle("Zlecenia");
	}

	@Override
	public void onResume() {
		super.onResume();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.orders_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.refreshOrders:
				getOrderStates();
				break;
		}
		return super.onOptionsItemSelected(item);
	}

	private static class ContextMenuItem {
		final static int MODIFY = 0, CANCEL = 1;
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
		OrderState o = orders.get(info.position);

		if (o.canBeModified()) {
			// if(!dataSourceType.equals("mbank"))
			//menu.add(0, ContextMenuItem.MODIFY, 0, "Modyfikuj");
			menu.add(0, ContextMenuItem.CANCEL, 0, "Anuluj zlecenie");
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
		OrderState orderState = orders.get(info.position);

		Intent intent;

		switch (item.getItemId()) {
			case ContextMenuItem.CANCEL:
				cancel(orderState.getId());
				break;

			case ContextMenuItem.MODIFY:
				intent = new Intent(this, OrderForm.class);
				intent.putExtra("orderId", orderState.getId());
				startActivity(intent);
				break;
		}
		return true;
	}

	private void refreshList() {
		if (orders == null)
			return;
		if (orders.size() == 0) {
			findViewById(R.id.noOrders).setVisibility(View.VISIBLE);
			findViewById(R.id.ordersInst).setVisibility(View.GONE);
		} else {
			findViewById(R.id.noOrders).setVisibility(View.GONE);
			findViewById(R.id.ordersInst).setVisibility(View.VISIBLE);
		}
		ordersList.setAdapter(new OrdersAdapter(this, orders));
	}

	private void getOrderStates() {
		perform(new ProcessPerformer() {
			public void showResults(boolean result) {
				refreshList();
			}

			public boolean perform(QuotesReceiver quotesReceiver, Trades trades) throws GpwException,
					InvalidPasswordException {
				orders = trades.getOrderStates();
				return true;
			}
		}, true);
	}

	private void cancel(final String id) {
		perform(new ProcessPerformer() {

			public void showResults(boolean result) {
				refreshList();
			}

			public boolean perform(QuotesReceiver quotesReceiver, Trades trades) throws GpwException,
					InvalidPasswordException {
				trades.cancel(id);
				orders = trades.getOrderStates();
				return true;
			}
		}, true);
	}

	@Override
	protected void initUi(GpwProvider gpwProvider, SQLiteDatabase sqlDb, HistoryService historyService) {
		getOrderStates();		
	}
}
