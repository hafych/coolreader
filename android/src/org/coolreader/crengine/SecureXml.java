/*
 * CoolReader for Android
 *
 * XML parser factory with fail-closed XXE protection.
 */

package org.coolreader.crengine;

import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public final class SecureXml {
	private SecureXml() {
	}

	public static SAXParser newSaxParser() throws ParserConfigurationException, SAXException {
		try {
			SAXParserFactory factory = SAXParserFactory.newInstance();
			factory.setNamespaceAware(true);
			factory.setValidating(false);
			factory.setXIncludeAware(false);
			factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
			factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
			factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
			return factory.newSAXParser();
		} catch (UnsupportedOperationException e) {
			ParserConfigurationException failure =
					new ParserConfigurationException("Required secure XML feature is unavailable");
			failure.initCause(e);
			throw failure;
		}
	}
}
