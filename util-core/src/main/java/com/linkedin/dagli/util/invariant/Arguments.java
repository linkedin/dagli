package com.linkedin.dagli.util.invariant;

import com.linkedin.dagli.util.closeable.Closeables;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Supplier;


/**
 * This class is heavily inspired by Guava's Preconditions class.  We use this class instead to avoid a dependency on
 * Guava.
 */
public abstract class Arguments {
  private Arguments() { }

  /**
   * Checks if a provided value is within the specified range (min <= value <= max), throwing an
   * {@link IllegalArgumentException} if not.
   *
   * @param value the value to check
   * @param minimumInclusive the minimum permissible index value (inclusive)
   * @param maximumInclusive the maximum permissible index value (inclusive)
   */
  public static long inInclusiveRange(long value, long minimumInclusive, long maximumInclusive) {
    return inInclusiveRange(value, minimumInclusive, maximumInclusive, "Value", null);
  }

  /**
   * Checks if a provided value is within the specified range (min <= value <= max), throwing an
   * {@link IllegalArgumentException} if not.
   *
   * @param value the value to check
   * @param minimumInclusive the minimum permissible index value (inclusive)
   * @param maximumInclusive the maximum permissible index value (inclusive)
   * @param valueName the name of the value (e.g. "size", "index", etc.); used to generate the exception's message
   * @param supplementaryMessage a supplier that provides a supplementary message for the thrown exception, or null
   */
  public static long inInclusiveRange(long value, long minimumInclusive, long maximumInclusive, String valueName,
      Supplier<String> supplementaryMessage) {
    if (value < minimumInclusive || value > maximumInclusive) {
      throw new IllegalArgumentException(
          valueName + " given as " + value + "; must be at least " + minimumInclusive + " and at most "
              + maximumInclusive + (supplementaryMessage == null ? "" : (": " + supplementaryMessage.get())));
    }
    return value;
  }

  /**
   * Checks if a provided value is within the specified range (min <= value <= max), throwing an
   * {@link IllegalArgumentException} if not.
   *
   * @param value the value to check
   * @param minimumInclusive the minimum permissible index value (inclusive)
   * @param maximumInclusive the maximum permissible index value (inclusive)
   */
  public static int inInclusiveRange(int value, int minimumInclusive, int maximumInclusive) {
    return inInclusiveRange(value, minimumInclusive, maximumInclusive, "Value", null);
  }

  /**
   * Checks if a provided value is within the specified range (min <= value <= max), throwing an
   * {@link IllegalArgumentException} if not.
   *
   * @param value the value to check
   * @param minimumInclusive the minimum permissible index value (inclusive)
   * @param maximumInclusive the maximum permissible index value (inclusive)
   * @param valueName the name of the value (e.g. "size", "index", etc.); used to generate the exception's message
   * @param supplementaryMessage a supplier that provides a supplementary message for the thrown exception, or null
   */
  public static int inInclusiveRange(int value, int minimumInclusive, int maximumInclusive, String valueName,
      Supplier<String> supplementaryMessage) {
    if (value < minimumInclusive || value > maximumInclusive) {
      throw new IllegalArgumentException(
          valueName + " given as " + value + "; must be at least " + minimumInclusive + " and at most "
              + maximumInclusive + (supplementaryMessage == null ? "" : (": " + supplementaryMessage.get())));
    }
    return value;
  }

  /**
   * Checks if a provided index is within the specified range, throwing an {@link IndexOutOfBoundsException} if not.
   * @param index the index value to check
   * @param minimumInclusive the minimum permissible index value (inclusive)
   * @param maximumExclusive the maximum permissible index value, exclusive (i.e. <code>index == maximumExclusive</code>
   *                         will throw an exception, but <code>index == maximumExclusive - 1</code> is acceptable.)
   * @param message a supplier that provides the message for the thrown exception
   */
  public static int indexInRange(int index, int minimumInclusive, int maximumExclusive, Supplier<String> message) {
    if (index < minimumInclusive || index >= maximumExclusive) {
      throw new IndexOutOfBoundsException(
          "Value of " + index + " provided; must be at least " + minimumInclusive + " and less than " + maximumExclusive
              + ": " + message.get());
    }
    return index;
  }

  /**
   * Checks if a provided index is within the specified range, throwing an {@link IndexOutOfBoundsException} if not.
   * @param index the index value to check
   * @param minimumInclusive the minimum permissible index value (inclusive)
   * @param maximumExclusive the maximum permissible index value, exclusive (i.e. <code>index == maximumExclusive</code>
   *                         will throw an exception, but <code>index == maximumExclusive - 1</code> is acceptable.)
   * @param message a supplier that provides the message for the thrown exception
   */
  public static long indexInRange(long index, long minimumInclusive, long maximumExclusive, Supplier<String> message) {
    if (index < minimumInclusive || index >= maximumExclusive) {
      throw new IndexOutOfBoundsException(
          "Value of " + index + " provided; must be at least " + minimumInclusive + " and less than " + maximumExclusive
              + ": " + message.get());
    }
    return index;
  }

