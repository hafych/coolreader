package org.coolreader.crengine;

final class BlockingResult<T> {
	private T value;
	private boolean completed;

	synchronized void complete(T value) {
		if (completed)
			throw new IllegalStateException(
					"Blocking result is already complete");
		this.value = value;
		completed = true;
		notifyAll();
	}

	synchronized T await() {
		boolean interrupted = false;
		while (!completed) {
			try {
				wait();
			} catch (InterruptedException e) {
				interrupted = true;
			}
		}
		if (interrupted)
			Thread.currentThread().interrupt();
		return value;
	}
}
