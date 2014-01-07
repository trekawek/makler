package pl.net.newton.Makler.httpClient;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import org.apache.http.HttpEntity;
import org.apache.http.entity.HttpEntityWrapper;

public class GzipDecompressingEntity extends HttpEntityWrapper {

	public GzipDecompressingEntity(HttpEntity wrapped) {
		super(wrapped);
	}

	public InputStream getContent() throws IOException {
		InputStream wrappedin = wrappedEntity.getContent();
		return new GZIPInputStream(wrappedin);
	}

	public long getContentLength() {
		return -1;
	}

}
