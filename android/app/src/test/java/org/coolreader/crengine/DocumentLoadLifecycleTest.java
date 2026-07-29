package org.coolreader.crengine;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class DocumentLoadLifecycleTest {
	@Test
	public void replacementInvalidatesTheWholePriorOpenChain() {
		DocumentLoadLifecycle lifecycle =
				new DocumentLoadLifecycle();
		DocumentLoadLifecycle.Request first =
				lifecycle.replace();
		DocumentLoadLifecycle.Request second =
				lifecycle.replace();

		assertFalse(lifecycle.isActive(first));
		assertTrue(lifecycle.isActive(second));
	}

	@Test
	public void staleCompletionCannotClearReplacement() {
		DocumentLoadLifecycle lifecycle =
				new DocumentLoadLifecycle();
		DocumentLoadLifecycle.Request stale =
				lifecycle.replace();
		DocumentLoadLifecycle.Request current =
				lifecycle.replace();

		assertFalse(lifecycle.complete(stale));
		assertTrue(lifecycle.isActive(current));
		assertTrue(lifecycle.complete(current));
		assertFalse(lifecycle.isActive(current));
	}

	@Test
	public void cancelAllowsAnotherRequest() {
		DocumentLoadLifecycle lifecycle =
				new DocumentLoadLifecycle();
		DocumentLoadLifecycle.Request first =
				lifecycle.replace();

		lifecycle.cancel();

		assertFalse(lifecycle.isActive(first));
		assertNotNull(lifecycle.replace());
	}

	@Test
	public void navigationCancelsPendingButPreservesPublishedDocument() {
		DocumentLoadLifecycle lifecycle =
				new DocumentLoadLifecycle();
		DocumentLoadLifecycle.Request pending =
				lifecycle.replace();

		assertTrue(lifecycle.cancelPending());
		assertFalse(lifecycle.isActive(pending));

		DocumentLoadLifecycle.Request published =
				lifecycle.replace();
		assertTrue(lifecycle.markPublished(published));
		assertFalse(lifecycle.cancelPending());
		assertTrue(lifecycle.isActive(published));
	}

	@Test
	public void staleRequestCannotPublishReplacement() {
		DocumentLoadLifecycle lifecycle =
				new DocumentLoadLifecycle();
		DocumentLoadLifecycle.Request stale =
				lifecycle.replace();
		DocumentLoadLifecycle.Request current =
				lifecycle.replace();

		assertFalse(lifecycle.markPublished(stale));
		assertTrue(lifecycle.isActive(current));
	}

	@Test
	public void closePermanentlyRejectsRequests() {
		DocumentLoadLifecycle lifecycle =
				new DocumentLoadLifecycle();
		DocumentLoadLifecycle.Request request =
				lifecycle.replace();

		assertTrue(lifecycle.close());
		assertFalse(lifecycle.close());
		assertTrue(lifecycle.isClosed());
		assertFalse(lifecycle.isActive(request));
		assertNull(lifecycle.replace());
	}
}