  /**
   * Checks if an argument is among a set of supported values.  This is done by linearly scanning a list of values
   * and comparing them with the argument value using {@link Objects#equals(Object, Object)}.
   *
   * @param val the value to check for presence in the acceptable items list
   * @param items the set of values that val is expected to belong to
   * @param <T> the type of values being checked
   * @return val
   * @throws IllegalArgumentException if val is not one of the acceptable values
   */
  @SafeVarargs // items does not leak from this class
  public static <T> T inSet(T val, Supplier<String> message, T... items) {
    for (T item : items) {
      if (Objects.equals(val, item)) {
        return val;
      }
    }

    throw new IllegalArgumentException(message.get());
  }

  /**
   * Checks if a collection of items is a subset of a set of supported values.
   *
   * @param values a collection of values that should be a subset of the provided items
   * @param messageGenerator given its 0-based position and the element itself, returns an error message for an item
   *                         in {@code values} that is not a member of {@code supportedSuperset}
   * @param supportedSuperset the set of values that set is expected to be a subset of (this does not have to be a
   *                          proper subset; i.e. it's acceptable if the subset has exactly the same items as the
   *                          superset)
   *
   * @param <T> the type of values being checked
   * @param <S> the type of collection for the subset
   * @return the subset
   * @throws IllegalArgumentException if the purported {@code values} are not a subset of {@code supportedSuperset}
   */
  public static <T, S extends Iterable<? extends T>> S subset(S values, Set<T> supportedSuperset,
      BiFunction<Integer, T, String> messageGenerator) {

    java.util.Iterator<? extends T> iterator = values.iterator();
    int index = 0;
    try {
      while (iterator.hasNext()) {
        T value = iterator.next();
        if (!supportedSuperset.contains(value)) {
          throw new IllegalArgumentException(messageGenerator.apply(index, value));
        }
        index++;
      }
    } finally {
      Closeables.tryClose(iterator); // handle closeable iterators
    }

    return values;
  }

  /**
   * Checks if an argument is among a set of acceptable values.
   *
   * @param val the value to check for presence in the acceptable items list
   * @param items the set of values that val is expected to belong to
   * @return val
   * @throws IllegalArgumentException if val is not one of the supported values
   */
  public static int inIntSet(int val, Supplier<String> message, int... items) {
    for (int item : items) {
      if (val == item) {
        return val;
      }
    }

    throw new IllegalArgumentException(message.get());
  }

  /**
   * Throws an IllegalArgumentException if the arguments are not equal to each other.
   *
   * @param value1 the first value to compare
   * @param value2 the second value to compare
   * @param message a supplier providing a message to include in the exception if the values are not equal
   * @throws IllegalArgumentException if condition is 'false'
   * @return the value (the method only returns if value1 == value2)
   */
  public static int equals(int value1, int value2, Supplier<String> message) {
    if (value1 != value2) {
      throw new IllegalArgumentException(message.get() + " (" + value1 + " != " + value2 + ")");
    }
    return value1;
  }

  /**
   * Throws an IllegalArgumentException if the argument is 'false'.
   *
   * @param condition the condition to check
   * @param message the message to include in the exception if condition is 'false'
   * @throws IllegalArgumentException if condition is 'false'
   */
  public static void check(boolean condition, String message) {
    if (!condition) {
      throw new IllegalArgumentException(message);
    }
  }

  /**
   * Throws an IllegalArgumentException if the argument is 'false'.
   *
   * @param condition the condition to check
   * @param message a supplier providing a message to include in the exception if condition is 'false'
   * @throws IllegalArgumentException if condition is 'false'
   */
  public static void check(boolean condition, Supplier<String> message) {
    if (!condition) {
      throw new IllegalArgumentException(message.get());
    }
  }

  /**
   * Throws an IllegalArgumentException if the argument is 'false'.
   *
   * @param condition the condition to check
   * @throws IllegalArgumentException if condition is 'false'
   */
  public static void check(boolean condition) {
    if (!condition) {
      throw new IllegalArgumentException();
    }
  }

  /**
   * Checks that all items are distinct (as determined by {@link Objects#equals(Object, Object)} and returns a set
   * containing these items.
   *
   * @param items the items whose distinctness should be checked
   * @param messageGenerator a function that generates an error message from the 0-based position of the first
   *                         non-distinct item in {@code items} and the item itself
   * @param <T> the type of the items being checked
   * @return a {@link Set} containing the items
   */
  public static <T> Set<T> distinct(Iterable<T> items, BiFunction<Integer, T, String> messageGenerator) {
    java.util.Iterator<T> iterator = items.iterator();
    HashSet<T> itemSet = new HashSet<>();
    try {
      int index = 0;
      while (iterator.hasNext()) {
        T next = iterator.next();
        if (!itemSet.add(next)) {
          throw new IllegalArgumentException(messageGenerator.apply(index, next));
        }
        index++;
      }
    } finally {
      Closeables.tryClose(iterator); // close the iterator, if appropriate
    }

    return itemSet;
  }
}
