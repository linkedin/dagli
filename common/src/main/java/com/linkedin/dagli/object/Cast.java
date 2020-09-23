package com.linkedin.dagli.object;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.transformer.AbstractPreparedTransformer1WithInput;
import com.linkedin.dagli.util.invariant.Arguments;


/**
 * Transformer that casts its input to a specified class.
 *
 * By default, a {@link ClassCastException} will be thrown if a non-null instance cannot be cast to the specified class.
 *
 * @param <T> the type to which inputs will be cast
 */
@ValueEquality
class Cast<T> extends AbstractPreparedTransformer1WithInput<Object, T, Cast<T>> {
  private static final long serialVersionUID = 1;

  private Class<? extends T> _targetClass = null;
  private boolean _nullIfUncastable = false;

  /**
   * Creates a new instance with no target class set ({@link #withTargetClass(Class)} must later be used to create a
   * valid instance).
   */
  public Cast() { }

  /**
   * Creates a new instance with the specified target class.
   *
   * @param targetClass the class to which input instances should be cast
   */
  public Cast(Class<? extends T> targetClass) {
    _targetClass = targetClass;
  }

  @Override
  public void validate() {
    super.validate();
    Arguments.check(_targetClass != null);
  }

  @Override
  public String getName() {
    return getShortName() + " " + _input1.getShortName();
  }

  @Override
  public String getShortName() {
    return "(" + _targetClass.getSimpleName() + ")";
  }

  /**
   * @param targetClass the class to which input instances should be cast
   * @return a copy of this instance that will cast its inputs to the specified target class
   */
  public Cast<T> withTargetClass(Class<? extends T> targetClass) {
    return clone(c -> c._targetClass = targetClass);
  }

  /**
   * @return a copy of this instance that will produce a {@code null} rather than throwing a {@link ClassCastException}
   *         if the input is not actually castable to the target class.
   */
  public Cast<T> withNullIfUncastable() {
    return clone(c -> c._nullIfUncastable = true);
  }

  @Override
  public T apply(Object obj) {
    if (_nullIfUncastable && !_targetClass.isInstance(obj)) {
      return null;
    }
    return _targetClass.cast(obj);
  }
}
