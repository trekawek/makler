package pl.net.newton.Makler.gpw.service;

import java.util.Calendar;

import pl.net.newton.Makler.common.GpwUtils;
import android.util.Log;

class UpdatingThread implements Runnable {
	private final QuotesService quotesService;

	private volatile Thread runner;

	private Calendar lastUpdate;

	UpdatingThread(QuotesService quotesService) {
		this.quotesService = quotesService;
	}

	public void run() {
		while (Thread.currentThread() == runner) {
			lastUpdate = Calendar.getInstance();
			int freq = this.quotesService.config.getFreqForeground();
			if (GpwUtils.gpwActive() && freq != 0 && this.quotesService.foreground) {
				try {
					this.quotesService.updateQuotes();
					notifyListeners();
				} catch (Exception e) {
					Log.e(QuotesService.TAG, "can't update quotes", e);
				}
			}
			Log.d(QuotesService.TAG, "waiting for " + freq + "s");
			try {
				Thread.sleep(1000 * freq);
			} catch (InterruptedException e) {
				Log.e(QuotesService.TAG, "Interrupted quote update loop", e);
				return;
			}
		}
	}

	private void notifyListeners() {
		for (QuotesListener listener : this.quotesService.listeners) {
			listener.quotesUpdated();
		}
	}

	public synchronized void startThread() {
		if (!isAlive()) {
			runner = new Thread(this);
			runner.start();
		}
	}

	public synchronized void stopThread() {
		if (runner != null) {
			Thread moribund = runner;
			runner = null;
			moribund.interrupt();
		}
	}

	private boolean isAlive() {
		if (runner == null) {
			return false;
		}
		if (!runner.isAlive()) {
			return false;
		}
		Calendar now = Calendar.getInstance();
		now.add(Calendar.SECOND, -(this.quotesService.config.getFreqForeground() + 30));
		return lastUpdate.after(now);
	}
}