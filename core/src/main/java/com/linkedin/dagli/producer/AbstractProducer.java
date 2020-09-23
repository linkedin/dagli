package com.linkedin.dagli.producer;

import com.linkedin.dagli.annotation.DefinesAllOrNoneOfTheseMethods;
import com.linkedin.dagli.annotation.Versioned;
import com.linkedin.dagli.annotation.equality.HandleEquality;
import com.linkedin.dagli.annotation.equality.IgnoredByValueEquality;
import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.annotation.producer.internal.IsAbstractProducer;
import com.linkedin.dagli.handle.ProducerHandle;
import com.linkedin.dagli.producer.internal.ProducerInternalAPI;
import com.linkedin.dagli.reducer.ClassReducerTable;
import com.linkedin.dagli.reducer.Reducer;
import com.linkedin.dagli.transformer.Transformer;
import com.linkedin.dagli.util.cloneable.AbstractCloneable;
import com.linkedin.dagli.util.equality.ValueEqualityChecker;
import com.linkedin.dagli.util.function.BooleanFunction2;
import com.linkedin.dagli.util.function.IntFunction1;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Base class for producers.  Implementors of Dagli DAG nodes (such as transformers, generators, views, etc.) are
 * <strong>strongly</strong> encouraged to derive from a descendant of this class (for example, a prepared transformer
 * of arity 4 should implement {@link com.linkedin.dagli.transformer.AbstractPreparedTransformer4}).
 *
 * Doing so reduces the chance your code will break in a future version of Dagli, and provides standard implementations
 * for common producer methods.
 *
 * @param <R> the type of return value for this producer
 * @param <S> the derived producer type
 */
