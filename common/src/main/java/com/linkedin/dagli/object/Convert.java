package com.linkedin.dagli.object;

import com.linkedin.dagli.function.FunctionResult1;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.transformer.PreparedTransformer1;
import com.linkedin.dagli.util.array.ArraysEx;


/**
 * Methods that create transformers for common conversions.
 *
 * Unless otherwise noted, conversions follow the convention that null always converts to null.
 */
public abstract class Convert {
  private Convert() { }

  /**
   * Conversion transformers from {@link java.lang.Object}s to something else.
   */
  public static abstract class Object {
    private Object() { }

    /**
     * Casts the values provided by the given input to the specified class.  A {@link ClassCastException} will be thrown
     * during DAG execution if the input cannot be cast to the specified type.
     *
     * @param input the {@link Producer} supplying input values to cast
     * @param targetClass the class to which the input values should be cast
     * @param <T> the type of the class to which to cast
     * @return a (prepared) transformer that will convert its input values to the target class
     */
    public static <T> PreparedTransformer1<java.lang.Object, T> toClass(Producer<?> input,
        Class<? extends T> targetClass) {
      return new Cast<T>(targetClass).withInput(input);
    }

    /**
     * Casts the values provided by the given input to the specified class.  If the input cannot be cast to the
     * specified type, the result will be null.
     *
     * @param input the {@link Producer} supplying input values to cast
     * @param targetClass the class to which the input values should be cast
     * @param <T> the type of the class to which to cast
     * @return a (prepared) transformer that will convert its input values to the target class
     */
    public static <T> PreparedTransformer1<java.lang.Object, T> toClassOrNull(
        Producer<?> input, Class<? extends T> targetClass) {
      return new Cast<T>(targetClass).withNullIfUncastable().withInput(input);
    }
  }

  /**
   * Conversion transformers from numbers to something else.
   */
  public static abstract class Number {
    private Number() { }

    /**
     * Converts a Number type (e.g. Integer, Long, Float...) to a Double.
     *
     * Depending on the concrete type of {@link Number}, the conversion may result in rounding or truncation.
     *
     * @param input the producer that will provide the Numbers to be converted.
     * @param <T> the subtype of Number to be converted
     * @return a transformer that converts its inputs to the desired Number subtype.
     */
    public static <T extends java.lang.Number> PreparedTransformer1<T, Double> toDouble(Producer<T> input) {
      return new FunctionResult1<T, Double>().withFunction(T::doubleValue).withNullResultOnNullInput().withInput(input);
    }

    /**
     * Converts a Number type (e.g. Integer, Long, Float...) to a Float.
     *
     * Depending on the concrete type of {@link Number}, the conversion may result in rounding or truncation.
     *
     * @param input the producer that will provide the Numbers to be converted.
     * @param <T> the subtype of Number to be converted
     * @return a transformer that converts its inputs to the desired Number subtype.
     */
    public static <T extends java.lang.Number> PreparedTransformer1<T, Float> toFloat(Producer<T> input) {
      return new FunctionResult1<T, Float>().withFunction(T::floatValue).withNullResultOnNullInput().withInput(input);
    }

    /**
     * Converts a Number type (e.g. Integer, Long, Float...) to a Long.
     *
     * Depending on the concrete type of {@link Number}, the conversion may result in rounding or truncation.
     *
     * @param input the producer that will provide the Numbers to be converted.
     * @param <T> the subtype of Number to be converted
     * @return a transformer that converts its inputs to the desired Number subtype.
     */
    public static <T extends java.lang.Number> PreparedTransformer1<T, Long> toLong(Producer<T> input) {
      return new FunctionResult1<T, Long>().withFunction(T::longValue).withNullResultOnNullInput().withInput(input);
    }

    /**
     * Converts a Number type (e.g. Integer, Long, Float...) to an Integer.
     *
     * Depending on the concrete type of {@link Number}, the conversion may result in rounding or truncation.
     *
     * @param input the producer that will provide the Numbers to be converted.
     * @param <T> the subtype of Number to be converted
     * @return a transformer that converts its inputs to the desired Number subtype.
     */
    public static <T extends java.lang.Number> PreparedTransformer1<T, Integer> toInteger(Producer<T> input) {
      return new FunctionResult1<T, Integer>().withFunction(T::intValue).withNullResultOnNullInput().withInput(input);
    }

    /**
     * Converts a Number type (e.g. Integer, Long, Float...) to a Short.
     *
     * Depending on the concrete type of {@link Number}, the conversion may result in rounding or truncation.
     *
     * @param input the producer that will provide the Numbers to be converted.
     * @param <T> the subtype of Number to be converted
     * @return a transformer that converts its inputs to the desired Number subtype.
     */
    public static <T extends java.lang.Number> PreparedTransformer1<T, Short> toShort(Producer<T> input) {
      return new FunctionResult1<T, Short>().withFunction(T::shortValue).withNullResultOnNullInput().withInput(input);
    }

