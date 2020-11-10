package com.linkedin.dagli.reducer;

import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.transformer.Transformer;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import java.util.List;
import java.util.Set;


/**
 * Reduces the case where a child is an idempotent with respect to its sole parent, allowing the child to be
 * replaced by its parent.
 *
 * For example, {@code DensifiedVector} is idempotent with itself--re-densifying a densified vector creates an
 * equivalent result.
 *
 * This class identifies parents that are idempotent to the child by their class alone; consequently, it is not suitable
 * if idempotence is conditional on the properties of the parent or the child.
 */
public class IdempotentClassReducer implements Reducer<Transformer<?>> {
  private final Set<Class<? extends Producer<?>>> _parentClasses;

  @Override
  public Level getLevel() {
    return Level.ESSENTIAL; // high value relative to the cost
  }

  /**
   * Creates a new reducer that will check if the target transformer's parent is one of the provided classes.  If it is,
   * the target will be replaced by the parent.
   *
   * @param parentClasses the set of classes of parents that are idempotent to the target transformer
   */
  @SafeVarargs
  public IdempotentClassReducer(Class<? extends Producer<?>>... parentClasses) {
    _parentClasses = new ObjectArraySet<>(parentClasses);
  }

  @Override
  @SuppressWarnings({"unchecked", "rawtypes"})
  public void reduce(Transformer<?> target, Context context) {
    List<? extends Producer<?>> parents = context.getParents(target);
    if (parents.size() == 1) {
      Producer<?> parent = parents.get(0);
      if (_parentClasses.contains(parent.getClass())) {
        context.tryReplaceUnviewed(target, () -> (Producer) parent);
      }
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
    return _parentClasses.equals(((IdempotentClassReducer) o)._parentClasses);
  }

  @Override
  public int hashCode() {
    return _parentClasses.hashCode();
  }
}
