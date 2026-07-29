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
		DocumentLoadLifecycle.Interaction firstInteraction =
				lifecycle.interaction();
		DocumentLoadLifecycle.Request first =
				lifecycle.replace();
		DocumentLoadLifecycle.Interaction secondInteraction =
				lifecycle.interaction();
		DocumentLoadLifecycle.Request second =
				lifecycle.replace();
		DocumentLoadLifecycle.Interaction currentInteraction =
				lifecycle.interaction();

		assertFalse(lifecycle.isActive(first));
		assertTrue(lifecycle.isActive(second));
		assertFalse(lifecycle.isInteractionActive(
				firstInteraction));
		assertFalse(lifecycle.isInteractionActive(
				secondInteraction));
		assertTrue(lifecycle.isInteractionActive(
				currentInteraction));
	}

	@Test
	public void staleCompletionCannotClearReplacement() {
		DocumentLoadLifecycle lifecycle =
				new DocumentLoadLifecycle();
		DocumentLoadLifecycle.Request stale =
				lifecycle.replace();
		DocumentLoadLifecycle.Request current =
				lifecycle.replace();
		DocumentLoadLifecycle.Interaction currentInteraction =
				lifecycle.interaction();

		assertFalse(lifecycle.complete(stale));
		assertTrue(lifecycle.isActive(current));
		assertTrue(lifecycle.complete(current));
		assertFalse(lifecycle.isActive(current));
		assertFalse(lifecycle.isInteractionActive(
				currentInteraction));
	}

	@Test
	public void cancelAllowsAnotherRequest() {
		DocumentLoadLifecycle lifecycle =
				new DocumentLoadLifecycle();
		DocumentLoadLifecycle.Request first =
				lifecycle.replace();
		DocumentLoadLifecycle.Interaction firstInteraction =
				lifecycle.interaction();

		lifecycle.cancel();

		assertFalse(lifecycle.isActive(first));
		assertFalse(lifecycle.isInteractionActive(
				firstInteraction));
		assertNotNull(lifecycle.interaction());
		assertNotNull(lifecycle.replace());
	}

	@Test
	public void navigationCancelsPendingButPreservesPublishedDocument() {
		DocumentLoadLifecycle lifecycle =
				new DocumentLoadLifecycle();
		DocumentLoadLifecycle.Request pending =
				lifecycle.replace();
		DocumentLoadLifecycle.Interaction pendingInteraction =
				lifecycle.interaction();

		assertTrue(lifecycle.cancelPending());
		assertFalse(lifecycle.isActive(pending));
		assertFalse(lifecycle.isInteractionActive(
				pendingInteraction));

		DocumentLoadLifecycle.Request published =
				lifecycle.replace();
		DocumentLoadLifecycle.Interaction publishedInteraction =
				lifecycle.interaction();
		assertTrue(lifecycle.markPublished(published));
		assertFalse(lifecycle.cancelPending());
		assertTrue(lifecycle.isActive(published));
		assertFalse(lifecycle.isInteractionActive(
				publishedInteraction));
		assertNotNull(lifecycle.interaction());
	}

	@Test
	public void publishedCompletionKeepsCurrentDocumentInteraction() {
		DocumentLoadLifecycle lifecycle =
				new DocumentLoadLifecycle();
		DocumentLoadLifecycle.Request published =
				lifecycle.replace();
		DocumentLoadLifecycle.Interaction interaction =
				lifecycle.interaction();

		assertTrue(lifecycle.markPublished(published));
		assertTrue(lifecycle.complete(published));
		assertTrue(lifecycle.isInteractionActive(interaction));
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
		assertNull(lifecycle.interaction());
		assertNull(lifecycle.replace());
	}
}
