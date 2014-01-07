package pl.net.newton.Makler.httpClient;

import java.util.Comparator;

import org.apache.http.cookie.Cookie;

public class CookieIdentityComparator implements Comparator<Cookie> {
	public int compare(final Cookie c1, final Cookie c2) {
		int res = c1.getName().compareTo(c2.getName());
		if (res == 0) {
			// do not differentiate empty and null domains
			String d1 = c1.getDomain();
			if (d1 == null) {
				d1 = "";
			}
			String d2 = c2.getDomain();
			if (d2 == null) {
				d2 = "";
			}
			res = d1.compareToIgnoreCase(d2);
		}
		if (res == 0) {
			String p1 = c1.getPath();
			if (p1 == null) {
				p1 = "/";
			}
			String p2 = c2.getPath();
			if (p2 == null) {
				p2 = "/";
			}
			res = p1.compareTo(p2);
		}
		return res;
	}
}
