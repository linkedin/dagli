package com.linkedin.dagli.visualization.ascii;

import com.linkedin.dagli.dag.DAG;
import com.linkedin.dagli.dag.DAG1x1;
import com.linkedin.dagli.function.FunctionResult2;
import com.linkedin.dagli.generator.Constant;
import com.linkedin.dagli.placeholder.Placeholder;
import org.junit.jupiter.api.Test;


public class AsciiVisualizationTest {
  @Test
  public void test() {
    Placeholder<String> placeholder = new Placeholder<>();
    Constant<String> prefix = new Constant<>("prefix:");
    FunctionResult2<String, String, String> concatenation =
        new FunctionResult2<>(String::concat).withInputs(prefix, placeholder);
    DAG1x1.Prepared<String, String> dag = DAG.Prepared.withPlaceholder(placeholder).withOutput(concatenation);
    new AsciiVisualization().render(dag.graph());
  }
}
