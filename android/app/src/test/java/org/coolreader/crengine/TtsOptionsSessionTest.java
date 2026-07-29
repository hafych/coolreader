package org.coolreader.crengine;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class TtsOptionsSessionTest {
	@Test
	public void replacementInvalidatesOnlyTheSameChannel() {
		TtsOptionsSession session = new TtsOptionsSession();
		TtsOptionsSession.Request firstVoices =
				session.replace(TtsOptionsSession.Channel.VOICES);
		TtsOptionsSession.Request locales =
				session.replace(TtsOptionsSession.Channel.LOCALES);
		TtsOptionsSession.Request secondVoices =
				session.replace(TtsOptionsSession.Channel.VOICES);

		assertFalse(session.isActive(firstVoices));
		assertTrue(session.isActive(locales));
		assertTrue(session.isActive(secondVoices));
		assertFalse(session.complete(firstVoices));
		assertTrue(session.complete(secondVoices));
		assertTrue(session.complete(locales));
	}

	@Test
	public void completionClearsOnlyItsExactRequest() {
		TtsOptionsSession session = new TtsOptionsSession();
		TtsOptionsSession.Request engines =
				session.replace(TtsOptionsSession.Channel.ENGINES);

		assertNotNull(engines);
		assertTrue(session.complete(engines));
		assertFalse(session.complete(engines));
		assertFalse(session.isActive(engines));
	}

	@Test
	public void cancelInvalidatesOnlyTheSelectedChannel() {
		TtsOptionsSession session = new TtsOptionsSession();
		TtsOptionsSession.Request locales =
				session.replace(TtsOptionsSession.Channel.LOCALES);
		TtsOptionsSession.Request voices =
				session.replace(TtsOptionsSession.Channel.VOICES);

		session.cancel(TtsOptionsSession.Channel.VOICES);

		assertTrue(session.isActive(locales));
		assertFalse(session.isActive(voices));
		assertFalse(session.complete(voices));
		assertTrue(session.complete(locales));
	}

	@Test
	public void closeInvalidatesAllChannelsAndRejectsNewWork() {
		TtsOptionsSession session = new TtsOptionsSession();
		TtsOptionsSession.Request engines =
				session.replace(TtsOptionsSession.Channel.ENGINES);
		TtsOptionsSession.Request initialization =
				session.replace(
						TtsOptionsSession.Channel.INITIALIZATION);

		assertTrue(session.close());
		assertTrue(session.isClosed());
		assertFalse(session.isActive(engines));
		assertFalse(session.complete(initialization));
		assertFalse(session.close());
		assertNull(session.replace(TtsOptionsSession.Channel.VOICES));
	}
}
