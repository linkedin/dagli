package com.linkedin.dagli.reducer;

import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.transformer.TransformerVariadic;
import com.linkedin.dagli.transformer.TransformerWithInputBound;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;


/**
 * Reduces the case where a child's inputs are associative relative to one or more of its parents, allowing the parents'
 * inputs to be directly accepted by the child (removing the parent) while still producing an equivalent result.  The
 * child must be variadic and the parent must be either variadic or unary.
 *
 * For example, {@code DensifiedVector} is associative relative to a {@code CompositeSparseVector}: creating a composite
 * sparse vector from a set of source vectors and then densifying it yields a result equivalent to just densifying the
 * source vectors directly.
 *
 * Careless use of this reducer could potentially make the DAG more expensive to execute, by removing an intermediate
 * parent whose result might be used elsewhere in the graph.  This reducer is best used only in situations where
 * consuming the parent's inputs directly is not substantively more expensive than consuming the parent's result (since
 * that parent might still be present in the reduced graph if it has other children).
 *
 * This class identifies associative parents by their class alone; consequently, it is not suitable if associativity is
 * conditional on the properties of the parent or the child.
 */
public class AssociativeClassReducer<V> implements Reducer<TransformerVariadic<V, ?>> {
  private final Set<Class<? extends TransformerWithInputBound<? extends V, ?>>> _parentClasses;

  @Override
  public Level getLevel() {
    return Level.ESSENTIAL; // high value relative to the cost
  }

  /**
   * Creates a new reducer that will check if the target transformer's parent is one of the provided classes.  If it is,
   * the the target transformer will disintermediate the parent and accept its inputs directly (if possible).
   *
   * @param parentClasses the set of classes of parents that are associative relative to the target transformer
   */
  @SafeVarargs
  public AssociativeClassReducer(Class<? extends TransformerWithInputBound<? extends V, ?>>... parentClasses) {
    _parentClasses = new ObjectArraySet<>(parentClasses);
  }

  @Override
  @SuppressWarnings({"unchecked", "rawtypes"})
  public void reduce(TransformerVariadic<V, ?> target, Context context) {
    List<? extends Producer<? extends V>> parents = context.getParents(target);
    if (parents.stream().anyMatch(parent -> _parentClasses.contains(parent.getClass()))) {
      ArrayList<Producer<? extends V>> newParentsList = new ArrayList<>(parents.size());
      for (Producer<? extends V> parent : parents) {
        if (_parentClasses.contains(parent.getClass())) {
          newParentsList.addAll((List<Producer<? extends V>>) context.getParents(parent));
        } else {
          newParentsList.add(parent);
        }
      }
      context.tryReplaceUnviewed(target, () -> (Producer) target.withInputs(newParentsList));
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
    return _parentClasses.equals(((AssociativeClassReducer<?>) o)._parentClasses);
  }

  @Override
  public int hashCode() {
    return _parentClasses.hashCode();
  }
}
