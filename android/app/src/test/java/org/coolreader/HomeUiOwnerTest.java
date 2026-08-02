package org.coolreader;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class HomeUiOwnerTest {
	@Test
	public void installRejectsNullAndCloseIsPermanent() {
		HomeUiOwner owner = new HomeUiOwner();
		assertFalse(owner.install(null));
		assertNull(owner.frame());
		assertFalse(owner.isPresent());
		assertNull(owner.close());
		assertTrue(owner.isClosed());
		assertFalse(owner.install(null));
	}
}
