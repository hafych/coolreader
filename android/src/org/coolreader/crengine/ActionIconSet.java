/*
 * CoolReader for Android
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 */

package org.coolreader.crengine;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Immutable themed icon overrides owned by one Activity generation.
 */
final class ActionIconSet {
	private final Map<String, Integer> overrides;

	private ActionIconSet(Map<String, Integer> overrides) {
		this.overrides = Collections.unmodifiableMap(
				new HashMap<>(overrides));
	}

	static ActionIconSet empty() {
		return new ActionIconSet(Collections.emptyMap());
	}

	static Builder builder() {
		return new Builder();
	}

	int iconFor(ReaderAction action) {
		Integer override = overrides.get(action.id);
		return override != null ? override : action.iconId;
	}

	static final class Builder {
		private final Map<String, Integer> overrides = new HashMap<>();

		Builder override(ReaderAction action, int iconId) {
			if (iconId != 0)
				overrides.put(action.id, iconId);
			return this;
		}

		ActionIconSet build() {
			return new ActionIconSet(overrides);
		}
	}
}
