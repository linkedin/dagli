package com.linkedin.dagli.dl4j;

import com.linkedin.dagli.util.array.ArraysEx;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.indexing.INDArrayIndex;
import org.nd4j.linalg.indexing.NDArrayIndex;


/**
 * Accumulates example input and label {@link INDArray}s into minibatches.
 */
class Minibatcher {
  private int _minibatchOffset;
  private final int _minibatchSize;
  private final INDArray[] _featureMinibatches;
  private final INDArray[] _labelMinibatches;
  private final INDArray[] _featureMaskMinibatches;
  private final INDArray[] _labelMaskMinibatches;
  private final AbstractInputConverter<?, ?>[] _featureAccessors;
  private final AbstractInputConverter<?, ?>[] _labelAccessors;

  /**
   * Creates a new instance.
   *
   * @param minibatchSize the desired minibatch size
   * @param featuresAccessors accessors for fetching the input INDArrays from an Object[]
   * @param labelAccessors accessors for fetching the label INDArrays from an Object[]
   */
  Minibatcher(int minibatchSize, AbstractInputConverter<?, ?>[] featuresAccessors,
      AbstractInputConverter<?, ?>[] labelAccessors) {
    _minibatchSize = minibatchSize;

    _featureMinibatches = new INDArray[featuresAccessors.length];
    _featureMaskMinibatches = new INDArray[featuresAccessors.length];
    _labelMinibatches = new INDArray[labelAccessors.length];
    _labelMaskMinibatches = new INDArray[labelAccessors.length];

    _featureAccessors = featuresAccessors;
    _labelAccessors = labelAccessors;

    clear(); // creates the minibatch arrays
  }

  /**
   * Clears the current minibatch.  This should be done after each minibatch is retrieved to make room for subsequent
   * data.
   */
  void clear() {
    ArraysEx.mapArray(_featureAccessors, s -> _featureMinibatches, accesor -> accesor.createMinibatch(_minibatchSize));
    ArraysEx.mapArray(_labelAccessors, s -> _labelMinibatches, accesor -> accesor.createMinibatch(_minibatchSize));

    ArraysEx.mapArray(_featureAccessors, s -> _featureMaskMinibatches,
        accesor -> accesor.createMinibatchMask(_minibatchSize));
    ArraysEx.mapArray(_labelAccessors, s -> _labelMaskMinibatches,
        accesor -> accesor.createMinibatchMask(_minibatchSize));

    _minibatchOffset = 0;
  }

  /**
   * Gets the portion of the provided minibatches that have been filled with example data.  If the minibatch is full
   * the passed minibatches array is returned.
   *
   * @param minibatches the minibatch data whose subarrays should be retrieved
   * @return the completed portion of the provided minibatches (possibly the passed minibatches as-is, if the
   *         minibatches are full)
   */
  private INDArray[] currentMinibatch(INDArray[] minibatches) {
    if (_minibatchOffset == _minibatchSize) {
      return minibatches.clone();
    }

    INDArrayIndex exampleRows = NDArrayIndex.interval(0, _minibatchOffset);
    return ArraysEx.mapArray(minibatches, INDArray[]::new,
        original -> original == null ? null : original.get(exampleRows));
  }

  /**
   * Gets the accumulated example features since the last {@link #clear()} as a minibatch.  If the minibatch is not yet
   * full these will have fewer examples than the designated minibatch size.
   *
   * @return an array of INDArrays, one per input, containing the minibatched data
   */
  INDArray[] getFeatureMinibatchINDArrays() {
    return currentMinibatch(_featureMinibatches);
  }

  /**
   * Gets the accumulated example labels since the last {@link #clear()} as a minibatch.  If the minibatch is not yet
   * full these will have fewer examples than the designated minibatch size.
   *
   * @return an array of INDArrays, one per label, containing the minibatched data
   */
  INDArray[] getLabelMinibatchINDArrays() {
    return currentMinibatch(_labelMinibatches);
  }

  /**
   * Gets the accumulated example feature masks since the last {@link #clear()} as a minibatch.  If the minibatch is not
   * yet full these will have fewer examples than the designated minibatch size.
   *
   * @return an array of INDArrays, one per input, containing the minibatched mask data
   */
  INDArray[] getFeatureMaskMinibatchINDArrays() {
    return currentMinibatch(_featureMaskMinibatches);
  }

  /**
   * Gets the accumulated example label masks since the last {@link #clear()} as a minibatch.  If the minibatch is not
   * yet full these will have fewer examples than the designated minibatch size.
   *
   * @return an array of INDArrays, one per label, containing the minibatched mask data
   */
  INDArray[] getLabelMaskMinibatchINDArrays() {
    return currentMinibatch(_labelMaskMinibatches);
  }

  /**
   * @return the number of examples in the current minibatch.
   */
  int exampleCount() {
    return _minibatchOffset;
  }

  boolean isEmpty() {
    return _minibatchOffset == 0;
  }

  boolean isFull() {
    return _minibatchOffset == _minibatchSize;
  }

  /**
   * Adds an example (provided within the Object[] of inputs provided to a transformer) to the minibatch.  The minibatch
   * must not be already full or a runtime exception will be thrown.
   *
   * @param values the inputs provided to a neural network transformer containing the example features/labels
   */
  void addExample(Object[] values) {
    assert _minibatchOffset < _minibatchSize;

    for (int i = 0; i < _featureAccessors.length; i++) {
      _featureAccessors[i].writeToINDArray(values, _featureMinibatches[i], _featureMaskMinibatches[i],
          _minibatchOffset);
    }
    for (int i = 0; i < _labelAccessors.length; i++) {
      _labelAccessors[i].writeToINDArray(values, _labelMinibatches[i], _labelMaskMinibatches[i], _minibatchOffset);
    }
    _minibatchOffset++;
  }
}
