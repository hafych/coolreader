package org.coolreader.crengine;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class OptionsDialogRequestSessionTest {
	@Test
	public void replacementMakesOnlyLatestRequestActive() {
		OptionsDialogRequestSession<String> session =
				new OptionsDialogRequestSession<>();
		OptionsDialogRequestSession.Request<String> first =
				session.replace("browser");
		OptionsDialogRequestSession.Request<String> second =
				session.replace("tts");

		assertFalse(session.isActive(first));
		assertTrue(session.isActive(second));
		assertEquals("tts", second.getMode());
	}

	@Test
	public void completionClaimsExactRequestOnce() {
		OptionsDialogRequestSession<String> session =
				new OptionsDialogRequestSession<>();
		OptionsDialogRequestSession.Request<String> stale =
				session.replace("old");
		OptionsDialogRequestSession.Request<String> current =
				session.replace("current");

		assertFalse(session.complete(stale));
		assertTrue(session.isActive(current));
		assertTrue(session.complete(current));
		assertFalse(session.complete(current));
	}

	@Test
	public void nullModeIsRejectedWithoutReplacingOwner() {
		OptionsDialogRequestSession<String> session =
				new OptionsDialogRequestSession<>();
		OptionsDialogRequestSession.Request<String> current =
				session.replace("browser");

		assertNull(session.replace(null));
		assertTrue(session.isActive(current));
	}

	@Test
	public void cancelInvalidatesPendingAndAllowsReplacement() {
		OptionsDialogRequestSession<String> session =
				new OptionsDialogRequestSession<>();
		OptionsDialogRequestSession.Request<String> stale =
				session.replace("browser");

		session.cancel();
		session.cancel();

		assertFalse(session.isActive(stale));
		assertTrue(session.isActive(
				session.replace("replacement")));
	}

	@Test
	public void closePermanentlyRejectsLateAndNewWork() {
		OptionsDialogRequestSession<String> session =
				new OptionsDialogRequestSession<>();
		OptionsDialogRequestSession.Request<String> request =
				session.replace("browser");

		assertTrue(session.close());
		assertFalse(session.close());
		assertTrue(session.isClosed());
		assertFalse(session.isActive(request));
		assertFalse(session.complete(request));
		assertNull(session.replace("new"));
	}
}
