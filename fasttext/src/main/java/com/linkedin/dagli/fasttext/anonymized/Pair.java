package com.linkedin.dagli.fasttext.anonymized;

public class Pair<K, V> {

	private K key_;
	private V value_;

	public Pair(K key, V value) {
		this.key_ = key;
		this.value_ = value;
	}

	public K getKey() {
		return key_;
	}

	public V getValue() {
		return value_;
	}

	public void setKey(K key) {
		this.key_ = key;
	}

	public void setValue(V value) {
		this.value_ = value;
	}

}
