/*
 * CoolReader for Android
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 */

package org.coolreader.crengine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.EnumSet;

import org.junit.Test;

public class ReaderEngineCommandPolicyTest {
	private static EnumSet<ReaderCommand> movementCommands() {
		return EnumSet.of(
					ReaderCommand.DCMD_BEGIN,
					ReaderCommand.DCMD_LINEUP,
					ReaderCommand.DCMD_PAGEUP,
					ReaderCommand.DCMD_PAGEDOWN,
					ReaderCommand.DCMD_LINEDOWN,
					ReaderCommand.DCMD_LINK_FORWARD,
					ReaderCommand.DCMD_LINK_BACK,
					ReaderCommand.DCMD_LINK_GO,
					ReaderCommand.DCMD_END,
					ReaderCommand.DCMD_GO_POS,
					ReaderCommand.DCMD_GO_PAGE,
					ReaderCommand.DCMD_BOOKMARK_GO_N,
					ReaderCommand
							.DCMD_GO_PAGE_DONT_SAVE_HISTORY,
					ReaderCommand.DCMD_MOVE_BY_CHAPTER,
					ReaderCommand.DCMD_GO_SCROLL_POS,
					ReaderCommand
							.DCMD_TOGGLE_PAGE_SCROLL_VIEW,
					ReaderCommand.DCMD_SCROLL_BY,
					ReaderCommand
							.DCMD_SELECT_FIRST_SENTENCE,
					ReaderCommand
							.DCMD_SELECT_NEXT_SENTENCE,
					ReaderCommand
							.DCMD_SELECT_PREV_SENTENCE,
					ReaderCommand
							.DCMD_SELECT_MOVE_LEFT_BOUND_BY_WORDS,
					ReaderCommand
							.DCMD_SELECT_MOVE_RIGHT_BOUND_BY_WORDS);
	}

	@Test
	public void onlyRotationMetadataIsReaderScoped() {
		for (ReaderCommand command : ReaderCommand.values()) {
			ReaderEngineCommandPolicy.Scope expected =
					command == ReaderCommand
							.DCMD_SET_ROTATION_INFO_FOR_AA
									? ReaderEngineCommandPolicy
											.Scope.READER
									: ReaderEngineCommandPolicy
											.Scope.DOCUMENT;
			assertEquals(
					command.name(),
					expected,
					ReaderEngineCommandPolicy.scopeOf(command));
		}
	}

	@Test
	public void unknownCommandDefaultsToDocumentScope() {
		assertEquals(
				ReaderEngineCommandPolicy.Scope.DOCUMENT,
				ReaderEngineCommandPolicy.scopeOf(null));
		assertFalse(
				ReaderEngineCommandPolicy.movesDocument(null));
	}

	@Test
	public void movementClassificationIsExhaustive() {
		EnumSet<ReaderCommand> movementCommands =
				movementCommands();
		for (ReaderCommand command : ReaderCommand.values()) {
			assertEquals(
					command.name(),
					movementCommands.contains(command),
					ReaderEngineCommandPolicy
							.movesDocument(command));
		}
	}

	@Test
	public void everyMovementCommandIsDocumentScoped() {
		for (ReaderCommand command : movementCommands()) {
			assertTrue(
					ReaderEngineCommandPolicy.scopeOf(command)
							== ReaderEngineCommandPolicy
									.Scope.DOCUMENT);
		}
	}
}
