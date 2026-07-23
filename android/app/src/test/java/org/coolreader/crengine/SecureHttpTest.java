package org.coolreader.crengine;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SecureHttpTest {
	private static final int TIMEOUT_MILLIS = 3000;
	private static final char[] KEY_PASSWORD = "test-only".toCharArray();

	@Test
	public void trustedCertificateAndMatchingHostnameConnect() throws Exception {
		X509Certificate ca = loadCertificate("ca-cert.pem");
		try (TestHttpsServer server =
					 TestHttpsServer.start(loadCertificate("localhost-cert.pem"), ca)) {
			HttpsURLConnection connection = openTrusted(server.url("/"), ca);
			assertEquals(200, connection.getResponseCode());
			connection.disconnect();
		}
	}

	@Test
	public void selfSignedCertificateIsRejected() throws Exception {
		X509Certificate selfSigned = loadCertificate("selfsigned-cert.pem");
		assertEquals(selfSigned.getSubjectX500Principal(), selfSigned.getIssuerX500Principal());
		try (TestHttpsServer server = TestHttpsServer.start(selfSigned)) {
			assertTlsRejected(SecureHttp.openHttps(server.url("/")));
		}
	}

	@Test
	public void expiredCertificateIsRejectedEvenWhenCaIsTrusted() throws Exception {
		X509Certificate ca = loadCertificate("ca-cert.pem");
		X509Certificate expired = loadCertificate("expired-cert.pem");
		try {
			expired.checkValidity();
			fail("The expired test certificate must not be valid");
		} catch (CertificateExpiredException expected) {
			// Expected fixture state.
		}
		try (TestHttpsServer server = TestHttpsServer.start(expired, ca)) {
			assertTlsRejected(openTrusted(server.url("/"), ca));
		}
	}

	@Test
	public void hostnameMismatchIsRejectedEvenWhenCaIsTrusted() throws Exception {
		X509Certificate ca = loadCertificate("ca-cert.pem");
		X509Certificate wrongHost = loadCertificate("wrong-cert.pem");
		try (TestHttpsServer server = TestHttpsServer.start(wrongHost, ca)) {
			assertTlsRejected(openTrusted(server.url("/"), ca));
		}
	}

	@Test
	public void redirectToUntrustedOriginPerformsNewTlsValidation()
			throws Exception {
		X509Certificate ca = loadCertificate("ca-cert.pem");
		X509Certificate trusted = loadCertificate("localhost-cert.pem");
		X509Certificate selfSigned = loadCertificate("selfsigned-cert.pem");
		try (TestHttpsServer untrustedServer = TestHttpsServer.start(selfSigned);
			 TestHttpsServer redirectServer =
					 TestHttpsServer.start(trusted, ca)) {
			redirectServer.redirectTo(untrustedServer.url("/target"));

			HttpsURLConnection first = openTrusted(redirectServer.url("/"), ca);
			assertEquals(302, first.getResponseCode());
			assertTrue(!first.getInstanceFollowRedirects());
			URL target = OPDSUtil.resolveRedirect(
					redirectServer.url("/"), first.getHeaderField("Location"));
			assertNotEquals(
					redirectServer.url("/").getPort(),
					target.getPort());
			first.disconnect();

			assertTlsRejected(SecureHttp.openHttps(target));
		}
	}

	@Test
	public void httpsOnlyEntryPointRejectsPlainHttp() throws Exception {
		try {
			SecureHttp.openHttps(new URL("http://localhost/"));
			fail("Plain HTTP must not be accepted by the HTTPS-only path");
		} catch (IOException expected) {
			assertTrue(expected.getMessage().contains("HTTPS"));
		}
	}

	@Test
	public void openAppliesMandatoryConnectionPolicy() throws Exception {
		URL url = new URL(null, "http://catalog.example/feed",
				new URLStreamHandler() {
					@Override
					protected URLConnection openConnection(URL target) {
						return new FakeHttpConnection(target, null);
					}

					@Override
					protected URLConnection openConnection(
							URL target, Proxy proxy) {
						return openConnection(target);
					}
				});
		HttpURLConnection connection = SecureHttp.open(url, null);
		assertFalse(connection.getInstanceFollowRedirects());
		assertFalse(connection.getAllowUserInteraction());
		assertFalse(connection.getUseCaches());
		assertEquals(
				SecureHttp.CONNECT_TIMEOUT_MILLIS,
				connection.getConnectTimeout());
		assertEquals(
				SecureHttp.READ_TIMEOUT_MILLIS,
				connection.getReadTimeout());
	}

	@Test
	public void authorizationIsRestrictedToSameHttpsOrigin() throws Exception {
		URL origin = new URL("https://catalog.example/feed");
		FakeHttpConnection sameOrigin = new FakeHttpConnection(
				new URL("https://CATALOG.example:443/next"), null);
		assertTrue(SecureHttp.applyOriginAuthorization(
				sameOrigin, origin, "Basic secret"));
		assertEquals(
				"Basic secret",
				sameOrigin.getRequestProperty("Authorization"));

		FakeHttpConnection crossOrigin = new FakeHttpConnection(
				new URL("https://evil.example/next"), null);
		assertFalse(SecureHttp.applyOriginAuthorization(
				crossOrigin, origin, "Basic secret"));
		assertNull(crossOrigin.getRequestProperty("Authorization"));

		FakeHttpConnection plaintext = new FakeHttpConnection(
				new URL("http://catalog.example/next"), null);
		assertFalse(SecureHttp.applyOriginAuthorization(
				plaintext, origin, "Basic secret"));
		assertNull(plaintext.getRequestProperty("Authorization"));
	}

	@Test
	public void referrerIsSameOriginAndDropsSensitiveComponents()
			throws Exception {
		FakeHttpConnection sameOrigin = new FakeHttpConnection(
				new URL("https://catalog.example/book"), null);
		assertTrue(SecureHttp.applyOriginReferrer(
				sameOrigin,
				"https://user:pass@catalog.example/feed?sid=secret#part"));
		assertEquals(
				"https://catalog.example/feed",
				sameOrigin.getRequestProperty("Referer"));

		FakeHttpConnection crossOrigin = new FakeHttpConnection(
				new URL("https://evil.example/book"), null);
		assertFalse(SecureHttp.applyOriginReferrer(
				crossOrigin,
				"https://catalog.example/feed?sid=secret"));
		assertNull(crossOrigin.getRequestProperty("Referer"));
	}

	@Test
	public void redirectPolicyRejectsDowngradeAndExcessHops()
			throws Exception {
		try {
			SecureHttp.resolveRedirect(
					new URL("https://catalog.example/feed"),
					"http://catalog.example/next", 0);
			fail("HTTPS downgrade must be rejected");
		} catch (IOException expected) {
			assertTrue(expected.getMessage().contains("downgrade"));
		}
		try {
			SecureHttp.resolveRedirect(
					new URL("https://catalog.example/feed"),
					"/next", SecureHttp.MAX_REDIRECTS);
			fail("Redirects beyond the configured hop limit must be rejected");
		} catch (IOException expected) {
			assertTrue(expected.getMessage().contains("Too many"));
		}
	}

	@Test
	public void declaredResponseLengthIsValidatedAsLong() throws Exception {
		FakeHttpConnection accepted = new FakeHttpConnection(
				new URL("https://catalog.example/feed"), "1024");
		assertEquals(
				1024L,
				SecureHttp.requireContentLengthWithin(accepted, 1024));

		FakeHttpConnection oversized = new FakeHttpConnection(
				new URL("https://catalog.example/feed"), "4294967296");
		try {
			SecureHttp.requireContentLengthWithin(oversized, 1024);
			fail("Oversized Content-Length must be rejected");
		} catch (IOException expected) {
			assertTrue(expected.getMessage().contains("size limit"));
		}
	}

	private static HttpsURLConnection openTrusted(
			URL url, X509Certificate ca) throws Exception {
		HttpsURLConnection connection = SecureHttp.openHttps(url);
		connection.setSSLSocketFactory(createTrustedClientFactory(ca));
		configureTimeouts(connection);
		return connection;
	}

	private static void assertTlsRejected(HttpsURLConnection connection)
			throws Exception {
		configureTimeouts(connection);
		try {
			connection.getResponseCode();
			fail("TLS validation must reject the connection");
		} catch (SSLException expected) {
			// The concrete subtype differs between the JDK and Android providers.
		} finally {
			connection.disconnect();
		}
	}

	private static void configureTimeouts(HttpsURLConnection connection) {
		connection.setConnectTimeout(TIMEOUT_MILLIS);
		connection.setReadTimeout(TIMEOUT_MILLIS);
	}

	private static SSLSocketFactory createTrustedClientFactory(
			X509Certificate ca) throws Exception {
		KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
		trustStore.load(null);
		trustStore.setCertificateEntry("test-ca", ca);
		TrustManagerFactory trustManagers = TrustManagerFactory.getInstance(
				TrustManagerFactory.getDefaultAlgorithm());
		trustManagers.init(trustStore);
		SSLContext context = SSLContext.getInstance("TLS");
		context.init(null, trustManagers.getTrustManagers(), new SecureRandom());
		return context.getSocketFactory();
	}

	private static SSLContext createServerContext(Certificate... chain)
			throws Exception {
		KeyStore keyStore = KeyStore.getInstance("PKCS12");
		keyStore.load(null);
		keyStore.setKeyEntry(
				"server", loadPrivateKey(), KEY_PASSWORD, chain);
		KeyManagerFactory keyManagers = KeyManagerFactory.getInstance(
				KeyManagerFactory.getDefaultAlgorithm());
		keyManagers.init(keyStore, KEY_PASSWORD);
		SSLContext context = SSLContext.getInstance("TLS");
		context.init(keyManagers.getKeyManagers(), null, new SecureRandom());
		return context;
	}

	private static X509Certificate loadCertificate(String name) throws Exception {
		try (InputStream input = resource(name)) {
			return (X509Certificate) CertificateFactory
					.getInstance("X.509")
					.generateCertificate(input);
		}
	}

	private static PrivateKey loadPrivateKey() throws Exception {
		String pem;
		try (InputStream input = resource("server-key.pem")) {
			pem = new String(readAll(input), StandardCharsets.US_ASCII);
		}
		String encoded = pem
				.replace("-----BEGIN PRIVATE KEY-----", "")
				.replace("-----END PRIVATE KEY-----", "")
				.replaceAll("\\s", "");
		byte[] key = Base64.getDecoder().decode(encoded);
		return KeyFactory.getInstance("RSA")
				.generatePrivate(new PKCS8EncodedKeySpec(key));
	}

	private static InputStream resource(String name) {
		InputStream input =
				SecureHttpTest.class.getResourceAsStream("/tls/" + name);
		if (input == null)
			throw new IllegalStateException("Missing TLS fixture: " + name);
		return input;
	}

	private static byte[] readAll(InputStream input) throws IOException {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		byte[] buffer = new byte[4096];
		int count;
		while ((count = input.read(buffer)) >= 0)
			output.write(buffer, 0, count);
		return output.toByteArray();
	}

	private static final class FakeHttpConnection
			extends HttpURLConnection {
		private final String contentLength;

		FakeHttpConnection(URL url, String contentLength) {
			super(url);
			this.contentLength = contentLength;
		}

		@Override
		public String getHeaderField(String name) {
			if ("Content-Length".equalsIgnoreCase(name))
				return contentLength;
			return null;
		}

		@Override
		public void disconnect() {
		}

		@Override
		public boolean usingProxy() {
			return false;
		}

		@Override
		public void connect() {
		}
	}

	private static final class TestHttpsServer implements AutoCloseable {
		private final SSLServerSocket server;
		private final ExecutorService executor;
		private volatile URL redirectTarget;

		private TestHttpsServer(
				SSLServerSocket server, ExecutorService executor) {
			this.server = server;
			this.executor = executor;
		}

		static TestHttpsServer start(Certificate... chain) throws Exception {
			SSLServerSocket server = (SSLServerSocket) createServerContext(chain)
					.getServerSocketFactory()
					.createServerSocket(0, 16, InetAddress.getLoopbackAddress());
			ExecutorService executor = Executors.newCachedThreadPool(runnable -> {
				Thread thread = new Thread(runnable, "tls-fixture-server");
				thread.setDaemon(true);
				return thread;
			});
			TestHttpsServer fixture = new TestHttpsServer(server, executor);
			executor.execute(fixture::acceptConnections);
			return fixture;
		}

		URL url(String path) throws Exception {
			return new URL(
					"https", "localhost", server.getLocalPort(), path);
		}

		void redirectTo(URL target) {
			redirectTarget = target;
		}

		private void acceptConnections() {
			while (!server.isClosed()) {
				try {
					SSLSocket socket = (SSLSocket) server.accept();
					executor.execute(() -> respond(socket));
				} catch (IOException e) {
					if (!server.isClosed())
						throw new IllegalStateException("TLS fixture server failed", e);
				}
			}
		}

		private void respond(SSLSocket socket) {
			try (SSLSocket connection = socket;
				 BufferedReader input = new BufferedReader(new InputStreamReader(
						 connection.getInputStream(), StandardCharsets.US_ASCII));
				 OutputStream output = connection.getOutputStream()) {
				String line;
				while ((line = input.readLine()) != null && !line.isEmpty()) {
					// Consume request headers before writing the response.
				}
				URL target = redirectTarget;
				String response;
				if (target == null) {
					response =
							"HTTP/1.1 200 OK\r\n" +
							"Content-Length: 2\r\n" +
							"Connection: close\r\n\r\nok";
				} else {
					response =
							"HTTP/1.1 302 Found\r\n" +
							"Location: " + target + "\r\n" +
							"Content-Length: 0\r\n" +
							"Connection: close\r\n\r\n";
				}
				output.write(response.getBytes(StandardCharsets.US_ASCII));
				output.flush();
			} catch (IOException expectedForRejectedHandshake) {
				// Negative TLS tests intentionally terminate during the handshake.
			}
		}

		@Override
		public void close() {
			try {
				server.close();
			} catch (IOException ignored) {
				// Best-effort cleanup in tests.
			}
			executor.shutdownNow();
		}
	}
}
