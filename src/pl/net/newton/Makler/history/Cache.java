package pl.net.newton.Makler.history;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.util.Log;

public class Cache {
	private static final String TAG = "Makler";

	private Map<String, CacheEntry> map;

	private int capacity;

	private int validTime;

	private Context ctx;

	private String filePrefix;

	public Cache(String filePrefix, int capacity, int validTime, Context ctx) {
		this.map = new HashMap<String, CacheEntry>(capacity);
		this.capacity = capacity;
		this.validTime = validTime;
		this.ctx = ctx;
		this.filePrefix = filePrefix;
	}

	public synchronized void addEntry(String key, EntryList entry) {
		if (map.size() == capacity) {
			removeOldest();
		}
		CacheEntry cEntry = new CacheEntry(entry);
		map.put(key, cEntry);
		saveToDisk(key);
	}

	public synchronized boolean hasKey(String key) {
		if (!map.containsKey(key)) {
			getFromDisk(key);
		}
		if (!map.containsKey(key)) {
			return false;
		}

		CacheEntry cEntry = map.get(key);
		if ((Calendar.getInstance().getTimeInMillis() - cEntry.calendar.getTimeInMillis()) > (validTime * 1000)) {
			map.remove(key);
			return false;
		}
		return true;
	}

	public synchronized EntryList getEntry(String key) {
		if (!hasKey(key)) {
			return null;
		}
		return map.get(key).entry;
	}

	private void removeOldest() {
		Calendar oldest = null;
		String oldestKey = null;
		for (String key : map.keySet()) {
			CacheEntry entry = map.get(key);
			if (oldest == null || oldest.after(entry.calendar)) {
				oldest = entry.calendar;
				oldestKey = key;
			}
		}
		if (oldestKey != null) {
			map.remove(oldestKey);
		}
	}

	private void saveToDisk(final String entry) {
		if (!map.containsKey(entry)) {
			return;
		}

		new Thread(new Runnable() {
			public void run() {
				try {
					synchronized (map.get(entry)) {
						Log.d(TAG, "saving " + entry + " to disk");
						FileOutputStream fos = ctx.openFileOutput(filePrefix + entry, Context.MODE_PRIVATE);
						ObjectOutputStream oos = new ObjectOutputStream(fos);
						oos.writeObject(map.get(entry));
						oos.close();
						Log.d(TAG, "saved " + entry + " to disk");
					}
				} catch (Exception e) {
					Log.e(TAG, "error in saving history", e);
				}
			}
		}).start();

	}

	private synchronized void getFromDisk(String entry) {
		try {
			Log.d(TAG, "loading " + entry + " from disk");
			File f = ctx.getFileStreamPath(filePrefix + entry);
			if (!f.exists()) {
				Log.d(TAG, entry + " not found");
				return;
			}
			Calendar cal = Calendar.getInstance();
			cal.setTimeInMillis(f.lastModified());
			if ((Calendar.getInstance().getTimeInMillis() - cal.getTimeInMillis()) > (validTime * 1000)) {
				Log.d(TAG, entry + " out of date");
				if (!f.delete()) {
					Log.e(TAG, "Can't delete file " + f.getPath());
				}
				return;
			}

			loadCacheEntry(entry);
			Log.d(TAG, "loaded " + entry);
		} catch (IOException e) {
			Log.e(TAG, "error in loading history", e);
		}
	}

	private void loadCacheEntry(String entry) throws IOException {
		FileInputStream fis = ctx.openFileInput(filePrefix + entry);
		try {
			ObjectInputStream ois = new ObjectInputStream(fis);
			CacheEntry e = (CacheEntry) ois.readObject();
			map.put(entry, e);
		} catch (ClassNotFoundException e) {
			Log.e(TAG, "Can't find class", e);
		} finally {
			fis.close();
		}
	}

	public static class CacheEntry implements Serializable {
		private static final long serialVersionUID = -3188392713226765929L;

		private EntryList entry;

		private Calendar calendar;

		public CacheEntry(EntryList entry) {
			this.entry = entry;
			this.calendar = Calendar.getInstance();
		}
	}
}
