package com.linkedin.dagli.nn;

import com.linkedin.dagli.util.invariant.Arguments;
import java.io.Serializable;
import java.util.Objects;


/**
 * An immutable amount of training progress as measured in {@link TrainingUnit}s; analogous to
 * {@link java.time.temporal.TemporalAmount}.
 */
public class TrainingAmount implements Serializable {
  private static final long serialVersionUID = 1;

  /**
   * Represents an absence of training progress.
   */
  public static final TrainingAmount ZERO = new TrainingAmount(TrainingUnit.EXAMPLES, 0);
  public static final TrainingAmount INFINITY = new TrainingAmount(TrainingUnit.EXAMPLES, Double.POSITIVE_INFINITY);

  // instance fields
  private final TrainingUnit _unit;
  private final double _amount;

  /**
   * @param count the number of epochs
   * @return a {@link TrainingAmount} representing the specified number of epochs
   */
  public static TrainingAmount epochs(double count) {
    return new TrainingAmount(TrainingUnit.EPOCHS, count);
  }

  /**
   * @param count the number of minibatches
   * @return a {@link TrainingAmount} representing the specified number of minibatches
   */
  public static TrainingAmount minibatches(double count) {
    return new TrainingAmount(TrainingUnit.MINIBATCHES, count);
  }

  /**
   * @param count the number of examples
   * @return a {@link TrainingAmount} representing the specified number of examples
   */
  public static TrainingAmount examples(double count) {
    return new TrainingAmount(TrainingUnit.EXAMPLES, count);
  }

  /**
   * @return true if this instance represents a finite amount of training, false otherwise
   */
  public boolean isFinite() {
    return _amount != Double.POSITIVE_INFINITY;
  }

  /**
   * @return true if this instance represents a zero amount of training
   */
  public boolean isZero() {
    return _amount == 0;
  }

  /**
   * @return the {@link TrainingUnit} in which this training amount was originally expressed
   */
  public TrainingUnit getUnit() {
    return _unit;
  }

  /**
   * @return the amount of training this instance represents, with a unit type that may be retrieved by
   *         {@link #getUnit()}
   */
  public double getAmount() {
    return _amount;
  }

  /**
   * Gets this training amount as expressed in the requested unit, given the training context.
   *
   * @param targetUnit the {@link TrainingUnit} in which to express this training amount
   * @param context the training context containing information regarding the number of examples per minibatch, etc.
   * @return this training amount as expressed in the requested unit, rounded up if necessary
   */
  public double get(TrainingUnit targetUnit, TrainingUnit.Context context) {
    return _unit.toUnit(targetUnit, _amount, context);
  }

  /**
   * Gets this training amount as expressed in the requested unit, given the training context.  If the amount cannot
   * be expressed exactly as an integer, the returned amount will be rounded up.
   *
   * @param targetUnit the {@link TrainingUnit} in which to express this training amount
   * @param context the training context containing information regarding the number of examples per minibatch, etc.
   * @return this training amount as expressed in the requested unit, rounded up if necessary
   */
  public long getAsLong(TrainingUnit targetUnit, TrainingUnit.Context context) {
    return (long) Math.ceil(_unit.toUnit(targetUnit, _amount, context));
  }

  /**
   * Creates a new instance denoting a certain amount of training progress.
   *
   * Fractional amounts are allowed; this permits instances to express amounts of training such as "half an epoch".
   *
   * @param unit the type of the amount being provided
   * @param amount the amount of training progress this instance will represent
   */
  public TrainingAmount(TrainingUnit unit, double amount) {
    Arguments.check(amount >= 0, "Amount of training " + amount + " is not >= 0");
    _unit = unit;
    _amount = amount;
  }

  @Override
  public int hashCode() {
    if (_amount == 0) { // all 0s are equal
      return 0;
    } else if (_amount == Double.POSITIVE_INFINITY) {
      return Double.hashCode(Double.POSITIVE_INFINITY);
    } else {
      return Objects.hash(_unit, _amount);
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof TrainingAmount)) {
      return false;
    }

    TrainingAmount other = (TrainingAmount) obj;
    return (_amount == 0 || _amount == Double.POSITIVE_INFINITY || _unit == other._unit) && _amount == other._amount;
  }

  @Override
  public String toString() {
    return _amount + " " + _unit;
  }
}
