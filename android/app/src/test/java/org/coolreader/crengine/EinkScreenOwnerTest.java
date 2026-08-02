package org.coolreader.crengine;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class EinkScreenOwnerTest {
	@Test
	public void installIsExclusiveAndCloseIsPermanent() {
		EinkScreenOwner owner = new EinkScreenOwner();
		EinkScreen first = new EinkScreenDummy();
		EinkScreen second = new EinkScreenDummy();

		assertFalse(owner.install(null));
		assertTrue(owner.install(first));
		assertSame(first, owner.get());
		assertFalse(owner.install(second));
		assertSame(first, owner.get());

		assertTrue(owner.close());
		assertTrue(owner.isClosed());
		assertNull(owner.get());
		assertFalse(owner.install(second));
		assertFalse(owner.close());
	}
}
