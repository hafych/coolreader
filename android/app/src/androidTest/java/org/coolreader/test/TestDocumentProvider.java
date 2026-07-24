package org.coolreader.test;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class TestDocumentProvider extends ContentProvider {
	public static final String AUTHORITY = "org.coolreader.test.documents";
	public static final Uri PERSISTED_BOOK =
			Uri.parse("content://" + AUTHORITY + "/persisted.fb2");
	public static final Uri TEMPORARY_BOOK =
			Uri.parse("content://" + AUTHORITY + "/temporary.fb2");

	private static final byte[] BOOK_BYTES = (
			"<?xml version=\"1.0\" encoding=\"utf-8\"?>"
					+ "<FictionBook xmlns=\"http://www.gribuser.ru/xml/fictionbook/2.0\">"
					+ "<description><title-info><genre>fiction</genre>"
					+ "<author><first-name>Smoke</first-name>"
					+ "<last-name>Test</last-name></author>"
					+ "<book-title>Persisted URI smoke test</book-title>"
					+ "<lang>en</lang></title-info></description>"
					+ "<body><section><title><p>Test</p></title>"
					+ "<p>Content opened through a persistable URI grant.</p>"
					+ "</section></body></FictionBook>")
			.getBytes(StandardCharsets.UTF_8);

	@Override
	public boolean onCreate() {
		return true;
	}

	@Override
	public String getType(Uri uri) {
		return "application/octet-stream";
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
						String[] selectionArgs, String sortOrder) {
		String[] columns = projection != null && projection.length > 0
				? projection
				: new String[] {OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE};
		MatrixCursor cursor = new MatrixCursor(columns, 1);
		MatrixCursor.RowBuilder row = cursor.newRow();
		for (String column : columns) {
			if (OpenableColumns.DISPLAY_NAME.equals(column))
				row.add(column, displayName(uri));
			else if (OpenableColumns.SIZE.equals(column))
				row.add(column, BOOK_BYTES.length);
			else
				row.add(column, null);
		}
		return cursor;
	}

	@Override
	public ParcelFileDescriptor openFile(Uri uri, String mode)
			throws FileNotFoundException {
		if (!"r".equals(mode))
			throw new FileNotFoundException("Test provider is read-only");
		File file = new File(
				providerContext().getCacheDir(), "provider-" + displayName(uri));
		try (FileOutputStream output = new FileOutputStream(file)) {
			output.write(BOOK_BYTES);
			output.getFD().sync();
		} catch (IOException e) {
			FileNotFoundException failure =
					new FileNotFoundException("Cannot create test document");
			failure.initCause(e);
			throw failure;
		}
		return ParcelFileDescriptor.open(
				file, ParcelFileDescriptor.MODE_READ_ONLY);
	}

	private android.content.Context providerContext() {
		android.content.Context context = getContext();
		if (context == null)
			throw new IllegalStateException("Provider has no context");
		return context;
	}

	private String displayName(Uri uri) {
		String name = uri != null ? uri.getLastPathSegment() : null;
		return name != null && name.endsWith(".fb2") ? name : "test.fb2";
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		throw new UnsupportedOperationException("read-only");
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		throw new UnsupportedOperationException("read-only");
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
					  String[] selectionArgs) {
		throw new UnsupportedOperationException("read-only");
	}
}
