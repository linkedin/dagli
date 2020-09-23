package com.linkedin.dagli.dag;

import com.linkedin.dagli.math.hashing.DoubleXorShift;
import com.linkedin.dagli.placeholder.Placeholder;


/**
 * Placeholder used to help determine whether two DAGs are functionally identical.
 *
 * Ordinarily, placeholders have handle-equality (approximately the same as reference equality); positional placeholders
 * are instead equal is their "positions" (i.e. the index of the input they correspond to in the encompassing DAG) are
 * equal.
 *
 * @param <R> the type of result produced by the placeholder
 */
class PositionPlaceholder<R> extends Placeholder<R> {
  private static final long serialVersionUID = 1L;

  private final int _position;

  PositionPlaceholder(int position) {
    _position = position;
  }

  @Override
  protected boolean computeEqualsUnsafe(Placeholder<R> other) {
    return other instanceof PositionPlaceholder && _position == ((PositionPlaceholder) other)._position;
  }

  @Override
  protected int computeHashCode() {
    return (int) DoubleXorShift.hash(_position, PositionPlaceholder.class.hashCode());
  }
}
