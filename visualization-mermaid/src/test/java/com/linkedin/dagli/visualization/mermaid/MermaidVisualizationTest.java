package com.linkedin.dagli.visualization.mermaid;

import com.linkedin.dagli.dag.DAG;
import com.linkedin.dagli.dag.DAG1x1;
import com.linkedin.dagli.dl4j.NeuralNetwork;
import com.linkedin.dagli.function.FunctionResult2;
import com.linkedin.dagli.generator.Constant;
import com.linkedin.dagli.math.vector.DenseVector;
import com.linkedin.dagli.nn.layer.NNClassification;
import com.linkedin.dagli.nn.layer.NNDenseLayer;
import com.linkedin.dagli.placeholder.Placeholder;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/**
 * Tests the output of the Mermaid visualizer.  These tests are inherently brittle and will likely require updates in
 * the future.
 */
public class MermaidVisualizationTest {
  @Test
  public void simpleTest() {
    Placeholder<String> placeholder = new Placeholder<>();
    Constant<String> prefix = new Constant<>("prefix:");
    FunctionResult2<String, String, String> concatenation =
        new FunctionResult2<>(String::concat).withInputs(prefix, placeholder);
    DAG1x1.Prepared<String, String> dag = DAG.Prepared.withPlaceholder(placeholder).withOutput(concatenation);

    String expectedOutput =
        "graph TD\n" + "FunctionResult2-0(\"String::concat(...)\")\n" + "Placeholder-1[(\"Placeholder\")]\n"
            + "Constant-2([\"''prefix:''\"])\n" + "Constant-2 --> FunctionResult2-0\n"
            + "Placeholder-1 --> FunctionResult2-0";

    compareOutput(expectedOutput, new MermaidVisualization().render(dag.graph()));
  }

  @Test
  public void valuesTest() {
    Placeholder<String> placeholder = new Placeholder<>();
    Constant<String> prefix = new Constant<>("prefix:");
    FunctionResult2<String, String, String> concatenation =
        new FunctionResult2<>(String::concat).withInputs(prefix, placeholder);
    DAG1x1.Prepared<String, String> dag = DAG.Prepared.withPlaceholder(placeholder).withOutput(concatenation);

    String expectedOutput = "graph TD\n" + "Constant-0([\"''prefix:''\"])\n" + "Placeholder-1[(\"Placeholder\")]\n"
        + "FunctionResult2-2(\"String::concat(...)\")\n" + "FunctionResult2-2-Out((\" \"))\n"
        + "style FunctionResult2-2-Out display:none\n"
        + "FunctionResult2-2 --> |\"prefix:Hello\"| FunctionResult2-2-Out\n"
        + "Constant-0 --> |\"prefix:\"| FunctionResult2-2\n" + "Placeholder-1 --> |\"Hello\"| FunctionResult2-2";

    compareOutput(expectedOutput, new MermaidVisualization().render(dag, "Hello"));
  }

  private static void compareOutput(String expected, String actual) {
    Assertions.assertEquals(toLines(expected), toLines(actual));
  }

  private static Set<String> toLines(String output) {
    // eliminate numbers, which might vary among equivalent outputs
    output = output.replaceAll("[0-9]+", "#");
    return new HashSet<>(Arrays.asList(output.split("\n")));
  }

  @Test
  public void embeddedGraphTest() throws IOException {
    Placeholder<DenseVector> features = new Placeholder<>("features");
    Placeholder<String> label = new Placeholder<>("label");

    NNDenseLayer denseLayer = new NNDenseLayer().withInputFromDenseVector(features);
    NNClassification<String> classification =
        new NNClassification<String>().withFeaturesInput(denseLayer).withMultinomialLabelInput(label);

    NeuralNetwork nn = new NeuralNetwork().withLossLayers(classification);

    try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(MermaidVisualizationTest.class.getResourceAsStream("/EmbeddedVisualization.txt"),
            StandardCharsets.UTF_8))) {
      compareOutput(reader.lines().collect(Collectors.joining("\n")),
          new MermaidVisualization().render(DAG.withPlaceholders(features, label).withOutput(nn)));
    }
  }
}
