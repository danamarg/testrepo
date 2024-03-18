package com.dca.file.simplesax;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xml.sax.SAXException;

import com.dca.file.AthenaStandardInputDocument;
import com.dca.file.DocumentLoadingContext;

/**
 * The starting point of the handlers.  Accepts a single <Athena> element and hands
 * the contents to be read by a ASIFContentHandler.
 */
public class ASIFHandler extends AbstractElementHandler {
	private final DocumentLoadingContext context;
	private ASIFContentHandler handler = null;

	public ASIFHandler(DocumentLoadingContext context) {
		this.context = context;
	}

	/**
	 * Return the OpenRocketDocument read from the file, or <code>null</code> if a document
	 * has not been read yet.
	 * 
	 * @return	the document read, or null.
	 */
	public AthenaStandardInputDocument getDocument() {
		return handler.getDocument();
	}

	@Override
	public ElementHandler openElement(String element, HashMap<String, String> attributes) {

		// Check for unknown elements
		if (!element.equals("openrocket")) {
			System.err.println("Unknown element " + element + ", ignoring.");
			return null;
		}

		// Check for first call
		if (handler != null) {
			System.err.println("Multiple document elements found, ignoring later "
					+ "ones.");
			return null;
		}

		// Check version number
		String version = null;
		String creator = attributes.remove("creator");
		String docVersion = attributes.remove("version");
		for (String v : DocumentConfig.SUPPORTED_VERSIONS) {
			if (v.equals(docVersion)) {
				version = v;
				break;
			}
		}
		if (version == null) {
			String str = "Unsupported document version";
			if (docVersion != null)
				str += " " + docVersion;
			if (creator != null && !creator.trim().equals(""))
				str += " (written using '" + creator.trim() + "')";
			str += ", attempting to read file anyway.";
		}

		context.setFileVersion(parseVersion(docVersion));

		handler = new ASIFContentHandler(context);
		return handler;
	}


	private int parseVersion(String docVersion) {
		if (docVersion == null)
			return 0;

		Matcher m = Pattern.compile("^([0-9]+)\\.([0-9]+)$").matcher(docVersion);
		if (m.matches()) {
			int major = Integer.parseInt(m.group(1));
			int minor = Integer.parseInt(m.group(2));
			return major * DocumentConfig.FILE_VERSION_DIVISOR + minor;
		} else {
			return 0;
		}
	}

	@Override
	public void closeElement(String element, HashMap<String, String> attributes,
			String content) throws SAXException {
		attributes.remove("version");
		attributes.remove("creator");
		super.closeElement(element, attributes, content);
	}


}

