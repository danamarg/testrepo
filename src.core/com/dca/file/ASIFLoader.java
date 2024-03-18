package com.dca.file;

import java.io.InputStream;

public interface ASIFLoader {

	public void load(DocumentLoadingContext context, InputStream source, String fileName) throws ASIFLoadException;

}
