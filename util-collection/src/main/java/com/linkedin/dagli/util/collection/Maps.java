package com.linkedin.dagli.util.collection;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;


/**
 * Utility class for manipulating {@link java.util.Map}s.
 */
public abstract class Maps {
  private Maps() { }

  /**
   * A trivial {@link BiFunction} that, for an entry passed as key and value parameters, returns the key.
   *
   * @param <K> the type of the key
   * @param <V> the type of the value
   * @return a function that returns the key of a passed entry
   */
  public static <K, V> BiFunction<K, V, K> entryKey() {
    return (k, v) -> k;
  }

  /**
   * A trivial {@link BiFunction} that, for an entry passed as key and value parameters, returns the value.
   *
   * @param <K> the type of the key
   * @param <V> the type of the value
   * @return a function that returns the value of a passed entry
   */
  public static <K, V> BiFunction<K, V, V> entryValue() {
    return (k, v) -> v;
  }

  /**
   * Returns a new {@link Map} where the entries from an original {@link Map} have been transformed by the provided
   * key and value transformation methods.
   *
   * @param original the original map whose entries should be r
   * @param keyTransformer a function that accepts an entry from the original map and returns a new key
   * @param valueTransformer a function that accepts an entry from the original map and returns a new value
   * @param constructor a function (typically a constructor, e.g. HashMap::new) that maps the expected size of the
   *                    resulting map to a (presumably new) map instance that will be returned by this method
   * @param throwOnDuplicateTransformedKey if true, a {@link IllegalArgumentException} will be thrown if there are
   *                                       duplicate transformed keys; if false, no exception will result and the value
   *                                       associated with the transformed key in the resulting map can be any of the
   *                                       transformed values generated from the same original entries
   * @param discardOnNullTransformedKey if true, entries in the original map for which the keyTransformer returns null
   *                                    will be ignored and not affect resulting map; otherwise, null is treated like
   *                                    any other key value (note that not all classes implementing {@link Map} support
   *                                    null keys!)
   * @param <M> the type of the transformed map
   * @param <X> the type of key in the transformed map
   * @param <Y> the type of value in the transformed map
   * @param <K> the type of key in the original map
   * @param <V> the type of value in the original and transformed maps
   * @return a transformed map containing the same values as the original map but with transformed keys
   */
  public static <M extends Map<X, Y>, K, V, X, Y> M replaceEntries(Map<K, V> original,
      BiFunction<K, V, X> keyTransformer, BiFunction<K, V, Y> valueTransformer, IntFunction<M> constructor,
      boolean throwOnDuplicateTransformedKey, boolean discardOnNullTransformedKey) {
    M result = constructor.apply(original.size());

    original.forEach((key, value) -> {
      X transformedKey = keyTransformer.apply(key, value);
      if (discardOnNullTransformedKey && transformedKey == null) {
        return; // ignore null keys
      } else if (throwOnDuplicateTransformedKey && result.containsKey(transformedKey)) {
        throw new IllegalArgumentException(
            "Transforming the key " + key + " resulting in the duplicate transformed key " + transformedKey);
      }
      result.put(transformedKey, valueTransformer.apply(key, value));
    });

    return result;
  }

  /**
   * Returns a new {@link Map} where the entries from an original {@link Map} have been transformed by the provided
   * key and value transformation methods.
   *
   * If there are duplicate transformed keys, the value associated with that key in the resulting map can be any of the
   * transformed values generated from the same original entries.
   *
   * @param original the original map whose entries should be r
   * @param keyTransformer a function that accepts an entry from the original map and returns a new key
   * @param valueTransformer a function that accepts an entry from the original map and returns a new value
   * @param constructor a function (typically a constructor, e.g. HashMap::new) that maps the expected size of the
   *                    resulting map to a (presumably new) map instance that will be returned by this method
   * @param <M> the type of the transformed map
   * @param <X> the type of key in the transformed map
   * @param <Y> the type of value in the transformed map
   * @param <K> the type of key in the original map
   * @param <V> the type of value in the original and transformed maps
   * @return a transformed map containing the same values as the original map but with transformed keys
   */
  public static <M extends Map<X, Y>, K, V, X, Y> M replaceEntries(Map<K, V> original,
      BiFunction<K, V, X> keyTransformer, BiFunction<K, V, Y> valueTransformer, IntFunction<M> constructor) {
    return replaceEntries(original, keyTransformer, valueTransformer, constructor, false, false);
  }

