package pl.net.newton.Makler.httpClient;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.zip.GZIPInputStream;

import javax.xml.parsers.DocumentBuilder;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;

import android.util.Log;

public class Connector {
	private static final String TAG = "Makler";

	private String host;

	private int port;

	protected DefaultHttpClient client;

	protected DocumentBuilder builder;

	public Connector(String host, int port) {
		this.host = host;
		this.port = port;
		HttpParams params = new BasicHttpParams();
		HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
		HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);
		HttpProtocolParams.setUseExpectContinue(params, false);
		HttpConnectionParams.setConnectionTimeout(params, 20000);
		HttpConnectionParams.setSoTimeout(params, 20000);
		client = new DefaultHttpClient(params);
	}

	public InputStream get(String path, String query) throws IOException {
		HttpResponse resp;
		HttpGet req = new HttpGet(uriFromPath(path, query));
		resp = client.execute(req);
		Header contentEncoding = resp.getFirstHeader("Content-Encoding");
		HttpEntity entity = resp.getEntity();
		if ((contentEncoding != null) && "gzip".equalsIgnoreCase(contentEncoding.getValue())) {
			return new GZIPInputStream(entity.getContent());
		} else {
			return entity.getContent();
		}
	}

	protected URI uriFromPath(String path, String query) {
		URI u;
		try {
			if (port == 443) {
				u = new URI("https", null, host, port, path, query, null);
			} else {
				u = new URI("http", null, host, port, path, query, null);
			}
			return u;
		} catch (URISyntaxException e) {
			Log.e(TAG, "Can't parse URI", e);
			return null;
		}
	}
}