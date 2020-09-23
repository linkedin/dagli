package com.linkedin.dagli.nn.dl4j;

import com.linkedin.dagli.dl4j.DotProductVertex;
import org.deeplearning4j.nn.conf.ComputationGraphConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.Sgd;


public class DL4JDotProductTest {
  @Test
  public void test() {
    ComputationGraphConfiguration conf = new NeuralNetConfiguration.Builder().updater(new Sgd(0.01))
        .graphBuilder()
        .addInputs("input1", "input2")
        .addVertex("dot", new DotProductVertex(), "input1", "input2")
        .allowNoOutput(true)
        .build();

    ComputationGraph net = new ComputationGraph(conf);
    net.init();

    // [ 3 2 ]
    // [ 2 3 ]
    // .
    // [ 6 5 ]
    // [ 2 3 ]
    INDArray res = net.feedForward(new INDArray[]{Nd4j.createFromArray(3.0, 2.0, 2.0, 3.0).reshape(2, 2),
        Nd4j.createFromArray(6.0, 5.0, 2.0, 3.0).reshape(2, 2)}, false).get("dot");

    System.out.println(res);

    Assertions.assertEquals(28, res.getDouble(0));
    Assertions.assertEquals(13, res.getDouble(1));
  }
}
