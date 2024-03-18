package com.dca.file;

import java.io.IOException;
import java.io.InputStream;

public abstract class AbstractAthenaLoader implements ASIFLoader{

	/**
	 * Loads an ATHENA Standard Input file from the specified InputStream.
	 */
	@Override
	public final void load(DocumentLoadingContext context, InputStream source, String fileName) throws ASIFLoadException {
		
		try {
			loadFromStream(context, source, fileName);
		} catch (ASIFLoadException e) {
			throw e;
		} catch (IOException e) {
			throw new ASIFLoadException("I/O error: " + e.getMessage(), e);
		}
	}
	
	
	
	/**
	 * This method is called by the default implementations of #load(File) 
	 * and load(InputStream) to load the config.
	 * 
	 * @throws ASIFLoadException if an error occurs during loading.
	 */
	protected abstract void loadFromStream(DocumentLoadingContext context, InputStream source, String fileName) throws IOException, ASIFLoadException;
	
	
}
