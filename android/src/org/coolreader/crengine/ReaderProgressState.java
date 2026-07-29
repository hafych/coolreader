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
				true, position, titleResource, title);
		return previous.active ? Change.UPDATE : Change.FIRST;
	}

	synchronized boolean hide() {
		if (!snapshot.active)
			return false;
		snapshot = Snapshot.HIDDEN;
		return true;
	}

	Snapshot snapshot() {
		return snapshot;
	}

	static final class Snapshot {
		private static final Snapshot HIDDEN =
				new Snapshot(false, -1, 0, null);

		private final boolean active;
		private final int position;
		private final int titleResource;
		private final String title;

		private Snapshot(
				boolean active,
				int position,
				int titleResource,
				String title) {
			this.active = active;
			this.position = position;
			this.titleResource = titleResource;
			this.title = title;
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
	}
}
