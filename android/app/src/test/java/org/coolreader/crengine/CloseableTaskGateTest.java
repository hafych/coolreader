package org.coolreader.crengine;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class CloseableTaskGateTest {
	@Test
	public void replacementInvalidatesOnlyThePreviousGeneration() {
		CloseableTaskGate gate = new CloseableTaskGate();
		CloseableTaskGate.Token first = gate.replace();
		CloseableTaskGate.Token second = gate.replace();

		assertFalse(gate.isActive(first));
		assertTrue(gate.isActive(second));
		assertFalse(gate.isClosed());
	}

	@Test
	public void cancelIsIdempotentAndAllowsAnotherGeneration() {
		CloseableTaskGate gate = new CloseableTaskGate();
		CloseableTaskGate.Token first = gate.replace();

		gate.cancel();
		gate.cancel();

		assertFalse(gate.isActive(first));
		CloseableTaskGate.Token second = gate.replace();
		assertNotNull(second);
		assertTrue(gate.isActive(second));
	}

	@Test
	public void completionClearsOnlyItsExactGeneration() {
		CloseableTaskGate gate = new CloseableTaskGate();
		CloseableTaskGate.Token stale = gate.replace();
		CloseableTaskGate.Token current = gate.replace();

		assertFalse(gate.complete(stale));
		assertTrue(gate.isActive(current));
		assertTrue(gate.complete(current));
		assertFalse(gate.complete(current));
		assertFalse(gate.isActive(current));
	}

	@Test
	public void nullIsNeverAnActiveGeneration() {
		CloseableTaskGate gate = new CloseableTaskGate();

		assertFalse(gate.isActive(null));
		assertFalse(gate.complete(null));
	}

	@Test
	public void closePermanentlyRejectsWork() {
		CloseableTaskGate gate = new CloseableTaskGate();
		CloseableTaskGate.Token token = gate.replace();

		assertTrue(gate.close());
		assertFalse(gate.close());
		assertTrue(gate.isClosed());
		assertFalse(gate.isActive(token));
		assertNull(gate.replace());
	}
}
