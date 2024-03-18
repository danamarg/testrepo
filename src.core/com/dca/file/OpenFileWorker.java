package com.dca.file;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.URL;

import javax.swing.SwingWorker;

/**
 * A SwingWorker thread that opens a rocket design file.
 * 
 */
public class OpenFileWorker extends SwingWorker<AthenaStandardInputDocument, Void> {

	private final File file;
	private final URL jarURL;
	private final GeneralAthenaLoader loader;

	public OpenFileWorker(File file) {
		this.file = file;
		this.jarURL = null;
		loader = new GeneralAthenaLoader(file);
	}


	public OpenFileWorker(URL fileURL) {
		this.jarURL = fileURL;
		this.file = null;
		loader = new GeneralAthenaLoader(fileURL);
	}

	public GeneralAthenaLoader getRocketLoader() {
		return loader;
	}

	@Override
	protected AthenaStandardInputDocument doInBackground() throws Exception {
		InputStream is;

		// Get the correct input stream
		if (file != null) {
			is = new FileInputStream(file);
		} else {
			is = jarURL.openStream();
		}

		// Buffer stream unless already buffered
		if (!(is instanceof BufferedInputStream)) {
			is = new BufferedInputStream(is);
		}

		// Encapsulate in a ProgressInputStream
		is = new ProgressInputStream(is);

		try {
			String fileName = file != null && file.getName() != null ? file.getName().replaceFirst("[.][^.]+$", "") : null;
			AthenaStandardInputDocument document = loader.load(is, fileName);

			// Set document state
			document.setFile(file);
			document.setSaved(true);

			return document;
		} finally {
			try {
				is.close();
			} catch (Exception e) {
				e.printStackTrace();
				System.err.println("Error closing file");
			}
		}
	}




	private class ProgressInputStream extends FilterInputStream {

		private final int size;
		private int readBytes = 0;
		private int progress = -1;

		protected ProgressInputStream(InputStream in) {
			super(in);
			int s;
			try {
				s = in.available();
			} catch (IOException e) {
				e.printStackTrace();
				System.err.println("Exception while estimating available bytes!");
				s = 0;
			}
			size = Math.max(s, 1);
		}



		@Override
		public int read() throws IOException {
			int c = in.read();
			if (c >= 0) {
				readBytes++;
				setProgress();
			}
			if (isCancelled()) {
				throw new InterruptedIOException("OpenFileWorker was cancelled");
			}
			return c;
		}

		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			int n = in.read(b, off, len);
			if (n > 0) {
				readBytes += n;
				setProgress();
			}
			if (isCancelled()) {
				throw new InterruptedIOException("OpenFileWorker was cancelled");
			}
			return n;
		}

		@Override
		public int read(byte[] b) throws IOException {
			int n = in.read(b);
			if (n > 0) {
				readBytes += n;
				setProgress();
			}
			if (isCancelled()) {
				throw new InterruptedIOException("OpenFileWorker was cancelled");
			}
			return n;
		}

		@Override
		public long skip(long n) throws IOException {
			long nr = in.skip(n);
			if (nr > 0) {
				readBytes += nr;
				setProgress();
			}
			if (isCancelled()) {
				throw new InterruptedIOException("OpenFileWorker was cancelled");
			}
			return nr;
		}

		@Override
		public synchronized void reset() throws IOException {
			in.reset();
			readBytes = size - in.available();
			setProgress();
			if (isCancelled()) {
				throw new InterruptedIOException("OpenFileWorker was cancelled");
			}
		}



		private void setProgress() {
			int p = MathUtil.clamp(readBytes * 100 / size, 0, 100);
			if (progress != p) {
				progress = p;
				OpenFileWorker.this.setProgress(progress);
			}
		}
	}
}
