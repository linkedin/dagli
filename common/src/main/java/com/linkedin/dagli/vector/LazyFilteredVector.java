package com.linkedin.dagli.vector;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.math.vector.AbstractVector;
import com.linkedin.dagli.math.vector.Vector;
import com.linkedin.dagli.math.vector.VectorElementFilteredIterator;
import com.linkedin.dagli.math.vector.VectorElementIterator;
import com.linkedin.dagli.transformer.AbstractPreparedTransformer1WithInput;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.io.Serializable;


/**
 * Lazily filters the elements of a vector by creating a new vector that wraps the input and contains only the desired
 * indices.  Note that changes to the underlying vector will modify the filtered vector and should be avoided.
 */
@ValueEquality
public class LazyFilteredVector extends AbstractPreparedTransformer1WithInput<Vector, Vector, LazyFilteredVector> {
  private static final long serialVersionUID = 1;

  private LongOpenHashSet _indicesToKeep;

  /**
   * Assigns the set of indices which will be kept in the transformed vector.  All elements at other indices will be
   * removed (set to 0).
   *
   * @param indicesToKeep the set of indices to keep.  It is assumed that the FilteredVector takes ownership of this
   *                      set, which should not be subsequently modified.
   * @return a copy of this instance that will use the provided set of kept indices
   */
  public LazyFilteredVector withIndicesToKeep(LongOpenHashSet indicesToKeep) {
    return clone(c -> c._indicesToKeep = indicesToKeep);
  }

  /**
   * A {@link Vector} wrapping another whose elements are lazily filtered by their indices.
   */
  private static class Filtered extends AbstractVector implements Serializable {
    private static final long serialVersionUID = 1;

    private final Vector _wrappedVector;
    private final LongSet _indicesToKeep;

    /**
     * Creates a new instance.
     *
     * @param wrapped the vector to be filtered.  It will not be modified and will be wrapped by this instance.
     * @param indicesToKeep the indices of the elements that should be kept; all other indices will have a value of 0.
     */
    Filtered(Vector wrapped, LongSet indicesToKeep) {
      _wrappedVector = wrapped;
      _indicesToKeep = indicesToKeep;
    }

    @Override
    public Class<? extends Number> valueType() {
      return _wrappedVector.valueType();
    }

    @Override
    public double get(long l) {
      return _indicesToKeep.contains(l) ? _wrappedVector.get(l) : 0;
    }

    @Override
    public VectorElementIterator iterator() {
      return new VectorElementFilteredIterator(_wrappedVector.iterator(),
          (index, value) -> _indicesToKeep.contains(index));
    }

    @Override
    public VectorElementIterator reverseIterator() {
      return new VectorElementFilteredIterator(_wrappedVector.reverseIterator(),
          (index, value) -> _indicesToKeep.contains(index));
    }

    @Override
    public VectorElementIterator unorderedIterator() {
      return new VectorElementFilteredIterator(_wrappedVector.unorderedIterator(),
          (index, value) -> _indicesToKeep.contains(index));
    }
  }

  @Override
  public Vector apply(Vector valA) {
    return new Filtered(valA, _indicesToKeep);
  }
}
