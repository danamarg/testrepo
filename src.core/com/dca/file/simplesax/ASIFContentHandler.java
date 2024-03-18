package com.dca.file.simplesax;

import java.util.HashMap;

import com.dca.file.AthenaStandardInputDocument;
import com.dca.file.DocumentLoadingContext;


/**
 * Handles the content of the <Athena> tag.
 */
class ASIFContentHandler extends AbstractElementHandler {
	private final DocumentLoadingContext context;

	private boolean rocketDefined = false;
	private boolean simulationsDefined = false;
	private boolean datatypesDefined = false;

	public ASIFContentHandler(DocumentLoadingContext context) {
		this.context = context;
	}

	public AthenaStandardInputDocument getDocument() {
		if (!rocketDefined)
			return null;
		return context.getASIFDocument();
	}

	@Override
	public ElementHandler openElement(String element, HashMap<String, String> attributes) {

		if (element.equals("rocket")) {
			if (rocketDefined) {
				System.err.println("Multiple rocket designs within one document, "
								+ "ignoring later ones.");
				return null;
			}
			rocketDefined = true;
			return new ComponentParameterHandler(getDocument().getRocket(), context);
		}

		if (element.equals("datatypes")) {
			if (datatypesDefined) {
				System.err.println("Multiple datatype blocks. Ignoring later ones.");
				return null;
			}
			datatypesDefined = true;
			return new DatatypeHandler(this, context);
		}

		if (element.equals("simulations")) {
			if (simulationsDefined) {
				System.err.println("Multiple simulation definitions within one document, "
								+ "ignoring later ones.");
				return null;
			}
			simulationsDefined = true;
			return new SimulationsHandler(getDocument(), context);
		}

		if (element.equals("photostudio")) {
			return new PhotoStudioHandler(context.getOpenRocketDocument().getPhotoSettings());
		}
		return null;
	}
}

