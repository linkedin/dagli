package com.linkedin.dagli.vector;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.math.vector.DenseFloatArrayVector;
import com.linkedin.dagli.math.vector.Vector;
import com.linkedin.dagli.preparer.AbstractStreamPreparerVariadic;
import com.linkedin.dagli.preparer.PreparerContext;
import com.linkedin.dagli.preparer.PreparerResult;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.transformer.AbstractPreparableTransformerVariadic;
import com.linkedin.dagli.transformer.AbstractPreparedTransformerVariadic;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import java.util.List;
import org.apache.commons.rng.core.source64.XoRoShiRo128PlusPlus;


/**
 * Produces a (dense) feature vector from one or more vectors.  Determines the mapping from the original indices to the
 * new, dense indices by scanning over all the input when prepared.
 *
 * The current implementation admits, for sake of efficiency, a very, very small possibility of collision, which could
 * result in the resulting dense vectors omitting one or more elements in the input vectors; except for the case of
 * adversarial inputs intended to induce such collisions, however, it is not (in the author's opinion) a practical
 * concern.
 *
 * The {@link Prepared} transformer will ignore any encountered indices in the input not seen during preparation.
 */
@ValueEquality
public class DensifiedVector
    extends AbstractPreparableTransformerVariadic<Vector, DenseFloatArrayVector, DensifiedVector.Prepared, DensifiedVector> {

  private static final long serialVersionUID = 1;

  // seeds used for the position-based "hash" generator (a RNG)
  private static final long SEED1 = 0x94fa58c6dcae1ba0L; // arbitrary random value
  private static final long SEED2 = 0xb7121ac241548249L; // arbitrary random value
  private static final int MISSING_VALUE_MARKER = -1;

  /**
   * Create a new instance.
   */
  public DensifiedVector() {
    super();
  }

  /**
   * Creates a new instance with the provided inputs.
   *
   * @param inputs the inputs to be densified
   */
  @SafeVarargs
  public DensifiedVector(Producer<? extends Vector>... inputs) {
    super(inputs);
  }

  /**
   * Preparer for a {@link DensifiedVector}.
   */
  private static class Preparer
      extends AbstractStreamPreparerVariadic<Vector, DenseFloatArrayVector, Prepared> {
    private final Long2IntOpenHashMap _indexMap;

    Preparer() {
      _indexMap = new Long2IntOpenHashMap(100);
      _indexMap.defaultReturnValue(MISSING_VALUE_MARKER);
    }

    @Override
    public PreparerResult<Prepared> finish() {
      Prepared densifier = new Prepared(_indexMap);
      return new PreparerResult<>(densifier);
    }

    @Override
    public void process(List<Vector> values) {
      // we'll generate a pseudounique number for every position in the iterable (0, 1, 2, 3...) using a very cheap
      // pseudorandom number generator.  This particular generator is known to have "less-random" lower bits, but due to
      // how we use the result this isn't a real concern.
      XoRoShiRo128PlusPlus positionHashGenerator = new XoRoShiRo128PlusPlus(SEED1, SEED2);

      for (Vector vectorElements : values) {
        final long positionHash = positionHashGenerator.next(); // the position's "hash"
        vectorElements.forEach((index, value) -> {
          long newIndex = positionHash + index;
          if (_indexMap.get(newIndex) < 0) {
            _indexMap.put(newIndex, _indexMap.size());
          }
        });
      }
    }
  }

  @Override
  protected Preparer getPreparer(PreparerContext context) {
    return new Preparer();
  }

  /**
   * Transformer that uses a provided "densificiation map" to map sparse indices in input vectors to dense indices
   * in a resultant {@link DenseFloatArrayVector}.
   */
  @ValueEquality
  public static class Prepared extends AbstractPreparedTransformerVariadic<Vector, DenseFloatArrayVector, Prepared> {
    private static final long serialVersionUID = 1;

    private final Long2IntOpenHashMap _densificationMap;

    /**
     * Creates a new prepared vector densifier with the specified densification map and inputs.
     * @param densificationMap Maps from the sparse index to the dense index; default value must be < 0
     */
    public Prepared(Long2IntOpenHashMap densificationMap) {
      super();
      _densificationMap = densificationMap;
      if (densificationMap.defaultReturnValue() >= 0) {
        throw new IllegalArgumentException("Densification map must have default value < 0");
      }
    }

    @Override
    public DenseFloatArrayVector apply(List<? extends Vector> val) {
      // we'll generate a pseudounique number for every position in the iterable (0, 1, 2, 3...) using a very cheap
      // pseudorandom number generator.  This particular generator is known to have "less-random" lower bits, but due to
      // how we use the result this isn't a real concern.
      XoRoShiRo128PlusPlus positionHashGenerator = new XoRoShiRo128PlusPlus(SEED1, SEED2);

      float[] res = new float[_densificationMap.size()];

      for (Vector vectorElements : val) {
        long positionHash = positionHashGenerator.next(); // the position's "hash"
        vectorElements.forEach((idx, value) -> {
          int index = _densificationMap.get(positionHash + idx);
          if (index >= 0) {
            res[index] = (float) value;
          }
        });
      }

      return DenseFloatArrayVector.wrap(res);
    }
  }
}
