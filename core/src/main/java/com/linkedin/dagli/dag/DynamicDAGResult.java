package com.linkedin.dagli.dag;

import com.linkedin.dagli.handle.ProducerHandle;
import com.linkedin.dagli.objectio.ObjectReader;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;


public class DynamicDAGResult<R> extends AbstractDynamicDAGResult<R> {
  private DynamicDAG.Prepared<R> _preparedDAG;

  /**
   * Creates a new instance with the provided outputs
   * @param outputs the output value {@link ObjectReader}s
   * @param outputToIndexMap a map of output handles to their indices in the output list
   */
  DynamicDAGResult(DynamicDAG.Prepared<R> preparedDAG, ObjectReader<?>[] outputs,
      Object2IntOpenHashMap<ProducerHandle<?>> outputToIndexMap) {
    super(outputs, outputToIndexMap);
    _preparedDAG = preparedDAG;
  }

  /**
   * @return the {@link DAG2x2.Prepared} DAG that was prepared (trained) on the provided preparation (training) data.
   */
  public DynamicDAG.Prepared<R> getPreparedDAG() {
    return _preparedDAG;
  }

  public static class Prepared<R> extends AbstractDynamicDAGResult<R> {
    /**
     * Creates a new instance with the provided outputs
     * @param outputs the output value {@link ObjectReader}s
     * @param outputToIndexMap a map of output handles to their indices in the output list
     */
    Prepared(ObjectReader<?>[] outputs, Object2IntOpenHashMap<ProducerHandle<?>> outputToIndexMap) {
      super(outputs, outputToIndexMap);
    }
  }
}
