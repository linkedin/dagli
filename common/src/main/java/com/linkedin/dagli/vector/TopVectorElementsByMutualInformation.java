package com.linkedin.dagli.vector;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.input.SparseFeatureVectorInput;
import com.linkedin.dagli.math.vector.Vector;
import com.linkedin.dagli.math.vector.VectorElement;
import com.linkedin.dagli.preparer.AbstractStreamPreparer2;
import com.linkedin.dagli.preparer.PreparerContext;
import com.linkedin.dagli.preparer.PreparerResult;
import com.linkedin.dagli.producer.MissingInput;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.transformer.AbstractPreparableTransformer2;
import com.linkedin.dagli.transformer.PreparedTransformer2;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import java.util.TreeSet;



/**
 * Filters a vector to only include the top K elements of a vector with the maximum mutual information relative to the
 * labels.  Mutual information is calculated for each element index over the set of preparation examples, and those
 * elements at the indices with the highest mutual information will be kept.  This set of top K indices will be the same
 * across all examples, so resultant vectors will generally have fewer than K remaining non-zero elements after
 * filtering (unless all the top K elements by MI happened to be non-zero in the vector).
 *
 * This can be used as a cheap feature selection mechanism.  When calculating the mutual information, vector elements
 * are considered to be either zero or non-zero; beyond this, the value is irrelevant (e.g. 5 is the same as 5000).
 */
