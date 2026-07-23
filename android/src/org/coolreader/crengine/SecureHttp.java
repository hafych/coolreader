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
	public static final int CONNECT_TIMEOUT_MILLIS = 60_000;
	public static final int READ_TIMEOUT_MILLIS = 60_000;
	public static final int MAX_REDIRECTS = 5;
	public static final long MAX_TRANSFER_TIME_MILLIS =
			15L * 60L * 1_000L;

	public static HttpURLConnection open(URL url, Proxy proxy) throws IOException {
		URLConnection raw =
				proxy == null ? url.openConnection() : url.openConnection(proxy);
		if (!(raw instanceof HttpURLConnection))
			throw new IOException("Only HTTP and HTTPS URLs are supported");
		HttpURLConnection connection = (HttpURLConnection) raw;
		connection.setInstanceFollowRedirects(false);
		connection.setAllowUserInteraction(false);
		connection.setUseCaches(false);
		connection.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
		connection.setReadTimeout(READ_TIMEOUT_MILLIS);
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

	public static boolean isRedirect(int statusCode) {
		return statusCode == HttpURLConnection.HTTP_MOVED_PERM
				|| statusCode == HttpURLConnection.HTTP_MOVED_TEMP
				|| statusCode == HttpURLConnection.HTTP_SEE_OTHER
				|| statusCode == 307
				|| statusCode == 308;
	}

	public static URL resolveRedirect(
			URL current, String location, int redirectsFollowed)
			throws IOException {
		if (redirectsFollowed >= MAX_REDIRECTS)
			throw new IOException("Too many redirects");
		if (location == null || location.trim().isEmpty())
			throw new IOException("Redirect is missing a destination");
		URL resolved = new URL(current, location);
		String protocol = resolved.getProtocol();
		if (!"http".equalsIgnoreCase(protocol)
				&& !"https".equalsIgnoreCase(protocol))
			throw new IOException("Unsupported redirect protocol");
		if ("https".equalsIgnoreCase(current.getProtocol())
				&& !"https".equalsIgnoreCase(protocol))
			throw new IOException("HTTPS downgrade redirect is not allowed");
		return resolved;
	}

	public static boolean isSameOrigin(URL first, URL second) {
		return first != null && second != null
				&& first.getProtocol().equalsIgnoreCase(second.getProtocol())
				&& first.getHost().equalsIgnoreCase(second.getHost())
				&& effectivePort(first) == effectivePort(second);
	}

	public static boolean applyOriginAuthorization(
			HttpURLConnection connection, URL credentialOrigin,
			String authorizationValue) {
		URL target = connection.getURL();
		if (credentialOrigin == null
				|| authorizationValue == null
				|| !"https".equalsIgnoreCase(credentialOrigin.getProtocol())
				|| !"https".equalsIgnoreCase(target.getProtocol())
				|| !isSameOrigin(credentialOrigin, target))
			return false;
		connection.setRequestProperty("Authorization", authorizationValue);
		return true;
	}

	public static boolean applyOriginReferrer(
			HttpURLConnection connection, String referrerValue) {
		if (referrerValue == null)
			return false;
		try {
			URL referrer = new URL(referrerValue);
			if (!isSameOrigin(referrer, connection.getURL()))
				return false;
			URL sanitized = new URL(
					referrer.getProtocol(), referrer.getHost(),
					referrer.getPort(), referrer.getPath());
			connection.setRequestProperty(
					"Referer", sanitized.toExternalForm());
			return true;
		} catch (Exception ignored) {
			return false;
		}
	}

	public static long requireContentLengthWithin(
			HttpURLConnection connection, long maxBytes) throws IOException {
		if (maxBytes < 0)
			throw new IllegalArgumentException("maxBytes must be non-negative");
		String value = connection.getHeaderField("Content-Length");
		if (value == null || value.trim().isEmpty())
			return -1;
		final long contentLength;
		try {
			contentLength = Long.parseLong(value.trim());
		} catch (NumberFormatException e) {
			throw new IOException("Invalid HTTP Content-Length", e);
		}
		if (contentLength < 0 || contentLength > maxBytes)
			throw new IOException("HTTP response exceeds configured size limit");
		return contentLength;
	}

	private static int effectivePort(URL url) {
		int port = url.getPort();
		return port >= 0 ? port : url.getDefaultPort();
	}

	private SecureHttp() {
	}
}
