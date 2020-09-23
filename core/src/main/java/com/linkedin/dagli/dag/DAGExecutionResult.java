package com.linkedin.dagli.dag;

import com.linkedin.dagli.objectio.ObjectReader;
import com.linkedin.dagli.preparer.PreparerResult;
import com.linkedin.dagli.util.array.AutoCloseableArray;
import java.util.Arrays;
import java.util.Objects;


/**
 * This class is used internally by Dagli to store the result of executing a DAG.
 * @param <R> the type of the result of the DAG (e.g. for a DAG1x2 this would be a Tuple2).
 */
class DAGExecutionResult<R, N extends PreparedDAGTransformer<R, N>> implements AutoCloseable {
  private final PreparerResult<N> _preparerResult;
  private final ObjectReader<?>[] _outputs;

  /**
   * Creates a new result.
   *
   * @param preparerResult the prepared DAGs (for new and preparation data)
   * @param outputs the outputs of the DAG; may be null if the executor prepared the DAG but did not generate any
   *                output values
   */
  public DAGExecutionResult(PreparerResult<N> preparerResult, ObjectReader<?>[] outputs) {
    assert outputs == null || Arrays.stream(outputs).noneMatch(Objects::isNull);
    _preparerResult = preparerResult;
    _outputs = outputs;
  }

  public PreparerResult<N> getPreparerResult() {
    return _preparerResult;
  }

  public ObjectReader<?>[] getOutputs() {
    return _outputs;
  }

  @Override
  public void close() {
    AutoCloseableArray.close(_outputs);
  }
}
