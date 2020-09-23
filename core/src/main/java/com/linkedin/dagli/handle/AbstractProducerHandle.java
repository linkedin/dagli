package com.linkedin.dagli.handle;

import com.linkedin.dagli.annotation.Versioned;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.util.cloneable.AbstractCloneable;
import java.io.Serializable;


/**
 * Base class for handles to producers.  Depending on the type of handle, it may refer to a (logically) unique producer
 * (logically unique as multiple instances may exist in memory, but they will all be equivalent) or to a set of
 * producers matching some particular property (such as all producers that were "prepared" from a specific producer--
 * there will only at most one such producer in any DAG, but there can be more than one globally as that original
 * producer could have been used in multiple prepared DAGs).
 *
 * All handles are:
 * (1) Immutable
 * (2) Tied to a type of producer (i.e. the Producer interface or type deriving from it).  Handles contain the actual
 *     name of this type as a String to allow for dynamic verification of the type when invoking the handle.
 *
 * An example use for a handle is retrieving the prepared producer from a prepared DAG corresponding to an original
 * producer in the original DAG (before preparing).
 *
 * Direct use of handles is an "advanced" feature; generally you do not need to do so as Dagli provides convenience APIs
 * that will allow passing a producer rather than its handle.
 *
 * @param <T> the type of producer that will be retrieved with this handle.  This may be a supertype of the actual
 *            concrete producer type(s) that will be returned.
 * @param <S> the type of the derived handle
 */
@Versioned
abstract class AbstractProducerHandle<T extends Producer, S extends AbstractProducerHandle<T, S>>
    extends AbstractCloneable<S>
    implements Serializable {
  private static final long serialVersionUID = 1;

  private final String _targetClassName;

  public AbstractProducerHandle(String producerClassName) {
    _targetClassName = producerClassName;
  }

  /**
   * Gets the class name of the type of producer this handle refers to (note: this may be an interface name, not a class
   * per se).
   *
   * @return the class name as obtained via the Class::getName() method
   */
  protected String getTargetClassName() {
    return _targetClassName;
  }

  /**
   * Tests if the provided candidate could be a valid target of this handle by checking if it has a class which is
   * the same as or a subclass of the handle's target class.  E.g. if the target class is "Producer", any candidate is
   * acceptable; if it is, however, PreparedTransformer4 only a prepared transformer with an arity of 4 will be valid.
   *
   * @param candidateTarget the producer whose validity as a target of this handle is to be checked
   * @return true if the candidate is a valid target, false otherwise
   * @throws ClassNotFoundException if the handle's target class cannot be loaded
   */
  public boolean isValidTarget(Producer<?> candidateTarget) throws ClassNotFoundException {
    return Class.forName(_targetClassName).isAssignableFrom(candidateTarget.getClass());
  }

  /**
   * Casts a proposed target to the type of the handle, throwing an exception if the proposed target does not have a
   * type that is the same or a subtype of the handle's target type.
   *
   * @param candidateTarget the proposed target to cast to the target type of this handle
   * @throws ClassCastException if the proposed target's type is not the same or a subtype of the handle's target type
   * @throws RuntimeException if the handle's target type cannot be loaded
   * @return candidateTarget, cast to the handle's target type
   */
  @SuppressWarnings("unchecked")
  public T castTarget(Producer<?> candidateTarget) {
    try {
      if (!isValidTarget(candidateTarget)) {
        throw new ClassCastException(
            "Handle type mismatch: the target of the handle is of class " + candidateTarget.getClass().getName() + ", which is not the same type (or a "
                + "subtype) of the target type of the handle, " + _targetClassName);
      }
    } catch (ClassNotFoundException e) {
      throw new RuntimeException("The target type of the handle, " + _targetClassName
          + ", could not be loaded and therefore it could not be checked.", e);
    }

    return (T) candidateTarget;
  }
}