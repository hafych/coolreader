package org.coolreader.crengine;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class RootViewRefreshSessionTest {
	@Test
	public void replacementInvalidatesOnlyTheSameChannel() {
		RootViewRefreshSession session =
				new RootViewRefreshSession();
		RootViewRefreshSession.Request oldRecent =
				session.replace(
						RootViewRefreshSession.Channel.RECENT_BOOKS);
		RootViewRefreshSession.Request filesystem =
				session.replace(
						RootViewRefreshSession.Channel.FILESYSTEM);

		RootViewRefreshSession.Request newRecent =
				session.replace(
						RootViewRefreshSession.Channel.RECENT_BOOKS);

		assertFalse(session.isActive(oldRecent));
		assertTrue(session.isActive(newRecent));
		assertTrue(session.isActive(filesystem));
		assertFalse(session.complete(oldRecent));
		assertTrue(session.complete(newRecent));
	}

	@Test
	public void viewReplacementInvalidatesEveryPendingRefresh() {
		RootViewRefreshSession session =
				new RootViewRefreshSession();
		RootViewRefreshSession.Request recent =
				session.replace(
						RootViewRefreshSession.Channel.RECENT_BOOKS);
		RootViewRefreshSession.Request online =
				session.replace(
						RootViewRefreshSession.Channel.ONLINE_CATALOGS);

		session.replaceView();

		assertFalse(session.isActive(recent));
		assertFalse(session.isActive(online));
		assertFalse(session.complete(recent));
		RootViewRefreshSession.Request library =
				session.replace(
						RootViewRefreshSession.Channel.LIBRARY);
		assertTrue(session.isActive(library));
	}

	@Test
	public void completionClaimsOnlyTheExactRequestOnce() {
		RootViewRefreshSession session =
				new RootViewRefreshSession();
		RootViewRefreshSession.Request request =
				session.replace(
						RootViewRefreshSession.Channel.ONLINE_CATALOGS);

		assertTrue(session.complete(request));
		assertFalse(session.complete(request));
		assertFalse(session.isActive(request));
	}

	@Test
	public void closePermanentlyRejectsRefreshes() {
		RootViewRefreshSession session =
				new RootViewRefreshSession();
		RootViewRefreshSession.Request request =
				session.replace(
						RootViewRefreshSession.Channel.FILESYSTEM);

		assertTrue(session.close());
		assertFalse(session.isActive(request));
		assertTrue(session.isClosed());
		assertFalse(session.close());
		assertNull(session.replace(
				RootViewRefreshSession.Channel.RECENT_BOOKS));
	}
}
