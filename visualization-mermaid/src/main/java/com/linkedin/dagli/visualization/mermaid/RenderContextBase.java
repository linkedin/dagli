package com.linkedin.dagli.visualization.mermaid;

import com.linkedin.dagli.annotation.struct.Struct;
import com.linkedin.dagli.dag.Graph;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.util.collection.LazyMap;


/**
 * Stores the per-render context for a Mermaid visualization.
 */
@Struct("RenderContext")
class RenderContextBase {
  // the StringBuilder that accumulates the output
  StringBuilder _output;

  // the DAG of producers
  Graph<Producer<?>> _dag;

  // the subgraphs for each producer (for most producers, the subgraph is null)
  LazyMap<Producer<?>, Graph<Object>> _producerToSubgraphMap;

  // outgoing edge text for each producer
  LazyMap<Producer<?>, String> _producerToEdgeTextMap;

  // we need to assign each DAG/subgraph node a consistent, unique, short ID
  LazyMap<Producer<?>, String> _producerToIDMap;
}
