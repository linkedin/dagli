package com.linkedin.dagli.dl4j;

import com.linkedin.dagli.math.mdarray.MDArray;
import com.linkedin.dagli.nn.result.NNResult;
import com.linkedin.dagli.producer.Producer;
import java.util.Arrays;
import org.nd4j.linalg.api.ndarray.INDArray;


public class DL4JResult extends NNResult {
  private static final long serialVersionUID = 1;

  public static abstract class InternalAPI extends NNResult.InternalAPI {
    InternalAPI() { }

    public static Producer<INDArray> toINDArray(Producer<? extends DL4JResult> nnResultProducer, int outputIndex) {
      return new INDArrayFromDL4JNNResult(outputIndex).withInput(nnResultProducer);
    }
  }

  private final INDArray[] _outputs;

  public DL4JResult(INDArray... outputs) {
    // check that all outputs have a row-major layout ('c' ordering)
    assert Arrays.stream(outputs).allMatch(output -> output.ordering() == 'c');

    _outputs = outputs;
  }

  @Override
  protected MDArray getAsMDArray(int outputIndex) {
    return new INDArrayAsMDArray(_outputs[outputIndex]);
  }

  protected INDArray getAsINDArray(int outputIndex) {
    return _outputs[outputIndex];
  }
}
