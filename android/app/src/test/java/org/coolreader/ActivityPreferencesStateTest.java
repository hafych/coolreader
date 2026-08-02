package org.coolreader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.SharedPreferences;

import org.junit.Test;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Exercises the real {@link ActivityPreferencesState} exclusive-owner
 * lifecycle used by CoolReader for SharedPreferences access.
 */
public class ActivityPreferencesStateTest {
	@Test
	public void ensureLazilyCreatesOnce() {
		ActivityPreferencesState state =
				new ActivityPreferencesState();
		AtomicInteger creations = new AtomicInteger();
		SharedPreferences token = fakePreferences();
		SharedPreferences first = state.ensure(() -> {
			creations.incrementAndGet();
			return token;
		});
		SharedPreferences second = state.ensure(() -> {
			creations.incrementAndGet();
			return token;
		});
		assertSame(token, first);
		assertSame(first, second);
		assertEquals(1, creations.get());
		assertSame(first, state.get());
	}

	@Test
	public void closeIsPermanentAndDropsReference() {
		ActivityPreferencesState state =
				new ActivityPreferencesState();
		SharedPreferences token = fakePreferences();
		state.ensure(() -> token);
		assertTrue(state.close());
		assertTrue(state.isClosed());
		assertNull(state.get());
		assertNull(state.ensure(() -> token));
		assertFalse(state.close());
	}

	@Test
	public void nullFactoryRejectedWhileOpen() {
		ActivityPreferencesState state =
				new ActivityPreferencesState();
		try {
			state.ensure(null);
			fail("expected IllegalArgumentException");
		} catch (IllegalArgumentException expected) {
			// expected
		}
	}

	/**
	 * Identity-only SharedPreferences token. The owner never invokes
	 * preference methods; it only retains and publishes the reference.
	 */
	private static SharedPreferences fakePreferences() {
		return (SharedPreferences) Proxy.newProxyInstance(
				SharedPreferences.class.getClassLoader(),
				new Class<?>[] { SharedPreferences.class },
				(proxy, method, args) -> {
					String name = method.getName();
					if ("hashCode".equals(name))
						return System.identityHashCode(proxy);
					if ("equals".equals(name))
						return proxy == args[0];
					if ("toString".equals(name))
						return "ActivityPreferencesStateTest.fake";
					Class<?> returnType = method.getReturnType();
					if (returnType == boolean.class)
						return false;
					if (returnType == int.class)
						return 0;
					if (returnType == long.class)
						return 0L;
					if (returnType == float.class)
						return 0f;
					return null;
				});
	}
}
