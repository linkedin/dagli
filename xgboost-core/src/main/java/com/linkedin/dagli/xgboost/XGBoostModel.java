package com.linkedin.dagli.xgboost;

import com.linkedin.dagli.math.vector.DenseVector;
import java.util.Collections;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import ml.dmlc.xgboost4j.java.Booster;
import ml.dmlc.xgboost4j.java.DMatrix;
import ml.dmlc.xgboost4j.java.XGBoostError;


/**
 * Static methods for conducting inference with an XGBoost model
 */
abstract class XGBoostModel {

  // This lock avoids segfault-causing contention between thread-local configuration and XGBoost inference
  private static final ReentrantReadWriteLock LOCK = new ReentrantReadWriteLock();

  // This is used to track whether the thread is properly configured to perform prediction with a single thread,
  // rather than spawning multiple threads.
  // Due to the internals of XGBoost each top-level (Java) thread must be configured and configuration is sticky to
  // the threads, not to the XGBoost instance.  Additionally, training (which its configured the number of threads)
  // effectively "clears" this setting, and so the thread must be configured again before it does prediction.
  static final ThreadLocal<Boolean> IS_THREAD_CONFIGURED_FOR_SINGLE_THREADED_PREDICTION =
      ThreadLocal.withInitial(() -> false);

  /**
   * Sets the number of threads for prediction to 1.
   *
   * Inside XGBoost, this is accomplished via omp_set_num_threads, which appears to behave as a thread-local
   * property.  Consequently, setting this value in other threads has no effect, so we must configure XGBoost
   * in every thread before it's use.
   *
   * Note that calling this once per thread, REGARDLESS OF WHICH XGBOOST INSTANCE IS USED, should be enough.
   * The nthread parameter is implemented in such a way that it's sticky to the thread, not the instance.
   *
   * A lock is used to prevent this configuration from running in parallel with other XGBoost native calls,
   * which triggers segfaults.
   */
  private static void configureBooster(Booster booster) {
    if (IS_THREAD_CONFIGURED_FOR_SINGLE_THREADED_PREDICTION.get()) {
      return;
    }
    LOCK.writeLock().lock();
    try {
      booster.setParam("nthread", 1);
      IS_THREAD_CONFIGURED_FOR_SINGLE_THREADED_PREDICTION.set(true);
    } catch (XGBoostError err) {
      throw new RuntimeException(err);
    } finally {
      LOCK.writeLock().unlock();
    }
  }

  /**
   * Given an example represented as a {@link DenseVector}, makes a "raw" prediction of a float array.  It does this
   * by calling a supplied {@link PredictAsFloatsMethod}.
   *
   * @param vector the feature vector for which we should make a prediction
   * @return an array of floats whose meaning depends on the supplied {@link PredictAsFloatsMethod} and the model
   */
  static float[] predictAsFloats(Booster booster, DenseVector vector, PredictAsFloatsMethod predictor) {
    configureBooster(booster);

    // get lock to make this mutually exclusive to thread-local configuration
    LOCK.readLock().lock();

    DMatrix dmatrix = null;
    try {
      dmatrix =
          new DMatrix(Collections.singleton(AbstractXGBoostModel.makeDenseLabeledPoint(null, 0, vector)).iterator(),
              null);
      return predictor.predictAsFloats(booster, dmatrix);
    } catch (XGBoostError err) {
      throw new RuntimeException("XGBoost threw an exception during inference", err);
    } finally {
      if (dmatrix != null) {
        dmatrix.dispose(); // free memory allocated by native library immediately
      }
      LOCK.readLock().unlock();
    }
  }
}
