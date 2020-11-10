package com.linkedin.dagli.liblinear;

import com.jeffreypasternack.liblinear.Model;
import com.jeffreypasternack.liblinear.SolverType;
import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.math.vector.DenseVector;
import com.linkedin.dagli.math.vector.Vector;
import com.linkedin.dagli.math.vector.VectorElement;
import com.linkedin.dagli.preparer.AbstractStreamPreparer3;
import com.linkedin.dagli.preparer.PreparerContext;
import com.linkedin.dagli.preparer.PreparerResult;
import com.linkedin.dagli.producer.MissingInput;
import com.linkedin.dagli.transformer.PreparedTransformer1;
import com.linkedin.dagli.transformer.PreparedTransformer3;
import com.linkedin.dagli.vector.LazyFilteredVector;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.TreeSet;


/**
 * Transformer that learns to filter out all but the K "most important" elements of a vector.  It does this by:
 * (1) learning a linear model from the inputted (multinomial) labeled examples
 * (2) finding the K features that have the highest sum of the absolute values of their weight (recall that, except for
 *     binary classification, each feature will have one weight per label).
 * (3) these are then taken as the K "most important" features and used to filter the elements in each input vector.
 *     Observe that the number of non-zero elements remaining in each vector after filtering may be less than K.
 *
 * Please note that there are many caveats with this approach, and the total sum of weights need not correspond to how
 * well a feature predicts a label.  This is easy to see if one considers the case of, say, L2-normalized logistic
 * regression with multiple identical features: the model will spread the weight over these copies and the per-feature
 * weight will be less than if there had just been one copy.  Rather, this transformation represents a common
 * approximation that, while theoretically flawed, can nonetheless be used in practice as a method of dimensionality
 * reduction to discard unimportant/irrelevant features from the data.
 */
@ValueEquality
public class TopVectorElementsByLiblinearWeight extends AbstractLiblinearTransformer<
    Object,
    Vector,
    PreparedTransformer3<Number, Object, DenseVector, Vector>,
    TopVectorElementsByLiblinearWeight> {

  private static final long serialVersionUID = 1;

  protected int _topK = 100;

  /**
   * Sets the limit of how many "most important", top features will be retained.
   *
   * @param k how many top features to retain
   * @return a copy of this instance with the specified top-k value.
   */
  public TopVectorElementsByLiblinearWeight withTopK(int k) {
    return this.clone(c -> c._topK = k);
  }

  @Override
  public Preparer getPreparer(PreparerContext context) {
    return new Preparer(context, this);
  }

  private static class Preparer
      extends AbstractStreamPreparer3<Number, Object, DenseVector, Vector, PreparedTransformer3<Number, Object, DenseVector, Vector>> {
    private final LiblinearClassification.Preparer<Object> _classifierPreparer;
    private final TopVectorElementsByLiblinearWeight _owner;

    public Preparer(PreparerContext context, TopVectorElementsByLiblinearWeight owner) {
      _owner = owner;
      _classifierPreparer =
          new LiblinearClassification.Preparer<>(context, _owner.copyTo(LiblinearClassification::new));
    }

    @Override
    public PreparerResult<PreparedTransformer3<Number, Object, DenseVector, Vector>> finish() {
      // train the linear classifier
      PreparerResult<LiblinearClassification.Prepared<Object>> classifierResult = _classifierPreparer.finish();

      // extract the underlying liblinear Mode
      LiblinearClassification.Prepared<Object> prepared = classifierResult.getPreparedTransformerForNewData();
      Model model = prepared.getModel();

      // Figure out how many *sets* of weights are being used.
      // For some types of models, such as max-entropy models, liblinear uses multiple sets of weights such that each
      // feature has a different weight for each label.  These weights are interleaved; for example, if there are three
      // labels, the weight array will look like this:
      // [(Weight for feature #1, label A), (Weight for feature #1, label B), (Weight for feature #1, label C),
      //  (Weight for feature #2, label A), (Weight for feature #2, label B)...]
      // The total length of the weight array in this example will thus be 3 * (number of features).
      final int weightSetCount;
      if (_owner._solverType.isSupportVectorRegression()
          || model.getNrClass() == 2 && _owner._solverType != SolverType.MCSVM_CS) {
        // if this is regression, or a binary problem that's not modeled with MCSVM (multi-class SVM), there will only
        // be one set of weights:
        weightSetCount = 1;
      } else {
        // otherwise, the number of sets of weights is the number of classes:
        weightSetCount = model.getNrClass();
      }

      // track the top features as we loop through the weights
      TreeSet<VectorElement> top = new TreeSet<>();

      // get the weights and start looping through them
      double[] weights = model.getFeatureWeights();
      for (int i = 0; i < prepared._featureCount; i++) { // loop through all features
        double sum = 0;
        // sum all the weights for feature #i (recall that the weights are interleaved such that all the weights for a
        // given feature are adjacent)
        for (int j = i; j < i + weightSetCount; j++) {
          sum += Math.abs(weights[j]);
        }

        // Remember this feature if its among the current k best seen so far, trimming top if its too large.
        // Doing this ensures that our complexity is O(nlog(k)) rather than O(nlog(n)).
        if (top.size() < _owner._topK || sum > top.first().getValue()) {
          if (top.size() == _owner._topK) {
            top.pollFirst();
          }

          top.add(new VectorElement(i, sum));
        }
      }

      // assemble the set of IDs that will kept when vectors are filtered (those with the highest weight per liblinear)
      LongOpenHashSet elementIDs = new LongOpenHashSet(top.size());
      for (VectorElement item : top) {
        elementIDs.add(item.getIndex());
      }


      // Cast LazyFilteredVector to accept a DenseVector rather than a Vector
      PreparedTransformer1<DenseVector, Vector> lazyFiltered =
          PreparedTransformer1.cast(new LazyFilteredVector().withIndicesToKeep(elementIDs));
      // Return the prepared LazyFilteredVector transformer as our result, supplementing its arity to match that of this
      // transformer:
      return new PreparerResult<>(lazyFiltered.internalAPI()
          .withPrependedArity2(MissingInput.get())
          .internalAPI()
          .withPrependedArity3(MissingInput.get()));
    }

    @Override
    public void process(Number weight, Object label, DenseVector featureVector) {
      _classifierPreparer.process(weight, label, featureVector);
    }
  }
}
