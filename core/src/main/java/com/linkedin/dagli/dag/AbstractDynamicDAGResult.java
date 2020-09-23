package com.linkedin.dagli.dag;

import com.linkedin.dagli.handle.ProducerHandle;
import com.linkedin.dagli.objectio.ObjectReader;
import com.linkedin.dagli.objectio.tuple.TupleReader;
import com.linkedin.dagli.objectio.WrappedObjectReader;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.util.invariant.Arguments;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.Arrays;


class AbstractDynamicDAGResult<R> extends WrappedObjectReader<R> {
  private final ObjectReader<?>[] _outputs;
  private final Object2IntOpenHashMap<ProducerHandle<?>> _outputToIndexMap;

  /**
   * Creates a new instance with the provided outputs
   * @param outputs the output value {@link ObjectReader}s
   * @param outputToIndexMap a map of output handles to their indices in the output list
   */
  @SuppressWarnings("unchecked")
  public AbstractDynamicDAGResult(ObjectReader<?>[] outputs, Object2IntOpenHashMap<ProducerHandle<?>> outputToIndexMap) {
    super((ObjectReader<R>) (outputs.length == 1 ? outputs[0] : new TupleReader<>(outputs)));
    _outputs = outputs;
    _outputToIndexMap = outputToIndexMap;
  }

  /**
   * Gets the {@link ObjectReader} containing the results for the specified output of the dynamic DAG.
   *
   * @param output the output whose results are sought
   * @param <T> the type of result of the specified output
   * @return the {@link ObjectReader} containing the results for the specified output of the dynamic DAG
   */
  public <T> ObjectReader<T> getResult(Producer<T> output) {
    return getResult(output.internalAPI().getHandle());
  }

  /**
   * Gets the {@link ObjectReader} containing the results for the specified output of the dynamic DAG.
   *
   * @param outputHandle the handle of the output whose results are sought
   * @param <T> the type of result of the specified output
   * @return the {@link ObjectReader} containing the results for the specified output of the dynamic DAG
   */
  @SuppressWarnings("unchecked")
  public <T> ObjectReader<T> getResult(ProducerHandle<? extends Producer<T>> outputHandle) {
    int index = _outputToIndexMap.getOrDefault(outputHandle, -1);
    Arguments.check(index >= 0, "The given output producer is not among those used to create the dynamic DAG");
    return (ObjectReader<T>) _outputs[index];
  }

  @Override
  public void close() {
    Arrays.stream(_outputs).forEach(ObjectReader::close);
  }
}
