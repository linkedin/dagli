package com.linkedin.dagli.dl4j;

import com.linkedin.dagli.annotation.Versioned;
import com.linkedin.dagli.math.mdarray.MDArrays;
import com.linkedin.dagli.transformer.DynamicInputs;
import com.linkedin.dagli.util.cloneable.AbstractCloneable;
import java.io.Serializable;
import org.nd4j.linalg.api.buffer.DataType;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;


/**
 * Represents a specific input that can be written into a per-input {@link INDArray} minibatch.
 *
 * When creating new instances, {@code null} may be passed for the input accessor (which would otherwise tie the
 * instance to a specific neural network).  This may then later be provided via
 * {@link #withInputAccessor(DynamicInputs.Accessor)}.
 *
 * @param <T> the type of the input to be written
 */
@Versioned
public abstract class AbstractInputConverter<T, S extends AbstractInputConverter<T, S>>
    extends AbstractCloneable<S> implements Serializable {
  private static final long serialVersionUID = 1;

  private DynamicInputs.Accessor<? extends T> _inputAccessor;

  /**
   * The shape of the {@link INDArray} for this example <strong>per example</strong> (i.e. with no minibatch
   * dimension).  For instance, the shape of a vector input is 1-D, specifying the (maximum) number of values in the
   * vector.
   */
  protected final long[] _exampleSubarrayShape;
  protected final long _exampleSubarrayElementCount;

  protected final long[] _exampleMaskSubarrayShape;
  protected final long _exampleMaskSubarrayElementCount;

  protected final DataType _dataType;

  /**
   * Returns a copy of this instance that will use the specified input accessor.  Input accessors (and thus the
   * result of this method) will only be valid for a particular set of neural network transformer inputs in a particular
   * order.
   *
   * @param inputAccessor the input accessor to be used
   * @return a copy of this instance that will use the specified accessor; this instance will consequently only be valid
   *         for the neural network corresponding to the accessor.
   */
  public S withInputAccessor(DynamicInputs.Accessor<? extends T> inputAccessor) {
    return clone(c -> ((AbstractInputConverter<T, S>) c)._inputAccessor = inputAccessor);
  }

  /**
   * Creates a new minibatch {@link INDArray} with capacity for the specified number of examples (for this input's
   * data only).  The minibatch array will use a row-major ('c') layout.
   *
   * @param exampleCount the number of examples to be accommodated by this minibatch
   * @return a new {@link INDArray} with capacity for the specified number of examples
   */
  public final INDArray createMinibatch(int exampleCount) {
    long[] shape = MDArrays.concatenate(new long[] { exampleCount }, _exampleSubarrayShape);
    return Nd4j.zeros(_dataType, shape); // this creates an array with 'c' (row-major) ordering, as desired
  }

  /**
   * Creates a new minibatch {@link INDArray} mask with capacity for the specified number of examples (for this input's
   * data only).  The mask array will use a row-major ('c') layout.
   *
   * @param exampleCount the number of examples to be accommodated by this minibatch
   * @return a new {@link INDArray} mask with capacity for the specified number of examples, or null if this input does
   *         not use masking
   */
  public final INDArray createMinibatchMask(int exampleCount) {
    if (_exampleMaskSubarrayShape == null) {
      return null;
    }
    long[] shape = MDArrays.concatenate(new long[] { exampleCount }, _exampleSubarrayShape);
    return Nd4j.zeros(DataType.FLOAT, shape); // this creates an array with 'c' (row-major) ordering, as desired
  }

  /**
   * Creates a new instance of this input-to-INDArray processor.
   *
   * @param inputAccessor the accessor used to find the corresponding input value in the raw Object[] of values passed
   *                      to a neural network transformer; to avoid tying to the instance of a specific neural network,
   *                      null may be provided ({@link #withInputAccessor(DynamicInputs.Accessor)} must be used to
   *                      set it later, before processing inputs)
   * @param exampleSubarrayShape the shape of the {@link INDArray}, <strong>per example</strong>; for instance, the
   *                             shape of a sequence of vectors is [max sequence length, max vector length]
   * @param exampleMaskSubarrayShape the shape of the mask {@link INDArray}, <strong>per example</strong>; for instance,
   *                                 the shape of the mask for a sequence of vectors is [max sequence length]
   * @param dataType the data type of the example {@link INDArray}
   */
  public AbstractInputConverter(DynamicInputs.Accessor<? extends T> inputAccessor, long[] exampleSubarrayShape,
      long[] exampleMaskSubarrayShape, DataType dataType) {
    _inputAccessor = inputAccessor;

    _exampleSubarrayShape = exampleSubarrayShape;
    _exampleSubarrayElementCount = MDArrays.elementCount(_exampleSubarrayShape);

    _exampleMaskSubarrayShape = exampleMaskSubarrayShape;
    _exampleMaskSubarrayElementCount =
        exampleMaskSubarrayShape == null ? 0 : MDArrays.elementCount(_exampleMaskSubarrayShape);

    _dataType = dataType;
  }

  /**
   * Writes the input value to a {@link INDArray} minibatch subarray corresponding to a particular example (the subarray
   * consists of all values with the indices [exampleIndex, *, *, *...]).  The INDArray must be in row-major ('c')
   * format.
   *
   * @param value the input value to write
   * @param array an {@link INDArray} to which the example's input value should be written
   * @param mask a mask array to which a mask should be written, if applicable (may be null if no mask is to be used)
   * @param exampleIndex the index of the example within the minibatch (this will always correspond to the first
   *                     dimension of the passed {@link INDArray} minibatch)
   */
  public abstract void writeValueToINDArrays(T value, INDArray array, INDArray mask, int exampleIndex);

  /**
   * Parses the input value from a "raw" array of inputs provided to a transformer and then writes that value to a
   * {@link INDArray} minibatch subarray corresponding to a particular example (the subarray consists of all values with
   * the indices [exampleIndex, *, *, *...]).  The INDArray must be in row-major ('c') format.
   *
   * @param inputs the array of "raw" inputs provided to the transformer
   * @param array an {@link INDArray} to which the example's input value should be written
   * @param mask a mask array to which a mask should be written, if applicable (may be null if no mask is to be used)
   * @param exampleIndex the index of the example within the minibatch (this will always correspond to the first
   *                     dimension of the passed {@link INDArray} minibatch)
   */
  public final void writeToINDArray(Object[] inputs, INDArray array, INDArray mask, int exampleIndex) {
    writeValueToINDArrays(_inputAccessor.get(inputs), array, mask, exampleIndex);
  }
}
