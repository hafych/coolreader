package org.coolreader.crengine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pure child-list ownership without constructing FileInfo nodes that need
 * Android (use null-safe structural paths only).
 */
public class FileInfoChildrenTest {
	@Test
	public void emptyAndClear() {
		FileInfoChildren children = new FileInfoChildren();
		assertEquals(0, children.dirCount());
		assertEquals(0, children.fileCount());
		assertTrue(children.isEmpty());
		assertFalse(children.hasDirs());
		assertFalse(children.hasFiles());
		assertNull(children.filesOrNull());
		assertNull(children.dirsOrNull());
		children.clear();
		assertTrue(children.isEmpty());
	}
}
