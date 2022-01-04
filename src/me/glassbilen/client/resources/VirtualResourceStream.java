package me.glassbilen.client.resources;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

public class VirtualResourceStream extends URLStreamHandler {
	private final byte[] content;

	public VirtualResourceStream(byte[] content) {
		this.content = content;
	}

	@Override
	protected URLConnection openConnection(URL url) throws IOException {
		return new URLConnection(url) {
			@Override
			public void connect() throws IOException {
				connected = true;
			}

			@Override
			public long getContentLengthLong() {
				return content.length;
			}

			@Override
			public InputStream getInputStream() throws IOException {
				return new ByteArrayInputStream(content);
			}
		};
	}
}
