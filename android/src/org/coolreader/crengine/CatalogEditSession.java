/*
 * CoolReader for Android
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 */

package org.coolreader.crengine;

/**
 * Owns confirmation and the one terminal action of an OPDS editor.
 */
final class CatalogEditSession {
	enum TerminalAction {
		SAVE,
		CANCEL,
		DELETE,
	}

	private boolean confirming;
	private TerminalAction terminalAction;

	synchronized boolean beginConfirmation() {
		if (confirming || terminalAction != null)
			return false;
		confirming = true;
		return true;
	}

	synchronized boolean claim(TerminalAction action) {
		if (action == null || terminalAction != null)
			return false;
		terminalAction = action;
		confirming = false;
		return true;
	}

	synchronized boolean cancelConfirmation() {
		if (!confirming || terminalAction != null)
			return false;
		confirming = false;
		return true;
	}

	synchronized boolean isConfirming() {
		return confirming && terminalAction == null;
	}

	synchronized boolean isTerminal() {
		return terminalAction != null;
	}

	synchronized TerminalAction getTerminalAction() {
		return terminalAction;
	}
}
