package org.coolreader.crengine;

import java.util.ArrayList;

final class DeferredTaskQueue<T> {
	interface Dispatcher<T> {
		boolean dispatch(T task, long delay);
	}

	private static final class Entry<T> {
		final T task;
		final long delay;

		Entry(T task, long delay) {
			this.task = task;
			this.delay = delay;
		}
	}

	private final ArrayList<Entry<T>> pending = new ArrayList<>();
	private Dispatcher<T> dispatcher;

	synchronized int attach(Dispatcher<T> dispatcher) {
		this.dispatcher = dispatcher;
		if (dispatcher == null || pending.isEmpty())
			return 0;
		int delivered = 0;
		try {
			while (delivered < pending.size()) {
				Entry<T> entry = pending.get(delivered);
				if (!dispatcher.dispatch(entry.task, entry.delay))
					break;
				delivered++;
			}
		} finally {
			if (delivered > 0)
				pending.subList(0, delivered).clear();
		}
		return delivered;
	}

	synchronized boolean post(T task, long delay) {
		if (task == null)
			throw new IllegalArgumentException("Task must not be null");
		Entry<T> entry = new Entry<>(task, Math.max(0, delay));
		if (dispatcher == null || !pending.isEmpty()) {
			pending.add(entry);
			return false;
		}
		try {
			if (dispatcher.dispatch(entry.task, entry.delay))
				return true;
		} catch (RuntimeException failure) {
			pending.add(entry);
			throw failure;
		}
		pending.add(entry);
		return false;
	}

	synchronized int pendingCount() {
		return pending.size();
	}
}
