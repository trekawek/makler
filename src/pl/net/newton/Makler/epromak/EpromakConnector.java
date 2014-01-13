package pl.net.newton.Makler.epromak;

import java.io.ByteArrayInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import pl.net.newton.Makler.R;
import pl.net.newton.Makler.common.DataSource;
import pl.net.newton.Makler.common.DateFormatUtils;
import pl.net.newton.Makler.gpw.ex.GpwException;
import pl.net.newton.Makler.gpw.ex.InvalidPasswordException;
import android.os.Environment;
import android.util.Log;

public class EpromakConnector extends pl.net.newton.Makler.httpClient.Connector {
	private static final String TAG = "Makler";

	private String idlog, idses, username, password;

	private Map<String, String> factids;

	private static final int port = 443;

	private Document staticData = null;

	public static boolean debug = false;

	public EpromakConnector(String name, String password, String host, Boolean trust, DataSource type) {
		super(host, port, trust);
		this.username = name;
		this.password = password;
	}

	public void login() throws GpwException, InvalidPasswordException {
		client.getCookieStore().clear();
		idlog = null;
		idses = null;
		List<NameValuePair> data = new ArrayList<NameValuePair>();
		String resp;

		data.add(new BasicNameValuePair("j_username", username));
		data.add(new BasicNameValuePair("j_password", password));

		data.add(new BasicNameValuePair("logNoweHaslo1", ""));
		data.add(new BasicNameValuePair("ajax", "true"));
		try {
			resp = EntityUtils.toString(sendCommand("/epromak/epromak/login", null, new UrlEncodedFormEntity(
					data), false, null));
		} catch (Exception e) {
			throw new GpwException(e);
		}
		if (resp.contains("W przypadku dalszych"))
			throw new InvalidPasswordException(resp);
		if (resp.contains("zablokowany"))
			throw new InvalidPasswordException(resp);
		if (resp.contains("Nieprawidłowy login lub hasło"))
			throw new InvalidPasswordException(resp);
		if (resp.contains("Failure of server APACHE bridge"))
			throw new GpwException("Serwer Epromak jest niedostępny");
		getStaticData(true);
		Element bUserStaticData = getElementByName(staticData, "dataset", "bUserStaticData");
		idlog = bUserStaticData.getAttribute("idlog");
		idses = bUserStaticData.getAttribute("idses");

		NodeList factids = getElementByName(staticData, "dataset", "fACTid").getChildNodes();
		this.factids = new HashMap<String, String>();
		for (int i = 0, l = factids.getLength(); i < l; i++) {
			NamedNodeMap n = factids.item(i).getAttributes();
			String fname = n.getNamedItem("fname").getNodeValue();
			String value = n.getNamedItem("value").getNodeValue();
			this.factids.put(fname, value);
		}
		selectAccount();
	}

	public Boolean keepAlive() {
		try {
			HttpEntity e = sendCommand("/epromak/epromak/keepalive", null, null, false, null);
			parseXml(e.getContent());
			return true;
		} catch (Exception e) {
			Log.e(TAG, e.toString());
			return false;
		}
	}

	public void selectAccount() throws GpwException, InvalidPasswordException {
		Element e = getElementByName(getStaticData(false), "dataset", "sAktPakietyZlecen");
		String nrRach = e.getAttribute("nrRach");
		String nrPok = e.getAttribute("nrPOK");

		Document doc = builder.newDocument();
		Element dataset = doc.createElement("dataset");
		dataset.setAttribute("name", "bClientConfig");
		Element d = doc.createElement("bClientConfig");
		d.setAttribute("newRach", nrRach);
		d.setAttribute("newPOK", nrPok);
		dataset.appendChild(d);

		sendXml("/epromak/epromak/dbwrite", "changeRach", dataset, null);
	}

	public Document sendXml(String path, String action, Element element, List<Header> headers)
			throws GpwException, InvalidPasswordException {
		return sendXml(path, action, getStringFromNode(element), headers);
	}

