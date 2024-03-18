package com.dca.file;

public class DocumentLoadingContext {
	
	private int fileVersion;
	private AthenaStandardInputDocument document;
	
	public int getFileVersion() {
		return fileVersion;
	}
	
	public void setFileVersion(int fileVersion) {
		this.fileVersion = fileVersion;
	}
	

	
	public AthenaStandardInputDocument getASIFDocument() {
		return document;
	}
	
	public void setASIFDocument(AthenaStandardInputDocument document) {
		this.document = document;
	}
	
}
