package pl.net.newton.Makler.httpClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.zip.GZIPInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pl.net.newton.Makler.gpw.ex.GpwException;
import pl.net.newton.Makler.httpClient.DefaultCookieStore;

public class Connector {
	private String host;

	private int port;

	protected DefaultHttpClient client;

	private static final Boolean USE_PROXY = false;

	private static final String PROXY_HOST = "192.168.40.222";

	private static final int PROXY_PORT = 8888;

	protected DocumentBuilder builder;

	public Connector(String host, int port) {
		this(host, port, false);
	}

	public Connector(String host, int port, boolean trust) {
		this.host = host;
		this.port = port;
		HttpParams params = new BasicHttpParams();
		HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
		HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);
		HttpProtocolParams.setUseExpectContinue(params, false);
		HttpConnectionParams.setConnectionTimeout(params, 20000);
		HttpConnectionParams.setSoTimeout(params, 20000);

		SchemeRegistry schReg = new SchemeRegistry();
		schReg.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
		if (USE_PROXY || trust) {
			schReg.register(new Scheme("https", new EasySSLSocketFactory(), 443));
		} else {
			schReg.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));
		}

		ClientConnectionManager conMgr = new ThreadSafeClientConnManager(params, schReg);

		client = new DefaultHttpClient(conMgr, params);
		if (USE_PROXY) {
			HttpHost proxy = new HttpHost(PROXY_HOST, PROXY_PORT);
			client.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
		}
		client.setCookieStore(new DefaultCookieStore());
		client.setKeepAliveStrategy(new ConnectionKeepAliveStrategy() {
			public long getKeepAliveDuration(HttpResponse response, HttpContext context) {
				return 60; // seconds
			}
		});

		DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
		try {
			this.builder = builderFactory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
	}

	public HttpEntity sendCommand(String path, String query, HttpEntity data, Boolean get,
			List<Header> headers) throws GpwException, IllegalStateException, IOException {
		HttpResponse resp;
		if (get) {
			HttpGet req = new HttpGet(uriFromPath(path, query));
			setHeaders(req, headers);
			try {
				resp = client.execute(req);
			} catch (Exception e) {
				throw new GpwException(e);
			}
		} else {
			HttpPost req = new HttpPost(uriFromPath(path, query));
			if (data != null)
				req.setEntity(data);
			setHeaders(req, headers);
			try {
				resp = client.execute(req);
			} catch (Exception e) {
				throw new GpwException(e);
			}
		}
		Header contentEncoding = resp.getFirstHeader("Content-Encoding");
		HttpEntity entity = resp.getEntity();
		if ((contentEncoding != null) && contentEncoding.getValue().equalsIgnoreCase("gzip")) {
			entity = new GzipDecompressingEntity(entity);
		}
		return entity;
	}

	protected void setHeaders(HttpRequestBase req, List<Header> headers) {
		req.setHeader("Cache-Control", "no-cache");
		req.setHeader("User-Agent",
				"Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/535.2 (KHTML, like Gecko) Chrome/15.0.874.106 Safari/535.2");
		req.setHeader("Accept-Language", "pl");
		req.setHeader("Connection", "Keep-Alive");
		if (headers != null)
			for (Header h : headers)
				req.setHeader(h);
	}

	protected URI uriFromPath(String path, String query) {
		return uriFromPath(path, query, this.host, this.port);
	}

	protected URI uriFromPath(String path, String query, String host, Integer port) {
		URI u;
		if (port == null)
			port = -1;
		try {
			if (port == 80) {
				u = new URI("http", null, host, port, path, query, null);
			} else {
				u = new URI("https", null, host, port, path, query, null);
			}
			return u;
		} catch (URISyntaxException e) {
			e.printStackTrace();
			return null;
		}
	}

	public Element getElementByName(Document doc, String tagname, String name) {
		NodeList l = doc.getElementsByTagName(tagname);
		int length = l.getLength();
		for (int i = 0; i < length; i++) {
			Node atr = l.item(i).getAttributes().getNamedItem("name");
			if (atr == null)
				continue;
			if (atr.getNodeValue().equals(name))
				return (Element) l.item(i);
		}
		return null;
	}

	public static String getStringFromNode(Node root) throws GpwException {
		StringBuilder result = new StringBuilder();

		if (root.getNodeType() == 3)
			result.append(root.getNodeValue());
		// else if(root.getNodeType() == 9)
		// result.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		else {
			NodeList nodes = root.getChildNodes();
			StringBuffer attrs = new StringBuffer();

			int la = root.getAttributes().getLength();
			int ln = nodes.getLength();
			for (int k = 0; k < la; ++k) {
				attrs.append(root.getAttributes().item(k).getNodeName()).append("=\"")
						.append(root.getAttributes().item(k).getNodeValue()).append("\"");
				if (k < (la - 1))
					attrs.append(" ");
			}
			result.append("<").append(root.getNodeName());
			if (la > 0)
				result.append(" ").append(attrs);
			if (nodes.getLength() == 0)
				result.append("/>");
			else
				result.append(">");

			for (int i = 0; i < ln; i++) {
				Node node = nodes.item(i);
				result.append(getStringFromNode(node));
			}
			if (ln != 0)
				result.append("</").append(root.getNodeName()).append(">");
		}
		return result.toString();
	}

	public DocumentBuilder getBuilder() {
		return builder;
	}

	public BufferedReader readUrl(String url) throws URISyntaxException, ClientProtocolException, IOException {
		URI uri = new URI(url);
		HttpGet method = new HttpGet(uri);
		HttpResponse res = client.execute(method);
		InputStream data = res.getEntity().getContent();
		InputStreamReader reader = new InputStreamReader(data);
		return new BufferedReader(reader);
	}
}