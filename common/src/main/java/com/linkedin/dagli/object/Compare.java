package com.linkedin.dagli.object;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.transformer.AbstractPreparedTransformer2;
import com.linkedin.dagli.transformer.PreparedTransformer2;
import java.util.Comparator;
import java.util.Objects;


/**
 * Provides the {@link #is(Producer)} method for creating comparison transformers, e.g.
 * {@code Compare.is(firstProducer).equalTo(secondProducer)}
 */
public abstract class Compare {
  private Compare() { }

  /**
   * Creates a builder that will allow for the creation of a desired comparison transformer.
   *
   * @param first the first thing being being compared
   * @param <T> the type of the first thing being compared
   * @return a builder that will allow for the creation of a desired comparison transformer
   */
  public static <T> CompareBuilder<T> is(Producer<T> first) {
    return new CompareBuilder<>(first);
  }

  /**
   * Builder for creating comparison transformers.
   *
   * @param <T> the type of the first thing being compared
   */
  public static class CompareBuilder<T> {
    private final Producer<T> _first;

    private CompareBuilder(Producer<T> first) {
      _first = first;
    }

    /**
     * @param second the second producer providing an input to compare
     * @return a prepared transformer that checks whether the values from both the first and second producers are equal
     *         as determined by {@link Objects#equals(Object, Object)}
     */
    public PreparedTransformer2<Object, Object, Boolean> equalTo(Producer<?> second) {
      return new Equal(_first, second);
    }

    /**
     * @param second the second producer providing an input to compare
     * @return a prepared transformer that checks whether the values from both the first and second producers are not
     *         equal as determined by {@link Objects#equals(Object, Object)}
     */
    public PreparedTransformer2<Object, Object, Boolean> notEqualTo(Producer<?> second) {
      return new NotEqual(_first, second);
    }

    /**
     * @param second the second producer providing an input to compare
     * @return a prepared transformer that checks whether the values from both the first and second producers are
     *         reference-equal (==)
     */
    public PreparedTransformer2<Object, Object, Boolean> referenceEqualTo(Producer<?> second) {
      return new ReferenceEqual(_first, second);
    }

    /**
     * @param second the second producer providing an input to compare
     * @return a prepared transformer that checks whether the values from both the first and second producers are not
     *         reference-equal (!=)
     */
    public PreparedTransformer2<Object, Object, Boolean> notReferenceEqualTo(Producer<?> second) {
      return new NotReferenceEqual(_first, second);
    }

    /**
     * @param second the second producer providing an input to compare
     * @return a prepared transformer that checks whether the value from the first transformer is greater than the value
     *         from the second
     */
    public <U extends Comparable<? super T>> PreparedTransformer2<T, U, Boolean> greaterThan(
        Producer<? extends U> second) {
      return new GreaterThan<T, U>(_first, second, SecondValueComparator.INSTANCE);
    }

    /**
     * @param second the second producer providing an input to compare
     * @return a prepared transformer that checks whether the value from the first transformer is greater than or equal
     *         to the value from the second
     */
    public <U extends Comparable<? super T>> PreparedTransformer2<T, U, Boolean> greaterThanOrEqualTo(
        Producer<? extends U> second) {
      return new GreaterThanOrEqual<T, U>(_first, second, SecondValueComparator.INSTANCE);
    }

    /**
     * @param second the second producer providing an input to compare
     * @return a prepared transformer that checks whether the value from the first transformer is less than the value
     *         from the second
     */
    public <U extends Comparable<? super T>> PreparedTransformer2<U, T, Boolean> lessThan(
        Producer<? extends U> second) {
      return new GreaterThan<U, T>(second, _first, Comparator.naturalOrder());
    }

    /**
     * @param second the second producer providing an input to compare
     * @return a prepared transformer that checks whether the value from the first transformer is less than or equal
     *         to the value from the second
     */
    public <U extends Comparable<? super T>> PreparedTransformer2<U, T, Boolean> lessThanOrEqualTo(
        Producer<? extends U> second) {
      return new GreaterThanOrEqual<U, T>(second, _first, Comparator.naturalOrder());
    }

    /**
     * @param second the second producer providing an input to compare
     * @param comparator a {@link Comparator} used to compare the values
     * @return a prepared transformer that checks whether the value from the first transformer is greater than the value
     *         from the second
     */
    public PreparedTransformer2<T, T, Boolean> greaterThan(
        Producer<? extends T> second, Comparator<? super T> comparator) {
      return new GreaterThan<T, T>(_first, second, comparator);
    }

