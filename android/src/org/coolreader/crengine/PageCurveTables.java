package org.coolreader.crengine;

final class PageCurveTables {
	private final int[] sine;
	private final int[] arcsine;
	private final int[] sourceAngle;
	private final int[] destinationShift;

	PageCurveTables(int lastIndex, int scale) {
		if (lastIndex <= 0)
			throw new IllegalArgumentException(
					"Table last index must be positive");
		if (scale <= 0)
			throw new IllegalArgumentException(
					"Table scale must be positive");

		sine = new int[lastIndex + 1];
		arcsine = new int[lastIndex + 1];
		sourceAngle = new int[lastIndex + 1];
		destinationShift = new int[lastIndex + 1];
		for (int i = 0; i <= lastIndex; i++) {
			double angle = Math.PI / 2 * i / lastIndex;
			sine[i] = (int) Math.round(Math.sin(angle) * scale);
			double x = (double) i / lastIndex;
			arcsine[i] =
					(int) Math.round(Math.asin(x) * scale);

			double dx = i * (Math.PI / 2 - 1.0) / lastIndex;
			angle = shiftAngle(dx);
			sourceAngle[i] = (int) Math.round(angle * scale);
			destinationShift[i] =
					(int) Math.round(Math.sin(angle) * scale);
		}
	}

	int sine(int index) {
		return sine[index];
	}

	int arcsine(int index) {
		return arcsine[index];
	}

	int sourceAngle(int index) {
		return sourceAngle[index];
	}

	int destinationShift(int index) {
		return destinationShift[index];
	}

	private static double shiftAngle(double dx) {
		double lower = 0;
		double upper = Math.PI / 2;
		double candidate = 0;
		for (int i = 0; i < 15; i++) {
			candidate = (lower + upper) / 2;
			double shifted = candidate - Math.sin(candidate);
			if (shifted < dx)
				lower = candidate;
			else
				upper = candidate;
		}
		return candidate;
	}
}
