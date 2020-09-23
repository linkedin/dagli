package com.linkedin.dagli.reducer;

import com.linkedin.dagli.producer.ChildProducer;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.transformer.Transformer1;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import java.util.Objects;
import java.util.Set;


/**
 * Reduces the situation where a child is the inverse operation of its sole parent with respect to one of the parent's
 * inputs, allowing the child to be replaced by that grandparent.
 *
 * For example, {@link com.linkedin.dagli.transformer.Value1FromTuple} is the inverse of
 * {@link com.linkedin.dagli.transformer.Tupled2} with respect to the second input at index 1, since creating a tuple
 * with a value in its second position and then pulling out the value in the second position always yields the original
 * value.
 *
 * This class identifies an inverted parent by their class alone; consequently, it is not suitable if this relationship
 * depends on the properties of the parent or the child.
 */
public class InverseClassReducer implements Reducer<Transformer1<?, ?>> {
  private final Set<Class<? extends ChildProducer<?>>> _parentClasses;
  private final int _parentInput;

  @Override
  public Level getLevel() {
    return Level.ESSENTIAL; // high value relative to the cost
  }

  /**
   * Creates a new reducer that will check if the target producer's parent is one of the provided classes.  If it is,
   * the target will be replaced by the parent's input at the specified index.  Input indices are 0-based; e.g. the
   * second input has index 1.
   *
   * @param parentInput the 0-based index of the parent's input that should replace the target
   * @param parentClasses the set of classes of parents that, when their child is the target producer, the child's
   *                      result will be always be input #{@code parentInput} of the parent
   */
  @SafeVarargs
  public InverseClassReducer(int parentInput, Class<? extends ChildProducer>... parentClasses) {
    _parentClasses = new ObjectArraySet<>(parentClasses);
    _parentInput = parentInput;
  }

  @Override
  public void reduce(Transformer1<?, ?> target, Context context) {
    com.linkedin.dagli.producer.Producer<?> parent = context.getParents(target).get(0);
    if (_parentClasses.contains(parent.getClass())) {
      context.tryReplaceUnviewed(target, () -> (Producer) context.getParents(parent).get(_parentInput));
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    InverseClassReducer that = (InverseClassReducer) o;
    return _parentInput == that._parentInput && _parentClasses.equals(that._parentClasses);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_parentClasses, _parentInput);
  }
}
