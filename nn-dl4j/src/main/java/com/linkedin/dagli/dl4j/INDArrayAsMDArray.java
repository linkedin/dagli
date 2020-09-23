package com.linkedin.dagli.dl4j;

import com.linkedin.dagli.math.mdarray.AbstractMDArray;
import com.linkedin.dagli.util.invariant.Arguments;
import org.nd4j.linalg.api.ndarray.INDArray;


/**
 * Provides an implementation of an {@link com.linkedin.dagli.math.mdarray.MDArray} that is backed by an {@link INDArray}.
 *
 */
public class INDArrayAsMDArray extends AbstractMDArray {
  private static final long serialVersionUID = 1;

  private final INDArray _indArray;

  /**
   * Creates a new instance backed by the provided {@link INDArray}.  The {@link INDArray} must be in row-major format
   * (i.e. @{code indArray.ordering == 'c'}).  Note that 'c' ordering is the default for {@link INDArray}s.
   *
   * @param indArray the {@link INDArray} that will back this instance; changes to this array will be reflected in this
   *                 instance
   */
  public INDArrayAsMDArray(INDArray indArray) {
    super(indArray.shape());
    Arguments.check(indArray.ordering() == 'c');
    _indArray = indArray;
  }

  /**
   * @return the underlying {@link INDArray} backing this instance.
   */
  public INDArray getINDArray() {
    return _indArray;
  }

  @Override
  public Class<? extends Number> valueType() {
    return DL4JUtil.toPrimitiveType(_indArray.dataType());
  }

  @Override
  public double getAsDouble(long... indices) {
    return _indArray.getDouble(indices);
  }

  @Override
  public double getAsDouble(long offset) {
    return _indArray.getDouble(offset);
  }

  @Override
  public double getAsDoubleUnsafe(long offset) {
    return _indArray.getDoubleUnsafe(offset);
  }

  @Override
  public long getAsLong(long... indices) {
    return _indArray.getLong(indices);
  }

  @Override
  public long getAsLong(long offset) {
    return _indArray.getLong(offset);
  }

  @Override
  public void close() {
    if (_indArray.closeable()) {
      _indArray.close();
    }
  }
}
