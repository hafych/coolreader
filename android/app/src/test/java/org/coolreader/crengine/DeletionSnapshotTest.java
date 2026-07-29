package org.coolreader.crengine;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

public class DeletionSnapshotTest {
	@Test
	public void captureCopiesTargetAndParent() {
		MutableValue parent = new MutableValue("/books");
		MutableValue target =
				new MutableValue("/books/title.epub");

		DeletionSnapshot<MutableValue> snapshot =
				DeletionSnapshot.capture(
						target,
						parent,
						MutableValue::new);
		target.value = "/changed";
		parent.value = "/other";

		assertEquals(
				"/books/title.epub",
				snapshot.getTarget().value);
		assertEquals(
				"/books",
				snapshot.getParent().value);
	}

	@Test
	public void gettersNeverExposeBackingCopies() {
		DeletionSnapshot<MutableValue> snapshot =
				DeletionSnapshot.capture(
						new MutableValue("/book.fb2"),
						new MutableValue("/library"),
						MutableValue::new);

		MutableValue first = snapshot.getTarget();
		first.value = "/mutated";

		assertEquals(
				"/book.fb2",
				snapshot.getTarget().value);
		assertNotSame(first, snapshot.getTarget());
	}

	@Test
	public void missingTargetOrParentIsExplicit() {
		assertNull(DeletionSnapshot.capture(
				null,
				new MutableValue("/parent"),
				MutableValue::new));

		DeletionSnapshot<MutableValue> snapshot =
				DeletionSnapshot.capture(
						new MutableValue("/standalone.epub"),
						null,
						MutableValue::new);
		assertNull(snapshot.getParent());
	}

	@Test
	public void copierIsRequiredForCapturedValues() {
		assertThrows(
				IllegalArgumentException.class,
				() -> DeletionSnapshot.capture(
						new MutableValue("/book"),
						null,
						null));
	}

	private static final class MutableValue {
		private String value;

		private MutableValue(String value) {
			this.value = value;
		}

		private MutableValue(MutableValue other) {
			this.value = other.value;
		}
	}
}
