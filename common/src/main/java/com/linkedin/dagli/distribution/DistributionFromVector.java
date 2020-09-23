package com.linkedin.dagli.distribution;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.generator.Constant;
import com.linkedin.dagli.math.distribution.ArrayDiscreteDistribution;
import com.linkedin.dagli.math.distribution.DiscreteDistribution;
import com.linkedin.dagli.math.vector.Vector;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.transformer.AbstractPreparedTransformer2;
import java.util.Map;
import java.util.NoSuchElementException;


/**
 * Creates a distribution from the probabilities stored in a {@link Vector} given a {@link Map} that provides the labels
 * corresponding to each non-zero vector element.  Elements in the {@link Vector} with negative values are ignored
 * (negative probabilities are not permitted); however, by default, probabilities can exceed 1.
 *
 * @param <L> the type of label in the resulting distribution
 */
@ValueEquality
public class DistributionFromVector<L> extends AbstractPreparedTransformer2<
    Vector,
    Map<Long, ? extends L>,
    DiscreteDistribution<L>,
    DistributionFromVector<L>> {

  private static final long serialVersionUID = 1;

  private boolean _throwOnMissingLabel = true;
  private Normalization _normalization = Normalization.NONE;

  public enum Normalization {
    /**
     * No normalization is applied to the resulting distribution.
     */
    NONE,

    /**
     * The probabilities in the resulting distribution will be normalized to sum to 1.0.
     */
    SUM_TO_ONE,

    /**
     * The probabilities in the resulting distribution will be normalized to sum to 1.0 only if they would otherwise
     * sum to more than 1.0.  If the distribution's probabilities sum to less than 1.0, they will not be changed.
     */
    SUM_TO_ONE_OR_LESS,

    /**
     * The probabilities in the resulting distribution will be clipped such that any "probability" that would otherwise
     * be greater than 1.0 will be changed to 1.0.
     */
    CLIPPED,
  }

  /**
   * Returns a copy of this instance that will use the specified {@link Normalization} scheme.
   *
   * By default, no normalization is applied ({@link Normalization#NONE}): the resulting distribution's probabilities
   * may sum to more than 1.0 and individual probabilities may themselves be more than 1.0.
   *
   * @param normalization the type of {@link Normalization} to use
   * @return a copy of this instance that will use the specified {@link Normalization} scheme
   */
  public DistributionFromVector<L> withNormalization(Normalization normalization) {
    return clone(c -> c._normalization = normalization);
  }

  /**
   * Returns a copy of this instance that will not <strong>not</strong> throw an exception if the input vector has
   * a non-zero element whose index is not present in the index-to-label map.  Instead, those elements will be ignored.
   * The default behavior is to throw an exception.
   *
   * Element values corresponding to missing labels are <strong>not</strong> taken into account during normalization;
   * for example, if {@link Normalization#SUM_TO_ONE} is used and an element with a positive probability is ignored, the
   * resulting distribution will still have probabilities summing to 1.0.
   *
   * @return a copy of this instance that will ignore missing labels
   */
  public DistributionFromVector<L> withMissingLabelsIgnored() {
    return clone(c -> c._throwOnMissingLabel = false);
  }

  /**
   * Returns a copy of this instance that will create distributions from the vectors provided by the specified input.
   *
   * @param vectorInput the {@link Producer} providing the vector inputs to be transformed into distributions
   * @return a copy of this instance that will create distributions from the vectors provided by the specified input
   */
  public DistributionFromVector<L> withVectorInput(Producer<? extends Vector> vectorInput) {
    return super.withInput1(vectorInput);
  }

  /**
   * Returns a copy of this instance that will use the index-to-label map provided by the specified input.
   * This index-to-label map will typically, but does not have to, be the same for all examples.
   *
   * Unless {@link #withMissingLabelsIgnored()} is used, an exception will be thrown if all indices of non-zero
   * vector elements do not have a corresponding label in the map.
   *
   * @param indexToLabelMapInput a producer providing map from a vector element's index to its corresponding label
   * @return a copy of this instance that will use the index-to-label map provided by the specified input
   */
  public DistributionFromVector<L> withIndexToLabelMapInput(Producer<? extends Map<Long, ? extends L>> indexToLabelMapInput) {
    return super.withInput2(indexToLabelMapInput);
  }

  /**
   * Returns a copy of this instance that will use the specified index-to-label map for all examples.
   *
   * Unless {@link #withMissingLabelsIgnored()} is used, an exception will be thrown if all indices of non-zero
   * vector elements do not have a corresponding label in the map.
   *
   * @param indexToLabelMap a map from a vector element's index to its corresponding label
   * @return a copy of this instance that will use the index-to-label map provided by the specified input
   */
  public DistributionFromVector<L> withIndexToLabelMap(Map<Long, ? extends L> indexToLabelMap) {
    return withIndexToLabelMapInput(new Constant<>(indexToLabelMap));
  }

  @Override
  public DiscreteDistribution<L> apply(Vector vector, Map<Long, ? extends L> indexToLabelMap) {
    // determine number of labels so we can create label array
    int labelCount = Math.toIntExact(vector.size64());

    // this is safe because the wrap() method below can safely accept Object[] masquerading as L[] (per its Javadoc)
    L[] labelArray = (L[]) new Object[labelCount];
    double[] probabilityArray = new double[labelCount];

    com.linkedin.dagli.math.vector.VectorElementIterator elementIterator = vector.unorderedIterator();
    for (int i = 0; i < labelCount; i++) {
      int offset = i; // local variables used in closures like the one below must be (effectively) final
      elementIterator.next((index, value) -> {
        if (!indexToLabelMap.containsKey(index)) {
          if (_throwOnMissingLabel) {
            throw new NoSuchElementException("No label mapping exists for vector element " + index);
          } // else ... do nothing; the values in the labelArray/probabilityArray at offset i will be null/0 and this
          // element will ultimately be ignored by the wrap() call below (elements with probability 0 are discarded.)
        } else {
          labelArray[offset] = indexToLabelMap.get(index);
          probabilityArray[offset] = Math.max(0, value); // negative probabilities are not allowed
        }
      });
    }

    // normalize our putative probabilities
    switch (_normalization) {
      case SUM_TO_ONE:
      case SUM_TO_ONE_OR_LESS:
        double sum = 0;
        for (double val : probabilityArray) {
          sum += val;
        }
        if (sum > 1.0 || (sum > 0.0 && _normalization == Normalization.SUM_TO_ONE)) {
          for (int i = 0; i < probabilityArray.length; i++) {
            probabilityArray[i] /= sum;
          }
        }
        break;
      case CLIPPED:
        for (int i = 0; i < probabilityArray.length; i++) {
          probabilityArray[i] = Math.min(1.0, probabilityArray[i]);
        }
        break;
      case NONE:
        break; // no-op
      default:
        throw new IllegalArgumentException("Unknown normalization mode " + _normalization);
    }

    return ArrayDiscreteDistribution.wrap(labelArray, probabilityArray);
  }
}

