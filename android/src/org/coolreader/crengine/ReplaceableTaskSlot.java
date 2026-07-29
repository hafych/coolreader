package org.coolreader.crengine;

final class ReplaceableTaskSlot {
	private GuardedTask current;

	synchronized Replacement replace(Runnable task) {
		if (task == null)
			throw new IllegalArgumentException("task must not be null");
		Runnable previous = current;
		current = new GuardedTask(task);
		return new Replacement(previous, current);
	}

	synchronized Runnable cancel() {
		Runnable canceled = current;
		current = null;
		return canceled;
	}

	private synchronized boolean claim(GuardedTask task) {
		if (current != task)
			return false;
		current = null;
		return true;
	}

	private final class GuardedTask implements Runnable {
		private final Runnable delegate;

		private GuardedTask(Runnable delegate) {
			this.delegate = delegate;
		}

		@Override
		public void run() {
			if (claim(this))
				delegate.run();
		}
	}

	static final class Replacement {
		private final Runnable previous;
		private final Runnable current;

		private Replacement(Runnable previous, Runnable current) {
			this.previous = previous;
			this.current = current;
		}

		Runnable previous() {
			return previous;
		}

		Runnable current() {
			return current;
		}
	}
}
