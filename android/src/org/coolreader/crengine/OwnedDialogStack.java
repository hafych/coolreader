/*
 * CoolReader for Android
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 */

package org.coolreader.crengine;

import java.util.ArrayList;
import java.util.List;

/**
 * Tracks nested Activity-owned dialogs from parent to current child.
 */
final class OwnedDialogStack<T> {
	private final ArrayList<T> dialogs = new ArrayList<>();

	synchronized void opened(T dialog) {
		if (dialog == null)
			throw new IllegalArgumentException(
					"dialog must not be null");
		dialogs.remove(dialog);
		dialogs.add(dialog);
	}

	synchronized void closed(T dialog) {
		dialogs.remove(dialog);
	}

	synchronized T current() {
		return dialogs.isEmpty()
				? null
				: dialogs.get(dialogs.size() - 1);
	}

	synchronized boolean isActive() {
		return !dialogs.isEmpty();
	}

	synchronized List<T> takeAllForClose() {
		ArrayList<T> result =
				new ArrayList<>(dialogs.size());
		for (int i = dialogs.size() - 1; i >= 0; i--)
			result.add(dialogs.get(i));
		dialogs.clear();
		return result;
	}
}
