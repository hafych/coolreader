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
 * Defines which native commands belong to one document interaction.
 */
final class ReaderEngineCommandPolicy {
	enum Scope {
		DOCUMENT,
		READER
	}

	private ReaderEngineCommandPolicy() {
	}

	static Scope scopeOf(ReaderCommand command) {
		return command
				== ReaderCommand.DCMD_SET_ROTATION_INFO_FOR_AA
						? Scope.READER
						: Scope.DOCUMENT;
	}

	static boolean movesDocument(ReaderCommand command) {
		if (command == null)
			return false;
		switch (command) {
			case DCMD_BEGIN:
			case DCMD_LINEUP:
			case DCMD_PAGEUP:
			case DCMD_PAGEDOWN:
			case DCMD_LINEDOWN:
			case DCMD_LINK_FORWARD:
			case DCMD_LINK_BACK:
			case DCMD_LINK_GO:
			case DCMD_END:
			case DCMD_GO_POS:
			case DCMD_GO_PAGE:
			case DCMD_BOOKMARK_GO_N:
			case DCMD_GO_PAGE_DONT_SAVE_HISTORY:
			case DCMD_MOVE_BY_CHAPTER:
			case DCMD_GO_SCROLL_POS:
			case DCMD_TOGGLE_PAGE_SCROLL_VIEW:
			case DCMD_SCROLL_BY:
			case DCMD_SELECT_FIRST_SENTENCE:
			case DCMD_SELECT_NEXT_SENTENCE:
			case DCMD_SELECT_PREV_SENTENCE:
			case DCMD_SELECT_MOVE_LEFT_BOUND_BY_WORDS:
			case DCMD_SELECT_MOVE_RIGHT_BOUND_BY_WORDS:
				return true;
			default:
				return false;
		}
	}
}
