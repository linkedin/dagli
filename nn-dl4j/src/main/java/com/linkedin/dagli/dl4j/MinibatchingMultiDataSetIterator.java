package com.linkedin.dagli.dl4j;

import com.linkedin.dagli.objectio.ObjectIterator;
import com.linkedin.dagli.objectio.ObjectReader;
import java.util.Arrays;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.MultiDataSet;
import org.nd4j.linalg.dataset.api.MultiDataSetPreProcessor;
import org.nd4j.linalg.dataset.api.iterator.MultiDataSetIterator;


/**
 * A {@link MultiDataSetIterator} whose underlying data comes from the Object[]s provided as inputs to transformers.
 * {@link com.linkedin.dagli.math.vector.Vector}s.
 *
 * If not read to completion, the {@link #close()} method should be called to clean up any outstanding resources.
 */
class MinibatchingMultiDataSetIterator implements MultiDataSetIterator, AutoCloseable {
  private final Minibatcher _minibatcher;
  private final Object[][] _buffer;
  private final ObjectReader<Object[]> _examplesReader;
  private ObjectIterator<Object[]> _examplesIterator;

  /**
   * Creates a new instance.
   *
   * @param examplesReader the ObjectReader that will provide the example data; not closed by this instance's
   *                       {@link #close()} method.
   * @param minibatchSize the desired minibatch size
   * @param inputAccessors accessors for fetching the input INDArrays from an Object[]
   * @param labelAccessors accessors for fetching the label INDArrays from an Object[]
   */
  MinibatchingMultiDataSetIterator(ObjectReader<Object[]> examplesReader, int minibatchSize,
      AbstractInputConverter<?, ?>[] inputAccessors, AbstractInputConverter<?, ?>[] labelAccessors) {
    _minibatcher = new Minibatcher(minibatchSize, inputAccessors, labelAccessors);
    _buffer = new Object[minibatchSize][];
    _examplesReader = examplesReader;
    _examplesIterator = _examplesReader.iterator();
  }


  @Override
  public MultiDataSet next(int num) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public void setPreProcessor(MultiDataSetPreProcessor preProcessor) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public MultiDataSetPreProcessor getPreProcessor() {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public boolean resetSupported() {
    return true;
  }

  @Override
  public boolean asyncSupported() {
    return true;
  }

  @Override
  public void reset() {
    close();
    _minibatcher.clear();
    _examplesIterator = _examplesReader.iterator();
  }

  @Override
  public boolean hasNext() {
    return _examplesIterator != null && _examplesIterator.hasNext();
  }

  private static boolean isValidINDArray(INDArray arr) {
    for (long dim : arr.shape()) {
      if (dim == 0) {
        return false;
      }
    }

    return true;
  }

  @Override
  public MultiDataSet next() {
    int count = _examplesIterator.next(_buffer);
    for (int i = 0; i < count; i++) {
      _minibatcher.addExample(_buffer[i]);
    }

    org.nd4j.linalg.dataset.MultiDataSet result =
        new org.nd4j.linalg.dataset.MultiDataSet(_minibatcher.getFeatureMinibatchINDArrays(),
            _minibatcher.getLabelMinibatchINDArrays(), _minibatcher.getFeatureMaskMinibatchINDArrays(),
            _minibatcher.getLabelMaskMinibatchINDArrays());

    assert Arrays.stream(result.getFeatures()).allMatch(MinibatchingMultiDataSetIterator::isValidINDArray);

    if (_examplesIterator.hasNext()) {
      _minibatcher.clear();
    } else {
      close(); // no more examples; auto-close ourself
    }

    return result;
  }

  @Override
  public void close() {
    if (_examplesIterator != null) {
      _examplesIterator.close();
      _examplesIterator = null; // can't use it any further after call to close()
    }
  }
}
