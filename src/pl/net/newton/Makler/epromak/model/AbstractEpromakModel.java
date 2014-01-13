package pl.net.newton.Makler.epromak.model;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import org.w3c.dom.Element;

public abstract class AbstractEpromakModel {
	protected Map<String, String> data = null;

	protected AbstractEpromakModel() {

	}

	protected AbstractEpromakModel(String s, String[] fields) {
		if (s == null || s.length() == 0)
			return;
		data = new Hashtable<String, String>();
		String[] a = s.split(";");
		for (int i = 0; i < a.length && i < fields.length; i++)
			data.put(fields[i], a[i]);
	}

	protected AbstractEpromakModel(Element e, String[] fields) {
		this.data = new HashMap<String, String>();
		for (int i = 0; i < fields.length; i++)
			data.put(fields[i], e.getAttribute(fields[i]));
	}

	public String get(String name) {
		return data.get(name);
	}
}
