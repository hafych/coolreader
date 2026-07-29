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
 * Generic clone-on-boundary target and parent captured before confirmation.
 */
public final class DeletionSnapshot<T> {
	public interface Copier<T> {
		T copy(T value);
	}

	private final Copier<T> copier;
	private final T target;
	private final T parent;

	private DeletionSnapshot(
			Copier<T> copier,
			T target,
			T parent) {
		this.copier = copier;
		this.target = target;
		this.parent = parent;
	}

	public static <T> DeletionSnapshot<T> capture(
			T target,
			T parent,
			Copier<T> copier) {
		if (target == null)
			return null;
		if (copier == null)
			throw new IllegalArgumentException(
					"copier must not be null");
		return new DeletionSnapshot<>(
				copier,
				copier.copy(target),
				parent != null ? copier.copy(parent) : null);
	}

	public T getTarget() {
		return copier.copy(target);
	}

	public T getParent() {
		return parent != null ? copier.copy(parent) : null;
	}
}
