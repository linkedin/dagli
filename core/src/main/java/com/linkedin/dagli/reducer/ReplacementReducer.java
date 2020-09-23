package com.linkedin.dagli.reducer;

import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.util.cloneable.AbstractCloneable;
import java.util.Objects;
import java.util.function.Function;


/**
 * Reducer that replaces a {@link Producer} with another instance of the exact same type according to a replacement
 * function.
 *
 * This reducer is meant for simple cases where a producer is replaced with a modified copy of itself, perhaps changing
 * a property or replacing an input with a {@link com.linkedin.dagli.generator.Constant}.  The producer provided to the
 * function will have correct parents, but possibly not correct grandparents.  The replacement function must
 * consequently not rely on any methods that examine the {@link Producer}'s ancestry beyond its parents, such as
 * {@link Producer#hasConstantResult()},
 * {@link com.linkedin.dagli.producer.ChildProducer#ancestors(com.linkedin.dagli.producer.ChildProducer, int)}, etc.
 *
 * @param <S> the type of the producer to be reduced
 */
public class ReplacementReducer<S extends AbstractCloneable<S> & Producer<?>> implements Reducer<S> {
  private final Function<? super S, ? extends S> _replacer;
  private final Level _level;

  /**
   * Creates a new instance that will use the provider function to replace instances to be transformer.
   *
   * @param level the level of this reducer
   * @param replacer a function that provides the replacement for a provider producer instance; if the returned instance
   *                 is equals() to the supplied instance, no replacement will be performed
   */
  public ReplacementReducer(Level level, Function<? super S, ? extends S> replacer) {
    _level = Objects.requireNonNull(level);
    _replacer = Objects.requireNonNull(replacer);
  }

  /**
   * Creates a new instance that will use the provider function to replace instances to be transformer, with a level
   * of {@link Level#NORMAL}.
   *
   * @param replacer a function that provides the replacement for a provider producer instance; if the returned instance
   *                 is equals() to the supplied instance, no replacement will be performed
   */
  public ReplacementReducer(Function<? super S, ? extends S> replacer) {
    this(Level.NORMAL, replacer);
  }

  @Override
  public void reduce(S target, Context context) {
    S current = context.withCurrentParents(target);
    S replacement = _replacer.apply(current);
    if (!current.equals(replacement)) {
      context.replaceWithSameClass(current, replacement);
    }
  }

  @Override
  public Level getLevel() {
    return _level;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ReplacementReducer<?> that = (ReplacementReducer<?>) o;
    return _replacer.equals(that._replacer) && _level == that._level;
  }

  @Override
  public int hashCode() {
    return Objects.hash(_replacer, _level);
  }
}
