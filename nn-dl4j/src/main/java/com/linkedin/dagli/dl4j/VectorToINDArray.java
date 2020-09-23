package com.linkedin.dagli.dl4j;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.math.number.PrimitiveNumberTypes;
import com.linkedin.dagli.math.vector.Vector;
import com.linkedin.dagli.transformer.AbstractPreparedTransformer1WithInput;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;


/**
 * Transforms a Dagli {@link Vector} into a DL4J NDArray.  The Vector must have no elements with negative indices and,
 * as NDArrays are dense, the highest index should not be "too large" (e.g. no more than {@link Integer#MAX_VALUE}, with
 * the exact maximum depending on your JVM).
 */
@ValueEquality
class VectorToINDArray extends AbstractPreparedTransformer1WithInput<Vector, INDArray, VectorToINDArray> {
  private static final long serialVersionUID = 1;

  @Override
  public INDArray apply(Vector vec) {
    return PrimitiveNumberTypes.isStorableAsSinglePrecisionFloat(vec.valueType()) ? Nd4j.createFromArray(
        vec.toFloatArray()) : Nd4j.createFromArray(vec.toDoubleArray());
  }
}
