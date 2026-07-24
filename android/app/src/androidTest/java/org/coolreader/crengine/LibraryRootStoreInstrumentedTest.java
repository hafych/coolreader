package org.coolreader.crengine;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class LibraryRootStoreInstrumentedTest {
	private SharedPreferences preferences;
	private FakePermissionAccess permissions;
	private LibraryRootStore store;

	@Before
	public void setUp() {
		Context context = InstrumentationRegistry.getInstrumentation()
				.getTargetContext();
		preferences = context.getSharedPreferences(
				LibraryRootStore.PREFS_NAME, Context.MODE_PRIVATE);
		preferences.edit().clear().commit();
		permissions = new FakePermissionAccess();
		store = new LibraryRootStore(
				preferences,
				permissions,
				uri -> "root-a".equals(uri.getLastPathSegment())
						? "Books A" : "Books B");
	}

	@After
	public void tearDown() {
		preferences.edit().clear().commit();
	}

	@Test
	public void rootsCanBeReplacedAndRemovedWithoutFileOperations() {
		Uri rootA = Uri.parse("content://test/tree/root-a");
		Uri rootB = Uri.parse("content://test/tree/root-b");

		assertFalse(store.addOrReplace(null, rootA));
		permissions.granted.add(rootA);
		assertTrue(store.addOrReplace(null, rootA));

		List<LibraryRootStore.Entry> roots = store.getRoots();
		assertEquals(1, roots.size());
		assertEquals("Books A", roots.get(0).getLabel());
		assertTrue(roots.get(0).isAccessGranted());

		permissions.granted.remove(rootA);
		assertFalse(store.getRoots().get(0).isAccessGranted());

		permissions.granted.add(rootB);
		assertTrue(store.addOrReplace(rootA, rootB));
		assertEquals(1, store.getRoots().size());
		assertEquals(rootB, store.getRoots().get(0).getUri());
		assertEquals(rootA, permissions.released.get(0));

		assertTrue(store.remove(rootB));
		assertTrue(store.getRoots().isEmpty());
		assertEquals(rootB, permissions.released.get(1));
	}

	private static final class FakePermissionAccess
			implements LibraryRootStore.PermissionAccess {
		final Set<Uri> granted = new HashSet<>();
		final List<Uri> released = new ArrayList<>();

		@Override
		public boolean hasReadPermission(Uri uri) {
			return granted.contains(uri);
		}

		@Override
		public void releaseReadPermission(Uri uri) {
			released.add(uri);
			granted.remove(uri);
		}
	}
}
