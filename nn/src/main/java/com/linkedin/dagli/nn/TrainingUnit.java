package com.linkedin.dagli.nn;

import java.io.Serializable;


/**
 * An unit of progress in training can be measured in many ways, such as number of examples, epochs, minibatches, etc.,
 * analogous to {@link java.time.temporal.TemporalUnit}.
 */
public enum TrainingUnit {
  /**
   * Training time measured by the number of examples processed.
   */
  EXAMPLES {
    @Override
    double toUnit(TrainingUnit targetUnit, double amount, Context context) {
      switch (targetUnit) {
        case EXAMPLES:
          return amount;
        case MINIBATCHES:
          return amount / context._examplesPerMinibatch;
        case EPOCHS:
          return amount / context._examplesPerEpoch;
        default:
          throw new IllegalArgumentException();
      }
    }
  },

  /**
   * Training time measured by the number of minibatches processed.
   */
  MINIBATCHES {
    @Override
    double toUnit(TrainingUnit targetUnit, double amount, Context context) {
      switch (targetUnit) {
        case EXAMPLES:
          return context._examplesPerMinibatch * amount;
        case MINIBATCHES:
          return amount;
        case EPOCHS:
          return amount / context.minibatchesPerEpoch();
        default:
          throw new IllegalArgumentException();
      }
    }
  },

  /**
   * Training time measured by the number of epochs completed.
   */
  EPOCHS {
    @Override
    double toUnit(TrainingUnit targetUnit, double amount, Context context) {
      switch (targetUnit) {
        case EXAMPLES:
          return context._examplesPerEpoch * amount;
        case MINIBATCHES:
          return context.minibatchesPerEpoch() * amount;
        case EPOCHS:
          return amount;
        default:
          throw new IllegalArgumentException();
      }
    }
  };

  private static long divideAndRoundUp(long numerator, long denominator) {
    return (numerator + denominator - 1) / denominator;
  }

  /**
   * Converts an amount in this unit to another unit.
   *
   * @param targetUnit the target unit
   * @param amount the amount
   * @param context the context in which the conversion takes place
   * @return the amount as expressed in the target unit, rounded up if necessary
   */
  abstract double toUnit(TrainingUnit targetUnit, double amount, Context context);

  /**
   * Information about a given training scenario required to convert training amounts from one unit to another.
   */
  public static class Context implements Serializable {
    private static final long serialVersionUID = 1;

    /**
     * @return the number of examples per minibatch
     */
    public long getExamplesPerMinibatch() {
      return _examplesPerMinibatch;
    }

    /**
     * @return the number of examples per epoch
     */
    public long getExamplesPerEpoch() {
      return _examplesPerEpoch;
    }

    private final long _examplesPerMinibatch;
    private final long _examplesPerEpoch;

    public Context(long examplesPerMinibatch, long examplesPerEpoch) {
      _examplesPerMinibatch = examplesPerMinibatch;
      _examplesPerEpoch = examplesPerEpoch;
    }

    private long minibatchesPerEpoch() {
      return divideAndRoundUp(_examplesPerEpoch, _examplesPerMinibatch);
    }
  }
}
