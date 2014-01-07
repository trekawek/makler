package pl.net.newton.Makler.mbank;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import pl.net.newton.Makler.gpw.ex.GpwException;
import pl.net.newton.Makler.gpw.ex.InvalidPasswordException;
import android.util.Log;

public class MbankConnector extends pl.net.newton.Makler.httpClient.Connector {
	private static final String TAG = "Makler";

	private String state, event, viewstate;

	private String sessnum;

	public MbankConnector(String host, int port) {
		super(host, port, true);
	}

	public void login(String login, String password) throws InvalidPasswordException, GpwException {
		String loginPage = sendCommand("/", null, null, true, null, true);
		loginPage = sendCommand("/", null, null, true, null, true);
		String seed = matchGroup("name=\"seed\" id=\"seed\" value=\"([^\"]+)\"", loginPage);

		List<NameValuePair> data = new ArrayList<NameValuePair>();
		data.add(new BasicNameValuePair("seed", seed));
		data.add(new BasicNameValuePair("localDT", SimpleDateFormat.getDateTimeInstance().format(new Date())));
		data.add(new BasicNameValuePair("customer", login));
		data.add(new BasicNameValuePair("password", password));

		String page = sendCommand("/logon.aspx", null, data, false, null, false);
		if (page.contains("Błąd logowania"))
			throw new InvalidPasswordException("Nieprawidłowe hasło");
		event = null;
		viewstate = "";
		openQuotes();
	}

	public Document sendXml(String path, String action, Element element, List<Header> headers, String host,
			Integer port) throws GpwException {
		return sendXml(path, action, getStringFromNode(element), headers, host, port);
	}

	public Document sendXml(String path, String action, String data, List<Header> headers, String host,
			Integer port) throws GpwException {
		String req = generateXml(action, data);
		// Log.d("Makler >> Epromak", req);

		if (headers == null)
			headers = new ArrayList<Header>();
		// headers.add(new BasicHeader("sessnum", sessnum));
		headers.add(new BasicHeader("idses", sessnum));
		headers.add(new BasicHeader("idlog", ""));
		headers.add(new BasicHeader("login", ""));
		headers.add(new BasicHeader("Content-Type", "text/xml"));

		HttpEntity en = null;
		try {
			en = sendCommand(path, null, new StringEntity(req, HTTP.UTF_8), false, headers, host, port);
			if (en == null) {
				return null;
			}
			return parseXml(en.getContent());
		} catch (Exception ex) {
			throw new GpwException(ex);
		}
	}

	public HttpEntity sendCommand(String path, String query, HttpEntity data, Boolean get,
			List<Header> headers, String host, Integer port) throws ClientProtocolException, IOException {
		HttpResponse resp;
		if (get) {
			HttpGet req = new HttpGet(uriFromPath(path, query, host, port));
			// Log.d(TAG, "GET url: " + req.getURI().toString());
			setHeaders(req, headers);
			resp = client.execute(req);
		} else {
			HttpPost req = new HttpPost(uriFromPath(path, query, host, port));
			// Log.d(TAG, "POST url: " + req.getURI().toString());
			if (data != null)
				req.setEntity(data);
			setHeaders(req, headers);
			resp = client.execute(req);
		}
		return resp.getEntity();
	}

	private Document parseXml(InputStream is) {
		Document doc;
		try {
			doc = builder.parse(is);
			// Log.d("Makler << Epromak", getStringFromNode(doc.getDocumentElement()));
		} catch (Exception e) {
			return null;
		}

		Element err;
		Boolean thr = false;
		try {
			err = (Element) doc.getElementsByTagName("ERR").item(0);
			if (err.hasAttribute("code") && !err.getAttribute("code").equals("0"))
				thr = true;
		} catch (Exception e) {
			return null;
		}
		if (thr)
			return null;
		return doc;
	}

	private String generateXml(String action, String s) {
		return new StringBuilder("<EPROMAK><ERR code=\"0\"/><ACT name=\"").append(action)
				.append("\"/><CTX login=\"\"")
				// .append(" sessnum=\"")
				.append(" idses=\"").append(sessnum).append("\" idlog=\"\"/>").append(s).append("</EPROMAK>")
				.toString();
	}

