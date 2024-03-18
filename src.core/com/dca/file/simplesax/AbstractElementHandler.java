package com.dca.file.simplesax;

import java.util.HashMap;


import org.xml.sax.SAXException;


/**
 * An abstract base class for creating an ElementHandler.  This implements the close
 * methods so that warnings are generated for spurious content.
 * 
 */
public abstract class AbstractElementHandler implements ElementHandler {
	
	@Override
	public abstract ElementHandler openElement(String element,
			HashMap<String, String> attributes) throws SAXException;
	
	/**
	 * {@inheritDoc}
	 * <p>
	 * The default implementation is to add warnings for any textual content or attributes.
	 * This is useful for generating warnings for unknown XML attributes.
	 */
	@Override
	public void closeElement(String element, HashMap<String, String> attributes,
			String content) throws SAXException {
		
		if (!content.trim().equals("")) {
			System.err.println("Unknown text in element '" + element
					+ "', ignoring.");
		}
		if (!attributes.isEmpty()) {
			System.err.println("Unknown attributes in element '" + element
					+ "', ignoring.");
		}
	}
	
	/**
	 * {@inheritDoc}
	 * <p>
	 * The default implementation is a no-op.
	 */
	@Override
	public void endHandler(String element, HashMap<String, String> attributes,
			String content) throws SAXException {
		// No-op
	}
	
	
	/**
	 * Helper method for parsing a double value safely.
	 * 
	 * @param str		the string to parse
	 * @return			the double value, or NaN if an error occurred
	 */
	protected double parseDouble(String str) {
		try {
			return Double.parseDouble(str);
		} catch (NumberFormatException e) {
			return Double.NaN;
		}
	}
}
