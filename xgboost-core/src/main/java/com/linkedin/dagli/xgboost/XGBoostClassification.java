package com.linkedin.dagli.xgboost;

import com.linkedin.dagli.annotation.equality.HandleEquality;
import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.math.distribution.ArrayDiscreteDistribution;
import com.linkedin.dagli.math.distribution.DiscreteDistribution;
import com.linkedin.dagli.math.distribution.DiscreteDistributions;
import com.linkedin.dagli.math.distribution.LabelProbability;
import com.linkedin.dagli.math.vector.DenseVector;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import ml.dmlc.xgboost4j.java.Booster;


/**
 * Gradient Boosted Decision Tree classifier.
 *
 * @param <L> the type of the label
 */
@ValueEquality
public class XGBoostClassification<L>
    extends AbstractXGBoostModel<L, DiscreteDistribution<L>, XGBoostClassification.Prepared<L>, XGBoostClassification<L>> {

  private static final long serialVersionUID = 1;

  @Override
  protected XGBoostObjective getObjective(int labelCount) {
    return labelCount > 2 ? XGBoostObjective.CLASSIFICATION_SOFTMAX : XGBoostObjective.CLASSIFICATION_LOGISTIC_REGRESSION;
  }

  @Override
  protected XGBoostObjectiveType getObjectiveType() {
    return XGBoostObjectiveType.CLASSIFICATON;
  }

  @Override
  protected XGBoostClassification.Prepared<L> createPrepared(Object2IntOpenHashMap<L> labelMap, Booster booster) {
    return new XGBoostClassification.Prepared<L>(labelMap, booster);
  }

  /**
   * A transformer that makes predictions (in the form of discrete distributions over the labels) from a trained XGBoost
   * classification model.
   *
   * @param <L> the type of the label predicted
   */
  @HandleEquality
  public static class Prepared<L>
      extends AbstractXGBoostModel.Prepared<L, DiscreteDistribution<L>, XGBoostClassification.Prepared<L>> {

    private static final long serialVersionUID = 1;

    private final Int2ObjectOpenHashMap<L> _idLabelMap;

    /**
     * Creates a new instance.
     *
     * @param labelMap a map from labels to their indices in the XGBoost model
     * @param booster the trained XGBoost classification model
     */
    public Prepared(Object2IntOpenHashMap<L> labelMap, Booster booster) {
      super(booster);

      _idLabelMap = new Int2ObjectOpenHashMap<L>(labelMap.size());
      labelMap.object2IntEntrySet().forEach(entry -> _idLabelMap.put(entry.getIntValue(), entry.getKey()));
    }

    @Override
    public DiscreteDistribution<L> apply(Number weight, L label, DenseVector vector) {
      final float[] probs =
          XGBoostModel.predictAsFloats(_booster, vector, (booster, dmatrix) -> booster.predict(dmatrix)[0]);

      // for binary and degenerate (0 or 1 label) problems, the model used should be logistic regression, producing a
      // single probability:
      assert _idLabelMap.size() > 2 || probs.length == 1;

      switch (_idLabelMap.size()) {
        case 2:
          return new ArrayDiscreteDistribution<>(((L[]) new Object[]{_idLabelMap.get(0), _idLabelMap.get(1)}),
              new double[]{1 - probs[0], probs[0]});
        case 1: // degenerate case with only a single label; probability is presumably always ~1
          return new ArrayDiscreteDistribution<>(((L[]) new Object[]{_idLabelMap.get(0)}), new double[]{probs[0]});
        case 0: // degenerate problem with no data and hence no labels
          return DiscreteDistributions.empty();
        default:
          return new ArrayDiscreteDistribution<>(IntStream.range(0, probs.length)
              .mapToObj(i -> new LabelProbability<>(_idLabelMap.get(i), probs[i]))
              .filter(lp -> lp.getLabel() != null)
              .collect(Collectors.toList()));
      }
    }
  }
}
