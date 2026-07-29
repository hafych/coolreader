package org.coolreader.crengine;

import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class HyphDictRegistryTest {
	@Test
	public void valuesReturnIndependentOrderedSnapshots() {
		Engine.HyphDict[] first = Engine.HyphDict.values();
		assertTrue(first.length > 3);
		assertSame(Engine.HyphDict.NONE, first[0]);
		assertSame(Engine.HyphDict.ALGORITHM, first[1]);

		first[0] = null;
		Engine.HyphDict[] second = Engine.HyphDict.values();
		assertSame(Engine.HyphDict.NONE, second[0]);
	}

	@Test
	public void frozenNativeSnapshotRejectsLateFilePublication() {
		Engine.HyphDict[] frozen = Engine.HyphDict.freezeValues();
		assertTrue(frozen.length > 3);
		frozen[0] = null;

		assertSame(
				Engine.HyphDict.NONE,
				Engine.HyphDict.values()[0]);
		assertFalse(
				Engine.HyphDict.fromFile(
						new File("late-hyphenation.pattern")));
	}
}
