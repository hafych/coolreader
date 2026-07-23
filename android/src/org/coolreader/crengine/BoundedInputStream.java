/*
 * CoolReader for Android
 *
 * Input stream which fails once a caller attempts to read beyond its budget.
 */

package org.coolreader.crengine;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public final class BoundedInputStream extends FilterInputStream {
	private long remaining;

	public BoundedInputStream(InputStream inputStream, long maxBytes) {
		super(inputStream);
		if (maxBytes < 0)
			throw new IllegalArgumentException("maxBytes must be non-negative");
		remaining = maxBytes;
	}

	@Override
	public int read() throws IOException {
		if (remaining == 0)
			return ensureEndOfStream();
		int value = super.read();
		if (value >= 0)
			remaining--;
		return value;
	}

	@Override
	public int read(byte[] buffer, int offset, int length) throws IOException {
		if (length == 0)
			return 0;
		if (remaining == 0)
			return ensureEndOfStream();
		int allowed = (int)Math.min((long)length, remaining);
		int count = super.read(buffer, offset, allowed);
		if (count > 0)
			remaining -= count;
		return count;
	}

	@Override
	public long skip(long count) throws IOException {
		long skipped = super.skip(Math.min(count, remaining));
		remaining -= skipped;
		return skipped;
	}

	@Override
	public int available() throws IOException {
		return (int)Math.min((long)super.available(), remaining);
	}

	private int ensureEndOfStream() throws IOException {
		if (super.read() == -1)
			return -1;
		throw new IOException("Input exceeds configured byte limit");
	}
}
