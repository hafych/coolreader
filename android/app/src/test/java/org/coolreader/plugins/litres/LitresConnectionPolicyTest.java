package org.coolreader.plugins.litres;

import org.junit.Test;

import java.net.URL;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LitresConnectionPolicyTest {
	@Test
	public void apiRequestsStayOnPinnedOrigin() throws Exception {
		assertTrue(LitresConnection.isTrustedApiUrl(
				new URL("https://robot.litres.ru/pages/test")));
		assertFalse(LitresConnection.isTrustedApiUrl(
				new URL("https://evil.example/pages/test")));
		assertFalse(LitresConnection.isTrustedApiUrl(
				new URL("http://robot.litres.ru/pages/test")));
		assertFalse(LitresConnection.isTrustedApiUrl(
				new URL("https://robot.litres.ru:444/pages/test")));
	}
}
