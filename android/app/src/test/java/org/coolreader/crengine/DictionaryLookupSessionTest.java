package org.coolreader.crengine;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class DictionaryLookupSessionTest {
	@Test
	public void normalizationTrimsOnlyOuterNonWordContent() {
		assertEquals(
				"reader's-guide",
				DictionaryLookupSession.normalizeQuery(
						"  …reader's-guide?! "));
	}

	@Test
	public void normalizationAcceptsSingleAndSupplementaryLetters() {
		assertEquals(
				"Я",
				DictionaryLookupSession.normalizeQuery("«Я»"));
		String supplementaryLetter =
				new String(Character.toChars(0x10400));
		assertEquals(
				supplementaryLetter,
				DictionaryLookupSession.normalizeQuery(
						"!" + supplementaryLetter + "?"));
	}

	@Test
	public void normalizationKeepsTrailingCombiningMarks() {
		String decomposed = "Cafe\u0301";

		assertEquals(
				decomposed,
				DictionaryLookupSession.normalizeQuery(
						"(" + decomposed + ")"));
	}

	@Test
	public void normalizationRejectsMissingWordCharacters() {
		assertNull(DictionaryLookupSession.normalizeQuery(null));
		assertNull(DictionaryLookupSession.normalizeQuery(""));
		assertNull(DictionaryLookupSession.normalizeQuery(" …!? "));
	}

	@Test
	public void replacementAndCompletionAreExact() {
		DictionaryLookupSession session =
				new DictionaryLookupSession();
		DictionaryLookupSession.Request stale =
				session.replace("old");
		DictionaryLookupSession.Request current =
				session.replace("current");

		assertFalse(session.isActive(stale));
		assertFalse(session.complete(stale));
		assertTrue(session.isActive(current));
		assertEquals("current", current.getQuery());
		assertTrue(session.complete(current));
		assertFalse(session.complete(current));
	}

	@Test
	public void nullQueryOwnsShowDictionaryRequest() {
		DictionaryLookupSession session =
				new DictionaryLookupSession();
		DictionaryLookupSession.Request request =
				session.replace(null);

		assertTrue(session.isActive(request));
		assertNull(request.getQuery());
	}

	@Test
	public void cancelAndCloseRejectLateWork() {
		DictionaryLookupSession session =
				new DictionaryLookupSession();
		DictionaryLookupSession.Request canceled =
				session.replace("query");
		session.cancel();
		session.cancel();

		assertFalse(session.isActive(canceled));
		DictionaryLookupSession.Request closed =
				session.replace("next");
		assertTrue(session.close());
		assertFalse(session.close());
		assertTrue(session.isClosed());
		assertFalse(session.isActive(closed));
		assertNull(session.replace("late"));
	}
}
