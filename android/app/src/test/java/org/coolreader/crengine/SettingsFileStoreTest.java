package org.coolreader.crengine;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileInputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

public class SettingsFileStoreTest {
	@Rule
	public final TemporaryFolder temporaryFolder =
			new TemporaryFolder();

	private final SettingsFileStore store =
			new SettingsFileStore();

	@Test
	public void saveRoundTripsProperties() throws Exception {
		File target = temporaryFolder.newFile("settings.ini");
		Properties settings = new Properties();
		settings.setProperty("font.face", "Literata");
		settings.setProperty("font.size", "24");

		store.save(target, settings);

		Properties restored = load(target);
		assertEquals("Literata", restored.getProperty("font.face"));
		assertEquals("24", restored.getProperty("font.size"));
	}

	@Test
	public void saveTruncatesThePreviousSnapshot() throws Exception {
		File target = temporaryFolder.newFile("settings.ini");
		Properties first = new Properties();
		first.setProperty("obsolete", "must disappear");
		first.setProperty("current", "old");
		store.save(target, first);

		Properties second = new Properties();
		second.setProperty("current", "new");
		store.save(target, second);

		Properties restored = load(target);
		assertEquals("new", restored.getProperty("current"));
		assertFalse(restored.containsKey("obsolete"));
	}

	@Test
	public void nullSnapshotCannotTruncateExistingSettings()
			throws Exception {
		File target = temporaryFolder.newFile("settings.ini");
		Properties settings = new Properties();
		settings.setProperty("preserved", "yes");
		store.save(target, settings);

		try {
			store.save(target, null);
			fail("null settings must be rejected");
		} catch (IllegalArgumentException expected) {
			assertEquals(
					"settings must not be null",
					expected.getMessage());
		}

		assertEquals(
				"yes",
				load(target).getProperty("preserved"));
	}

	private static Properties load(File file) throws Exception {
		Properties restored = new Properties();
		try (FileInputStream input = new FileInputStream(file)) {
			restored.load(input);
		}
		return restored;
	}
}
