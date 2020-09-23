package com.linkedin.dagli.nn;

/**
 * Specifies the type of floating point numbers used.  This can have a profound impact on the performance of a neural
 * network.
 */
public enum FloatingPointPrecision {
  /**
   * 16-bit floating point values.  Operations with half-precision values on GPUs can be much slower <strong>or</strong>
   * much faster, depending on the GPU.
   */
  HALF,

  /**
   * 32-bit floating point values.
   */
  SINGLE,

  /**
   * 64-bit floating point values.  Operations with double-precision values on GPUs will typically carry a
   * <strong>very</strong> heavy performance penalty, but certain cards (e.g. Nvidia's Volta series) suffer far less
   * than others.
   */
  DOUBLE;
}
