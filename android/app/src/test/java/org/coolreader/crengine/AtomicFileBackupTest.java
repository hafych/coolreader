package org.coolreader.crengine;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AtomicFileBackupTest {
	@Rule
	public final TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Test
	public void createKeepsSourceAndRotatesFourVerifiedGenerations() throws Exception {
		File source = temporaryFolder.newFile("library.sqlite");
		for (int version = 1; version <= 5; version++) {
			write(source, "database-" + version);
			assertTrue(AtomicFileBackup.create(source));
			assertEquals("database-" + version, read(source));
		}

		assertEquals("database-5", read(AtomicFileBackup.backupFile(source, 2)));
		assertEquals("database-4", read(AtomicFileBackup.backupFile(source, 3)));
		assertEquals("database-3", read(AtomicFileBackup.backupFile(source, 4)));
		assertEquals("database-2", read(AtomicFileBackup.backupFile(source, 5)));
		assertFalse(new File(source.getAbsolutePath() + ".good.bak.pending").exists());
	}

	@Test
	public void restoreAtomicallyReplacesTargetAndKeepsBackup() throws Exception {
		File target = temporaryFolder.newFile("library.sqlite");
		write(target, "known-good");
		assertTrue(AtomicFileBackup.create(target));
		write(target, "corrupted-current");

		assertTrue(AtomicFileBackup.restore(target));

		assertEquals("known-good", read(target));
		assertEquals("known-good", read(AtomicFileBackup.backupFile(target, 2)));
		assertFalse(new File(target.getAbsolutePath() + ".restore.pending").exists());
		assertFalse(new File(target.getAbsolutePath() + ".restore.previous").exists());
	}

	@Test
	public void missingBackupLeavesCurrentTargetUntouched() throws Exception {
		File target = temporaryFolder.newFile("library.sqlite");
		write(target, "current");

		assertFalse(AtomicFileBackup.restore(target));
		assertEquals("current", read(target));
	}

	private static void write(File file, String value) throws Exception {
		Files.write(file.toPath(), value.getBytes(StandardCharsets.UTF_8));
	}

	private static String read(File file) throws Exception {
		return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
	}
}
