package com.linkedin.dagli.nn;

import com.linkedin.dagli.math.mdarray.MDArray;
import java.util.Map;


/**
 * Interface for an instance that provides the parameters associated with an arbitrary key.
 *
 * @param <K> the type of the key used to identify a set of parameters
 */
public interface ParameterStore<K> {
  /**
   * Gets the set of parameters associated with the specified key as a {@link Map} from paramater table names to their
   * corresponding parameter {@link MDArray}s.
   *
   * If a key is valid but not associated with any parameters, an empty map is returned.  Invalid keys will cause a
   * {@link java.util.NoSuchElementException}.
   *
   * @param key a key referring to a parameter set
   * @return a {@link Map} of parameter table names to {@link MDArray} parameter tables.
   */
  Map<String, ? extends MDArray> getParameters(K key);
}