  /**
   * Returns a new {@link Map} where the values from an original {@link Map} have been transformed by a provided
   * transformation method, keeping the same keys.
   *
   * @param original the original map whose values should be replaced
   * @param valueTransformer the transformer that maps the old values to the new values
   * @param constructor a function (typically a constructor, e.g. HashMap::new) that maps the expected size of the
   *                    resulting map to a (typically new) map instance that will be returned by this method
   * @param discardNullTransformedValue if true and the valueTransformer returns null, the entry in the original map
   *                                    will be ignored and not appear in the resulting map; otherwise, null is treated
   *                                    like any value (though not all {@link Map} implementations support null values!)
   * @param <M> the type of the transformed map
   * @param <Y> the type of value in the transformed map
   * @param <K> the type of key in the original map
   * @param <V> the type of value in the original and transformed maps
   * @return a transformed map containing the same keys as the original map but with transformed values
   */
  public static <M extends Map<K, Y>, Y, K, V> M replaceValues(Map<K, V> original, Function<V, Y> valueTransformer,
      IntFunction<M> constructor, boolean discardNullTransformedValue) {

    M result = constructor.apply(original.size());
    original.forEach((key, value) -> {
      Y transformedValue = valueTransformer.apply(value);
      if (!discardNullTransformedValue || transformedValue != null) {
        result.put(key, transformedValue);
      }
    });

    return result;
  }

  /**
   * Returns a new {@link Map} where the values from an original {@link Map} have been transformed by a provided
   * transformation method, keeping the same keys.
   *
   * @param original the original map whose values should be replaced
   * @param valueTransformer the transformer that maps the old values to the new values
   * @param <Y> the type of value in the transformed map
   * @param <K> the type of key in the original map
   * @param <V> the type of value in the original and transformed maps
   * @return a transformed map containing the same keys as the original map but with transformed values
   */
  public static <Y, K, V> Map<K, Y> replaceValues(Map<K, V> original, Function<V, Y> valueTransformer) {
    return replaceValues(original, valueTransformer, HashMap::new, false);
  }

  /**
   * Returns a new {@link Map} where the keys from an original {@link Map} have been transformed by a provided
   * transformation method, keeping the same values.
   *
   * @param original the original map whose keys should be replaced
   * @param keyTransformer the transformer that maps the old keys to the new keys
   * @param constructor a function (typically a constructor, e.g. HashMap::new) that maps the expected size of the
   *                    resulting map to a (typically new) map instance that will be returned by this method
   * @param throwOnDuplicateTransformedKey if true, a {@link IllegalArgumentException} will be thrown if multiple keys
   *                                       in the original map are transformed into the same key; if false, no exception
   *                                       will result and the value of the transformed key in the resulting map can
   *                                       be any of the values associated with those original keys
   * @param discardNullTransformedKey if true, if the keyTransformer returns null the entry in the original map will be
   *                                  ignored and not appear in the resulting map; otherwise, null is treated like any
   *                                  other key value (note that not all classes implementing {@link Map} support null
   *                                  keys!)
   * @param <M> the type of the transformed map
   * @param <X> the type of key in the transformed map
   * @param <K> the type of key in the original map
   * @param <V> the type of value in the original and transformed maps
   * @return a transformed map containing the same values as the original map but with transformed keys
   */
  public static <M extends Map<X, V>, X, K, V> M replaceKeys(Map<K, V> original, Function<K, X> keyTransformer,
      IntFunction<M> constructor, boolean throwOnDuplicateTransformedKey, boolean discardNullTransformedKey) {
    return replaceEntries(original, (k, v) -> keyTransformer.apply(k), entryValue(), constructor,
        throwOnDuplicateTransformedKey, discardNullTransformedKey);
  }

