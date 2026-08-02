package org.coolreader;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.coolreader.crengine.DocumentSource;
import org.junit.Test;

public class ExternalDocumentStateTest {
	@Test
	public void setAndClearTrackCurrentSource() {
		ExternalDocumentState state = new ExternalDocumentState();
		DocumentSource source =
				DocumentSource.fromLegacyLocation("/tmp/a.fb2");

		assertSame(source, state.set(source));
		assertTrue(state.isPresent());
		assertSame(source, state.get());

		state.clear();
		assertFalse(state.isPresent());
		assertNull(state.get());
	}

	@Test
	public void closeIsPermanentAndReleasesSource() {
		ExternalDocumentState state = new ExternalDocumentState();
		DocumentSource source =
				DocumentSource.fromLegacyLocation("/tmp/a.fb2");
		state.set(source);

		assertSame(source, state.close());
		assertTrue(state.isClosed());
		assertNull(state.get());
		assertFalse(state.isPresent());
		assertNull(state.set(
				DocumentSource.fromLegacyLocation("/tmp/b.fb2")));
		state.clear();
		assertNull(state.close());
	}
}
