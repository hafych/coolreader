package org.coolreader.crengine;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MetadataScanSessionStateTest {
	@Test
	public void finishOnceAndIncomplete() {
		MetadataScanSessionState state = new MetadataScanSessionState();
		assertTrue(state.isComplete());
		assertFalse(state.isFinished());
		state.markIncomplete();
		assertFalse(state.isComplete());
		assertTrue(state.beginFinish());
		assertTrue(state.isFinished());
		assertFalse(state.beginFinish());
		assertTrue(state.close());
		assertTrue(state.isClosed());
		assertFalse(state.beginFinish());
		assertFalse(state.close());
	}
}
