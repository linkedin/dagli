package com.linkedin.dagli.dag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


/**
 * Base class to make it easier to define {@link Graph}s.
 *
 * Implementors should define the {@link #nodes()} and {@link #parents(Object)} methods.
 *
 * @param <V> the type of vertex in teh graph
 */
public abstract class AbstractGraph<V> implements Graph<V> {
  private HashMap<V, List<V>> _children;

  @Override
  public List<? extends V> children(V vertex) {
    if (_children == null) {
      _children = new HashMap<>();
      nodes().forEach(
          node -> parents(node).forEach(parent -> _children.computeIfAbsent(parent, k -> new ArrayList<>()).add(node)));
    }
    return _children.get(vertex);
  }
}
