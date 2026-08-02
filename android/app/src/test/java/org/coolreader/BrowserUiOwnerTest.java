package org.coolreader;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class BrowserUiOwnerTest {
	@Test
	public void installRejectsNullAndCloseIsPermanent() {
		BrowserUiOwner owner = new BrowserUiOwner();
		assertFalse(owner.install(null, null, null, null));
		assertNull(owner.browser());
		assertNull(owner.frame());
		assertFalse(owner.isPresent());
		assertNull(owner.close());
		assertTrue(owner.isClosed());
		assertFalse(owner.install(null, null, null, null));
	}
}