    /**
     * @param second the second producer providing an input to compare
     * @param comparator a {@link Comparator} used to compare the values
     * @return a prepared transformer that checks whether the value from the first transformer is greater than or equal
     *         to the value from the second
     */
    public PreparedTransformer2<T, T, Boolean> greaterThanOrEqualTo(
        Producer<? extends T> second, Comparator<? super T> comparator) {
      return new GreaterThanOrEqual<>(_first, second, comparator);
    }

    /**
     * @param second the second producer providing an input to compare
     * @param comparator a {@link Comparator} used to compare the values
     * @return a prepared transformer that checks whether the value from the first transformer is less than the value
     *         from the second
     */
    public PreparedTransformer2<T, T, Boolean> lessThan(
        Producer<? extends T> second, Comparator<? super T> comparator) {
      return new GreaterThan<T, T>(second, _first, comparator);
    }

    /**
     * @param second the second producer providing an input to compare
     * @param comparator a {@link Comparator} used to compare the values
     * @return a prepared transformer that checks whether the value from the first transformer is less than or equal
     *         to the value from the second
     */
    public PreparedTransformer2<T, T, Boolean> lessThanOrEqualTo(
        Producer<? extends T> second, Comparator<? super T> comparator) {
      return new GreaterThanOrEqual<>(second, _first, comparator);
    }
  }

  // Same as Comparator.naturalOrder() except that the second value's compareTo method is used rather than the first's
  private enum SecondValueComparator implements Comparator<Object> {
    INSTANCE;

    @Override
    @SuppressWarnings("unchecked")
    public int compare(Object value1, Object value2) {
      return -((Comparable) value2).compareTo(value1);
    }
  }

  @ValueEquality
  static class GreaterThan<T, U>
      extends AbstractPreparedTransformer2<T, U, Boolean, GreaterThan<T, U>> {
    private static final long serialVersionUID = 1;
    private final Comparator<Object> _comparator;

    @SuppressWarnings("unchecked")
    GreaterThan(Producer<? extends T> first, Producer<? extends U> second, Comparator<?> comparator) {
      super(first, second);
      _comparator = (Comparator<Object>) comparator;
    }

    @Override
    public Boolean apply(T value1, U value2) {
      return _comparator.compare(value1, value2) > 0;
    }
  }

  @ValueEquality
  static class GreaterThanOrEqual<T, U>
      extends AbstractPreparedTransformer2<T, U, Boolean, GreaterThanOrEqual<T, U>> {
    private static final long serialVersionUID = 1;
    private final Comparator<Object> _comparator;

    @SuppressWarnings("unchecked")
    GreaterThanOrEqual(Producer<? extends T> first, Producer<? extends U> second, Comparator<?> comparator) {
      super(first, second);
      _comparator = (Comparator<Object>) comparator;
    }

    @Override
    public Boolean apply(T value1, U value2) {
      return _comparator.compare(value1, value2) > 0;
    }
  }

  @ValueEquality
  static class Equal extends AbstractPreparedTransformer2<Object, Object, Boolean, Equal> {
    private static final long serialVersionUID = 1;

    Equal(Producer<?> first, Producer<?> second) {
      super(first, second);
    }

    @Override
    public Boolean apply(Object value1, Object value2) {
      return Objects.equals(value1, value2);
    }
  }

  @ValueEquality
  static class NotEqual extends AbstractPreparedTransformer2<Object, Object, Boolean, Equal> {
    private static final long serialVersionUID = 1;

    NotEqual(Producer<?> first, Producer<?> second) {
      super(first, second);
    }

    @Override
    public Boolean apply(Object value1, Object value2) {
      return !Objects.equals(value1, value2);
    }
  }

  @ValueEquality
  static class ReferenceEqual extends AbstractPreparedTransformer2<Object, Object, Boolean, Equal> {
    private static final long serialVersionUID = 1;

    ReferenceEqual(Producer<?> first, Producer<?> second) {
      super(first, second);
    }

    @Override
    public Boolean apply(Object value1, Object value2) {
      return value1 == value2;
    }
  }

  @ValueEquality
  static class NotReferenceEqual extends AbstractPreparedTransformer2<Object, Object, Boolean, Equal> {
    private static final long serialVersionUID = 1;

    NotReferenceEqual(Producer<?> first, Producer<?> second) {
      super(first, second);
    }

    @Override
    public Boolean apply(Object value1, Object value2) {
      return value1 != value2;
    }
  }
}
