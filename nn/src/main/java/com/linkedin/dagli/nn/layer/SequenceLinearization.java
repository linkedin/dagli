package com.linkedin.dagli.nn.layer;

/**
 * Specifies how a sequence of vectors is linearized.  Not all linearization modes will necessarily be supported in all
 * contexts by a given underlying neural network library.  This is because a linearization that does not correspond to
 * its underlying format for vector sequences (e.g. "row-major and [sequence index, vector element index]") cannot be
 * accomplished by a simple reshape (and would thus be undesirable/expensive).
 */
public enum SequenceLinearization {
  /**
   * Use the default linearization for the underlying neural network library being used.  This is a good option when you
   * don't care how the linearization is done (e.g. when converting the output of a recurrent layer into input features
   * for a dense layer.)
   */
  DEFAULT,

  /**
   * All elements at a particular element index (across vectors in all timesteps) are contiguous.  For example,
   * the linearization for the sequence of vectors [1, 2], [3, 4], [5, 6] would be [1, 3, 5, 2, 4, 6].
   */
  BY_ELEMENT_INDEX,

  /**
   * All elements in each vector are contiguous.  For example, the linearization for the sequence of vectors [1, 2],
   * [3, 4], [5, 6] would be [1, 2, 3, 4, 5, 6].
   */
  BY_TIMESTEP,
}
