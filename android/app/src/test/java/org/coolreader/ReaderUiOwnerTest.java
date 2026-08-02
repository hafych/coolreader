package org.coolreader;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ReaderUiOwnerTest {
	@Test
	public void installRejectsNullAndCloseIsPermanent() {
		ReaderUiOwner owner = new ReaderUiOwner();
		assertFalse(owner.install(null, null));
		assertNull(owner.view());
		assertNull(owner.frame());
		assertFalse(owner.isPresent());
		assertNull(owner.close());
		assertTrue(owner.isClosed());
		assertFalse(owner.install(null, null));
		assertNull(owner.close());
	}
}
