/*
 * CoolReader for Android
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 */

package org.coolreader.crengine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

public class ExternalDocumentValidatorTest {
	@Rule
	public final TemporaryFolder temporaryFolder =
			new TemporaryFolder();

	private final ExternalDocumentValidator validator =
			new ExternalDocumentValidator();

	@Test
	public void contentUriRemainsResolverOwned() {
		DocumentSource source = DocumentSource.contentUri(
				"content://provider/books/7",
				false);

		assertSame(
				source,
				validator.validate(
						source,
						"application/octet-stream"));
	}

	@Test
	public void specificMimeAddsFormatWithoutOpeningLocalFile() {
		DocumentSource source = DocumentSource.file(
				new File(
						temporaryFolder.getRoot(),
						"not-created.bin")
						.getAbsolutePath());

		DocumentSource validated = validator.validate(
				source,
				"application/epub+zip");

		assertEquals(DocumentFormat.EPUB, validated.getFormat());
		assertEquals("application/epub+zip", validated.getMimeType());
	}

	@Test
	public void genericMimeProbesLocalContent() throws Exception {
		File file = temporaryFolder.newFile("book.bin");
		byte[] body = ("<?xml version=\"1.0\"?>"
				+ "<FictionBook></FictionBook>")
				.getBytes(StandardCharsets.UTF_8);
		try (FileOutputStream output =
					new FileOutputStream(file)) {
			output.write(body);
		}

		DocumentSource validated = validator.validate(
				DocumentSource.file(file.getAbsolutePath()),
				"application/octet-stream");

		assertEquals(DocumentFormat.FB2, validated.getFormat());
		assertEquals(body.length, validated.getSize());
	}

	@Test
	public void unreadableOrUnsupportedGenericSourceIsRejected()
			throws Exception {
		File file = temporaryFolder.newFile("binary.bin");
		try (FileOutputStream output =
					new FileOutputStream(file)) {
			output.write(new byte[] {'M', 'Z', 0, 0, 1, 2, 3});
		}

		assertNull(validator.validate(
				DocumentSource.file(file.getAbsolutePath()),
				"application/octet-stream"));
		assertNull(validator.validate(
				DocumentSource.file(
						new File(
								temporaryFolder.getRoot(),
								"missing.bin")
								.getAbsolutePath()),
				"application/octet-stream"));
	}
}
