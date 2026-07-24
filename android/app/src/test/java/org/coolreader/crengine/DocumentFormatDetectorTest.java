package org.coolreader.crengine;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class DocumentFormatDetectorTest {
	@Test
	public void octetStreamEpubIsDetectedFromPackage() throws Exception {
		byte[] epub = zip(
				new Entry("mimetype", "application/epub+zip"),
				new Entry("META-INF/container.xml", "<container/>"));

		assertEquals(
				DocumentFormat.EPUB,
				DocumentFormatDetector.resolve(
						new ByteArrayInputStream(epub),
						"download.bin",
						"application/octet-stream"));
	}

	@Test
	public void octetStreamDoesNotTrustMisleadingExtension() throws Exception {
		byte[] executable = new byte[] {
				'M', 'Z', 0, 0, 3, 0, 0, 0, 4, 0, 0, 0
		};

		assertNull(DocumentFormatDetector.resolve(
				new ByteArrayInputStream(executable),
				"pretend.epub",
				"application/octet-stream"));
	}

	@Test
	public void genericFb2IsDetectedFromRootElement() throws Exception {
		byte[] fb2 = ("<?xml version=\"1.0\" encoding=\"utf-8\"?>"
				+ "<FictionBook xmlns=\"http://www.gribuser.ru/xml/fictionbook/2.0\">"
				+ "</FictionBook>").getBytes(StandardCharsets.UTF_8);

		assertEquals(
				DocumentFormat.FB2,
				DocumentFormatDetector.resolve(
						new ByteArrayInputStream(fb2),
						"document",
						null));
	}

	@Test
	public void genericTextRequiresTextExtension() throws Exception {
		byte[] text = "A plain text document.\nSecond line."
				.getBytes(StandardCharsets.UTF_8);

		assertEquals(
				DocumentFormat.TXT,
				DocumentFormatDetector.resolve(
						new ByteArrayInputStream(text),
						"notes.txt",
						"application/octet-stream"));
		assertNull(DocumentFormatDetector.resolve(
				new ByteArrayInputStream(text),
				"notes.bin",
				"application/octet-stream"));
	}

	@Test
	public void supportedSpecificMimeDoesNotRequireProbe() throws Exception {
		assertTrue(!DocumentFormatDetector.requiresContentInspection(
				"application/epub+zip"));
		assertEquals(
				DocumentFormat.EPUB,
				DocumentFormatDetector.resolve(
						null, "anything", "application/epub+zip"));
	}

	private static byte[] zip(Entry... entries) throws Exception {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		try (ZipOutputStream zip = new ZipOutputStream(output)) {
			for (Entry entry : entries) {
				zip.putNextEntry(new ZipEntry(entry.name));
				zip.write(entry.value.getBytes(StandardCharsets.UTF_8));
				zip.closeEntry();
			}
		}
		return output.toByteArray();
	}

	private static final class Entry {
		final String name;
		final String value;

		Entry(String name, String value) {
			this.name = name;
			this.value = value;
		}
	}
}
