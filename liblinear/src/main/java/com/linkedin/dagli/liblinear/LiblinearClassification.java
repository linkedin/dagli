package com.linkedin.dagli.liblinear;

import com.jeffreypasternack.liblinear.Feature;
import com.jeffreypasternack.liblinear.FeatureNode;
import com.jeffreypasternack.liblinear.Linear;
import com.jeffreypasternack.liblinear.Model;
import com.jeffreypasternack.liblinear.Parameter;
import com.jeffreypasternack.liblinear.Problem;
import com.jeffreypasternack.liblinear.SolverType;
import com.linkedin.dagli.annotation.equality.DeepArrayValueEquality;
import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.math.distribution.ArrayDiscreteDistribution;
import com.linkedin.dagli.math.distribution.BinaryDistribution;
import com.linkedin.dagli.math.distribution.DiscreteDistribution;
import com.linkedin.dagli.math.vector.SparseDoubleMapVector;
import com.linkedin.dagli.math.vector.Vector;
import com.linkedin.dagli.preparer.AbstractStreamPreparer2;
import com.linkedin.dagli.preparer.PreparerContext;
import com.linkedin.dagli.preparer.PreparerResult;
import com.linkedin.dagli.util.invariant.Arguments;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;


/**
 * Logistic regression classifier backed by liblinear.  May be binary or multinomial (max-ent).
 *
 * @param <L> the type of label
 */
