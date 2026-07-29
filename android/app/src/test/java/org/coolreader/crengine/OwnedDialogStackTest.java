package org.coolreader.crengine;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class OwnedDialogStackTest {
	@Test
	public void closingChildRestoresParentAsCurrent() {
		OwnedDialogStack<Object> stack = new OwnedDialogStack<>();
		Object parent = new Object();
		Object child = new Object();

		stack.opened(parent);
		stack.opened(child);
		assertSame(child, stack.current());

		stack.closed(child);

		assertSame(parent, stack.current());
		assertTrue(stack.isActive());
	}

	@Test
	public void reopeningDialogMovesItToTheTopWithoutDuplicates() {
		OwnedDialogStack<String> stack = new OwnedDialogStack<>();
		stack.opened("first");
		stack.opened("second");
		stack.opened("first");

		assertEquals(
				Arrays.asList("first", "second"),
				stack.takeAllForClose());
	}

	@Test
	public void teardownReturnsChildrenFirstAndClearsOwnership() {
		OwnedDialogStack<String> stack = new OwnedDialogStack<>();
		stack.opened("parent");
		stack.opened("child");

		List<String> closing = stack.takeAllForClose();

		assertEquals(
				Arrays.asList("child", "parent"),
				closing);
		assertFalse(stack.isActive());
		assertNull(stack.current());
		assertTrue(stack.takeAllForClose().isEmpty());
	}
}
