package com.linkedin.dagli.dag;

import java.util.List;
import java.util.Set;


/**
 * Describes a directed multigraph (that is, multiple edges can exist between a pair of nodes, and all edges are
 * directed).
 *
 * For some graphs, the children and parents of a node in the graph are not required to be nodes in the graph; this is
 * the case when, e.g. representing a subgraph of a larger, encompassing graph.
 *
 * An example of such a graph is a pipeline of {@link com.linkedin.dagli.producer.Producer}s (which is also
 * acyclic and hence a DAG).
 *
 * This can be useful for visualization, debugging, and tests.
 *
 * @param <V> the type of the vertices in the graph
 */
public interface Graph<V> {
  /**
   * @return the set of all nodes considered to be in the graph (there may be edges to parents and children outside of
   *         this set, however, in the case of subgraphs)
   */
  Set<? extends V> nodes();

  /**
   * Gets a list of the children of a vertex.  A child may occur more than once in this list if more than one edge
   * exists between this parent and the child.
   *
   * @param vertex the parent whose children are sought; this vertex must exist within the graph
   * @return a list of the children of a vertex (an empty list if this is a leaf node), or null if the provided vertex
   *         is unknown to the graph (it is neither part of the {@link #nodes()} set nor a child of a member of the
   *         set).
   */
  List<? extends V> children(V vertex);

  /**
   * Gets a list of the parents of a vertex.  A parent may occur more than once in this list if more than one edge
   * exists between the parent and the child.  The order of the returned parents will correspond with the semantics of
   * the graph (e.g. {@link com.linkedin.dagli.producer.Producer}s have an ordered list of inputs).
   *
   * @param vertex the child whose parents are sought; this vertex must exist within the graph
   * @return a list of the parents of a vertex (an empty list if this is a root node), or null if the provided vertex
   *         is unknown to the graph (it is neither part of the {@link #nodes()} set nor a child of a member of the
   *         set).
   */
  List<? extends V> parents(V vertex);
}
