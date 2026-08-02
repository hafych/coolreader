package org.coolreader.crengine;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pure ownership contracts without constructing SettingsManager (needs
 * Activity). Exclusive install/close and require-after-close are covered.
 */
public class SettingsManagerOwnerTest {
	@Test
	public void installNullRejectedAndCloseIsPermanent() {
		SettingsManagerOwner owner = new SettingsManagerOwner();
		assertFalse(owner.install(null));
		assertNull(owner.get());
		assertTrue(owner.close());
		assertTrue(owner.isClosed());
		assertNull(owner.get());
		assertFalse(owner.install(null));
		assertFalse(owner.close());
	}

	@Test(expected = IllegalStateException.class)
	public void requireThrowsWhenEmpty() {
		new SettingsManagerOwner().require();
	}
}
