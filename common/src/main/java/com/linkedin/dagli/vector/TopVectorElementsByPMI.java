package com.linkedin.dagli.vector;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.preparer.AbstractStreamPreparer2;
import com.linkedin.dagli.preparer.PreparerContext;
import com.linkedin.dagli.preparer.PreparerResult;
import com.linkedin.dagli.producer.MissingInput;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.transformer.AbstractPreparableTransformer2;
import com.linkedin.dagli.transformer.PreparedTransformer2;
import com.linkedin.dagli.math.vector.Vector;
import com.linkedin.dagli.math.vector.VectorElement;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.TreeSet;


/**
 * Feature selection that ranks vector elements by their PMI with a boolean label, effectively ranking by P(Y|X) where
 * Y is the label and X is the vector element.  The returned vectors will be filtered to only include the top k features
 * (element indices); all other values will be 0.  This set of top K indices will be the same
 * across all examples, so resultant vectors will generally have fewer than K remaining non-zero elements after
 * filtering (unless all the top K elements by PMI happened to be non-zero in the vector).
 *
 * Vector elements are considered as only being zero or non-zero; e.g. a value of 5 will be treated the same as a value
 * of 5000.
 *
 * Pros: high-PMI features are "high signal" in predicting the label
 * Cons: no consideration of dependence among features; rare features may stochastically have high P(Y|X) and must be
 * discarded
 */
@ValueEquality
public class TopVectorElementsByPMI
    extends AbstractPreparableTransformer2<Boolean, Vector, Vector, PreparedTransformer2<Boolean, Vector, Vector>, TopVectorElementsByPMI> {

  private static final long serialVersionUID = 1;

  private int _maxElementsToKeep = 1000;
  private int _minOccurrenceCount = 10;

  /**
   * Sets the maximum number of elements (those indices with the highest PMI) to be kept.
   *
   * @param maxElementsToKeep the maximum number of indices to keep (this is in total, not per example)
   * @return a copy of this instance that will use the specified maximum.
   */
  public TopVectorElementsByPMI withMaxElementsToKeep(int maxElementsToKeep) {
    return clone(c -> c._maxElementsToKeep = maxElementsToKeep);
  }

  /**
   * Sets the minimum occurrence count needed for an element to be included, regardless of PMI.  Setting a minimum
   * prevents rare features from being spuriously included because they randomly happened to correlate with the label.
   *
   * The default minimum is 10.
   *
   * @param minOccurrenceCount the minimum number of times a feature must occur to be considered.  Features occurring
   *                           fewer times than this will never be included in the resultant filtered vectors,
   *                           regardless of their PMI.
   * @return a copy of this instance that will use the specified minimum.
   */
  public TopVectorElementsByPMI withMinOccurrenceCount(int minOccurrenceCount) {
    return clone(c -> c._minOccurrenceCount = minOccurrenceCount);
  }

  /**
   * Sets the input providing the label used to calculate the PMI for each feature (vector element).
   *
   * @param labelInput the label input
   * @return a copy of this instance that will use the specified input
   */
  public TopVectorElementsByPMI withLabelInput(Producer<Boolean> labelInput) {
    return clone(c -> c._input1 = labelInput);
  }

  /**
   * Sets the input providing the vector (features) that will be filtered based on their PMI.
   *
   * @param vectorInput the vector input
   * @return a copy of this instance that will use the specified input
   */
  public TopVectorElementsByPMI withVectorInput(Producer<? extends Vector> vectorInput) {
    return clone(c -> c._input2 = vectorInput);
  }

  /**
   * Preparer for the {@link TopVectorElementsByPMI} transformer.
   */
  private static class Preparer
      extends AbstractStreamPreparer2<Boolean, Vector, Vector, PreparedTransformer2<Boolean, Vector, Vector>> {
    private final int _maxElementsToKeep;
    private final int _minOccurrenceCount;

    private final Long2LongOpenHashMap _trueCounts = new Long2LongOpenHashMap(128);
    private final Long2LongOpenHashMap _totals = new Long2LongOpenHashMap(128);

    public Preparer(int maxElementsToKeep, int minOccurrenceCount) {
      _maxElementsToKeep = maxElementsToKeep;
      _minOccurrenceCount = minOccurrenceCount;
    }

    @Override
    public PreparerResult<PreparedTransformer2<Boolean, Vector, Vector>> finish() {
      TreeSet<VectorElement> candidates = new TreeSet<>();

      _trueCounts.long2LongEntrySet().fastForEach(indexAndCountWhenTruePair -> {
        long index = indexAndCountWhenTruePair.getLongKey();
        long countWhenTrue = indexAndCountWhenTruePair.getLongValue();

        long totalCount = _totals.get(index);
        if (totalCount < _minOccurrenceCount) {
          return;
        }

        // PMI is log (P(x, y)/(P(x)*P(y))) =
        // log(P(x, y)) - log(P(x)) - log(P(y)) =
        // log(count(x, y)) - log(total) - log(count(x)) + log(total) - log(count(y)) + log(total)
        // if we take y as the true/false label, log(total) and log(count(y)) are constant across all elements and can
        // thus be ignored
        double pmi = Math.log(countWhenTrue) - Math.log(totalCount);
        if (candidates.size() < _maxElementsToKeep || pmi > candidates.first().getValue()) {
          if (candidates.size() == _maxElementsToKeep) {
            candidates.pollFirst();
          }

          candidates.add(new VectorElement(index, pmi));
        }
      });

      LongOpenHashSet indexSet = new LongOpenHashSet(candidates.size());
      candidates.forEach(ve -> indexSet.add(ve.getIndex()));

      return new PreparerResult<>(
          new LazyFilteredVector().withIndicesToKeep(indexSet).internalAPI().withPrependedArity2(MissingInput.get()));
    }

    @Override
    public void process(Boolean valueA, Vector valueB) {
      valueB.forEach((index, value) -> {
        if (valueA) {
          _trueCounts.addTo(index, 1);
        }
        _totals.addTo(index, 1);
      });
    }
  }

  @Override
  protected Preparer getPreparer(PreparerContext context) {
    return new Preparer(_maxElementsToKeep, _minOccurrenceCount);
  }
}
