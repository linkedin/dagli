package com.linkedin.dagli.distribution;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.math.distribution.DiscreteDistribution;
import com.linkedin.dagli.math.vector.DenseFloatArrayVector;
import com.linkedin.dagli.transformer.AbstractPreparedTransformer1WithInput;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.Collection;
import java.util.Objects;


/**
 * {@link DenseVectorizedDistribution} is reserved for a future implementation of a preparable transformer corresponding
 * to its extant inner class, {@link DenseVectorizedDistribution.Prepared}.
 *
 * However, the same effect can already be achieved by combining other transformers.  If you want a dense vector but
 * don't know the labels in advance, you can do something like this:
 * <code>
 * DensifiedVector densifiedVector =
 *   new DensifiedVector().withInput(new SparseVectorizedDistribution().withInput([yourDistribution]));
 * </code>
 */
public abstract class DenseVectorizedDistribution {
  private DenseVectorizedDistribution() { }

  /**
   * Given a known sequence of labels, maps a discrete distribution to a dense vector of elements where:
   * (1) The element index is the same as the index of each label
   * (2) The value of the element is the probability in the distribution
   *
   * Unknown labels (those not included in the sequence of labels) are ignored and do not appear in the resulting vector.
   *
   * @param <T> the type of the labels in the discrete distribution being vectorized
   */
  @ValueEquality
  public static class Prepared<T>
      extends AbstractPreparedTransformer1WithInput<DiscreteDistribution<T>, DenseFloatArrayVector, Prepared<T>> {
    private static final long serialVersionUID = 1;

    private Object2IntOpenHashMap<T> _indices = null;

    /**
     * Creates a new instance with an empty label-to-index map
     */
    public Prepared() {
      super();
    }

    /**
     * Creates a new instance with a label-to-index mapping created from the given label space.  The index for each label
     * is implied by the iteration order in the supplied label space collection (the first label is assigned index 0).
     *
     * @param labelSpace the label space collection
     */
    public Prepared(Collection<T> labelSpace) {
      super();
      setLabelSpace(labelSpace);
    }

    /**
     * Creates a copy of this instance with a label-to-index mapping created from specified label space.  The index for
     * each label is implied by the iteration order in the supplied label space collection (the first label is assigned
     * index 0).
     *
     * @param labelSpace the label space collection
     * @return a copy of this instance with the specified label space
     */
    public Prepared<T> withLabelSpace(Collection<T> labelSpace) {
      return clone(c -> c.setLabelSpace(labelSpace));
    }

    private void setLabelSpace(Collection<T> labelSpace) {
      _indices = new Object2IntOpenHashMap<>();
      _indices.defaultReturnValue(-1);
      for (T label : labelSpace) {
        if (_indices.putIfAbsent(label, _indices.size()) != -1) {
          throw new IllegalArgumentException(
              "The label " + label.toString() + " occurs multiple times in the provided label sequence");
        }
      }
    }

    @Override
    public DenseFloatArrayVector apply(DiscreteDistribution<T> val1) {
      float[] result = new float[_indices.size()];
      val1.stream().forEach(labelProbability -> {
        Integer index = _indices.get(labelProbability.getLabel());
        if (index != null) {
          result[index] = (float) labelProbability.getProbability();
        }
      });

      return DenseFloatArrayVector.wrap(result);
    }

    @Override
    public void validate() {
      super.validate();
      Objects.requireNonNull(_indices,
          "A DenseVectorizedDistribution.Prepared transformer's label space has not been set via withLabelSpace(...)");
    }
  }
}
