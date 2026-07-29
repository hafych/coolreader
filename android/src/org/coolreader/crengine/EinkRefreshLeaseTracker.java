package org.coolreader.crengine;

import java.util.HashSet;
import java.util.Set;

final class EinkRefreshLeaseTracker {
	private final Set<Integer> clients = new HashSet<>();
	private Integer savedInterval;

	synchronized boolean acquire(int clientId, int currentInterval) {
		if (!clients.add(clientId))
			return false;
		if (clients.size() > 1)
			return false;
		savedInterval = currentInterval;
		return true;
	}

	synchronized Integer release(int clientId) {
		if (!clients.remove(clientId) || !clients.isEmpty())
			return null;
		Integer intervalToRestore = savedInterval;
		savedInterval = null;
		return intervalToRestore;
	}

	synchronized boolean isActive() {
		return !clients.isEmpty();
	}
}
