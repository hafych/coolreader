package org.coolreader.crengine;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class CatalogEditSessionTest {
	@Test
	public void confirmationCanStartOnlyOnce() {
		CatalogEditSession session =
				new CatalogEditSession();

		assertTrue(session.beginConfirmation());
		assertTrue(session.isConfirming());
		assertFalse(session.beginConfirmation());
		assertFalse(session.isTerminal());
	}

	@Test
	public void dismissedConfirmationReturnsToOpenEditor() {
		CatalogEditSession session =
				new CatalogEditSession();
		session.beginConfirmation();

		assertTrue(session.cancelConfirmation());
		assertFalse(session.cancelConfirmation());
		assertFalse(session.isConfirming());
		assertTrue(session.beginConfirmation());
	}

	@Test
	public void confirmationAcceptClaimsSave() {
		CatalogEditSession session =
				new CatalogEditSession();
		session.beginConfirmation();

		assertTrue(session.claim(
				CatalogEditSession.TerminalAction.SAVE));
		assertFalse(session.isConfirming());
		assertTrue(session.isTerminal());
		assertEquals(
				CatalogEditSession.TerminalAction.SAVE,
				session.getTerminalAction());
	}

	@Test
	public void confirmationRejectClaimsCancel() {
		CatalogEditSession session =
				new CatalogEditSession();
		session.beginConfirmation();

		assertTrue(session.claim(
				CatalogEditSession.TerminalAction.CANCEL));
		assertEquals(
				CatalogEditSession.TerminalAction.CANCEL,
				session.getTerminalAction());
	}

	@Test
	public void firstTerminalActionWins() {
		CatalogEditSession session =
				new CatalogEditSession();

		assertTrue(session.claim(
				CatalogEditSession.TerminalAction.DELETE));
		assertFalse(session.claim(
				CatalogEditSession.TerminalAction.SAVE));
		assertFalse(session.beginConfirmation());
		assertEquals(
				CatalogEditSession.TerminalAction.DELETE,
				session.getTerminalAction());
		assertFalse(session.claim(null));
	}

	@Test
	public void newSessionHasNoTerminalAction() {
		CatalogEditSession session =
				new CatalogEditSession();

		assertNull(session.getTerminalAction());
		assertFalse(session.isTerminal());
		assertFalse(session.isConfirming());
	}
}
