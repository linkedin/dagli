package com.linkedin.dagli.vector;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.input.SparseFeatureVectorInput;
import com.linkedin.dagli.math.vector.SparseFloatMapVector;
import com.linkedin.dagli.math.vector.Vector;
import com.linkedin.dagli.math.vector.VectorElement;
import com.linkedin.dagli.preparer.AbstractStreamPreparer1;
import com.linkedin.dagli.preparer.PreparerContext;
import com.linkedin.dagli.preparer.PreparerResult;
import com.linkedin.dagli.transformer.AbstractPreparableTransformer1WithInput;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.stream.StreamSupport;


/**
 * Filters out all but the k highest-sum (as calculated on preparation data) vector indices.
 */
@ValueEquality
public class TopVectorElementsByValue
    extends AbstractPreparableTransformer1WithInput<Vector, Vector, LazyFilteredVector, TopVectorElementsByValue> {
  private static final long serialVersionUID = 1;

  private int _maxElementsToKeep = 1000;

  /**
   * Sets the maximum number of elements (those indices with the highest sum) to be kept.
   *
   * The default value is 1000.
   *
   * @param maxElementsToKeep the maximum number of indices to keep (this is in total, not per example)
   * @return a copy of this instance that will use the specified maximum.
   */
  public TopVectorElementsByValue withMaxElementsToKeep(int maxElementsToKeep) {
    return clone(c -> c._maxElementsToKeep = maxElementsToKeep);
  }

  /**
   * @return an input configurator for the vector input of this transformer
   */
  public SparseFeatureVectorInput<TopVectorElementsByValue> withInput() {
    return new SparseFeatureVectorInput<>(this::withInput);
  }

  private static class Preparer extends AbstractStreamPreparer1<Vector, Vector, LazyFilteredVector> {
    private final SparseFloatMapVector _sum = new SparseFloatMapVector();
    private final int _k;

    /**
     * Creates a new preparer instance.
     *
     * @param k how many of the top elements (by total sum) to keep
     */
    Preparer(int k) {
      _k = k;
    }

    @Override
    public PreparerResult<LazyFilteredVector> finish() {
      LongOpenHashSet set = new LongOpenHashSet(_k);
      StreamSupport.stream(_sum.spliterator(), false)
          .sorted((e1, e2) -> (int) Math.signum(e2.getValue() - e1.getValue()))
          .limit(_k)
          .mapToLong(VectorElement::getIndex)
          .forEach(set::add);

      LazyFilteredVector filter = new LazyFilteredVector().withIndicesToKeep(set);
      return new PreparerResult<>(filter);
    }

    @Override
    public void process(Vector value) {
      _sum.addInPlace(value);
    }
  }

  @Override
  protected Preparer getPreparer(PreparerContext context) {
    return new Preparer(_maxElementsToKeep);
  }
}