@ValueEquality
public class LiblinearClassification<L>
    extends
    AbstractLiblinearTransformer<L, DiscreteDistribution<L>, LiblinearClassification.Prepared<L>, LiblinearClassification<L>> {

  private static final long serialVersionUID = 1;

  /**
   * Sets the solver type for liblinear; there are many choices, but the default is L2_LR (L2-normalized logistic
   * regression).  Currently you must use a logistic regression solver.
   *
   * @param type the type of solver to use.
   */
  @Override
  public LiblinearClassification<L> withSolverType(SolverType type) {
    Arguments.check(type.isLogisticRegressionSolver());
    return clone(c -> c._solverType = type);
  }

  @Override
  protected Preparer<L> getPreparer(PreparerContext context) {
    return new Preparer<>(context, this);
  }

  /* package-private */ static class Preparer<L>
      extends AbstractStreamPreparer2<L, Vector, DiscreteDistribution<L>, Prepared<L>> {
    private final Problem _problem = new Problem();
    private final Object2IntOpenHashMap<L> _labelIDMap;
    private final Long2IntOpenHashMap _featureIDMap;
    private final LiblinearClassification<L> _owner;
    private static final FeatureNode BIAS_PLACEHOLDER_FEATURE = new FeatureNode(Integer.MAX_VALUE, 0);

    private final List<Feature[]> _exampleFeatures; // will be copied to problem.x
    private final DoubleList _exampleLabels; // will be copied to problem.y

    public Preparer(PreparerContext context, LiblinearClassification<L> owner) {
      _labelIDMap = new Object2IntOpenHashMap<L>();
      _labelIDMap.defaultReturnValue(-1);
      _featureIDMap = new Long2IntOpenHashMap();
      _featureIDMap.defaultReturnValue(-1);

      // can't accommodate more than ~2 billion examples, but move forward under the assumption that there will be fewer
      // than estimated:
      int estimatedExampleCount = (int) Math.min(Integer.MAX_VALUE, context.getEstimatedExampleCount());
      _exampleFeatures = new ArrayList<>(estimatedExampleCount);
      _exampleLabels = new DoubleArrayList(estimatedExampleCount);

      _problem.bias = -1; // don't use built-in bias...it's broken!
      _owner = owner;
    }

    @Override
    public void process(L valueA, Vector valueB) {
      Long2IntMap m;
      Feature[] features;
      if (_owner.getBias() >= 0) {
        features = new Feature[Math.toIntExact(valueB.size64()) + 1];
        features[features.length - 1] = BIAS_PLACEHOLDER_FEATURE;
      } else {
        features = new Feature[Math.toIntExact(valueB.size64())];
      }

      int[] i = new int[1]; // boxed integer
      valueB.forEach((elementIndex, value) -> {
        int featureID = _featureIDMap.get(elementIndex);
        if (featureID < 0) {
          featureID = _featureIDMap.size() + 1; // liblinear for Java doesn't like 0 index features due to bug
          _featureIDMap.put(elementIndex, featureID);
        }
        features[i[0]++] = new FeatureNode(featureID, value);
      });

      int labelID = _labelIDMap.getInt(valueA);
      if (labelID < 0) { // marker value for "not there"
        labelID = _labelIDMap.size();
        _labelIDMap.put(valueA, labelID);
      }

      Arrays.sort(features, (f1, f2) -> f1.getIndex() - f2.getIndex());

      _exampleFeatures.add(features);
      _exampleLabels.add(labelID);
    }

    @Override
    public PreparerResult<Prepared<L>> finish() {
      _problem.l = _exampleFeatures.size(); // number of examples
      _problem.n = _featureIDMap.size() + (_owner.getBias() >= 0 ? 1 : 0);

      _problem.x = _exampleFeatures.toArray(new Feature[0][]);
      _problem.y = _exampleLabels.toDoubleArray();

      if (_owner.getBias() >= 0) {
        Feature biasFeature = new FeatureNode(_featureIDMap.size() + 1, _owner.getBias());
        for (Feature[] features : _problem.x) {
          features[features.length - 1] = biasFeature;
        }
      }

      Parameter parameter =
          new Parameter(_owner.getSolverType(), _owner.getLikelihoodVersusRegularizationLossMultiplier(),
              _owner.getEpsilon(), _owner.getSVREpsilonLoss());
      parameter.setThreadCount(_owner.getThreadCount());

      Linear.setDebugOutput(_owner._silent ? null : System.err);
      Model model = Linear.train(_problem, parameter);
      return new PreparerResult<>(
          new Prepared<L>(_owner._bias, model, _labelIDMap, _featureIDMap));
    }
  }

  /**
   * A trained liblinear linear classifier.
   *
   * @param <L> the type of label predicted
   */
  @ValueEquality
  public static class Prepared<L>
      extends AbstractLiblinearTransformer.Prepared<L, DiscreteDistribution<L>, Prepared<L>> {

    private static final long serialVersionUID = 1;

    @DeepArrayValueEquality
    private final L[] _labels; // Object[] masquerading as L[]: the "naked" array must not leak from this class
    private final boolean _isBinary;

    /**
     * Gets the weights associated with a particular label.  Note that, for binary problems (two labels), the weights
     * for one label are simply the negative values of the weights for the other.
     *
     * The bias is NOT included in these weights.  Please call getBiasForLabel(...) to get the bias.
     *
     * @param label the label for which you want to get the weights.  Note that multiclass Liblinear models use
     *              one-vs-all and have weights for every label.
     * @return a vector with all the weights corresponding to the label in the model.
     * @throws java.util.NoSuchElementException if the label was not seen during training
     */
    public Vector getWeightsForLabel(L label) {
      int index = getLabels().indexOf(label);
      if (index < 0) {
        throw new NoSuchElementException("The specified label " + label + " is not predicted by this model");
      }
      return getWeightsForLabelIndex(index);
    }

    /**
     * Gets the bias associated with a particular label.
     *
     * This is actually the value of the bias feature multiplied by the weight learned by the model for that feature.
     *
     * @param label the label for which you want to get the bias.  Note that multiclass Liblinear models use
     *              one-vs-all and have a bias for every label.
     * @return the bias value.
     * @throws java.util.NoSuchElementException if the label was not seen during training
     */
    public double getBiasForLabel(L label) {
      int index = getLabels().indexOf(label);
      if (index < 0) {
        throw new NoSuchElementException("The specified label " + label + " is not predicted by this model");
      }
      return getBiasForLabelIndex(index);
    }

    /**
     * Gets the labels predicted by this model.  These correspond with the labels seen in training, except that if
     * only "false" or only "true" boolean labels are seen in training the other label ("true" or "false", respectively)
     * will still be present in this list.
     *
     * @return a list of the labels; this should not be modified
     */
    @SuppressWarnings("unchecked")
    public List<L> getLabels() {
      // check for special, degenerate case where either false or true never seen during training
      if (_labels.length == 1 && _labels[0] instanceof Boolean) {
        ArrayList<Boolean> result = new ArrayList<>(2);
        result.add((Boolean) _labels[0]);
        result.add(!result.get(0));
        return (List<L>) result; // we know L is Boolean (or a supertype of Boolean)
      }

      return Arrays.asList(_labels);
    }

    // note that weightIndices in Liblinear start at 1, presumably to frustrate callers
    private double getWeightForLabel(int weightIndex, int labelIndex) {
      if (labelIndex == 1 && _model.getNrClass() == 1) { // degenerate case
        return -_model.getDecfunCoef(weightIndex, 0);
      } else {
        return _model.getDecfunCoef(weightIndex, labelIndex);
      }
    }

    private void checkIndex(int index) {
      if (_labels.length <= 2) {
        if (index >= 2) {
          throw new IndexOutOfBoundsException("Requested weights for label index " + index + " in a binary model");
        }
      } else {
        if (index > _labels.length) {
          throw new IndexOutOfBoundsException("Requested index for non-existant label: " + index);
        }
      }
    }

    /**
     * Gets the weights associated with a particular label.  Note that, for binary problems (two labels), the weights
     * for one label are simply the negative values of the weights for the other.
     *
     * The bias is NOT included in these weights.  Please call getBiasForLabelIndex(...) to get the bias.
     *
     * @param index the index of the label for which you want to get the weights.  If the model has only one label,
     *              an index value of 1 will be interpreted as corresponding to the weights for "not that label", i.e.
     *              a negation of the weights for the label at index 0.
     * @return a vector with all the weights corresponding to the label in the model.
     * @throws IndexOutOfBoundsException if the index does not correspond to a label
     */
    public Vector getWeightsForLabelIndex(int index) {
      checkIndex(index);

      SparseDoubleMapVector result = new SparseDoubleMapVector(_featureIDMap.size());
      _featureIDMap.long2IntEntrySet()
          .forEach(entry -> result.put(entry.getLongKey(), getWeightForLabel(entry.getIntValue(), index)));

      return result;
    }

    /**
     * Gets the bias associated with a particular label.
     *
     * This is actually the value of the bias feature multiplied by the weight learned by the model for that feature.
     *
     * @param index the index of the label for which you want to get the bias.  If the model has only one label,
     *              an index value of 1 will be interpreted as corresponding to the bias for "not that label", i.e.
     *              a negation of the bias for the label at index 0.
     * @return the bias value.
     * @throws IndexOutOfBoundsException if the index does not correspond to a label
     */
    public double getBiasForLabelIndex(int index) {
      checkIndex(index);

      if (_bias <= 0) {
        return 0;
      }

      return _bias * getWeightForLabel(_featureIDMap.size() + 1, index);
    }

    Model getModel() {
      return _model;
    }

    Long2IntOpenHashMap getFeatureIDMap() {
      return _featureIDMap;
    }

    @SuppressWarnings("unhecked")
    Prepared(double bias, Model model, Object2IntOpenHashMap<L> labelIDMap,
        Long2IntOpenHashMap featureIDMap) {
      super(bias, model, featureIDMap);

      _labels = (L[]) new Object[labelIDMap.size()];
      labelIDMap.forEach((label, id) -> _labels[id] = label);

      if (_labels.length == 2) {
        _isBinary = _labels[0] instanceof Boolean && _labels[1] instanceof Boolean;
      } else if (_labels.length == 1) {
        _isBinary = _labels[0] instanceof Boolean;
      } else {
        _isBinary = false;
      }

      // sanity check the reported numbers of classes to avoid possibility of logic errors later
      if ((_labels.length > 2 && _model.getNrClass() != _labels.length)
          || (_labels.length <= 2 && _model.getNrClass() > 2)) {
        throw new IllegalStateException(
            "Dagli wrapper and Liblinear disagree on the number of classes in the " + "model: Dagli thinks there "
                + _labels.length + " and Liblinear thinks there are " + _model.getNrClass());
      }
    }

    @Override
    @SuppressWarnings("unchecked")
    public DiscreteDistribution<L> apply(L val1, Vector val2) {
      ArrayList<Feature> features = new ArrayList<>(Math.toIntExact(val2.size64() + (_bias >= 0 ? 1 : 0)));
      val2.forEach((elementIndex, value) -> {
        int featureID = _featureIDMap.get(elementIndex);
        if (featureID >= 0) {
          features.add(new FeatureNode(featureID, value));
        }
      });
      if (_bias >= 0) {
        features.add(new FeatureNode(_featureIDMap.size() + 1, _bias));
      }

      double[] probabilities = new double[_labels.length];
      Linear.predictProbability(_model, features.toArray(new Feature[0]), probabilities);

      if (_isBinary) {
        return (DiscreteDistribution<L>) new BinaryDistribution(
            _labels[0].equals(Boolean.TRUE) ? probabilities[0] : 1 - probabilities[0]);
      } else {
        return new ArrayDiscreteDistribution<>(_labels, probabilities);
      }
    }
  }
}
