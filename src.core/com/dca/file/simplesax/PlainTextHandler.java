package com.dca.file.simplesax;

import java.util.HashMap;

/**
 * An element handler that does not allow any sub-elements.  If any are encountered
 * a warning is generated and they are ignored.
 */
public class PlainTextHandler extends AbstractElementHandler {
	public static final PlainTextHandler INSTANCE = new PlainTextHandler();

	private PlainTextHandler() {
	}

	@Override
	public ElementHandler openElement(String element, HashMap<String, String> attributes) {
		System.err.println("Unknown element " + element + ", ignoring.");
		return null;
	}

	@Override
	public void closeElement(String element, HashMap<String, String> attributes,
			String content) {
	}
}
