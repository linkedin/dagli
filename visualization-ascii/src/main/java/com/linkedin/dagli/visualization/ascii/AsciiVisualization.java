package com.linkedin.dagli.visualization.ascii;

import com.github.mdr.ascii.layout.Graph;
import com.github.mdr.ascii.layout.Layouter;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.util.collection.LazyMap;
import com.linkedin.dagli.visualization.AbstractVisualization;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import scala.Tuple2;
import scala.collection.JavaConversions;


/**
 * Renders Dagli DAGs via ASCII art rendering.
 */
public class AsciiVisualization extends AbstractVisualization<String, AsciiVisualization> {
  /**
   * Simple Vertex class that stores a string value "by reference" (rather than using value equality semantics).
   */
  private static class Vertex {
    final String _text;

    private Vertex(String text) {
      _text = text;
    }

    @Override
    public String toString() {
      return _text;
    }
  }

  /**
   * Given a {@link Graph} (such as that from a DAG class, e.g. DAG1x1), returns a String containing a visualization of
   * that graph as ASCII art.  Please note that this ASCII art may be arbitrarily wide and may not render correctly
   * if soft wrapping is employed when displaying it.
   *
   * @param graph a graph (such as a DAG) to render
   * @param producerOutputs a list of examples, expressed as a map of outputs for each producer, to render
   * @return the graph rendered as ASCII art
   */
  @Override
  @SuppressWarnings({"unchecked", "rawtypes"})
  protected String render(com.linkedin.dagli.dag.Graph<Producer<?>> graph,
      List<Map<Producer<?>, Object>> producerOutputs) {
    Map<Producer<?>, Vertex> producerToVertexMap =
        new LazyMap<>(graph.nodes(), p -> new Vertex(renderProducer(p, producerOutputs)));

    return Layouter.renderGraph(
        new Graph(JavaConversions.asScalaBuffer(new ArrayList<>(producerToVertexMap.values())).toList(),
            JavaConversions.asScalaBuffer(graph.nodes()
                .stream()
                .flatMap(node -> graph.children(node)
                    .stream()
                    .map(child -> new Tuple2<Object, Object>(producerToVertexMap.get(node),
                        producerToVertexMap.get(child))))
                .collect(Collectors.toList())).toList()));
  }

  private String renderProducer(Producer<?> producer, List<Map<Producer<?>, Object>> producerOutputs) {
    StringBuilder result = new StringBuilder(renderProducerAsString(producer));
    if (!producerOutputs.isEmpty()) {
      result.append("\n");
      for (int i = 0; i < producerOutputs.size(); i++) {
        if (producerOutputs.get(i).containsKey(producer)) {
          result.append("\nExample #")
              .append(i)
              .append(": ")
              .append(renderValueAsString(producerOutputs.get(i).get(producer)));
        }
      }
    }
    return result.toString();
  }
}