@ValueEquality
public class TopVectorElementsByMutualInformation
    extends AbstractPreparableTransformer2<Object, Vector, Vector, PreparedTransformer2<Object, Vector, Vector>, TopVectorElementsByMutualInformation> {

  private static final long serialVersionUID = 1;

  private int _maxElementsToKeep = 1000;

  /**
   * Sets the maximum number of elements (those with the highest mutual information) that will be kept in the resultant
   * filtered vectors.  The default value is 1000.
   *
   * @param maxElementsToKeep the maximum number of top-MI elements to keep (this is in total, not per example).
   * @return a copy of this instance with the specified maximum
   */
  public TopVectorElementsByMutualInformation withMaxElementsToKeep(int maxElementsToKeep) {
    return clone(c -> c._maxElementsToKeep = maxElementsToKeep);
  }

  /**
   * Sets the input providing the label used to calculate the mutual information
   *
   * @param labelInput the label input
   * @return a copy of this instance that will use the specified input
   */
  public TopVectorElementsByMutualInformation withLabelInput(Producer<?> labelInput) {
    return clone(c -> c._input1 = labelInput);
  }

  /**
   * Sets the input providing the vectors whose elements are to be filtered to include only those with the highest
   * mutual information (as measured across all examples observed during preparation).
   *
   * @param vectorInput the vector input
   * @return a copy of this instance that will use the specified input
   */
  public TopVectorElementsByMutualInformation withVectorInput(Producer<? extends Vector> vectorInput) {
    return clone(c -> c._input2 = vectorInput);
  }

  /**
   * @return an input configurator for the vector input of this transformer
   */
  public SparseFeatureVectorInput<TopVectorElementsByMutualInformation> withVectorInput() {
    return new SparseFeatureVectorInput<>(this::withVectorInput);
  }

  /**
   * Preparer for the {@link TopVectorElementsByMutualInformation} transformer.
   *
   * This class is package-private to facilitate testing.
   */
  static class Preparer
      extends AbstractStreamPreparer2<Object, Vector, Vector, PreparedTransformer2<Object, Vector, Vector>> {
    private final int _maxElementsToKeep;

    // maps from vector element index to a per-label table of how many times that label co-occurred with the event of
    // the element being non-zero non-zero
    private final Long2ObjectOpenHashMap<Object2LongOpenHashMap<Object>> _elementIndexToLabelCooccurrenceMap =
        new Long2ObjectOpenHashMap<>(128);

    // how many times each label was observed
    private final Object2LongOpenHashMap<Object> _labelTotals = new Object2LongOpenHashMap<>(16);

    // total observations (examples)
    private long _totalCount = 0;

    /**
     * Creates a new instance.
     *
     * @param maxElementsToKeep how many of the top elements (highest mutual information) should be kept?
     */
    Preparer(int maxElementsToKeep) {
      _maxElementsToKeep = maxElementsToKeep;
    }

    // sum all values in a something-to-long map
    private static long sumValues(Object2LongOpenHashMap<?> map) {
      long[] val = new long[1];
      map.values().forEach((long count) -> val[0] += count);
      return val[0];
    }

    /**
     * Gets a TreeSet containing the top vector elements (by index) and their calculated mutual information.
     *
     * Only the top _maxElementsToKeep elements will be returned.  This method is package-private to facilitate testing.
     *
     * @return a TreeSet containing the top vector elements (by index) and their calculated mutual information
     */
    TreeSet<VectorElement> calculateTopVectorElementsByMI() {
      TreeSet<VectorElement> candidates = new TreeSet<>();

      double logTotal = Math.log(_totalCount);

      // Mutual information is \sum_x \sum_y P(x, y) log (P(x, y)/(P(x)*P(y)))
      // = \sum_x \sum_y P(x, y) (log P(x, y) - log(P(x)) - log(P(y)))
      // = \sum_x \sum_y Count(x, y)/total (log P(x, y) - log(P(x)) - log(P(y)))
      // = (1/total) * \sum_x \sum_y Count(x, y) (log P(x, y) - log(P(x)) - log(P(y)))
      // (and we can just drop the (1/total) factor since it's constant for all elements)
      _elementIndexToLabelCooccurrenceMap.long2ObjectEntrySet().fastForEach(elementIndexAndLabelCooccurrencePair -> {
        final Object2LongOpenHashMap<Object> xAndYCooccurrenceCounts = elementIndexAndLabelCooccurrencePair.getValue();

        final long x = elementIndexAndLabelCooccurrencePair.getLongKey(); // index of the element
        final long xCount = sumValues(xAndYCooccurrenceCounts); // marginalize over y

        double[] mi = new double[1];

        _labelTotals.object2LongEntrySet().fastForEach(labelAndCountPair -> {
          final Object y = labelAndCountPair.getKey(); // the actual label
          final long yCount = labelAndCountPair.getLongValue();

          final long xAndYCount = xAndYCooccurrenceCounts.getOrDefault(y, 0);
          long notXAndYCount = yCount - xAndYCount;

          // The math, in terms of counts:
          // = (1/total) * \sum_x \sum_y Count(x, y) (log P(x, y) - log(P(x)) - log(P(y)))
          // = (1/total) * \sum_x \sum_y
          //    Count(x, y) (log Count(x, y) - log(total) - log(Count(x)) + log(total) - log(Count(y) + log(total))
          // = (1/total) * \sum_x \sum_y
          //    Count(x, y) (log Count(x, y) + log(total) - log(Count(x)) - log(Count(y))
          // (we factor out the (1/total) component and apply it outside this loop)
          if (xAndYCount > 0) { // P(x, y) > 0
            mi[0] += (xAndYCount * (Math.log(xAndYCount) + logTotal - Math.log(xCount) - Math.log(yCount)));
          }
          if (notXAndYCount > 0) { // P(!x, y) > 0
            long notXCount = _totalCount - xCount;
            mi[0] += notXAndYCount * (Math.log(notXAndYCount) + logTotal - Math.log(notXCount) - Math.log(yCount));
          }
        });

        mi[0] /= _totalCount; // * (1/total)

        if (candidates.size() < _maxElementsToKeep || mi[0] > candidates.first().getValue()) {
          if (candidates.size() == _maxElementsToKeep) {
            candidates.pollFirst();
          }

          candidates.add(new VectorElement(x, mi[0]));
        }
      });

      return candidates;
    }

    @Override
    public PreparerResult<PreparedTransformer2<Object, Vector, Vector>> finish() {
      TreeSet<VectorElement> candidates = calculateTopVectorElementsByMI();

      assert candidates.size() <= _maxElementsToKeep;
      LongOpenHashSet indexSet = new LongOpenHashSet(candidates.size());
      candidates.forEach(ve -> indexSet.add(ve.getIndex()));

      return new PreparerResult<>(
          new LazyFilteredVector().withIndicesToKeep(indexSet).internalAPI().withPrependedArity2(MissingInput.get()));
    }

    @Override
    public void process(Object label, Vector elements) {
      _totalCount++;
      _labelTotals.addTo(label, 1);

      elements.forEach((index, value) -> {
        Object2LongOpenHashMap<Object> perLabelCounts = _elementIndexToLabelCooccurrenceMap.get(index);
        if (perLabelCounts == null) {
          perLabelCounts = new Object2LongOpenHashMap<>(2);
          _elementIndexToLabelCooccurrenceMap.put(index, perLabelCounts);
        }

        perLabelCounts.addTo(label, 1);
      });
    }
  }

  @Override
  protected Preparer getPreparer(PreparerContext context) {
    return new Preparer(_maxElementsToKeep);
  }
}
