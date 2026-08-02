package org.coolreader.crengine;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ReaderNativeLifecycleTest {
	@Test
	public void normalLifecycleInitializesAndDestroysOnce() {
		ReaderNativeLifecycle lifecycle =
				new ReaderNativeLifecycle();

		assertTrue(lifecycle.claimCreate());
		assertFalse(lifecycle.claimCreate());
		assertTrue(lifecycle.markCreated());
		assertTrue(lifecycle.isActive());
		assertTrue(lifecycle.markInitialized());
		assertFalse(lifecycle.markInitialized());
		assertTrue(lifecycle.isInitialized());
		assertFalse(lifecycle.claimDestroy());

		assertTrue(lifecycle.close());
		assertFalse(lifecycle.close());
		assertFalse(lifecycle.isActive());
		assertFalse(lifecycle.isInitialized());
		assertTrue(lifecycle.claimDestroy());
		assertFalse(lifecycle.claimDestroy());
		assertNull(lifecycle.takeDoc());
	}

	@Test
	public void closeBeforeCreatePermanentlyRejectsNativeWork() {
		ReaderNativeLifecycle lifecycle =
				new ReaderNativeLifecycle();

		assertTrue(lifecycle.close());
		assertTrue(lifecycle.isClosed());
		assertFalse(lifecycle.claimCreate());
		assertFalse(lifecycle.markCreated());
		assertFalse(lifecycle.markInitialized());
		assertFalse(lifecycle.claimDestroy());
		assertFalse(lifecycle.attach(null));
	}

	@Test
	public void closeDuringCreateStillRequiresDestroy() {
		ReaderNativeLifecycle lifecycle =
				new ReaderNativeLifecycle();

		assertTrue(lifecycle.claimCreate());
		assertTrue(lifecycle.close());
		assertFalse(lifecycle.markCreated());
		assertFalse(lifecycle.isActive());
		assertFalse(lifecycle.markInitialized());
		assertTrue(lifecycle.claimDestroy());
		assertFalse(lifecycle.claimDestroy());
	}

	@Test
	public void closeAfterCreateRejectsLateInitialization() {
		ReaderNativeLifecycle lifecycle =
				new ReaderNativeLifecycle();

		assertTrue(lifecycle.claimCreate());
		assertTrue(lifecycle.markCreated());
		assertTrue(lifecycle.close());

		assertFalse(lifecycle.markInitialized());
		assertFalse(lifecycle.isInitialized());
		assertTrue(lifecycle.claimDestroy());
	}

	@Test
	public void initializationRequiresCompletedCreate() {
		ReaderNativeLifecycle lifecycle =
				new ReaderNativeLifecycle();

		assertFalse(lifecycle.markCreated());
		assertFalse(lifecycle.markInitialized());
		assertFalse(lifecycle.isActive());
		assertFalse(lifecycle.isInitialized());
		assertFalse(lifecycle.claimDestroy());
	}

	@Test
	public void takeDocRequiresDestroyClaim() {
		ReaderNativeLifecycle lifecycle =
				new ReaderNativeLifecycle();
		assertNull(lifecycle.takeDoc());
		assertTrue(lifecycle.claimCreate());
		assertTrue(lifecycle.markCreated());
		assertNull(lifecycle.takeDoc());
		assertTrue(lifecycle.close());
		assertTrue(lifecycle.claimDestroy());
		assertNull(lifecycle.takeDoc());
	}
}