@Versioned
@IgnoredByValueEquality
@DefinesAllOrNoneOfTheseMethods({"computeHashCode", "computeEqualsUnsafe"})
@IsAbstractProducer // used by Dagli's annotation processor
abstract class AbstractProducer<R, I extends ProducerInternalAPI<R, S>, S extends AbstractProducer<R, I, S>>
    extends AbstractCloneable<S>
    implements Producer<R> {
  private static final long serialVersionUID = 1;

  // we cache our derived classes' hash and equality functions if they are using "value" or "handle" equality
  private static final ConcurrentHashMap<Class<? extends AbstractProducer>, IntFunction1<AbstractProducer>>
      HASH_FUNCTION_CACHE = new ConcurrentHashMap<>();
  private static final ConcurrentHashMap<Class<? extends AbstractProducer>, BooleanFunction2<AbstractProducer, AbstractProducer>>
      EQUALITY_FUNCTION_CACHE = new ConcurrentHashMap<>();

  private long _uuidLeastSignificantBits;
  private long _uuidMostSignificantBits;

  // this flag is used to cache our knowledge of whether this instance is constant-result or not:
  private byte _isConstantResult = 0; // 0: unknown, 1 == no, 2 == yes

  // we cache the hash of this object to avoid the expense of constantly recomputing it (recall that all ProducerUtil
  // should be functionally immutable)
  private transient int _hashCode = 0; // transient to allow for changes in hashing logic

  private transient I _internalAPI = createInternalAPI();

  /**
   * Creates a new instance.  A UUID (used when referencing producers) will be randomly generated.
   */
  public AbstractProducer() {
    setRandomUUID(this);
  }

  /**
   * Creates a new instance that will assume the provided universally unique identifier (UUID).
   *
   * UUIDs are used to refer to producers, but should not participate in their {@link Object#equals(Object)} or
   * {@link Object#hashCode()} methods (except for {@link com.linkedin.dagli.placeholder.Placeholder}s.
   *
   * @param uuidMostSignificantBits the highest 64 bits of the UUID
   * @param uuidLeastSignificantBits the lowest 64 bits of the UUID
   */
  public AbstractProducer(long uuidMostSignificantBits, long uuidLeastSignificantBits) {
    _uuidMostSignificantBits = uuidMostSignificantBits;
    _uuidLeastSignificantBits = uuidLeastSignificantBits;
  }

  /**
   * May be overridden to provide a list of graph reducers that should apply to DAGs containing this node.
   *
   * Graph reducers simplify a graph by rewriting it (such as by removing or replacing its nodes).  The resulting graph
   * will be more efficient but maintain the same inputs and outputs.
   *
   * Although normally the reducers for a given class will always be the same, different instances of the same class may
   * return different lists of reducers.  However, the lists returned by two instances that differ only in their inputs
   * are assumed to always be the same.
   *
   * @return a list of graph reducers (that will not be modified) that should apply to DAGs containing this node
   */
  protected Collection<? extends Reducer<? super S>> getGraphReducers() {
    return Collections.emptyList();
  }

  /**
   * May be overridden to return a {@link ClassReducerTable} that provides sets of {@link Reducer}s for
   * different classes.  Instances of producers that belong to these classes (in the same DAG as this one) will be
   * subject to the corresponding reductions.
   *
   * To provide reductions that should apply to <i>this</i> instance, override {@link #getGraphReducers()} instead.
   * This method is intended to allow a producer to effectively specify reductions for <i>other</i> types of producers
   * when their code cannot be modified.  By convention, these added reductions should somehow relate to this producer
   * (e.g. a reduction that only applies when producers of this type are an ancestor).
   *
   * Although normally the {@link ClassReducerTable} returned by instances of a given class will always be the
   * same, different instances of the same class may return different tables.  However, the tables returned by two
   * instances that differ only in their inputs are assumed to always be the same.
   *
   * When reducing a DAG, producers may be added or removed, which may affect when and if the reducers from the
   * {@link ClassReducerTable} are applied; they may be only applied to some (or none) of the possible producers.
   *
   * In particular, the {@link Reducer}s in a {@link ClassReducerTable} should not depend on "cooperating" with other
   * {@link Reducer}s for correctness, as they cannot be guaranteed to run.
   *
   * The default implementation returns {@code null}, indicating that no reductions for other producers are requested by
   * this instance.
   *
   * @return a {@link ClassReducerTable} that should apply to the DAG containing this producer, or {@code null}
   */
  protected ClassReducerTable getClassReducerTable() {
    return null;
  }

  /**
   * Returns true if this producer will produce the same value for all examples <strong>in a given execution of the DAG
   * </strong>.  Unlike {@link Producer#hasConstantResult()}, this must be true regardless of the producer's ancestors
   * in the DAG (i.e. this producer should still have a constant result even if its parents are arbitrarily changed).
   *
   * For example, if we create a "Cardinality" PreparableTransformer that takes a single input and yields a
   * PreparedTransformer} that outputs the total number of distinct inputted values seen during preparation (for all
   * examples), the PreparableTransformer has a constant result.
   *
   * If this method returns true, the producer must have a constant result <strong>regardless of the examples in a
   * particular run</strong>; a node which produces the same value for all examples in some DAG executions but not
   * others is not considered to have a constant result.
   *
   * PreparableTransformers that are always-constant-result must also prepare to PreparedTransformers (for "preparation"
   * data and "new" data) that are themselves always-constant-result, also returning true for
   * {@link #hasAlwaysConstantResult()}.
   *
   * An always-constant-value {@link com.linkedin.dagli.transformer.PreparedTransformer} will be assumed by Dagli to
   * always return the same value regardless of its input values, even if those values are nulls.  Dagli may
   * consequently apply the transformer by passing null inputs to pre-compute its constant result value.
   *
   * Core types of Producers that have constant results include Constant, all TransformerViews and all
   * ConstantResultTransformations.
   *
   * Note that, even if this method returns false, Dagli can infer the producer will have a constant result when its
   * parents all have constant results; this is because all child nodes in Dagli are assumed to be deterministic, always
   * producing the same output given the same inputs).
   *
   * Because constant-resultness can be determined for any producer instance <strong>before</strong> executing its DAG,
   * it is very useful for certain correctness checks and optimizations.
   *
   * The default implementation of this method returns false.
   *
   * @return whether this producer will always have a constant result throughout any given DAG execution, regardless of
   *         its parents
   */
  protected boolean hasAlwaysConstantResult() {
    return false;
  }

  @Override
  public final boolean hasConstantResult() {
    // this is thread-safe because setting bytes in Java is atomic and we don't care if a race condition happens to
    // cause us to calculate constant-result-ness twice:
    if (_isConstantResult == 0) {
      _isConstantResult = ProducerUtil.hasConstantResult(this) ? (byte) 2 : (byte) 1;
    }

    return _isConstantResult == 2;
  }

  private static Class<?> getMostDerivedDeclaringClass(Class<?> mostDerivedClass, String methodName) {
    while (mostDerivedClass != null) {
      if (Arrays.stream(mostDerivedClass.getDeclaredMethods()).anyMatch(m -> m.getName().equals(methodName))) {
        break;
      }
      mostDerivedClass = mostDerivedClass.getSuperclass();
    }

    return mostDerivedClass;
  }

  private IntFunction1<AbstractProducer> getHashCodeFunction() {
    return HASH_FUNCTION_CACHE.computeIfAbsent(this.getClass(), clazz -> {
      ValueEquality valueEqualityAnnotation = clazz.getAnnotation(ValueEquality.class);
      HandleEquality handleEqualityAnnotation = clazz.getAnnotation(HandleEquality.class);
      if (valueEqualityAnnotation != null && handleEqualityAnnotation != null) {
        throw new IllegalStateException("A class may not be annotated with both @ValueEquality and @HandleEquality");
      }

      if (handleEqualityAnnotation != null) {
        return AbstractProducer::handleHashCode;
      }

      // the only alternative (and the default) is value equality; create the ValueEqualityChecker object
      ValueEqualityChecker<AbstractProducer> valueEqualityChecker =
          new ValueEqualityChecker<>((Class<AbstractProducer>) clazz);

      if (this instanceof ChildProducer) {
        if (valueEqualityAnnotation != null && valueEqualityAnnotation.commutativeInputs()) {
          return instance -> Transformer.hashCodeOfUnorderedInputs((ChildProducer) instance)
              + valueEqualityChecker.hashCode(instance);
        } else { // non-commutative inputs
          return instance -> Transformer.hashCodeOfInputs((ChildProducer) instance)
              + valueEqualityChecker.hashCode(instance);
        }
      } else {
        return valueEqualityChecker::hashCode;
      }
    });
  }

  private BooleanFunction2<AbstractProducer, AbstractProducer> getEqualsFunction() {
    return EQUALITY_FUNCTION_CACHE.computeIfAbsent(this.getClass(), clazz -> {
      ValueEquality valueEqualityAnnotation = clazz.getAnnotation(ValueEquality.class);
      HandleEquality handleEqualityAnnotation = clazz.getAnnotation(HandleEquality.class);
      if (valueEqualityAnnotation != null && handleEqualityAnnotation != null) {
        throw new IllegalStateException("A class may not be annotated with both @ValueEquality and @HandleEquality");
      }

      if (handleEqualityAnnotation != null) {
        return AbstractProducer::handleEquality;
      }

      // the only alternative (and the default) is value equality; create the ValueEqualityChecker object
      ValueEqualityChecker<AbstractProducer> valueEqualityChecker =
          new ValueEqualityChecker<>((Class<AbstractProducer>) clazz);

      if (this instanceof ChildProducer) {
        if (valueEqualityAnnotation != null && valueEqualityAnnotation.commutativeInputs()) {
          return (inst1, inst2) -> Transformer.sameUnorderedInputs((ChildProducer) inst1, (ChildProducer) inst2)
              && valueEqualityChecker.equals(inst1, inst2);
        } else { // non-commutative inputs
          return (inst1, inst2) -> Transformer.sameInputs((ChildProducer) inst1, (ChildProducer) inst2)
              && valueEqualityChecker.equals(inst1, inst2);
        }
      } else {
        return valueEqualityChecker::equals;
      }
    });
  }

  /**
   * Calculates the hashCode for this {@link Producer}.  Because {@link Producer}s are immutable, hashes are cached,
   * so subsequent calls to this method after the first will be extremely cheap.
   *
   * Derived classes should override {@link #computeHashCode()} instead of this method.
   *
   * @return the hash code for this {@link Producer}.
   */
  @Override
  public final int hashCode() {
    // if the _hashCode has already been calculated, return it
    if (_hashCode != 0) {
      return _hashCode;
    }

    // otherwise, calculate the hashCode.  If a hashCode of 0 is encountered, replace it with 1 (to avoid recalculating
    // it in the future).  Note that reading and writing an int field is an atomic operation and so, in a race
    // condition, the worst case is that the hash will be computed multiple times (the correct value will always be
    // returned).
    int calculatedHashCode = computeHashCode();
    _hashCode = (calculatedHashCode == 0 ? 1 : calculatedHashCode);
    return _hashCode;
  }

  /**
   * Implements the {@link Object#equals(Object)} method by taking the following steps:
   * (1) If <code>this == other</code>, returns true
   * (2) If other == null returns false
   * (3) If the class of <code>other</code> derives from this instance's class, returns the result of calling
   *     <code>this.computeEquals(other)</code>.
   * (4) If the class of this instance derives from <code>other</code>'s class, returns the result of calling
   *     <code>other.computeEquals(this)</code>
   *
   * Derived classes should implement their equality-testing logic by overriding
   * {@link #computeEqualsUnsafe(AbstractProducer)}.
   *
   * Note that, because this method is final, it is not currently possible for two instances of
   * {@link AbstractProducer}-derived classes to be equal unless one of those classes derives from the other.  This may
   * change in the future, and should not be relied upon by callers to this method.
   *
   * @param other the other instance to check for equality with this instance
   * @return true if this instance and other should be considered equivalent, false otherwise
   */
  @Override
  @SuppressWarnings("unchecked") // types are checked via reflection
  public final boolean equals(Object other) {
    if (this == other) { // fast identity path
      return true;
    } else if (!(other instanceof AbstractProducer)) { // check for null argument or "unrelated" other
      return false;
    } else if (this.hashCode() != other.hashCode()) {
      // Because AbstractProducers cache their hashCodes, this is a cheap way to figure out that the two instances
      // are not equal (especially when instances are being repeatedly compared, as is often the case)
      return false;
    }

    // if the hashCodes match and the hash function is reasonable, at this stage it's very likely the two instances are
    // equal; we just need to confirm this by checking the classes with reflection and then calling computeEquals()
    Class<? extends AbstractProducer> myClass = this.getClass();
    Class<?> otherClass = other.getClass();

    if (myClass.isAssignableFrom(otherClass)) {
      return computeEqualsUnsafe((S) other); // safe, since other's class is assignable to variables of our class
    } else if (otherClass.isAssignableFrom(myClass)) {
      // this instance is a subclass of other's class; call other's computeEquals method:
      return ((AbstractProducer) other).computeEqualsUnsafe(this);
    } else {
      // neither class derives from the other; assume they are not equal
      return false;
    }
  }

  /**
   * Derived classes may override this method to determine the equality of this instance with another.  Please note
   * that if you override this method, you <strong>must</strong> override {@link #computeHashCode()}, too.
   *
   * Generally, you do not need to override this method.  Use the {@link ValueEquality}
   * or, rarely, the {@link HandleEquality} annotation to decorate your producer instead.
   *
   * <strong>This method is (theoretically) "unsafe" if your implementation's method signature specifies generic type
   * parameters for the <code>other</code> parameter.</strong>.  This is because <code>other</code>'s type
   * parameterization cannot be guaranteed and in theory this could lead to heap pollution or
   * {@link ClassCastException}s depending on your implementation (see further discussion below).
   *
   * You can easily ensure the safety of this method by simply specifying the type of the <code>other</code>
   * parameter to be the <i>concrete</i> type rather than the parameterized type.  For example, the
   * {@link com.linkedin.dagli.generator.Constant} generator uses the signature
   * {@code protected boolean computeEqualsUnsafe(Constant other)} rather than
   * {@code protected boolean computeEqualsUnsafe(Constant<R> other)}.  This prevents your implementation from making
   * a (possibly erroneous) assumption about <code>other</code>'s generic type parameterization.
   *
   * {@link com.linkedin.dagli.placeholder.Placeholder} are considered equal only if they share the same handle
   * (will behavior consistent to that provided by the {@link HandleEquality} annotation.)
   * In general, two producers may be considered equal if:
   * (1) They will always produce the same values given the same inputs to their containing DAG
   * (2) Have either the same class or one's class is a subclass of the other's
   * (3) They are "semantically equivalent", meaning their methods and fields yield equivalent results, except for
   *    {@link #getShortName()}, {@link #getName()}, {@link #toString()} and any exceptions you document on your class
   *    (the idea is that the client should not be "surprised", since Dagli may give the client a Producer that is
   *    equals() to the specific Producer they requested rather than that exact requested instance.)
   *   (3a) Also, if one's class is a superclass of the other's, only the methods/fields defined by the superclass need
   *        to be equivalent (the subclass may have additional methods/fields that the superclass lacks).
   * (4) For transformers, they have the same <strong>set</strong> of inputs (the actual arity and ordering of the lists
   *     of inputs may differ, but every input of one must equals() at least one input of the other).  Typically
   *     transformers will be more restrictive when checking for equality (like checking that the lists of inputs of
   *     both transformers have the same elements in the same order), but it is not uncommon for the inputs to be
   *     commutative (order-invariant), as they might be for a transformer that sums its inputs.
   *
   * Equality testing is what allows Dagli to optimize the DAG to eliminate redundant work.
   *
   * Implementation discussion (why make this method "unsafe"?): the reason that we type <code>other</code> with our
   * type parameter of <code>S</code> rather than the concrete class corresponding to <code>S</code> is that Java
   * provides no way to specify "concreteType(S)" as the type for <code>other</code>.  We could instead require derived
   * classes to pass both their derived concrete class and their derived generic-type-parameterized class as type
   * parameters to this class, but this would obviously be cumbersome and the relationship between the two type
   * parameters (that they correspond to the same concrete class) would be impossible to enforce.  We could also safely
   * type <code>other</code> as some known superclass or interface, such as {@link Producer}, but this leads to more
   * cumbersome implementations (the parameter value must then be cast to the concrete derived class "manually" as the
   * first line in every implementation).  Moreover, it is hard to imagine a scenario where incorrect assumptions about
   * the type parameterization of the derived class will create problems in practice: heap-pollution is possible, but
   * only if this method were modifying state, which would be abnormal for an equality test.
   *
   * Consequently, the compromise we have chosen is to allow this method to make a (theoretically unsafe) assumption
   * about <code>other</code>, but this risk is easily eliminated by simply typing <code>other</code> as
   * the derived concrete type (without generic parameterization) in the implementation's method signature.
   *
   * @param other another producer that is guaranteed to be of this instance's concrete class (or a subclass); not null
   * @return whether this producer and the passed object can be considered functionally equivalent
   */
  protected boolean computeEqualsUnsafe(S other) {
    return getEqualsFunction().apply(this, other);
  }

  /**
   * Derived classes should override this method to calculate the hashCode of the instance if and only if they have
   * overriden the {@link #computeEqualsUnsafe(AbstractProducer)} method.  The hashCode produced must be consistent
   * with this method, such that two instances comparing equal must also have the same hashCode.
   *
   * @return a hashcode for this instance
   */
  protected int computeHashCode() {
    return getHashCodeFunction().apply(this);
  }

  /**
   * Compares this producer's handle with another producer's handle, verifying that they are equal according to
   * "handle equality" (see {@link HandleEquality} for details).  Handle equality is
   * <strong>not</strong> equivalent to comparing the handles returned by {@link #getHandle()} with the
   * {@link ProducerHandle#equals(Object)} method, which is more permissive.
   *
   * @param other the other producer to compare against
   * @return true if the two producers have the same handle and the same class, false otherwise
   */
  protected boolean handleEquality(S other) {
    return this._uuidLeastSignificantBits == ((AbstractProducer<?, ?, ?>) other)._uuidLeastSignificantBits
        && this._uuidMostSignificantBits == ((AbstractProducer<?, ?, ?>) other)._uuidMostSignificantBits
        && this.getClass().equals(other.getClass());
  }

  /**
   * Calculates a hash code for this producer's handle.  This will match the value returned by invoking
   * {@link ProducerHandle#hashCode()} on the handle returned by {@link #getHandle()}, but is more efficient.
   *
   * @return the hash code for this i
   */
  protected int handleHashCode() {
    return Long.hashCode(_uuidLeastSignificantBits) + Long.hashCode(Long.rotateLeft(_uuidMostSignificantBits, 3));
  }

  /**
   * Gets the internal API object for this instance.  The returned value is cached, so this method will only be
   * called once per instance, during construction of AbstractProducer.
   *
   * @implNote this method is called before the derived class's constructor runs; do not use any fields when calculating
   *           the return value.
   *
   * @return an internal API object
   */
  protected abstract I createInternalAPI();

  @Override
  public I internalAPI() {
    return _internalAPI;
  }

  protected abstract class InternalAPI implements ProducerInternalAPI<R, S> {
    @Override
    public Collection<? extends Reducer<? super S>> getGraphReducers() {
      return AbstractProducer.this.getGraphReducers();
    }

    @Override
    public ClassReducerTable getClassReducerTable() {
      return AbstractProducer.this.getClassReducerTable();
    }

    @Override
    public boolean hasAlwaysConstantResult() {
      return AbstractProducer.this.hasAlwaysConstantResult();
    }

    @Override
    public ProducerHandle<S> getHandle() {
      return AbstractProducer.this.getHandle();
    }
  }

  // It's possible this may be dangerous if createInternalAPI() ever did something that actually used fields on children
  // that, at the time of this call, haven't be read yet.  Since the internal API is entirely within the purview of
  // Dagli, however, this should be avoidable.
  private void readObject(ObjectInputStream inputStream) throws IOException, ClassNotFoundException {
    inputStream.defaultReadObject();
    _internalAPI = createInternalAPI();
  }

  // static method to set a random UUID on the specified instance
  private static void setRandomUUID(AbstractProducer t) {
    UUID newUUID = UUID.randomUUID();
    t._uuidLeastSignificantBits = newUUID.getLeastSignificantBits();
    t._uuidMostSignificantBits = newUUID.getMostSignificantBits();
  }

  @Override
  protected S clone() {
    S res = super.clone();
    setRandomUUID(res);
    ((AbstractProducer<?, I, ?>) res)._internalAPI = res.createInternalAPI();
    ((AbstractProducer<?, ?, ?>) res)._hashCode = 0;
    ((AbstractProducer<?, ?, ?>) res)._isConstantResult = 0;
    return res;
  }

  /**
   * Gets the handle for this transformer.  Handles are useful for referring to a {@link Producer} without requiring a
   * reference to the {@link Producer} instance itself.
   *
   * Handles can be serialized and deserialized, and a serialized and deserialized {@link Producer} instance will always
   * maintain the same handle.
   *
   * If two {@link Producer} instances share the same handle (this can happen via deserialization, for example) they
   * will be functionally identical: any fields visible outside the class will have equal values, and any methods
   * visible outside the class will have identical behavior.
   *
   * Conversely, two indistinguishable transformers will not <strong>necessarily</strong> share the same handle.
   *
   * @implNote usually, new transformers are implemented by deriving from an abstract class, such as
   * AbstractPreparedTransformer1; these classes already provide a suitable implementation of getHandle().
   *
   * @return the handle for this transformer
   */
  protected final ProducerHandle<S> getHandle() {
    return new ProducerHandle<S>((Class<S>) this.getClass(), _uuidMostSignificantBits,
        _uuidLeastSignificantBits);
  }

  @Override
  public String toString() {
    return getName();
  }
}