	public Document sendXml(String path, String action, String data, List<Header> headers)
			throws GpwException, InvalidPasswordException {
		String req = generateXml(action, data);
		if (headers == null)
			headers = new ArrayList<Header>();
		if (factids.containsKey(action))
			headers.add(new BasicHeader("fac", factids.get(action)));
		if (debug)
			logToFile(" >> " + req);
		HttpEntity en = null;
		try {
			en = sendCommand(path, null, new StringEntity(req, HTTP.UTF_8), false, headers);
			if (en == null)
				return null;
			return parseXml(en.getContent());
		} catch (Exception ex) {
			throw new GpwException(ex);
		}
	}

	private Document parseXml(InputStream is) throws GpwException, InvalidPasswordException {
		Document doc = null;
		try {
			doc = builder.parse(is);
			if (debug)
				logToFile(" << " + getStringFromNode(doc.getDocumentElement()));
		} catch (Exception e) {
			return null;
		}

		Element err;
		Boolean thr = false;
		try {
			err = (Element) doc.getElementsByTagName("ERR").item(0);
			if (err.hasAttribute("code") && !err.getAttribute("code").equals(R.string.zero))
				thr = true;
		} catch (Exception e) {
			return null;
		}
		if (thr) {
			/*
			 * if(err.getAttribute("desc").contains("Nieznany u")) { login(); throw new
			 * EpromakException("Rozłączono z serwerem. Nastąpi ponowne połączenie." ); } else
			 */
			throw new GpwException(err.getAttribute("desc"));
		}
		return doc;
	}

	private String generateXml(String action, String s) {
		return new StringBuilder("<EPROMAK><ERR code=\"0\"/><ACT name=\"").append(action)
				.append("\"/><CTX idlog=\"").append(idlog).append("\" idses=\"").append(idses).append("\"/>")
				.append(s).append("</EPROMAK>").toString();
	}

	public Document getStaticData(Boolean reload) throws GpwException {
		if (!reload && staticData != null)
			return staticData;
		HttpEntity e = sendCommand("/epromak/epromak/getdata/user/bStaticData/select", null, null, true, null);
		Document doc = null;

		try {
			byte[] responseBuffer = EntityUtils.toByteArray(e);
			if (new String(responseBuffer, 0, 9).equalsIgnoreCase("<!DOCTYPE")) {
				throw new GpwException("Ten serwis nie obsługuje systemu Epromak.");
			}
			doc = builder.parse(new ByteArrayInputStream(responseBuffer));
			// if(debug)
			// logToFile(" << " + getStringFromNode(doc.getDocumentElement()));
		} catch (GpwException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new GpwException(ex);
		}
		if (doc == null)
			return null;
		return staticData = doc;
	}

	@Override
	protected void setHeaders(HttpRequestBase req, List<Header> headers) {
		super.setHeaders(req, headers);
		if (idlog != null)
			req.setHeader("idlog", idlog);
		if (idses != null)
			req.setHeader("idses", idses);
	}

	@Override
	public HttpEntity sendCommand(String path, String query, HttpEntity data, Boolean get,
			List<Header> headers) throws GpwException {
		if (debug)
			logToFile((get ? "GET " : "POST ") + path);
		try {
			return super.sendCommand(path, query, data, get, headers);
		} catch (IOException e) {
			throw new GpwException(e);
		}
	}

	public static void logToFile(String line) {
		try {
			FileWriter f = new FileWriter(Environment.getExternalStorageDirectory() + "/makler.log", true);
			f.append(DateFormatUtils.formatYyyyMmDd());
			f.append(' ');
			f.append(line);
			f.append('\n');
			f.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void logToFile(Exception e) {
		try {
			FileWriter f = new FileWriter(Environment.getExternalStorageDirectory() + "/makler.log", true);
			f.append(DateFormatUtils.formatYyyyMmDd());
			f.append(' ');
			f.append(e.toString());
			f.append('\n');
			PrintWriter p = new PrintWriter(f);
			e.printStackTrace(p);
			f.append('\n');
			p.close();
			f.close();
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}
}