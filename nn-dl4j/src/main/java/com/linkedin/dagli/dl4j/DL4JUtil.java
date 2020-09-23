package com.linkedin.dagli.dl4j;

import com.linkedin.dagli.math.mdarray.MDArray;
import com.linkedin.dagli.math.vector.Vector;
import com.linkedin.dagli.nn.FloatingPointPrecision;
import org.nd4j.linalg.api.buffer.DataType;
import org.nd4j.linalg.api.ndarray.INDArray;


/**
 * Utility methods for interfacing Dagli with DL4J
 */
abstract class DL4JUtil {
  private DL4JUtil() { }

  /**
   * Translates an ND4J {@link DataType} into the smallest primitive numeric type capable of holding values of that
   * type.
   *
   * Special cases:
   * (1) BOOL is translated to byte.class
   * (2) UINT64 is translated to double.class (this potentially loses precision but preserves range).
   *
   * Certain types, such as COMPRESSED or UNKNOWN, are not supported; passing such non-numeric types will throw an
   * {@link IllegalArgumentException}.
   *
   * @param dataType the data type to convert to a numeric type
   * @return the smallest primitive numeric type capable of holding values of the provided {@link DataType}
   */
  public static Class<? extends Number> toPrimitiveType(DataType dataType) {
    switch (dataType) {
      case BOOL:
      case BYTE:
        return byte.class;

      case SHORT:
      case UBYTE:
        return short.class;

      case INT:
      case UINT16:
        return int.class;

      case HALF:
      case FLOAT:
      case BFLOAT16:
        return float.class;

      case LONG:
      case UINT32:
        return long.class;

      case UINT64: // unit64 -> double is lossy, but this approximation preserves range
      case DOUBLE:
        return double.class;

      case UNKNOWN:
      case COMPRESSED:
      case UTF8: // we could possibly call this a short, but that might lead to unexpected results
      default:
        throw new IllegalArgumentException("Data type has no corresponding primitive numeric type: " + dataType);
    }
  }

  public static DataType toDataType(FloatingPointPrecision precision) {
    switch (precision) {
      case HALF:
        return DataType.HALF;
      case SINGLE:
        return DataType.FLOAT;
      case DOUBLE:
        return DataType.DOUBLE;
      default:
        throw new IllegalArgumentException("Unknown precision type: " + precision); // should never happen
    }
  }

  /**
   * Copies a {@link Vector} into an {@link INDArray}.  The array must be row-major ('c' ordering).  Only elements with
   * non-negative indices less than the "{@code maxVectorLength}" are copied.
   *
   * This method is "unsafe" as it does not check that the copying does not exceed the bounds of the array.
   *
   * @param vector the vector to copy
   * @param maxVectorLength the maximum vector length (the highest allowable element index + 1)
   * @param array the target array
   * @param offset the linear offset in the target array where copying begins
   */
  public static void copyVectorToINDArrayUnsafe(Vector vector, long maxVectorLength, INDArray array, long offset) {
    assert array.ordering() == 'c';

    vector.forEach((index, value) -> {
      if (index >= 0 && index < maxVectorLength) {
        // for all elements within the expected bounds...
        array.putScalarUnsafe(offset + index, value);
      }
    });
  }

  /**
   * Copies a {@link MDArray} to an {@link INDArray}.  The target array must be row-major ('c' ordering).
   *
   * Elements are converted to double as part of the copying operation; this may cause long (64-bit integer) values to
   * lose precision.
   *
   * This method is "unsafe" as it does not check that the copying does not exceed the bounds of either array.
   *
   * @param sourceArray the array from which elements are copied
   * @param targetArray the array to which elements are copied
   * @param sourceOffset the offset in the source array at which copying begins
   * @param targetOffset the offset in the target array at which copying begins
   * @param count the number of elements copied
   */
  public static void copyMDArrayToINDArrayUnsafe(MDArray sourceArray, long sourceOffset, INDArray targetArray,
      long targetOffset, long count) {
    for (long i = 0; i < count; i++) {
      targetArray.putScalarUnsafe(targetOffset + count, sourceArray.getAsDoubleUnsafe(sourceOffset + count));
    }
  }
}
