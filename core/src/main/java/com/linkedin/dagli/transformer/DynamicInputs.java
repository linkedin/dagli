package com.linkedin.dagli.transformer;

import com.linkedin.dagli.annotation.Versioned;
import com.linkedin.dagli.producer.Producer;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;


/**
 * {@link DynamicInputs} provides a type-safe way to access the values supplied to a {@link TransformerDynamic}.
 *
 * After constructing it by providing a {@link Transformer} instance, you can obtain a {@link DynamicInputs.Accessor}
 * for each input producer that will fetch the (typed) value corresponding to that producer from the "raw" Object[]
 * provided to the dynamic transformer.  Instances of {@link DynamicInputs} are only valid for a specific
 * {@link Transformer} instance; attempting to reuse its accessors for a  different transformer will create logic
 * errors.
 *
 * Note that it is better to avoid dynamic transformers where possible.  The values supplied to other transformer types
 * have statically known input types and do not require the use of this class to safely access them.
 */
@Versioned
public class DynamicInputs implements Serializable {
  private static final long serialVersionUID = 1;

  private final Object2IntMap<Producer<?>> _producerIndices;

  // no-args constructor for Kryo
  private DynamicInputs() {
    _producerIndices = null;
  }

  /**
   * Creates a new instance.  Although any {@link Transformer} may be provided, this class is not generally useful for
   * transformers other than {@link TransformerDynamic}s; this is because other transformer types have statically known
   * input types.
   *
   * @param transformer the transformer whose inputs are to be accessed
   */
  public DynamicInputs(Transformer<?> transformer) {
    this(transformer.internalAPI().getInputList());
  }

  /**
   * Creates a new instance.
   *
   * @param inputList the transformer's list of inputs
   */
  public DynamicInputs(List<? extends Producer<?>> inputList) {
    _producerIndices = producerToIndexMap(inputList);
  }

  /**
   * Associates each element in a list of producers with its index in that list.  If a producer occurs multiple times
   * in the list, which index it is associated with is arbitrary (and immaterial for our purposes).
   *
   * @param producers the list of producers
   * @return a map from each producer to its index
   */
  private static Object2IntOpenHashMap<Producer<?>> producerToIndexMap(List<? extends Producer<?>> producers) {
    // in the case of duplicate producers in the list, there are multiple indices for that producer; which we store
    // is immaterial since each corresponding input slot will receive the same value
    Object2IntOpenHashMap<Producer<?>> result = new Object2IntOpenHashMap<>(producers.size());
    for (int i = 0; i < producers.size(); i++) {
      result.put(producers.get(i), i);
    }
    return result;
  }

  /**
   * Gets a {@link ConstantInputs} instance that maps from the constant-result input producers to the transformer to
   * their values.  See {@link Producer#hasConstantResult()} for more information about constant-result producers.
   *
   * @param values a "raw" Object[] of input values to the transformer (which example they correspond to is immaterial)
   * @return a new {@link ConstantInputs} instance that provides type-safe access to the constant value inputs to the
   *         transformer
   */
  public ConstantInputs constantInputs(Object[] values) {
    return new ConstantInputs(_producerIndices, values);
  }

  /**
   * Gets the {@link Accessor} object for the given producer that can be used to extract the corresponding input value
   * from an Object[] of input values supplied to the transformer associated with this {@link DynamicInputs} instance.
   * {@link NoSuchElementException} will be thrown if the supplied producer is not an input to this transformer.
   *
   * The returned {@link Accessor} objects are valid for a specific transformer instance.  For better performance, they
   * should be cached (for this instance only) rather than repeatedly re-fetched.
   *
   * @param input the input producer whose corresponding input value {@link Accessor} should be retrieved
   * @return an {@link Accessor} instance that can be used to retrieve typed input values for the given input from
   *         an Object[] of input values supplied to the transformer
   */
  public <T> Accessor<T> get(Producer<T> input) {
    int index = _producerIndices.getOrDefault(input, -1);
    if (index < 0) {
      throw new NoSuchElementException("The producer " + input + " is not an input to the transformer");
    }

    return new Accessor<>(index);
  }

  /**
   * An "accessor" that can be used to retrieve a typed input from an Object[] of untyped input values supplied to the
   * transformer associated with the originating {@link DynamicInputs} instance.
   *
   * These tokens are only valid for this specific transformer instance, and should never be used for another
   * transformer instance (even of the same class).  Such reuse will create logic errors.
   *
   * @param <T> the type of the input value
   */
  public static class Accessor<T> implements Serializable {
    private static final long serialVersionUID = 1;

    private final int _index;

    /**
     * Private no-args constructor for Kryo.
     */
    private Accessor() {
      this(-1);
    }

    /**
     * Creates a new accessor for the given input index.
     *
     * @param index the index of the input
     */
    public Accessor(int index) {
      _index = index;
    }

    /**
     * Gets the value for this accessor's input from an Object[] of "raw" values provided to the transformer
     *
     * @param inputArray the "raw" Object[] of inputs to the transformer
     * @return the typed input value
     */
    @SuppressWarnings("unchecked") // safe because of how these instances are created
    public T get(Object[] inputArray) {
      return (T) inputArray[_index]; // static typing of get(...) ensures that producer's value is T
    }

    /**
     * @return the index of the input corresponding to this accessor
     */
    public int getIndex() {
      return _index;
    }
  }

  /**
   * Provides the constant values provided to a transformer.
   */
  public static class ConstantInputs {
    private final HashMap<Producer<?>, Object> _producerToValueMap;

    private ConstantInputs(Object2IntMap<Producer<?>> producerIndices, Object[] values) {
      _producerToValueMap = new HashMap<>(producerIndices.size());
      for (Object2IntMap.Entry<Producer<?>> entry : producerIndices.object2IntEntrySet()) {
        if (entry.getKey().hasConstantResult()) {
          _producerToValueMap.put(entry.getKey(), values[entry.getIntValue()]);
        }
      }
    }

    @SuppressWarnings("unchecked") // cast safety ensured by static typing and semantics of Producers
    public <R> R get(Producer<R> producer) {
      if (!_producerToValueMap.containsKey(producer)) {
        throw new NoSuchElementException(
            "The producer " + producer + " is not a constant-result input to the transformer");
      }

      return (R) _producerToValueMap.get(producer); // value must be of type R because it came from Producer<R>
    }
  }
}
