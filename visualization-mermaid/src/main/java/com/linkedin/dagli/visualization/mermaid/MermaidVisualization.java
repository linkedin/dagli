package com.linkedin.dagli.visualization.mermaid;

import com.linkedin.dagli.dag.Graph;
import com.linkedin.dagli.generator.Generator;
import com.linkedin.dagli.placeholder.Placeholder;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.transformer.PreparableTransformer;
import com.linkedin.dagli.transformer.PreparedTransformer;
import com.linkedin.dagli.util.collection.LazyMap;
import com.linkedin.dagli.view.TransformerView;
import com.linkedin.dagli.visualization.AbstractVisualization;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * Renders Dagli DAGs to <a href="https://mermaid-js.github.io/mermaid/">Mermaid Markdown</a> code, which can then be
 * rendered in documentation, via a Mermaid web service, from the
 * <a href="https://mermaid-js.github.io/mermaid-live-editor/">Mermaid Live Demo</a>, etc.
 */
public class MermaidVisualization extends AbstractVisualization<String, MermaidVisualization> {
  private static final String OUTPUT_SUFFIX = "-Out";
  private static final String SUBGRAPH_SUFFIX = "-Subgraph";

  /**
   * Simple class holding the rendering context.
   */
  private static class Context {

  }

  @Override
  protected String render(Graph<Producer<?>> graph, List<Map<Producer<?>, Object>> producerOutputs) {
    int[] producerIDCounter = new int[1];
    RenderContext context = RenderContext.Builder.setOutput(new StringBuilder())
        .setDag(graph)
        .setProducerToSubgraphMap(new LazyMap<>(graph.nodes(), p -> p.internalAPI().subgraph()))
        .setProducerToEdgeTextMap(new LazyMap<>(graph.nodes(), k -> createEdgeText(k, producerOutputs)))
        .setProducerToIDMap(new LazyMap<>(graph.nodes(),
            p -> p.getClass().getSimpleName() + "-" + Integer.toString(producerIDCounter[0]++)))
        .build();

    context.getOutput().append("graph TD\n");

    for (Producer<?> producer : context.getDag().nodes()) {
      if (hasSubgraph(context, producer)) {
        renderSubgraph(context, producer);
      } else {
        renderProducerVertex(context, producer);
      }
    }

    // All vertices have been defined--loop again to add edges.  We have to look up and add edges from the lists of
    // parents for each child in the DAG to make sure the ordering of edges matches with the ordering of the inputs
    // to each node.
    for (Producer<?> child : context.getDag().nodes()) {
      renderProducerOutputEdge(context, child); // create edges to dummy vertices for leaves if rendering values
      Graph<Object> subgraph = context.getProducerToSubgraphMap().get(child);
      for (Producer<?> parent : context.getDag().parents(child)) {
        // if the child producer has a subgraph, and there is an edge from the current parent to a different node in
        // that subgraph (not this child producer), then we won't render an edge from the parent to the child because
        // it would be duplicative with the other edge(s) into the subgraph
        if (subgraph == null || subgraph.children(parent) == null || subgraph.children(parent)
            .stream()
            .allMatch(c -> c == parent)) {
          context.getOutput()
              .append(createEdgeLine(context.getProducerToIDMap().get(parent), context.getProducerToIDMap().get(child),
                  context.getProducerToEdgeTextMap().get(parent)));
        }
      }
    }

    return context.getOutput().toString();
  }

