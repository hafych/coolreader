package org.coolreader.crengine;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class BaseServiceDependenciesStateTest {
	private static ServiceDependencies snapshot() {
		return new ServiceDependencies(
				null, null, null, null, null, null, null, null);
	}

	@Test
	public void installIsExclusiveAndCloseIsPermanent() {
		BaseServiceDependenciesState state =
				new BaseServiceDependenciesState();
		ServiceDependencies first = snapshot();
		ServiceDependencies second = snapshot();

		assertFalse(state.install(null));
		assertTrue(state.install(first));
		assertSame(first, state.get());
		assertTrue(state.isPresent());
		assertFalse(state.install(second));
		assertSame(first, state.get());

		assertTrue(state.close());
		assertTrue(state.isClosed());
		assertNull(state.get());
		assertFalse(state.isPresent());
		assertFalse(state.install(second));
		assertFalse(state.close());
	}
}