  /**
   * Returns a new {@link Map} where the keys from an original {@link Map} have been transformed by a provided
   * transformation method, keeping the same values.
   *
   * If multiple original keys correspond to the same transformed key, the value corresponding to the transformed key
   * in the returned map can be any of the values corresponding to the original keys
   *
   * @param original the original map whose keys should be replaced
   * @param keyTransformer the transformer that maps the old keys to the new keys
   * @param constructor a function (typically a constructor, e.g. HashMap::new) that maps the expected size of the
   *                    resulting map to a (typically new) map instance that will be returned by this method
   * @param <M> the type of the transformed map
   * @param <X> the type of key in the transformed map
   * @param <K> the type of key in the original map
   * @param <V> the type of value in the original and transformed maps
   * @return a transformed map containing the same values as the original map but with transformed keys
   */
  public static <M extends Map<X, V>, X, K, V> M replaceKeys(Map<K, V> original, Function<K, X> keyTransformer,
      IntFunction<M> constructor) {
    return replaceKeys(original, keyTransformer, constructor, false, false);
  }

  /**
   * Returns a new {@link Map} where the keys from an original {@link Map} have been transformed by a provided
   * transformation method, keeping the same values.
   *
   * If multiple original keys correspond to the same transformed key, the value corresponding to the transformed key
   * in the returned map can be any of the values corresponding to the original keys
   *
   * @param original the original map whose keys should be replaced
   * @param keyTransformer the transformer that maps the old keys to the new keys
   * @param <X> the type of key in the transformed map
   * @param <K> the type of key in the original map
   * @param <V> the type of value in the original and transformed maps
   * @return a transformed map containing the same values as the original map but with transformed keys
   */
  public static <X, K, V> Map<X, V> replaceKeys(Map<K, V> original, Function<K, X> keyTransformer) {
    return replaceKeys(original, keyTransformer, HashMap::new);
  }

  /**
   * Returns a new {@link Map} containing only those entries from a map where the key satisfies some predicate.
   *
   * @param original the original map to be filtered
   * @param keepEntryPredicate a predicate that accepts a key and returns true if the entry with that key should be
   *                           included in the result and false if it should not
   * @param constructor a function (typically a constructor, e.g. HashMap::new) that maps the expected size of the
   *                    resulting map to a (typically new) map instance that will be returned by this method.  If the
   *                    resulting map is expected to be much smaller than the original, you may want to force the
   *                    instantiation of a smaller map, e.g. by passing {@code size -> new HashMap(size / 10)}.
   * @param <M> the type of the resulting map
   * @param <K> the type of the key in the map
   * @param <V> the type of the value in the map
   * @return a new {@link Map} that has the entries of the original filtered by their key
   */
  public static <M extends Map<K, V>, K, V> M filterByKey(Map<K, V> original, Predicate<K> keepEntryPredicate,
      IntFunction<M> constructor) {
    M result = constructor.apply(original.size());
    original.forEach((key, value) -> {
      if (keepEntryPredicate.test(key)) {
        result.put(key, value);
      }
    });

    return result;
  }

  /**
   * Returns a new {@link Map} containing only those entries from a map where the value satisfies some predicate.
   *
   * @param original the original map to be filtered
   * @param keepEntryPredicate a predicate that accepts a value and returns true if the entry with that value should be
   *                           included in the result and false if it should not
   * @param constructor a function (typically a constructor, e.g. HashMap::new) that maps the expected size of the
   *                    resulting map to a (typically new) map instance that will be returned by this method.  If the
   *                    resulting map is expected to be much smaller than the original, you may want to force the
   *                    instantiation of a smaller map, e.g. by passing {@code size -> new HashMap(size / 10)}.
   * @param <M> the type of the resulting map
   * @param <K> the type of the key in the map
   * @param <V> the type of the value in the map
   * @return a new {@link Map} that has the entries of the original filtered by their value
   */
  public static <M extends Map<K, V>, K, V> M filterByValue(Map<K, V> original, Predicate<V> keepEntryPredicate,
      IntFunction<M> constructor) {
    M result = constructor.apply(original.size());
    original.forEach((key, value) -> {
      if (keepEntryPredicate.test(value)) {
        result.put(key, value);
      }
    });

    return result;
  }
}
