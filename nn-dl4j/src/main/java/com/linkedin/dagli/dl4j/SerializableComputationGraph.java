package com.linkedin.dagli.dl4j;

import com.linkedin.dagli.util.io.InputSubstream;
import com.linkedin.dagli.util.io.OutputSubstream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.util.ModelSerializer;


/**
 * Serializes/deserializes a {@link ComputationGraph} by giving it its own virtual stream and using DL4J's
 * {@link ModelSerializer}.
 *
 * This is currently needed because:
 * (1) DL4J improperly assumes that a computation graph will be the last thing read or written to a stream
 * (2) {@link ComputationGraph}'s innate serialization doesn't work correctly (this is why ModelSerializer must be used)
 */
class SerializableComputationGraph implements Serializable {
  private static final long serialVersionUID = 1;

  private ComputationGraph _graph;

  /**
   * Creates a new instance.
   *
   * @param graph the graph to be wrapped (and serialized) by this instance
   */
  SerializableComputationGraph(ComputationGraph graph) {
    _graph = graph;
  }

  /**
   * @return the {@link ComputationGraph} wrapped by this instance
   */
  public ComputationGraph get() {
    return _graph;
  }

  private void readObject(ObjectInputStream in) throws ClassNotFoundException, IOException {
    try (InputSubstream substream = new InputSubstream(in)) {
      _graph = ModelSerializer.restoreComputationGraph(substream);
    }
  }

  private void writeObject(ObjectOutputStream out) throws IOException {
    try (OutputSubstream substream = new OutputSubstream(out, 4096)) {
      ModelSerializer.writeModel(_graph, substream, true);
    }
  }
}
