package com.linkedin.dagli.dag;

import com.linkedin.dagli.producer.ChildProducer;
import com.linkedin.dagli.producer.Producer;
import java.util.List;
import java.util.Map;

/**
 * Describes a graph (specifically, a directed acyclic graph) of Dagli nodes.
 *
 * This can be useful for reflection, visualization, etc.
 */
public interface Graph {
  /**
   * Retrieves a map from each parent to its children in the graph.  Note that the list of children may contain
   * duplicates as a node can have the same parent providing more than one of its inputs, and each such connection is
   * considered a distinct parent-child relationship.
   *
   * @return a map from each parent to its children
   */
  Map<Producer<?>, ? extends List<ChildProducer<?>>> getParentToChildrenMap();
}
