package org.coolreader.crengine;

import org.junit.Test;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class IterativeScanTraversalTest {
	@Test
	public void tenThousandNestedDirectoriesDoNotUseCallStack() {
		final int nodeCount = 10_000;
		Node root = deepTree(nodeCount);
		AtomicInteger discovered = new AtomicInteger();
		AtomicInteger completed = new AtomicInteger();

		boolean fullDepth = IterativeScanTraversal.traverse(
				root, nodeCount, () -> false,
				adapter(discovered, completed));

		assertTrue(fullDepth);
		assertEquals(nodeCount, discovered.get());
		assertEquals(nodeCount, completed.get());
	}

	@Test
	public void cancellationStopsBeforeDiscoveringAnotherNode() {
		Node root = deepTree(1_000);
		AtomicBoolean stopped = new AtomicBoolean();
		AtomicInteger discovered = new AtomicInteger();
		AtomicInteger completed = new AtomicInteger();
		IterativeScanTraversal.Adapter<Node> adapter =
				new IterativeScanTraversal.Adapter<Node>() {
					@Override
					public boolean discover(Node node) {
						if (discovered.incrementAndGet() == 50)
							stopped.set(true);
						return true;
					}

					@Override
					public int childCount(Node node) {
						return node.children.size();
					}

					@Override
					public Node childAt(Node node, int index) {
						return node.children.get(index);
					}

					@Override
					public void onCompleted(
							Node node, boolean fullDepth) {
						completed.incrementAndGet();
					}
				};

		assertFalse(IterativeScanTraversal.traverse(
				root, 1_000, stopped::get, adapter));
		assertEquals(50, discovered.get());
		assertEquals(0, completed.get());
	}

	@Test
	public void depthLimitReportsIncompleteTraversal() {
		Node root = deepTree(4);
		AtomicInteger discovered = new AtomicInteger();
		AtomicInteger completed = new AtomicInteger();

		assertFalse(IterativeScanTraversal.traverse(
				root, 2, () -> false,
				adapter(discovered, completed)));
		assertEquals(2, discovered.get());
		assertEquals(2, completed.get());
	}

	private static IterativeScanTraversal.Adapter<Node> adapter(
			AtomicInteger discovered, AtomicInteger completed) {
		return new IterativeScanTraversal.Adapter<Node>() {
			@Override
			public boolean discover(Node node) {
				discovered.incrementAndGet();
				return true;
			}

			@Override
			public int childCount(Node node) {
				return node.children.size();
			}

			@Override
			public Node childAt(Node node, int index) {
				return node.children.get(index);
			}

			@Override
			public void onCompleted(Node node, boolean fullDepth) {
				completed.incrementAndGet();
			}
		};
	}

	private static Node deepTree(int nodeCount) {
		Node root = new Node();
		Node current = root;
		for (int i = 1; i < nodeCount; i++) {
			Node child = new Node();
			current.children.add(child);
			current = child;
		}
		return root;
	}

	private static final class Node {
		final ArrayList<Node> children = new ArrayList<>(1);
	}
}
