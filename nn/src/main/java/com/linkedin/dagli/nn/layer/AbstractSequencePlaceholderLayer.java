package com.linkedin.dagli.nn.layer;

import com.linkedin.dagli.list.Size64;
import com.linkedin.dagli.list.VariadicList;
import com.linkedin.dagli.object.Max;
import com.linkedin.dagli.producer.MissingInput;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.util.collection.Iterables;
import java.util.List;


/**
 * Base class for layers that are placeholders for a provided sequence (or, more precisely, an {@link Iterable}).
 *
 * @param <A> the type of item in the sequence
 * @param <S> the type ultimately deriving from this class
 */
abstract class AbstractSequencePlaceholderLayer<A, R extends List<?>, S extends AbstractSequencePlaceholderLayer<A, R, S>>
    extends AbstractPlaceholderLayer<Iterable<? extends A>, R, S> {
  private static final long serialVersionUID = 1;

  Producer<Long> _sequenceLengthProvider = MissingInput.get();

  @Override
  public S withInput(Producer<? extends Iterable<? extends A>> input) {
    S res = super.withInput(input);
    ((AbstractSequencePlaceholderLayer<?, ?, ?>) res)._sequenceLengthProvider =
        new Max<Long>().withInput(new Size64().withInput(input));
    return res;
  }

  @Override
  List<? extends Producer<?>> getDynamicConfigurationInputProducers() {
    return Iterables.append(super.getDynamicConfigurationInputProducers(), _sequenceLengthProvider);
  }

  /**
   * Returns a copy of this layer that will use the specified inputs.
   *
   * @param inputs the producers providing the inputs to this layer
   *
   * @return a copy of this layer that will use the specified inputs
   */
  @SafeVarargs
  public final S withInputs(Producer<? extends A>... inputs) {
    return withInput(new VariadicList<A>().withInputs(inputs));
  }
}
