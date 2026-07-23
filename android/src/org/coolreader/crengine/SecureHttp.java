/*
 * CoolReader for Android
 * Copyright (C) 2026 CoolReader Next contributors
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 */

package org.coolreader.crengine;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;

import javax.net.ssl.HttpsURLConnection;

/**
 * Opens HTTP connections without replacing the platform TLS trust manager or
 * hostname verifier. Redirects stay explicit so every destination performs a
 * new scheme, certificate and hostname check.
 */
public final class SecureHttp {
	public static HttpURLConnection open(URL url, Proxy proxy) throws IOException {
		URLConnection raw =
				proxy == null ? url.openConnection() : url.openConnection(proxy);
		if (!(raw instanceof HttpURLConnection))
			throw new IOException("Only HTTP and HTTPS URLs are supported");
		HttpURLConnection connection = (HttpURLConnection) raw;
		connection.setInstanceFollowRedirects(false);
		return connection;
	}

	public static HttpsURLConnection openHttps(URL url) throws IOException {
		HttpURLConnection connection = open(url, null);
		if (!(connection instanceof HttpsURLConnection)) {
			connection.disconnect();
			throw new IOException("HTTPS is required");
		}
		return (HttpsURLConnection) connection;
	}

	private SecureHttp() {
	}
}
