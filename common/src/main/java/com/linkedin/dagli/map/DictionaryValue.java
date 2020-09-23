package com.linkedin.dagli.map;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.transformer.AbstractPreparedTransformer1WithInput;
import java.util.Map;


/**
 * DictionaryValue looks up its input as a key in a predefined map (the "dictionary") and returns the associated value.
 * If no entry for the key exists, a default value (by default, null) is returned.
 */
@ValueEquality
public class DictionaryValue<V> extends AbstractPreparedTransformer1WithInput<Object, V, DictionaryValue<V>> {
  private static final long serialVersionUID = 1;

  private Map<?, V> _dictionary = null;
  private V _defaultValue = null;

  /**
   * Returns a copy of this instance that uses the specified dictionary to be used by the dictionary transformer.
   * The input will be used as a key in this map to retrieve the resultant value.
   *
   * @param dictionary a map that will provide the values for the inputted keys
   * @return a copy of this instance that will use the specified dictionary
   */
  public DictionaryValue<V> withDictionary(Map<?, V> dictionary) {
    return clone(c -> c._dictionary = dictionary);
  }

  /**
   * Returns a copy of this instance that uses the specified default value when a key cannot be found in the dictionary.
   * By default, null is returned.
   *
   * @param defaultValue the default value to be used when the inputted key cannot be found in the dictionary
   * @return a copy of this instance that will use the specified default value
   */
  public DictionaryValue<V> withDefaultValue(V defaultValue) {
    return clone(c -> c._defaultValue = defaultValue);
  }

  @Override
  public V apply(Object value0) {
    return _dictionary.getOrDefault(value0, _defaultValue);
  }

  @Override
  public void validate() {
    super.validate();
    if (_dictionary == null) {
      throw new IllegalStateException(
          "No dictionary has been set for a DictionaryValue transformer.  Please call withDictionary(...) to set a "
          + "dictionary.");
    }
  }
}
