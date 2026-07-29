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
 * Immutable piecewise-linear gesture acceleration curve.
 */
final class GestureAcceleration {
	private static final int SHAPE_SCALE = 1_000;
	private static final int INTERPOLATION_STEPS = 100;
	private final int[] shape;

	GestureAcceleration(int[] shape) {
		if (shape == null || shape.length < 2)
			throw new IllegalArgumentException(
					"acceleration shape needs at least two points");
		if (shape[0] != 0 || shape[shape.length - 1] != SHAPE_SCALE)
			throw new IllegalArgumentException(
					"acceleration shape must span 0.." + SHAPE_SCALE);
		for (int index = 1; index < shape.length; index++) {
			if (shape[index] < shape[index - 1]
					|| shape[index] > SHAPE_SCALE)
				throw new IllegalArgumentException(
						"acceleration shape must be monotonic");
		}
		this.shape = shape.clone();
	}

	static GestureAcceleration legacy() {
		return new GestureAcceleration(new int[]{
				0, 6, 24, 54, 95, 146, 206,
				273, 345, 421, 500, 578, 654,
				726, 793, 853, 904, 945, 975, 993, 1000
		});
	}

	int apply(int start, int end, int value) {
		if (end <= start)
			return start;
		long clamped = Math.max((long) start, Math.min((long) end, value));
		long range = (long) end - start;
		long intervals = shape.length - 1L;
		long position = INTERPOLATION_STEPS
				* intervals
				* (clamped - start)
				/ range;
		int interval = (int) Math.min(
				intervals, position / INTERPOLATION_STEPS);
		int part = (int) (position % INTERPOLATION_STEPS);
		long scaledShape;
		if (interval == intervals) {
			scaledShape =
					(long) SHAPE_SCALE * INTERPOLATION_STEPS;
		} else {
			scaledShape =
					(long) shape[interval] * INTERPOLATION_STEPS
							+ (long) (shape[interval + 1] - shape[interval])
							* part;
		}
		long result = start
				+ range * scaledShape
				/ (SHAPE_SCALE * INTERPOLATION_STEPS);
		return (int) result;
	}
}