    /**
     * Converts a Number type (e.g. Integer, Long, Float...) to a Byte.
     *
     * Depending on the concrete type of {@link Number}, the conversion may result in rounding or truncation.
     *
     * @param input the producer that will provide the Numbers to be converted
     * @param <T> the subtype of Number to be converted
     * @return a transformer that converts its inputs to the desired Number subtype
     */
    public static <T extends java.lang.Number> PreparedTransformer1<T, Byte> toByte(Producer<T> input) {
      return new FunctionResult1<T, Byte>().withFunction(T::byteValue).withNullResultOnNullInput().withInput(input);
    }
  }

  /**
   * Conversion transformers from {@link Iterable}s of {@link java.lang.Number}s to something else.
   */
  public static abstract class Numbers {
    private Numbers() { }

    /**
     * Converts an {@link Iterable} of {@link java.lang.Number}s to an array of doubles.
     *
     * Depending on the concrete type of {@link java.lang.Number}, the conversion may result in rounding or truncation.
     *
     * @param input the producer that will provide the {@link java.lang.Number}s to be converted
     * @return a transformer that converts the provided input to an array of doubles
     */
    public static PreparedTransformer1<Iterable<? extends java.lang.Number>, double[]> toDoubleArray(
        Producer<? extends Iterable<? extends java.lang.Number>> input) {
      return new FunctionResult1<Iterable<? extends java.lang.Number>, double[]>().withFunction(ArraysEx::toDoublesLossy)
          .withNullResultOnNullInput()
          .withInput(input);
    }

    /**
     * Converts an {@link Iterable} of {@link java.lang.Number}s to an array of floats.
     *
     * Depending on the concrete type of {@link java.lang.Number}, the conversion may result in rounding or truncation.
     *
     * @param input the producer that will provide the {@link java.lang.Number}s to be converted
     * @param <T> the subtype of {@link java.lang.Number} to be converted
     * @return a transformer that converts the provided input to an array of floats
     */
    public static <T extends java.lang.Number> PreparedTransformer1<Iterable<T>, float[]> toFloatArray(
        Producer<? extends Iterable<T>> input) {
      return new FunctionResult1<Iterable<T>, float[]>().withFunction(ArraysEx::toFloatsLossy)
          .withNullResultOnNullInput()
          .withInput(input);
    }

    /**
     * Converts an {@link Iterable} of {@link java.lang.Number}s to an array of bytes.
     *
     * Depending on the concrete type of {@link java.lang.Number}, the conversion may result in rounding or truncation.
     *
     * @param input the producer that will provide the {@link java.lang.Number}s to be converted
     * @return a transformer that converts the provided input to an array of bytes
     */
    public static PreparedTransformer1<Iterable<? extends java.lang.Number>, byte[]> toByteArray(
        Producer<? extends Iterable<? extends java.lang.Number>> input) {
      return new FunctionResult1<Iterable<? extends java.lang.Number>, byte[]>().withFunction(ArraysEx::toBytesLossy)
          .withNullResultOnNullInput()
          .withInput(input);
    }

    /**
     * Converts an {@link Iterable} of {@link java.lang.Number}s to an array of shorts.
     *
     * Depending on the concrete type of {@link java.lang.Number}, the conversion may result in rounding or truncation.
     *
     * @param input the producer that will provide the {@link java.lang.Number}s to be converted
     * @return a transformer that converts the provided input to an array of shorts
     */
    public static PreparedTransformer1<Iterable<? extends java.lang.Number>, short[]> toShortArray(
        Producer<? extends Iterable<? extends java.lang.Number>> input) {
      return new FunctionResult1<Iterable<? extends java.lang.Number>, short[]>().withFunction(ArraysEx::toShortsLossy)
          .withNullResultOnNullInput()
          .withInput(input);
    }

    /**
     * Converts an {@link Iterable} of {@link java.lang.Number}s to an array of ints.
     *
     * Depending on the concrete type of {@link java.lang.Number}, the conversion may result in rounding or truncation.
     *
     * @param input the producer that will provide the {@link java.lang.Number}s to be converted
     * @return a transformer that converts the provided input to an array of ints
     */
    public static PreparedTransformer1<Iterable<? extends java.lang.Number>, int[]> toIntegerArray(
        Producer<? extends Iterable<? extends java.lang.Number>> input) {
      return new FunctionResult1<Iterable<? extends java.lang.Number>, int[]>().withFunction(ArraysEx::toIntegersLossy)
          .withNullResultOnNullInput()
          .withInput(input);
    }

