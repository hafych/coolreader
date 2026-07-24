/*
 * CoolReader for Android
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 */

package org.coolreader.crengine;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.UriPermission;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Persists user-selected Storage Access Framework library roots.
 *
 * Removing a root only forgets the URI grant. It never modifies documents in
 * the selected directory.
 */
public final class LibraryRootStore {
	static final String PREFS_NAME = "CR3LibraryRoots";
	static final String KEY_ROOT_URIS = "rootUris";
	private static final String KEY_LABEL_PREFIX = "label.";

	interface PermissionAccess {
		boolean hasReadPermission(Uri uri);
		void releaseReadPermission(Uri uri);
	}

	interface LabelResolver {
		String resolveLabel(Uri uri);
	}

	public static final class Entry {
		private final Uri uri;
		private final String label;
		private final boolean accessGranted;

		Entry(Uri uri, String label, boolean accessGranted) {
			this.uri = uri;
			this.label = label;
			this.accessGranted = accessGranted;
		}

		public Uri getUri() {
			return uri;
		}

		public String getLabel() {
			return label;
		}

		public boolean isAccessGranted() {
			return accessGranted;
		}
	}

	private final SharedPreferences preferences;
	private final PermissionAccess permissionAccess;
	private final LabelResolver labelResolver;

	public LibraryRootStore(Context context) {
		Context appContext = context.getApplicationContext();
		ContentResolver resolver = appContext.getContentResolver();
		preferences = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
		permissionAccess = new PermissionAccess() {
			@Override
			public boolean hasReadPermission(Uri uri) {
				for (UriPermission permission : resolver.getPersistedUriPermissions()) {
					if (permission.isReadPermission() && uri.equals(permission.getUri()))
						return true;
				}
				return false;
			}

			@Override
			public void releaseReadPermission(Uri uri) {
				try {
					resolver.releasePersistableUriPermission(
							uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
				} catch (SecurityException ignored) {
					// A revoked grant is already in the desired state.
				}
			}
		};
		labelResolver = uri -> {
			try (Cursor cursor = resolver.query(
					uri,
					new String[] {DocumentsContract.Document.COLUMN_DISPLAY_NAME},
					null, null, null)) {
				if (cursor != null && cursor.moveToFirst()) {
					int column = cursor.getColumnIndex(
							DocumentsContract.Document.COLUMN_DISPLAY_NAME);
					if (column >= 0) {
						String label = cursor.getString(column);
						if (label != null && !label.trim().isEmpty())
							return label.trim();
					}
				}
			} catch (RuntimeException ignored) {
				// The cached label remains usable if the provider is unavailable.
			}
			return fallbackLabel(uri);
		};
	}

	LibraryRootStore(SharedPreferences preferences,
					 PermissionAccess permissionAccess,
					 LabelResolver labelResolver) {
		this.preferences = preferences;
		this.permissionAccess = permissionAccess;
		this.labelResolver = labelResolver;
	}

	public List<Entry> getRoots() {
		Set<String> saved = preferences.getStringSet(
				KEY_ROOT_URIS, Collections.emptySet());
		List<Entry> result = new ArrayList<>(saved.size());
		for (String value : saved) {
			Uri uri = Uri.parse(value);
			String label = preferences.getString(labelKey(uri), null);
			if (label == null || label.trim().isEmpty())
				label = fallbackLabel(uri);
			result.add(new Entry(
					uri, label, permissionAccess.hasReadPermission(uri)));
		}
		Collections.sort(result, (left, right) ->
				String.CASE_INSENSITIVE_ORDER.compare(
						left.label, right.label));
		return result;
	}

	/**
	 * Adds a root, or atomically replaces a root whose access was lost.
	 */
	public boolean addOrReplace(Uri previousUri, Uri selectedUri) {
		if (selectedUri == null
				|| !ContentResolver.SCHEME_CONTENT.equalsIgnoreCase(
						selectedUri.getScheme())
				|| !permissionAccess.hasReadPermission(selectedUri))
			return false;

		Set<String> roots = mutableRoots();
		if (previousUri != null)
			roots.remove(previousUri.toString());
		roots.add(selectedUri.toString());

		SharedPreferences.Editor editor = preferences.edit()
				.putStringSet(KEY_ROOT_URIS, roots)
				.putString(labelKey(selectedUri),
						labelResolver.resolveLabel(selectedUri));
		if (previousUri != null && !previousUri.equals(selectedUri))
			editor.remove(labelKey(previousUri));
		if (!editor.commit())
			return false;

		if (previousUri != null && !previousUri.equals(selectedUri))
			permissionAccess.releaseReadPermission(previousUri);
		return true;
	}

	/**
	 * Forgets a root and its persisted read grant without deleting user files.
	 */
	public boolean remove(Uri uri) {
		if (uri == null)
			return false;
		Set<String> roots = mutableRoots();
		if (!roots.remove(uri.toString()))
			return false;
		boolean saved = preferences.edit()
				.putStringSet(KEY_ROOT_URIS, roots)
				.remove(labelKey(uri))
				.commit();
		if (saved)
			permissionAccess.releaseReadPermission(uri);
		return saved;
	}

	private Set<String> mutableRoots() {
		return new HashSet<>(preferences.getStringSet(
				KEY_ROOT_URIS, Collections.emptySet()));
	}

	private static String labelKey(Uri uri) {
		String encoded = Base64.encodeToString(
				uri.toString().getBytes(StandardCharsets.UTF_8),
				Base64.NO_WRAP | Base64.URL_SAFE);
		return KEY_LABEL_PREFIX + encoded;
	}

	private static String fallbackLabel(Uri uri) {
		String segment = uri != null ? uri.getLastPathSegment() : null;
		if (segment == null || segment.isEmpty())
			return "Library folder";
		String decoded = Uri.decode(segment);
		int separator = decoded.lastIndexOf(':');
		return separator >= 0 && separator + 1 < decoded.length()
				? decoded.substring(separator + 1) : decoded;
	}
}
