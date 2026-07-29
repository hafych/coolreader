package org.coolreader.db;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class ServiceBindingStateTest {
	@Test
	public void oneRegistrationCollectsCallbacksUntilConnection() {
		ServiceBindingState<Object> state =
				new ServiceBindingState<>();
		Runnable first = () -> { };
		Runnable second = () -> { };

		ServiceBindingState.BindRequest start =
				state.requestBind(first);
		ServiceBindingState.BindRequest wait =
				state.requestBind(second);
		Object binder = new Object();
		List<Runnable> callbacks =
				state.connected(start.getRegistration(), binder);

		assertTrue(start.shouldStartBinding());
		assertFalse(wait.shouldStartBinding());
		assertSame(start.getRegistration(), wait.getRegistration());
		assertEquals(2, callbacks.size());
		assertSame(first, callbacks.get(0));
		assertSame(second, callbacks.get(1));
		assertSame(binder, state.getBinder());
	}

	@Test
	public void connectedRequestRunsImmediatelyWithoutAnotherBind() {
		ServiceBindingState<Object> state =
				new ServiceBindingState<>();
		ServiceBindingState.BindRequest start =
				state.requestBind(null);
		Object binder = new Object();
		state.connected(start.getRegistration(), binder);
		Runnable callback = () -> { };

		ServiceBindingState.BindRequest ready =
				state.requestBind(callback);

		assertFalse(ready.shouldStartBinding());
		assertSame(callback, ready.getImmediateCallback());
		assertSame(start.getRegistration(), ready.getRegistration());
	}

	@Test
	public void failureDropsCallbacksAndAllowsFreshRetry() {
		ServiceBindingState<Object> state =
				new ServiceBindingState<>();
		ServiceBindingState.BindRequest failed =
				state.requestBind(() -> { });

		assertTrue(state.bindingFailed(failed.getRegistration()));
		ServiceBindingState.BindRequest retry =
				state.requestBind(null);

		assertTrue(retry.shouldStartBinding());
		assertFalse(state.isCurrent(failed.getRegistration()));
		assertTrue(state.isCurrent(retry.getRegistration()));
		assertFalse(state.bindingFailed(failed.getRegistration()));
	}

	@Test
	public void unbindRejectsLateConnectionAndDropsCallbacks() {
		ServiceBindingState<Object> state =
				new ServiceBindingState<>();
		ServiceBindingState.BindRequest old =
				state.requestBind(() -> { });

		assertSame(old.getRegistration(), state.unbind());
		assertTrue(
				state.connected(
						old.getRegistration(),
						new Object()).isEmpty());
		assertNull(state.getBinder());

		ServiceBindingState.BindRequest next =
				state.requestBind(null);
		assertTrue(next.shouldStartBinding());
		assertFalse(state.isCurrent(old.getRegistration()));
	}

	@Test
	public void disconnectKeepsRegistrationForPlatformReconnect() {
		ServiceBindingState<Object> state =
				new ServiceBindingState<>();
		ServiceBindingState.BindRequest start =
				state.requestBind(null);
		Object firstBinder = new Object();
		state.connected(start.getRegistration(), firstBinder);

		assertTrue(state.disconnected(start.getRegistration()));
		assertNull(state.getBinder());
		Runnable callback = () -> { };
		ServiceBindingState.BindRequest wait =
				state.requestBind(callback);
		Object replacementBinder = new Object();
		List<Runnable> callbacks =
				state.connected(
						start.getRegistration(),
						replacementBinder);

		assertFalse(wait.shouldStartBinding());
		assertEquals(1, callbacks.size());
		assertSame(callback, callbacks.get(0));
		assertTrue(
				state.isConnected(
						start.getRegistration(),
						replacementBinder));
		assertFalse(
				state.isConnected(
						start.getRegistration(),
						firstBinder));
	}
}
