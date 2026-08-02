package org.coolreader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.coolreader.crengine.ReaderView;
import org.coolreader.crengine.ReaderViewLayout;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Proves CoolReader reader teardown uses the closed-out {@link ReaderView}
 * identity from {@link ReaderUiOwner#close()}, not a post-close
 * {@code view()} lookup that is always null.
 */
public class ReaderUiTeardownTest {
	@Test
	public void closeForDestroyReturnsPriorAndNullsAccessor()
			throws Exception {
		ReaderUiOwner owner = new ReaderUiOwner();
		ReaderView installed = installSyntheticView(owner);

		assertSame(installed, owner.view());
		ReaderView closedOut =
				ReaderUiTeardown.closeForDestroy(owner);
		assertSame(installed, closedOut);
		assertNull(owner.view());
		assertTrue(owner.isClosed());
		// Second close must not invent another destroy target.
		assertNull(ReaderUiTeardown.closeForDestroy(owner));
	}

	@Test
	public void destroyClosedOutInvokesDestroyOnRetainedIdentity()
			throws Exception {
		ReaderUiOwner owner = new ReaderUiOwner();
		ReaderView installed = installSyntheticView(owner);
		ReaderView closedOut =
				ReaderUiTeardown.closeForDestroy(owner);

		AtomicReference<ReaderView> destroyed =
				new AtomicReference<>();
		AtomicInteger destroyCount = new AtomicInteger();
		ReaderUiTeardown.destroyClosedOut(closedOut, view -> {
			destroyed.set(view);
			destroyCount.incrementAndGet();
		});

		assertSame(installed, destroyed.get());
		assertSame(closedOut, destroyed.get());
		assertEquals(1, destroyCount.get());
		// Bug pattern: post-close view() is null so destroy would be skipped.
		assertNull(owner.view());
		ReaderUiTeardown.destroyClosedOut(
				owner.view(),
				view -> fail("must not destroy null view()"));
		assertEquals(1, destroyCount.get());
	}

	@Test
	public void destroyClosedOutIgnoresNullIdentityAndNullConsumer() {
		AtomicInteger destroyCount = new AtomicInteger();
		ReaderUiTeardown.destroyClosedOut(
				null, view -> destroyCount.incrementAndGet());
		assertEquals(0, destroyCount.get());
		// Null consumer must not throw.
		ReaderUiTeardown.destroyClosedOut(
				allocateWithoutConstructor(ReaderView.class), null);
	}

	@Test
	public void coolReaderOnDestroyWiresClosedOutDestroyNotViewLookup()
			throws Exception {
		String source = readCoolReaderSource();
		int onDestroy = source.indexOf("void onDestroy()");
		assertTrue("onDestroy not found", onDestroy >= 0);
		int bodyStart = source.indexOf('{', onDestroy);
		int bodyEnd = findMatchingBrace(source, bodyStart);
		String body = source.substring(bodyStart, bodyEnd + 1);

		assertTrue(
				"onDestroy must close reader UI via ReaderUiTeardown",
				body.contains("ReaderUiTeardown.closeForDestroy"));
		assertTrue(
				"onDestroy must destroy the closed-out identity",
				body.contains("ReaderUiTeardown.destroyClosedOut")
						&& body.contains("closingReader"));
		assertFalse(
				"onDestroy must not destroy via post-close view() lookup",
				body.contains("readerUi.view().destroy()"));
		assertFalse(
				"onDestroy must not re-query view() for destroy",
				body.contains("if (readerUi.view() != null)")
						|| body.contains(
						"if (readerUi.view()!= null)"));
	}

	/**
	 * Places a synthetic ReaderView into the real owner fields so close()/view()
	 * exercise the shipped ReaderUiOwner path without full ReaderView setup.
	 */
	private static ReaderView installSyntheticView(ReaderUiOwner owner)
			throws Exception {
		ReaderView synthetic =
				allocateWithoutConstructor(ReaderView.class);
		ReaderViewLayout frame =
				allocateWithoutConstructor(ReaderViewLayout.class);
		Field viewField =
				ReaderUiOwner.class.getDeclaredField("readerView");
		viewField.setAccessible(true);
		Field frameField =
				ReaderUiOwner.class.getDeclaredField("readerFrame");
		frameField.setAccessible(true);
		viewField.set(owner, synthetic);
		frameField.set(owner, frame);
		return synthetic;
	}

	@SuppressWarnings("unchecked")
	private static <T> T allocateWithoutConstructor(Class<T> type) {
		try {
			Constructor<Object> objectCtor =
					Object.class.getDeclaredConstructor();
			objectCtor.setAccessible(true);
			Class<?> rf = Class.forName(
					"sun.reflect.ReflectionFactory");
			Object factory = rf.getMethod("getReflectionFactory")
					.invoke(null);
			Constructor<?> serialCtor =
					(Constructor<?>) rf.getMethod(
									"newConstructorForSerialization",
									Class.class,
									Constructor.class)
							.invoke(factory, type, objectCtor);
			serialCtor.setAccessible(true);
			return (T) serialCtor.newInstance();
		} catch (ReflectiveOperationException primary) {
			try {
				Class<?> unsafeClass =
						Class.forName("sun.misc.Unsafe");
				Field theUnsafe =
						unsafeClass.getDeclaredField("theUnsafe");
				theUnsafe.setAccessible(true);
				Object unsafe = theUnsafe.get(null);
				return (T) unsafeClass
						.getMethod("allocateInstance", Class.class)
						.invoke(unsafe, type);
			} catch (ReflectiveOperationException secondary) {
				AssertionError error = new AssertionError(
						"Cannot allocate "
								+ type.getName()
								+ " without constructor for owner test");
				error.addSuppressed(primary);
				error.addSuppressed(secondary);
				throw error;
			}
		}
	}

	private static String readCoolReaderSource() throws IOException {
		Path[] candidates = new Path[] {
				Paths.get("src/org/coolreader/CoolReader.java"),
				Paths.get("../src/org/coolreader/CoolReader.java"),
				Paths.get("android/src/org/coolreader/CoolReader.java"),
				Paths.get(
						System.getProperty("user.dir", "."),
						"src/org/coolreader/CoolReader.java"),
				Paths.get(
						System.getProperty("user.dir", "."),
						"../src/org/coolreader/CoolReader.java"),
		};
		for (Path path : candidates) {
			if (Files.isRegularFile(path))
				return new String(
						Files.readAllBytes(path),
						StandardCharsets.UTF_8);
		}
		fail("CoolReader.java not found from "
				+ Paths.get("").toAbsolutePath());
		return "";
	}

	private static int findMatchingBrace(String source, int openIndex) {
		int depth = 0;
		for (int i = openIndex; i < source.length(); i++) {
			char c = source.charAt(i);
			if (c == '{')
				depth++;
			else if (c == '}') {
				depth--;
				if (depth == 0)
					return i;
			}
		}
		return source.length() - 1;
	}
}
