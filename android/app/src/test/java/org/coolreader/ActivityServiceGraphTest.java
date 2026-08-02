package org.coolreader;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ActivityServiceGraphTest {
	@Test
	public void installNullRejectedAndCloseIsPermanent() {
		ActivityServiceGraph graph = new ActivityServiceGraph();
		assertFalse(graph.install(null));
		assertNull(graph.engine());
		assertNull(graph.lifecycle());
		assertFalse(graph.isActive());
		assertTrue(graph.close());
		assertTrue(graph.isClosed());
		assertFalse(graph.install(null));
		assertFalse(graph.close());
	}
}
