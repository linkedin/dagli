package com.linkedin.dagli.math.distribution;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.Optional;
import java.util.stream.Stream;


/**
 * Degenerate distribution that has no entries.
 *
 * @param <T> The type of labels that will be associated with probabilities.
 */
class EmptyDiscreteDistribution<T> extends AbstractDiscreteDistribution<T> implements Serializable {
  private static final long serialVersionUID = 1;

  private static EmptyDiscreteDistribution _singleton = new EmptyDiscreteDistribution();

  private EmptyDiscreteDistribution() { }

  public static <T> EmptyDiscreteDistribution<T> get() {
    return _singleton;
  }

  // we use a custom readResolve() method to ensure that only a single instance exists, even when previously-serialized
  // copies are being read
  private Object readResolve() throws ObjectStreamException {
    return get();
  }

  @Override
  public double get(T label) {
    return 0.0;
  }

  @Override
  public Optional<LabelProbability<T>> max() {
    return Optional.empty();
  }

  @Override
  public long size64() {
    return 0;
  }

  @Override
  public Stream<LabelProbability<T>> stream() {
    return Stream.empty();
  }
}
