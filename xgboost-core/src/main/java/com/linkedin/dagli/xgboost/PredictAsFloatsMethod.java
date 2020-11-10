package com.linkedin.dagli.xgboost;

import ml.dmlc.xgboost4j.java.Booster;
import ml.dmlc.xgboost4j.java.DMatrix;
import ml.dmlc.xgboost4j.java.XGBoostError;


@FunctionalInterface
interface PredictAsFloatsMethod {
  /**
   * Given a {@link Booster} and a {@link DMatrix} containing a single row with the features for a single example,
   * calculates a prediction expressed as an array of floats.
   *
   * @param booster the XGBoost model to use
   * @param dmatrix a "matrix" comprised of a single row representing a single example's features
   * @return an array of floats representing a "prediction" of some kind
   * @throws XGBoostError if something goes wrong within XGBoost
   */
  float[] predictAsFloats(Booster booster, DMatrix dmatrix) throws XGBoostError;
}
