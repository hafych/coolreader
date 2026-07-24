/*
 * CoolReader for Android
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 */

package org.coolreader.crengine;

import java.util.ArrayDeque;

/**
 * Iterative depth-first traversal used by library discovery.
 */
final class IterativeScanTraversal {
	interface StopSignal {
		boolean isStopped();
	}

	interface Adapter<T> {
		boolean discover(T node);
		int childCount(T node);
		T childAt(T node, int index);
		void onCompleted(T node, boolean fullDepth);
	}

	private static final class Frame<T> {
		final T node;
		final int remainingDepth;
		boolean discovered;
		boolean fullDepth = true;
		int nextChild;

		Frame(T node, int remainingDepth) {
			this.node = node;
			this.remainingDepth = remainingDepth;
		}
	}

	private IterativeScanTraversal() {
	}

	static <T> boolean traverse(T root, int maxDepth,
			StopSignal stopSignal, Adapter<T> adapter) {
		if (root == null)
			throw new IllegalArgumentException("root must not be null");
		if (stopSignal == null)
			throw new IllegalArgumentException(
					"stopSignal must not be null");
		if (adapter == null)
			throw new IllegalArgumentException("adapter must not be null");
		if (maxDepth <= 0 || stopSignal.isStopped())
			return false;

		ArrayDeque<Frame<T>> stack = new ArrayDeque<>();
		stack.push(new Frame<>(root, maxDepth));
		boolean rootResult = false;
		while (!stack.isEmpty()) {
			if (stopSignal.isStopped())
				return false;
			Frame<T> frame = stack.peek();
			if (!frame.discovered) {
				frame.discovered = true;
				if (!adapter.discover(frame.node)) {
					stack.pop();
					markParentIncomplete(stack);
					continue;
				}
				frame.nextChild =
						adapter.childCount(frame.node) - 1;
			}

			if (frame.nextChild >= 0) {
				T child = adapter.childAt(
						frame.node, frame.nextChild--);
				if (frame.remainingDepth <= 1) {
					frame.fullDepth = false;
				} else {
					stack.push(new Frame<>(
							child, frame.remainingDepth - 1));
				}
				continue;
			}

			stack.pop();
			adapter.onCompleted(frame.node, frame.fullDepth);
			boolean result = frame.fullDepth;
			if (stack.isEmpty())
				rootResult = result;
			else if (!result)
				stack.peek().fullDepth = false;
		}
		return rootResult;
	}

	private static <T> void markParentIncomplete(
			ArrayDeque<Frame<T>> stack) {
		if (!stack.isEmpty())
			stack.peek().fullDepth = false;
	}
}