    /**
     * Converts an {@link Iterable} of {@link java.lang.Number}s to an array of longs.
     *
     * Depending on the concrete type of {@link java.lang.Number}, the conversion may result in rounding or truncation.
     *
     * @param input the producer that will provide the {@link java.lang.Number}s to be converted
     * @return a transformer that converts the provided input to an array of longs
     */
    public static PreparedTransformer1<Iterable<? extends java.lang.Number>, long[]> toLongArray(
        Producer<? extends Iterable<? extends java.lang.Number>> input) {
      return new FunctionResult1<Iterable<? extends java.lang.Number>, long[]>().withFunction(ArraysEx::toLongsLossy)
          .withNullResultOnNullInput()
          .withInput(input);
    }
  }

  /**
   * Conversion transformers from Strings to something else
   */
  public static abstract class String {
    private String() { }

    /**
     * Returns a transformer that converts a String input as if by Double::valueOf
     *
     * @param input the producer that will provide the input String
     * @return a transformer that performs the desired conversion
     */
    public static PreparedTransformer1<java.lang.String, Double> toDouble(Producer<java.lang.String> input) {
      return new FunctionResult1<java.lang.String, Double>()
          .withFunction(Double::valueOf)
          .withNullResultOnNullInput()
          .withInput(input);
    }

    /**
     * Returns a transformer that converts a String input as if by Float::valueOf
     *
     * @param input the producer that will provide the input String
     * @return a transformer that performs the desired conversion
     */
    public static PreparedTransformer1<java.lang.String, Float> toFloat(Producer<java.lang.String> input) {
      return new FunctionResult1<java.lang.String, Float>()
          .withFunction(Float::valueOf)
          .withNullResultOnNullInput()
          .withInput(input);
    }

    /**
     * Returns a transformer that converts a String input as if by Long::valueOf
     *
     * @param input the producer that will provide the input String
     * @return a transformer that performs the desired conversion
     */
    public static PreparedTransformer1<java.lang.String, Long> toLong(Producer<java.lang.String> input) {
      return new FunctionResult1<java.lang.String, Long>()
          .withFunction(Long::valueOf)
          .withNullResultOnNullInput()
          .withInput(input);
    }

    /**
     * Returns a transformer that converts a String input as if by Integer::valueOf
     *
     * @param input the producer that will provide the input String
     * @return a transformer that performs the desired conversion
     */
    public static PreparedTransformer1<java.lang.String, Integer> toInteger(Producer<java.lang.String> input) {
      return new FunctionResult1<java.lang.String, Integer>()
          .withFunction(Integer::valueOf)
          .withNullResultOnNullInput()
          .withInput(input);
    }

    /**
     * Returns a transformer that converts a String input as if by Short::valueOf
     *
     * @param input the producer that will provide the input String
     * @return a transformer that performs the desired conversion
     */
    public static PreparedTransformer1<java.lang.String, Short> toShort(Producer<java.lang.String> input) {
      return new FunctionResult1<java.lang.String, Short>()
          .withFunction(Short::valueOf)
          .withNullResultOnNullInput()
          .withInput(input);
    }

    /**
     * Returns a transformer that converts a String input as if by Byte::valueOf
     *
     * @param input the producer that will provide the input String
     * @return a transformer that performs the desired conversion
     */
    public static PreparedTransformer1<java.lang.String, Byte> toByte(Producer<java.lang.String> input) {
      return new FunctionResult1<java.lang.String, Byte>()
          .withFunction(Byte::valueOf)
          .withNullResultOnNullInput()
          .withInput(input);
    }

    /**
     * Returns a transformer that converts a String input as if by Boolean::valueOf
     *
     * @param input the producer that will provide the input String
     * @return a transformer that performs the desired conversion
     */
    public static PreparedTransformer1<java.lang.String, Boolean> toBoolean(Producer<java.lang.String> input) {
      return new FunctionResult1<java.lang.String, Boolean>()
          .withFunction(Boolean::valueOf)
          .withNullResultOnNullInput()
          .withInput(input);
    }
  }

  /**
   * Converts an object to its String representation via its toString() method.
   *
   * @param input the producer that will provide the objects to be Stringified
   * @param <T> the type of object to be Stringified
   * @return a transformer that will provide the String representation of the inputted object
   */
  public static <T> PreparedTransformer1<T, java.lang.String> toString(Producer<T> input) {
    return new FunctionResult1<T, java.lang.String>()
        .withFunction(T::toString)
        .withNullResultOnNullInput()
        .withInput(input);
  }
}
