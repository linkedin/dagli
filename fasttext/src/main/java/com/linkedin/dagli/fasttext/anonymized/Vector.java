package com.linkedin.dagli.fasttext.anonymized;

public class Vector {

	public int m_;
	public float[] data_;

	public Vector(int size) {
		m_ = size;
		data_ = new float[size];
	}

	public int size() {
		return m_;
	}

	public void zero() {
		for (int i = 0; i < m_; i++) {
			data_[i] = 0.0f;
		}
	}

	public void mul(float a) {
		for (int i = 0; i < m_; i++) {
			data_[i] *= a;
		}
	}

	public void addRow(final Matrix A, int i) {
		Utils.checkArgument(i >= 0);
		Utils.checkArgument(i < A.m_);
		Utils.checkArgument(m_ == A.n_);
		for (int j = 0; j < A.n_; j++) { // layer size
			data_[j] += A.data_[i][j];
		}
	}

	public void addRow(final Matrix A, int i, float a) {
		Utils.checkArgument(i >= 0);
		Utils.checkArgument(i < A.m_);
		Utils.checkArgument(m_ == A.n_);
		for (int j = 0; j < A.n_; j++) {
			data_[j] += a * A.data_[i][j];
		}
	}

	public void mul(final Matrix A, final Vector vec) {
		Utils.checkArgument(A.m_ == m_);
		Utils.checkArgument(A.n_ == vec.m_);
		for (int i = 0; i < m_; i++) {
			data_[i] = 0.0f;
			for (int j = 0; j < A.n_; j++) {
				data_[i] += A.data_[i][j] * vec.data_[j];
			}
		}
	}

	public float get(int i) {
		return data_[i];
	}

	public void set(int i, float value) {
		data_[i] = value;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		for (float data : data_) {
			builder.append(data).append(' ');
		}
		if (builder.length() > 1) {
			builder.setLength(builder.length() - 1);
		}
		return builder.toString();
	}

}
