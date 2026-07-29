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
 * Reader-owned animation timing samples and pure progress arithmetic.
 */
final class AnimationTiming {
	private static final int MAX_PROGRESS = 10_000;
	private static final long MILLIS_PER_MINUTE = 60_000L;
	private static final long MAX_DRAW_DURATION = 1_000L;

	private final long[] samples;
	private final long initialAverage;
	private long sum;
	private int position;
	private int count;

	AnimationTiming(int sampleWindow, long initialAverage) {
		if (sampleWindow <= 0)
			throw new IllegalArgumentException(
					"animation sample window must be positive");
		if (initialAverage <= 0
				|| initialAverage > MAX_DRAW_DURATION)
			throw new IllegalArgumentException(
					"initial animation average is out of range");
		this.samples = new long[sampleWindow];
		this.initialAverage = initialAverage;
	}

	long averageDrawDuration() {
		return count == 0 ? initialAverage : sum / count;
	}

	boolean hasSamples() {
		return count > 0;
	}

	void resetSamples(long duration) {
		if (duration <= 0)
			duration = 1;
		else if (duration > MAX_DRAW_DURATION)
			duration = initialAverage;
		sum = duration * samples.length;
		position = 0;
		count = samples.length;
		for (int index = 0; index < samples.length; index++)
			samples[index] = duration;
	}

	void recordDrawDuration(long duration) {
		if (duration <= 0)
			duration = 1;
		else if (duration > MAX_DRAW_DURATION)
			return;
		if (count < samples.length) {
			count++;
		} else {
			sum -= samples[position];
		}
		samples[position] = duration;
		sum += duration;
		position++;
		if (position == samples.length)
			position = 0;
	}

	static double scrollStep(int step, int steps, boolean accelerated) {
		if (steps <= 0)
			throw new IllegalArgumentException(
					"animation step count must be positive");
		int clampedStep = Math.max(0, Math.min(step, steps));
		double progress = (double) clampedStep / steps;
		if (accelerated)
			progress += (1.0 - progress) * progress * progress;
		return progress;
	}

	static int autoscrollProgress(
			long elapsedMillis,
			int characterCount,
			int charactersPerMinute) {
		if (elapsedMillis <= 0
				|| characterCount <= 0
				|| charactersPerMinute <= 0) {
			return 0;
		}
		long estimatedDuration = Math.max(
				1L,
				MILLIS_PER_MINUTE
						* characterCount
						/ charactersPerMinute);
		if (elapsedMillis >= estimatedDuration)
			return MAX_PROGRESS;
		return (int) (MAX_PROGRESS
				* elapsedMillis
				/ estimatedDuration);
	}
}
