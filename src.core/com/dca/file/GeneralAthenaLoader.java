package com.dca.file;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class GeneralAthenaLoader {

	
private static final int READ_BYTES = 300;

	
	private static final byte[] GZIP_SIGNATURE = { 31, -117 }; // 0x1f, 0x8b
	private static final byte[] ZIP_SIGNATURE = TextUtil.asciiBytes("PK");
	private static final byte[] ATHENA_SIGNATURE = TextUtil.asciiBytes("<athena");
	
	private final ASIFLoader asifLoader = new AthenaLoader();
	
	private final File baseFile;
	private final URL jarURL;
	private boolean isContainer;
	
	
	private final AthenaStandardInputDocument doc = ASIFDocumentFactory.createEmptyDocument();
	
	public GeneralAthenaLoader(File file) {
		this.baseFile = file;
		this.jarURL = null;
	}
	
	public GeneralAthenaLoader(URL jarURL) {
		this.baseFile = null;
		this.jarURL = jarURL;
	}
	
	/**
	 * Loads a rocket from the File object used in the constructor
	 */
	public final AthenaStandardInputDocument load() throws ASIFLoadException {
		InputStream stream = null;
		
		try {
			String fileName = baseFile != null && baseFile.getName() != null ? baseFile.getName().replaceFirst("[.][^.]+$", "") : null;
			stream = new BufferedInputStream(new FileInputStream(baseFile));
			load(stream, fileName);
			return doc;
			
		} catch (Exception e) {
			throw new ASIFLoadException("Exception loading file: " + baseFile + " , " + e.getMessage(), e);
		} finally {
			if (stream != null) {
				try {
					stream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public final AthenaStandardInputDocument load(InputStream source, String fileName) throws ASIFLoadException {
		try {
			loadStep1(source, fileName);
			doc.getRocket().enableEvents();
			return doc;
		} catch (Exception e) {
			throw new ASIFLoadException("Exception loading stream: " + e.getMessage(), e);
		}
	}
	
	
	/**
	 * This method determines the type file contained in the stream then calls the appropriate loading mechanism.
	 * 
	 * If the stream is a gzip file, the argument is wrapped in a GzipInputStream and the rocket loaded.
	 * 
	 * If the stream is a zip container, the first zip entry with name ending in .ork or .rkt is loaded as the rocket.
	 * 
	 * If the stream is neither, then it is assumed to be an xml file containing either an ork or rkt format rocket.
	 * 
	 * @param source
	 * @throws IOException
	 * @throws RocketLoadException
	 */
	private void loadStep1(InputStream source, String fileName) throws IOException, ASIFLoadException {
		
		// Check for mark() support
		if (!source.markSupported()) {
			source = new BufferedInputStream(source);
		}
		
		// Read using mark()
		byte[] buffer = new byte[READ_BYTES];
		int count;
		source.mark(READ_BYTES + 10);
		count = source.read(buffer);
		source.reset();
		
		if (count < 10) {
			throw new ASIFLoadException("Unsupported or corrupt file.");
		}
		
		
		// Detect the appropriate loader
		
		// Check for GZIP
		if (buffer[0] == GZIP_SIGNATURE[0] && buffer[1] == GZIP_SIGNATURE[1]) {
			isContainer = false;
			loadRocket(new GZIPInputStream(source), fileName);
			return;
		}
		
		// Check for ZIP (for future compatibility)
		if (buffer[0] == ZIP_SIGNATURE[0] && buffer[1] == ZIP_SIGNATURE[1]) {
			isContainer = true;
			// Search for entry with name *.ork
			ZipInputStream in = new ZipInputStream(source);
			ZipEntry entry = in.getNextEntry();
			if (entry == null) {
				throw new ASIFLoadException("Unsupported or corrupt file.");
			}
			if (entry.getName().matches(".*\\.[aA][tT][hH][eE][nN][aA]$")) {
				loadRocket(in, fileName);
			} else if (entry.getName().matches(".*\\.[aA][sS][iI][fF]$")) {
				loadRocket(in, fileName);
			} else if (entry.getName().matches(".*\\.[xX][mM][lL]$")) {
				loadRocket(in, fileName);
			}
			in.close();
			return;
		}
		
		isContainer = false;
		loadRocket(source, fileName);
	}
	
	private void loadRocket(InputStream source, String fileName) throws IOException, ASIFLoadException {
		
		// Check for mark() support
		if (!source.markSupported()) {
			source = new BufferedInputStream(source);
		}
		
		// Read using mark()
		byte[] buffer = new byte[READ_BYTES];
		int count;
		source.mark(READ_BYTES + 10);
		count = source.read(buffer);
		source.reset();
		
		if (count < 10) {
			throw new ASIFLoadException("Unsupported or corrupt file.");
		}
		
		// Check for ATHENA Standard Input File
		int match = 0;
		for (int i = 0; i < count; i++) {
			if (buffer[i] == ATHENA_SIGNATURE[match]) {
				match++;
				if (match == ATHENA_SIGNATURE.length) {
					loadUsing(asifLoader, source, fileName);
					return;
				}
			} else {
				match = 0;
			}
		}

		throw new ASIFLoadException("Unsupported or corrupt file.");
		
	}
	
	private void loadUsing(ASIFLoader loader, InputStream source, String fileName) throws ASIFLoadException {
		DocumentLoadingContext context = new DocumentLoadingContext();
		context.setASIFDocument(doc);
		loader.load(context, source, fileName);
	}
}
