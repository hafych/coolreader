package org.coolreader.crengine;

import org.junit.Test;

import java.io.IOException;
import java.net.URL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class OPDSUtilTest {
	@Test
	public void sameOriginNormalizesDefaultHttpsPort() throws Exception {
		assertTrue(OPDSUtil.isSameOrigin(
				new URL("https://catalog.example/feed"),
				new URL("https://CATALOG.example:443/next")));
	}

	@Test
	public void sameOriginRejectsHostSchemeAndPortChanges() throws Exception {
		URL origin = new URL("https://catalog.example/feed");
		assertFalse(OPDSUtil.isSameOrigin(origin, new URL("https://evil.example/feed")));
		assertFalse(OPDSUtil.isSameOrigin(origin, new URL("http://catalog.example/feed")));
		assertFalse(OPDSUtil.isSameOrigin(origin, new URL("https://catalog.example:444/feed")));
	}

	@Test
	public void redirectResolutionSupportsRelativeLocations() throws Exception {
		assertEquals(
				"https://catalog.example/books/2",
				OPDSUtil.resolveRedirect(
						new URL("https://catalog.example/books/1"),
						"/books/2").toString());
	}

	@Test
	public void catalogLinkResolutionNormalizesParentPath() throws Exception {
		assertEquals(
				"https://catalog.example:443/books/2",
				OPDSUtil.LinkInfo.convertHref(
						new URL("https://catalog.example:443/books/section/"),
						"../2"));
	}

	@Test
	public void catalogLinkResolutionRejectsNonHttpSchemes() throws Exception {
		assertEquals(null, OPDSUtil.LinkInfo.convertHref(
				new URL("https://catalog.example/feed"),
				"file:///data/local/tmp/secret"));
	}

	@Test(expected = IOException.class)
	public void redirectResolutionRejectsNonHttpSchemes() throws Exception {
		OPDSUtil.resolveRedirect(
				new URL("https://catalog.example/feed"),
				"file:///data/local/tmp/secret");
	}

	@Test
	public void basicAuthorizationUsesStandardSchemePrefix() {
		assertEquals("Basic dXNlcjpwYXNz",
				OPDSUtil.DownloadTask.encodePassword("user", "pass"));
	}

	@Test
	public void logUrlDropsCredentialsQueryAndFragment() throws Exception {
		assertEquals("https://catalog.example/feed",
				OPDSUtil.safeUrlForLog(
						new URL("https://user:pass@catalog.example/feed?sid=secret#fragment")));
	}
}
