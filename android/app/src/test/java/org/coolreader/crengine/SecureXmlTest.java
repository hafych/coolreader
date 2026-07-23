package org.coolreader.crengine;

import org.junit.Test;
import org.xml.sax.helpers.DefaultHandler;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

public class SecureXmlTest {
	@Test(expected = Exception.class)
	public void rejectsDoctypeDeclarations() throws Exception {
		String xml = "<?xml version=\"1.0\"?>"
				+ "<!DOCTYPE root [<!ENTITY xxe SYSTEM \"file:///etc/passwd\">]>"
				+ "<root>&xxe;</root>";
		SecureXml.newSaxParser().parse(
				new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)),
				new DefaultHandler());
	}
}
