package com.linkedin.dagli.fasttext.anonymized.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;


public class BufferedLineReader extends LineReader {
	private BufferedReader br_;

	// Optimized line splitting
	public static String[] splitLine(String line) {
		int pieceCount = 1;
		for (int i = 0; i < line.length(); i++) {
			switch (line.charAt(i)) {
				case ' ':
				case '\r':
				case '\t':
				case '\f':
				case '\0':
				case '\u000b':
					pieceCount++;
			}
		}

		String[] res = new String[pieceCount];
		int nextIndex = 0;
		int curStart = 0;
		for (int i = 0; i < line.length(); i++) {
			switch (line.charAt(i)) {
				case ' ':
				case '\r':
				case '\t':
				case '\f':
				case '\0':
				case '\u000b':
					res[nextIndex++] = line.substring(curStart, i);
					curStart = i + 1;
			}
		}

		res[nextIndex] = line.substring(curStart);
		return res;
	}

	public BufferedLineReader(String filename, String charsetName) throws IOException, UnsupportedEncodingException {
		super(filename, charsetName);
		br_ = new BufferedReader(new InputStreamReader(getInputStream(), charset_));
	}

	public BufferedLineReader(InputStream inputStream, String charsetName) throws UnsupportedEncodingException {
		super(inputStream, charsetName);
		br_ = new BufferedReader(new InputStreamReader(inputStream, charset_));
	}

	@Override
	public long skipLine(long n) throws IOException {
		if (n < 0L) {
			throw new IllegalArgumentException("skip value is negative");
		}
		String line;
		long currentLine = 0;
		long readLine = 0;
		synchronized (lock) {
			while (currentLine < n && (line = br_.readLine()) != null) {
				readLine++;
				if (line == null || line.isEmpty() || line.startsWith("#")) {
					continue;
				}
				currentLine++;
			}
			return readLine;
		}
	}

	@Override
	public String readLine() throws IOException {
		synchronized (lock) {
			String lineString = br_.readLine();
			while (lineString != null && (lineString.isEmpty() || lineString.startsWith("#"))) {
				lineString = br_.readLine();
			}
			return lineString;
		}
	}

	@Override
	public String[] readLineTokens() throws IOException {
		String line = readLine();
		if (line == null)
			return null;
		else
			return splitLine(line); // lineDelimitingRegex_.split(line, -1);
	}

	@Override
	public int read(char[] cbuf, int off, int len) throws IOException {
		synchronized (lock) {
			return br_.read(cbuf, off, len);
		}
	}

	@Override
	public void close() throws IOException {
		synchronized (lock) {
			if (br_ != null) {
				br_.close();
			}
		}
	}

	/**
	 * Gets the {@link InputStream} for the underlying file (which must not be null).
	 *
	 * @return a new {@link InputStream} for reading the underlying file
	 * @throws IOException if there's a problem opening the file
	 */
	protected InputStream getInputStream() throws IOException {
		return Files.newInputStream(file_);
	}

	@Override
	public void rewind() throws IOException {
		synchronized (lock) {
			if (br_ != null) {
				br_.close();
			}
			if (file_ != null) {
				br_ = new BufferedReader(new InputStreamReader(getInputStream(), charset_));
			} else {
				throw new UnsupportedOperationException("InputStream rewind not supported");
			}
		}
	}
}
