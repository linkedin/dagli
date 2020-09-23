package com.linkedin.dagli.xgboost;

import com.linkedin.dagli.annotation.equality.HandleEquality;
import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.math.vector.DenseVector;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import ml.dmlc.xgboost4j.java.Booster;


/**
 * Gradient Boosted Decision Tree regression.  The "labels" for training this regression can be any {@link Number}s,
 * though the actual predictions resulting from the model will be {@link Float}s.
 */
@ValueEquality
public class XGBoostRegression extends AbstractXGBoostModel<Number, Float, XGBoostRegression.Prepared, XGBoostRegression> {
  private static final long serialVersionUID = 1;

  @Override
  protected XGBoostObjective getObjective(int labelCount) {
    return XGBoostObjective.REGRESSION_SQUARED_ERROR;
  }

  @Override
  protected XGBoostObjectiveType getObjectiveType() {
    return XGBoostObjectiveType.REGRESSION;
  }

  @Override
  protected Prepared createPrepared(Object2IntOpenHashMap<Number> labelMap, Booster booster) {
    return new Prepared(booster);
  }

  /**
   * A trained XGBoost regression model.
   */
  @HandleEquality
  public static class Prepared
      extends AbstractXGBoostModel.Prepared<Number, Float, Prepared> {

    private static final long serialVersionUID = 1;

    /**
     * Creates a new instance.
     *
     * @param booster the trained XGBoost regression model.
     */
    public Prepared(Booster booster) {
      super(booster);
    }

    @Override
    public Float apply(Number weight, Number label, DenseVector vector) {
      return XGBoostModel.predictAsFloats(_booster, vector, (booster, dmatrix) -> booster.predict(dmatrix)[0])[0];
    }
  }
}
