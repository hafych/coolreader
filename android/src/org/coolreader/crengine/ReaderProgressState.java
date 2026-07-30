package org.coolreader.crengine;

final class ReaderProgressState {
	enum Change {
		NONE,
		FIRST,
		UPDATE
	}

	private volatile Snapshot snapshot = Snapshot.HIDDEN;

	synchronized Change show(
			int position,
			int titleResource,
			String title) {
		if (title == null)
			throw new IllegalArgumentException("title must not be null");
		Snapshot previous = snapshot;
		if (previous.active
				&& previous.position == position
				&& previous.titleResource == titleResource
				&& previous.title.equals(title)) {
			return Change.NONE;
		}
		snapshot = new Snapshot(
				true,
				position,
				titleResource,
				title,
				previous.cloudPosition);
		return previous.active ? Change.UPDATE : Change.FIRST;
	}

	synchronized boolean hide() {
		Snapshot previous = snapshot;
		if (!previous.active)
			return false;
		snapshot = new Snapshot(
				false,
				-1,
				0,
				null,
				previous.cloudPosition);
		return true;
	}

	synchronized Change showCloud(int position) {
		int normalizedPosition =
				Math.max(0, Math.min(10000, position));
		Snapshot previous = snapshot;
		if (previous.cloudPosition == normalizedPosition)
			return Change.NONE;
		snapshot = new Snapshot(
				previous.active,
				previous.position,
				previous.titleResource,
				previous.title,
				normalizedPosition);
		return previous.cloudPosition >= 0
				? Change.UPDATE
				: Change.FIRST;
	}

	synchronized boolean hideCloud() {
		Snapshot previous = snapshot;
		if (previous.cloudPosition < 0)
			return false;
		snapshot = new Snapshot(
				previous.active,
				previous.position,
				previous.titleResource,
				previous.title,
				-1);
		return true;
	}

	Snapshot snapshot() {
		return snapshot;
	}

	static final class Snapshot {
		private static final Snapshot HIDDEN =
				new Snapshot(false, -1, 0, null, -1);

		private final boolean active;
		private final int position;
		private final int titleResource;
		private final String title;
		private final int cloudPosition;

		private Snapshot(
				boolean active,
				int position,
				int titleResource,
				String title,
				int cloudPosition) {
			this.active = active;
			this.position = position;
			this.titleResource = titleResource;
			this.title = title;
			this.cloudPosition = cloudPosition;
		}

		boolean isActive() {
			return active;
		}

		int getPosition() {
			return position;
		}

		int getTitleResource() {
			return titleResource;
		}

		String getTitle() {
			return title;
		}

		boolean isCloudActive() {
			return cloudPosition >= 0;
		}

		int getCloudPosition() {
			return cloudPosition;
		}
	}
}
