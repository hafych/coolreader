/*
 * CoolReader for Android
 *
 * Input stream which enforces a total wall-clock transfer deadline.
 */

package org.coolreader.crengine;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.util.concurrent.TimeUnit;

public final class DeadlineInputStream extends FilterInputStream {
	private final long startedAtNanos;
	private final long maxDurationNanos;

	public DeadlineInputStream(InputStream inputStream, long maxDurationMillis) {
		super(inputStream);
		if (maxDurationMillis < 0)
			throw new IllegalArgumentException(
					"maxDurationMillis must be non-negative");
		startedAtNanos = System.nanoTime();
		maxDurationNanos =
				TimeUnit.MILLISECONDS.toNanos(maxDurationMillis);
	}

	@Override
	public int read() throws IOException {
		requireWithinDeadline();
		int value = super.read();
		requireWithinDeadline();
		return value;
	}

	@Override
	public int read(byte[] buffer, int offset, int length) throws IOException {
		if (length == 0)
			return 0;
		requireWithinDeadline();
		int count = super.read(buffer, offset, length);
		requireWithinDeadline();
		return count;
	}

	@Override
	public long skip(long count) throws IOException {
		requireWithinDeadline();
		long skipped = super.skip(count);
		requireWithinDeadline();
		return skipped;
	}

	private void requireWithinDeadline() throws InterruptedIOException {
		if (System.nanoTime() - startedAtNanos >= maxDurationNanos)
			throw new InterruptedIOException(
					"HTTP transfer time limit exceeded");
	}
}
