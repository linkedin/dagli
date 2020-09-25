package com.linkedin.dagli.math.number;

/**
 * Static utility methods for reasoning about primitive number types; boxed types (e.g. {@link Integer}) are not
 * supported.  Asserts will check for invalid non-primitive types passed to these methods, but when asserts are disabled
 * passing boxed types will yield incorrect results.
 */
public abstract class PrimitiveNumberTypes {
  private PrimitiveNumberTypes() { }

  /**
   * @return true iff type is {@code double.class} or {@code float.class}
   * @param type the primitive type whose floatingpointitude is to be queried
   */
  public static boolean isFloatingPoint(Class<? extends Number> type) {
    assert type.isPrimitive();
    return type == float.class || type == double.class;
  }

  /**
   * @return the number of bits needed to represent the specified type (for example, floats and ints both require 32
   *         bits)
   *
   * @param type the type whose bit width is sought
   */
  public static int bitCount(Class<? extends Number> type) {
    assert type.isPrimitive();

    if (type == byte.class) {
      return 8;
    } else if (type == short.class) {
      return 16;
    } else if (type == int.class) {
      return 32;
    } else if (type == long.class) {
      return 64;
    } else if (type == float.class) {
      return 32;
    } else if (type == double.class) {
      return 64;
    } else {
      throw new IllegalArgumentException("Unknown primitive number type: " + type);
    }
  }

  /**
   * Returns float.class if {@code minBits <= 32} and {@code double.class} otherwise.
   *
   * @param minBits the minimum bits the float should have
   * @return the smallest floating point primitive type with at least the number of bits specified (or
   *         {@code double.class} if the requested number of bits is greater than 64)
   */
  public static Class<? extends Number> floatOfSize(int minBits) {
    return minBits <= 32 ? float.class : double.class;
  }

  /**
   * Returns the smallest integer type with at least as many bits as specified, or {@code long.class} if the requested
   * number of bits is greater than 64.
   *
   * @param minBits the minimum number of bits the returned primitive type should have
   * @return the smallest integer type with the requested number of bits
   */
  public static Class<? extends Number> integerOfSize(int minBits) {
    if (minBits <= 8) {
      return byte.class;
    } else if (minBits <= 16) {
      return short.class;
    } else if (minBits <= 32) {
      return int.class;
    } else {
      return long.class;
    }
  }

  /**
   * Gets the smallest possible primitive type that can hold the sum of two primitive types.  If no type is large
   * enough (e.g. if summing two {@code long}s), {@code double.class} will be returned.
   *
   * @param type1 the first primitive type
   * @param type2 the second primitive type
   * @return the smallest possible primitive type capable of holding the sum of any two values of the given types
   */
  public static Class<? extends Number> sumType(Class<? extends Number> type1, Class<? extends Number> type2) {
    assert type1.isPrimitive();
    assert type2.isPrimitive();

    if (isFloatingPoint(type1) || isFloatingPoint(type2)) {
      return double.class;
    }

    int minBits = Math.max(bitCount(type1), bitCount(type2)) + 1; // + 1 due to carryover
    return minBits > 64 ? double.class : integerOfSize(minBits);
  }

  /**
   * Gets the smallest possible primitive type that can hold the product of two primitive types.  If no type is large
   * enough (e.g. if multiplying two {@code long}s), {@code double.class} will be returned.
   *
   * @param type1 the first primitive type
   * @param type2 the second primitive type
   * @return the smallest possible primitive type capable of holding the product of any two values of the given types
   */
  public static Class<? extends Number> productType(Class<? extends Number> type1, Class<? extends Number> type2) {
    assert type1.isPrimitive();
    assert type2.isPrimitive();

    if (isFloatingPoint(type1) || isFloatingPoint(type2)) {
      return double.class;
    }

    int minBits = bitCount(type1) + bitCount(type2);
    return minBits > 64 ? double.class : integerOfSize(minBits);
  }

  /**
   * Gets the smallest possible primitive type that can hold the negative value (e.g. -x) of a type.  For integers
   * (which in Java use two's complement representation) this is tricky because the smallest number in the integer's
   * range will have no corresponding positive value; therefore, the primitive type of the negation must be slightly
   * larger.
   *
   * Because there is no larger integer type available, the negated type of a long is taken to be a double.
   *
   * @param type the type being negated
   * @return the smallest possible primitive type capable of holding the negation of a value of the provided type
   */
  public static Class<? extends Number> negatedType(Class<? extends Number> type) {
    assert type.isPrimitive();

    if (isFloatingPoint(type)) {
      return type;
    } else if (type == long.class) {
      return double.class;
    }

    return integerOfSize(bitCount(type) + 1);
  }

  /**
   * Checks if the provided number type can be losslessly stored in a single-precision float (this is true for
   * single-precision floats, short integers, and bytes).
   *
   * @param type the numeric primitive type to check
   * @return true if values of the provided type can always be losslessly stored within a single-precision float
   */
  public static boolean isStorableAsSinglePrecisionFloat(Class<? extends Number> type) {
    assert type.isPrimitive();

    if (type == float.class) {
      return true;
    } else if (type == double.class) {
      return false;
    }

    return (bitCount(type) <= 16); // can be stored as a float if this is a short integer or smaller
  }

  /**
   * Calculates the smallest common type capable of holding the values of both provided types (losslessly if possible,
   * otherwise with the required range but possibly a loss of precision).  For example, the smallest common type for a
   * {@code long} and a {@code float} is considered to be a double.
   *
   * @param type1 the first type
   * @param type2 the second type
   * @return the smallest common type capable of holding values of both types
   */
  public static Class<? extends Number> smallestCommonType(Class<? extends Number> type1,
      Class<? extends Number> type2) {
    assert type1.isPrimitive();
    assert type2.isPrimitive();

    if (type1 == type2) {
      return type1;
    } else if (type1 == double.class || type2 == double.class) {
      return double.class;
    } else if (type1 == float.class) { // at this stage, we now know: type1 != type2 and neither type is a double
      // type2 must be an integer
      return bitCount(type2) <= 16 ? float.class : double.class;
    } else if (type2 == float.class) {
      // type1 must be an integer
      return bitCount(type1) <= 16 ? float.class : double.class;
    } else { // we now know both types are integers
      return bitCount(type1) > bitCount(type2) ? type1 : type2;
    }
  }

  /**
   * @return the smallest type capable of losslessly holding a given value
   * @param value the value whose type is sought
   */
  public static Class<? extends Number> smallestTypeForValue(double value) {
    long longValue = (long) value;
    if (longValue != value) { // this catches the case where value is a non-integer, infinite or NaN
      return (float) value == value ? float.class : double.class;
    }

    // at this point, the value must fit into a long integer; do a binary search to figure out the smallest type
    if (value >= 0) {
      if (value > Short.MAX_VALUE) {
        return value > Integer.MAX_VALUE ? long.class : int.class;
      } else {
        return value > Byte.MAX_VALUE ? short.class : byte.class;
      }
    } else {
      if (value < Short.MIN_VALUE) {
        return value < Integer.MIN_VALUE ? long.class : int.class;
      } else {
        return value < Byte.MIN_VALUE ? short.class : byte.class;
      }
    }
  }
}
