/*
 * CoolReader for Android
 * Copyright (C) 2026 CoolReader Next contributors
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 */

package org.coolreader.crengine;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

final class AtomicFileBackup {
	private static final int FIRST_GENERATION = 2;
	private static final int LAST_GENERATION = 5;

	private AtomicFileBackup() {
	}

	static boolean create(File source) {
		if (source == null || !source.isFile())
			return false;

		File pending = sibling(source, ".good.bak.pending");
		if (!removeIfPresent(pending) || !copyAndVerify(source, pending))
			return false;

		for (int generation = LAST_GENERATION;
			 generation > FIRST_GENERATION;
			 generation--) {
			File older = backupFile(source, generation);
			File newer = backupFile(source, generation - 1);
			if (!removeIfPresent(older)
					|| (newer.exists() && !newer.renameTo(older))) {
				pending.delete();
				return false;
			}
		}

		File current = backupFile(source, FIRST_GENERATION);
		if (!removeIfPresent(current) || !pending.renameTo(current)) {
			pending.delete();
			return false;
		}
		return true;
	}

	static boolean restore(File target) {
		if (target == null)
			return false;
		File backup = backupFile(target, FIRST_GENERATION);
		if (!backup.isFile())
			return false;

		File pending = sibling(target, ".restore.pending");
		File previous = sibling(target, ".restore.previous");
		if (!removeIfPresent(pending)
				|| !removeIfPresent(previous)
				|| !copyAndVerify(backup, pending))
			return false;

		boolean hadTarget = target.exists();
		if (hadTarget && !target.renameTo(previous)) {
			pending.delete();
			return false;
		}
		if (!pending.renameTo(target)) {
			if (hadTarget)
				previous.renameTo(target);
			pending.delete();
			return false;
		}
		if (previous.exists() && !previous.delete())
			previous.deleteOnExit();
		return true;
	}

	static File backupFile(File source, int generation) {
		return new File(source.getAbsolutePath() + ".good.bak." + generation);
	}

	private static File sibling(File file, String suffix) {
		return new File(file.getAbsolutePath() + suffix);
	}

	private static boolean removeIfPresent(File file) {
		return !file.exists() || (file.isFile() && file.delete());
	}

	private static boolean copyAndVerify(File source, File target) {
		long sourceLength = source.length();
		long sourceModified = source.lastModified();
		byte[] sourceDigest;
		try {
			sourceDigest = copyWithDigest(source, target);
			if (source.length() != sourceLength
					|| source.lastModified() != sourceModified
					|| target.length() != sourceLength
					|| !Arrays.equals(sourceDigest, digest(target))) {
				target.delete();
				return false;
			}
			return true;
		} catch (IOException e) {
			target.delete();
			return false;
		}
	}

	private static byte[] copyWithDigest(File source, File target) throws IOException {
		MessageDigest digest = sha256();
		byte[] buffer = new byte[64 * 1024];
		try (FileInputStream input = new FileInputStream(source);
			 FileOutputStream output = new FileOutputStream(target)) {
			int count;
			while ((count = input.read(buffer)) != -1) {
				output.write(buffer, 0, count);
				digest.update(buffer, 0, count);
			}
			output.flush();
			output.getFD().sync();
		}
		return digest.digest();
	}

	private static byte[] digest(File file) throws IOException {
		MessageDigest digest = sha256();
		byte[] buffer = new byte[64 * 1024];
		try (FileInputStream input = new FileInputStream(file)) {
			int count;
			while ((count = input.read(buffer)) != -1)
				digest.update(buffer, 0, count);
		}
		return digest.digest();
	}

	private static MessageDigest sha256() throws IOException {
		try {
			return MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			throw new IOException("SHA-256 is unavailable", e);
		}
	}
}