  private void renderSubgraph(RenderContext context, Producer<?> producer) {
    context.getOutput()
        .append("subgraph ")
        .append(context.getProducerToIDMap().get(producer))
        .append(SUBGRAPH_SUFFIX + "[\"")
        .append(" ") //.append(escapeText(renderProducerAsString(producer)))
        .append("\"]\n");

    // The Producer that "owns" the subgraph is also always part of the subgraph (at least until Mermaid supports edges
    // to/from subgraphs themselves in a non-beta form)
    renderProducerVertex(context, producer);

    String producerID = context.getProducerToIDMap().get(producer);
    int[] accumulator = new int[1];
    LazyMap<Object, String> nodeToIDMap = new LazyMap<>(context.getProducerToSubgraphMap().get(producer).nodes(),
        n -> producerID + (n == producer ? "" : "-" + accumulator[0]++));

    // Add every subgraph vertex and its edges
    for (Object node : context.getProducerToSubgraphMap().get(producer).nodes()) {
      if (node != producer) {
        // create vertex for this node
        context.getOutput()
            .append(nodeToIDMap.get(node))
            .append("[[\"")
            .append(escapeText(renderSubgraphNodeAsString(node)))
            .append("\"]]\n");
      }
    }

    // end the subgraph before appending all the edges
    context.getOutput().append("end\n");

    // add edges from each node's parents to the node
    for (Object node : context.getProducerToSubgraphMap().get(producer).nodes()) {
      for (Object parent : context.getProducerToSubgraphMap().get(producer).parents(node)) {
        String parentID = parent instanceof Producer ? context.getProducerToIDMap()
            .getOrComputeDefault((Producer<?>) parent, nodeToIDMap::get) : nodeToIDMap.get(parent);
        context.getOutput().append(parentID).append(" --> ").append(nodeToIDMap.get(node)).append("\n");
      }
    }
  }

  private static boolean hasSubgraph(RenderContext context, Producer<?> producer) {
    return context.getProducerToSubgraphMap().get(producer) != null;
  }

  private void renderProducerVertex(RenderContext context, Producer<?> producer) {
    String label = '"' + escapeText(renderProducerAsString(producer)) + '"';

    // add the node with styling dependent on its type; the aesthetic can be approximated as "the rounder it is, the
    // less work it takes to compute its output"
    if (producer instanceof Placeholder) {
      label = "[(" + label + ")]"; // cylinder
    } else if (producer instanceof Generator) {
      label = "([" + label + "])"; // pill-shaped
    } else if (producer instanceof TransformerView) {
      label = "[/" + label + "/]"; // parallelogram
    } else if (producer instanceof PreparableTransformer) {
      label = "[" + label + "]"; // rectangle
    } else if (producer instanceof PreparedTransformer) {
      label = "(" + label + ")"; // rounded rectangle
    } else {
      throw new IllegalArgumentException("Unknown type of DAG node: " + producer.getClass());
    }

    // output vertex definition
    context.getOutput().append(context.getProducerToIDMap().get(producer)).append(label).append("\n");
  }

  private void renderProducerOutputEdge(RenderContext context, Producer<?> producer) {
    if (context.getDag().children(producer).isEmpty() && !context.getProducerToEdgeTextMap()
        .get(producer)
        .isEmpty()) {
      String producerID = context.getProducerToIDMap().get(producer);
      // add terminal node so we can render example values for the DAG's output
      context.getOutput()
          .append(producerID).append(OUTPUT_SUFFIX + "((\" \"))\n")
          // add styling such that the added terminal node is hidden
          .append("style ").append(producerID).append(OUTPUT_SUFFIX + " display:none\n")
          // add edge connecting producer to its dummy output vertex
          .append(
              createEdgeLine(producerID, producerID + OUTPUT_SUFFIX, context.getProducerToEdgeTextMap().get(producer)));
    }
  }

  private String createEdgeText(Producer<?> producer, List<Map<Producer<?>, Object>> producerOutputs) {
    return escapeText(producerOutputs.stream()
        .map(m -> m.containsKey(producer) ? renderValueAsString(m.get(producer)) : "")
        .collect(Collectors.joining("\n")));
  }

  private static String createEdgeLine(String originID, String targetID, String edgeText) {
    return originID + " --> " + (edgeText == null || edgeText.isEmpty() ? "" : "|\"" + edgeText + "\"| ") + targetID
        + "\n";
  }

  /**
   * Escape characters that may interfere with Mermaid rendering of text; this is a simplistic approach and may still
   * admit some corner cases that do not render correctly, but the key is that the escaped string must be valid Mermaid
   * syntax.
   *
   * @param text the string to escape
   * @return the Mermaid-escaped string
   */
  private static String escapeText(String text) {
    // for some reason Mermaid seems to add extraneous spacing when the #quot; escape is used; as a workaround we
    // presently substitute a pair of single quotes in its place
    return text
        .replace("&", "&amp;") // the ordering of the replacements matters!
        .replace("#", "&#35;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\n", "<br>")
        .replace("\"", "''"); // don't use #quot;
  }
}
