package com.dca.file;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;

public class FileHandler {

	
	/**
	 * Open a file based on a URL.
	 * @param url		the file to open.
	 * @param parent	the parent window for dialogs.
	 * @return			<code>true</code> if opened successfully.
	 */
	public static void open(URL url) {
		String displayName = null;
		// First figure out the file name from the URL

		// Try using URI.getPath();
		try {
			URI uri = url.toURI();
			displayName = uri.getPath();
		} catch (URISyntaxException ignore) {
		}

		// Try URL-decoding the URL
		if (displayName == null) {
			try {
				displayName = URLDecoder.decode(url.toString(), "UTF-8");
			} catch (UnsupportedEncodingException ignore) {
			}
		}

		if (displayName == null) {
			displayName = "";
		}

		// Remove path from filename
		if (displayName.lastIndexOf('/') >= 0) {
			displayName = displayName.substring(displayName.lastIndexOf('/') + 1);
		}

		////	Open the file
		log.info("Opening file from url=" + url + " filename=" + displayName);

		OpenFileWorker worker = new OpenFileWorker(url);
		open(worker, displayName, true);
	}
	
	/**
	 * Open the specified file using the provided worker.
	 *
	 * @param worker	the OpenFileWorker that loads the file.
	 * @param displayName	the file name to display in dialogs.
	 * @param parent
	 * @param openRocketConfigDialog if true, will open the configuration dialog of the rocket.  This is useful for examples.
	 * @return the BasicFrame that was created, or null if not created successfully.
	 */
	private static BasicFrame open(OpenFileWorker worker, String displayName, Window parent, boolean openRocketConfigDialog) {
		////	Open the file in a Swing worker thread
		log.info("Starting OpenFileWorker");
		if (!SwingWorkerDialog.runWorker(parent, "Opening file", "Reading " + displayName + "...", worker)) {
			//	//	User cancelled the operation
			log.info("User cancelled the OpenFileWorker");
			return null;
		}

		////	Handle the document
		OpenRocketDocument doc = null;
		try {
			doc = worker.get();
		} catch (ExecutionException e) {
			Throwable cause = e.getCause();
			if (cause instanceof FileNotFoundException) {
				log.warn("File not found", cause);
				JOptionPane.showMessageDialog(parent,
						"File not found: " + displayName,
						"Error opening file", JOptionPane.ERROR_MESSAGE);
				return null;
			} else if (cause instanceof RocketLoadException) {
				log.warn("Error loading the file", cause);
				JOptionPane.showMessageDialog(parent,
						"Unable to open file '" + displayName + "': "
								+ cause.getMessage(),
								"Error opening file", JOptionPane.ERROR_MESSAGE);
				return null;
			} else {
				throw new BugException("Unknown error when opening file", e);
			}

		} catch (InterruptedException e) {
			throw new BugException("EDT was interrupted", e);
		}

		if (doc == null) {
			throw new BugException("Document loader returned null");
		}

		////	Show warnings
		WarningSet warnings = worker.getRocketLoader().getWarnings();
		if (!warnings.isEmpty()) {
			log.info("Warnings while reading file: " + warnings);
			WarningDialog.showWarnings(parent,
					new Object[] {
							//	//	The following problems were encountered while opening
							trans.get("BasicFrame.WarningDialog.txt1") + " " + displayName + ".",
							//	//	Some design features may not have been loaded correctly.
							trans.get("BasicFrame.WarningDialog.txt2")
			},
					//	//	Warnings while opening file
					trans.get("BasicFrame.WarningDialog.title"), warnings);
		}

		////	Open the frame
		log.debug("Opening new frame with the document");
		BasicFrame frame = new BasicFrame(doc);
		frame.setVisible(true);

		if (parent != null && parent instanceof BasicFrame) {
			((BasicFrame) parent).closeIfReplaceable();
		}
		if (openRocketConfigDialog) {
			ComponentConfigDialog.showDialog(frame, doc, doc.getRocket());
		}

		return frame;
	}
}
