package com.linkedin.dagli.fasttext.anonymized;

import java.io.IOException;
import java.io.InputStream;


/**
 * Read/write cpp primitive type
 *
 * @author Ivan
 *
 */
public class IOUtil {
	private byte[] int_bytes_ = new byte[4];
	private byte[] long_bytes_ = new byte[8];
	private byte[] double_bytes_ = new byte[8];

	public int readInt(InputStream is) throws IOException {
		is.read(int_bytes_);
		return getInt(int_bytes_);
	}

	public int getInt(byte[] b) {
		return (b[0] & 0xFF) << 0 | (b[1] & 0xFF) << 8 | (b[2] & 0xFF) << 16 | (b[3] & 0xFF) << 24;
	}

	public long getLong(byte[] b) {
		return (b[0] & 0xFFL) << 0 | (b[1] & 0xFFL) << 8 | (b[2] & 0xFFL) << 16 | (b[3] & 0xFFL) << 24
				| (b[4] & 0xFFL) << 32 | (b[5] & 0xFFL) << 40 | (b[6] & 0xFFL) << 48 | (b[7] & 0xFFL) << 56;
	}

	public double readDouble(InputStream is) throws IOException {
		is.read(double_bytes_);
		return getDouble(double_bytes_);
	}

	public double getDouble(byte[] b) {
		return Double.longBitsToDouble(getLong(b));
	}

	public byte[] intToByteArray(int i) {
		int_bytes_[0] = (byte) ((i >> 0) & 0xff);
		int_bytes_[1] = (byte) ((i >> 8) & 0xff);
		int_bytes_[2] = (byte) ((i >> 16) & 0xff);
		int_bytes_[3] = (byte) ((i >> 24) & 0xff);
		return int_bytes_;
	}

	public byte[] longToByteArray(long l) {
		long_bytes_[0] = (byte) ((l >> 0) & 0xff);
		long_bytes_[1] = (byte) ((l >> 8) & 0xff);
		long_bytes_[2] = (byte) ((l >> 16) & 0xff);
		long_bytes_[3] = (byte) ((l >> 24) & 0xff);
		long_bytes_[4] = (byte) ((l >> 32) & 0xff);
		long_bytes_[5] = (byte) ((l >> 40) & 0xff);
		long_bytes_[6] = (byte) ((l >> 48) & 0xff);
		long_bytes_[7] = (byte) ((l >> 56) & 0xff);

		return long_bytes_;
	}

	public byte[] doubleToByteArray(double d) {
		return longToByteArray(Double.doubleToRawLongBits(d));
	}
}
