package com.linkedin.dagli.util.cloneable;

import java.io.Serializable;
import java.util.function.Consumer;


/**
 * AbstractCloneable provides convenient clone methods which can be used to implement in-place builder mechanics (i.e.
 * "someObject.withProperty(X)" would be implemented as "this.clone(c -> c._property = x);").
 *
 * AbstractCloneable provides the same shallow cloning as Object.clone(), not deep copies, although the {@link #clone()}
 * method may be overridden to deeply clone some or all fields as desired.
 *
 * To make the clone method public, simply add this code to your class:
 * <pre> {@code
 * @Override
 * public YourClass clone() {
 *   return super.clone();
 * }
 * } </pre>
 *
 * @param <S> the type of the ultimate derived class that extends from this one; this should be a concrete class, such
 *            as {@code MyClass<String>}, not a wildcard class such as {@code MyClass<? super String>}
 */
public class AbstractCloneable<S extends AbstractCloneable<S>> implements Cloneable, Serializable {
  private static final long serialVersionUID = 1;

  @Override
  @SuppressWarnings("unchecked") // safe due to semantics of clone because S is this instance's class
  protected S clone() {
    try {
      return (S) super.clone();
    } catch (CloneNotSupportedException e) {
      throw new RuntimeException("Failed to clone self despite implementing Cloneable; this should never happen", e);
    }
  }

  /**
   * Clones this instance, then passes the clone to a modifier function before returning the (possibly modified) clone.
   *
   * This can be a very succinct way to express "make a copy of this instance but with some change", e.g.
   * {@code this.clone(c -> c._property = x);}
   *
   * @param modifier the modifier function that will be passed the clone
   * @return the (possibly modified) clone of this instance
   */
  protected S clone(Consumer<S> modifier) {
    S res = clone();
    modifier.accept(res);
    return res;
  }
}