	private void openQuotes() throws GpwException {
		String page;
		page = sendCommand("/accounts_list.aspx", null, null, true, null, true);
		page = sendCommand("/investitions.aspx", null, null, false, null, true);
		page = sendCommand("/di/di_quotation_basket_list.aspx", null,
				getParameters(page, "/di/di_quotation_basket_list.aspx"), false, null, true);
		page = sendCommand("/di/di_open_quotations.aspx", null,
				getParameters(page, "/di/di_open_quotations.aspx"), false, null, true);
		sessnum = matchGroup("openBasket\\('([^']+)'", page);
	}

	public static String matchGroup(String regexp, String body) {
		Pattern p = Pattern.compile(regexp);
		Matcher m = p.matcher(body);
		if (m.find())
			return m.group(1);
		else
			return null;
	}

	public String sendCommand(String path, String query, List<NameValuePair> data, Boolean get,
			List<Header> headers, Boolean updateState) throws GpwException {
		HttpEntity dataEn = null;
		Log.d(TAG, path);
		if (!get) {
			if (data == null) {
				data = new ArrayList<NameValuePair>();
				MbankClient.modifyParams(data, "__PARAMETERS", "");
			}
			if (state != null)
				MbankClient.modifyParams(data, "__STATE", state);
			if (viewstate != null)
				MbankClient.modifyParams(data, "__VIEWSTATE", viewstate);
			if (event != null)
				MbankClient.modifyParams(data, "__EVENTVALIDATION", event);
			// for(NameValuePair p : data)
			// Log.d(TAG, new StringBuilder(p.getName()).append(": ").append(p.getValue()).toString());
			try {
				dataEn = new UrlEncodedFormEntity(data);
			} catch (UnsupportedEncodingException e) {
				throw new GpwException(e);
			}
		}
		HttpEntity en;
		try {
			en = sendCommand(path, query, dataEn, get, headers);
		} catch (IOException e) {
			throw new GpwException(e);
		}
		String page = null;
		try {
			page = EntityUtils.toString(en);
		} catch (Exception e) {
		}

		if (page.contains("Alarm bezpieczeństwa"))
			throw new RuntimeException("Błąd w komunikacji z mBankiem.");
		if (page.contains("Operacja wykonana niepoprawnie")) {
			String message = matchGroup("\\<p class=\"message\"\\>([^\\<]+)", page);
			message = message.replace("&#x27;", "'");
			throw new GpwException(message);
		}
		if (page.contains("wystąpiły problemy komunikacji z systemem informatycznym domu inwestycyjnego"))
			throw new GpwException("Problem komunikacji między mBankiem a domem maklerskim.");
		if (updateState) {
			state = getHiddenFieldValue(page, "__STATE");
			viewstate = getHiddenFieldValue(page, "__VIEWSTATE");
			event = getHiddenFieldValue(page, "__EVENTVALIDATION");
		}
		return page;
	}

	public static String getHiddenFieldValue(String page, String name) {
		String regexp = new StringBuilder("name=\"").append(name).append("\" id=\"").append(name)
				.append("\" value=\"([^\"]*)\"").toString();
		return matchGroup(regexp, page);
	}

	public static List<NameValuePair> getParameters(String page, String path, String label) {
		String regexp = new StringBuilder("doSubmit\\('").append(path)
				.append("','[^']*','[^']*','([^']*)'[^\\>]+\\>").append(label).toString();
		String param = matchGroup(regexp, page);
		List<NameValuePair> data = new ArrayList<NameValuePair>();
		if (param == null)
			return null;
		data.add(new BasicNameValuePair("__PARAMETERS", param));
		return data;
	}

	public static List<NameValuePair> getParameters(String page, String path) {
		String regexp = new StringBuilder("doSubmit\\('").append(path).append("','[^']*','[^']*','([^']*)'")
				.toString();
		String param = matchGroup(regexp, page);
		if (param == null)
			return null;
		List<NameValuePair> data = new ArrayList<NameValuePair>();
		data.add(new BasicNameValuePair("__PARAMETERS", param));
		return data;
	}

	@Override
	protected void setHeaders(HttpRequestBase req, List<Header> headers) {
		super.setHeaders(req, headers);
		req.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
		req.setHeader("Accept-Charset", "ISO-8859-2,utf-8;q=0.7,*;q=0.3");
		req.setHeader("Accept-Encoding", "gzip,deflate,sdch");
		req.removeHeaders("Cache-Control");
		req.setHeader("Cache-Control", "max-age=0");
		req.setHeader("Origin", "https://www.mbank.com.pl");
		req.removeHeaders("Accept-Language");
		req.setHeader("Accept-Language", "pl-PL,pl;q=0.8,en-US;q=0.6,en;q=0.4");
	}
}
