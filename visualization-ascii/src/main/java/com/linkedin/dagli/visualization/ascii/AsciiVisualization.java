package com.linkedin.dagli.visualization.ascii;

import com.github.mdr.ascii.layout.Graph;
import com.github.mdr.ascii.layout.Layouter;
import com.linkedin.dagli.producer.ChildProducer;
import com.linkedin.dagli.producer.Producer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import scala.Tuple2;
import scala.collection.JavaConversions;


/**
 * Renders Dagli DAGs via ASCII art rendering.
 */
public class AsciiVisualization {
  private AsciiVisualization() { }

  /**
   * Given a {@link Graph} (such as that from a DAG class, e.g. DAG1x1), returns a String containing a visualization of
   * that graph as ASCII art.  Please note that this ASCII art may be arbitrarily wide and may not render correctly
   * if soft wrapping is employed when displaying it.
   *
   * @param graph a graph (such as a DAG) to render
   * @return the graph rendered as ASCII art
   */
  @SuppressWarnings("unchecked")
  public static String render(com.linkedin.dagli.dag.Graph graph) {
    Map<Producer<?>, ? extends List<ChildProducer<?>>>
        parentToChildrenMap = graph.getParentToChildrenMap();

    return Layouter.renderGraph(new Graph(
        JavaConversions.asScalaBuffer(new ArrayList<>(parentToChildrenMap.keySet())).toList(),
        JavaConversions.asScalaBuffer(parentToChildrenMap.entrySet()
            .stream()
            .flatMap(entry -> entry.getValue().stream().map(child -> new Tuple2<Object, Object>(entry.getKey(), child)))
            .collect(Collectors.toList())).toList()
    ));
  }
}
