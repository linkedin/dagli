package com.linkedin.dagli.generator;

import com.linkedin.dagli.annotation.equality.ValueEquality;


/**
 * Generates the index of the current row/example, starting at 0.  This is purely based on the position of the example
 * in the input (e.g. its index in the batch of examples provided to prepare(...) the DAG).
 *
 * Important note: when apply(...) is called on the DAG for a single example, the generated value will be 0.  Generally,
 * you should only assume that ExampleIndex will provide sensible values during preparation, and ignore the value it
 * provides in the resulting prepared transformer.
 *
 * For example, in a KFoldCrossTraining node we might use ExampleIndex to (after hashing) provide a way to direct
 * each example to a pseudo-random "bin" that will determine how the data is split for cross-training; since
 * cross-training doesn't occur at inference-time (apply-time) the generator is only used during preparation, so it's
 * safe.
 */
@ValueEquality
public class ExampleIndex extends AbstractGenerator<Long, ExampleIndex> {
  private static final long serialVersionUID = 1;

  public ExampleIndex() {
    // all IndexGenerators share the same UUID
    super(0xd005db20ae7917c7L, 0x18f32925475f7fb0L);
  }

  @Override
  public Long generate(long index) {
    return index;
  }
}
