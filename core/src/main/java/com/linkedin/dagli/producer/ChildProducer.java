package com.linkedin.dagli.producer;

import com.linkedin.dagli.producer.internal.AncestorSpliterator;
import com.linkedin.dagli.producer.internal.ChildProducerInternalAPI;
import com.linkedin.dagli.util.collection.LinkedStack;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;


/**
 * The base interface for child (non-root) producer interfaces.
 *
 * @param <R> the type of result produced by this producer.
 */
public interface ChildProducer<R> extends Producer<R> {
  @Override
  ChildProducerInternalAPI<R, ? extends ChildProducer<R>> internalAPI();

  /**
   * Casts a producer to an effective "supertype" interface.  The semantics of the producer guarantee that the returned
   * type is valid for the instance.
   *
   * Note that although this and other {@code cast(...)} methods are safe, this safety extends only to the
   * interfaces for which they are implemented.  The covariance and contravariance relationships existing for these
   * interfaces do not necessarily hold for their derived classes.  For example, a {@code PreparedTransformer<String>}
   * is also a {@code PreparedTransformer<Object>}, but a {@code MyTransformer<String>} cannot necessarily be safely
   * treated as a {@code MyTransformer<Object>}.
   *
   * @param childProducer the child producer to cast
   * @param <R> the type of result of the returned child producer
   * @return the passed child producer, typed to a new "supertype" of the original
   */
  @SuppressWarnings("unchecked")
  static <R> ChildProducer<R> cast(ChildProducer<? extends R> childProducer) {
    return (ChildProducer<R>) childProducer;
  }

  /**
   * Gets the list of parents of a (child) producer.
   *
   * Note that child producers are, from the perspective of client code, free to determine their parents arbitrarily,
   * and these parents may not be the same as the input producers provided via their public API.  The type and number of
   * a producer's parents should be regarded as an implementation detail and not relied upon for correctness of your
   * code (unless you also own the producer's implementation and can guarantee that it accords with your expectations).
   *
   * @param producer the producer whose parents are sought
   * @return a list of parents of the provided producer; this list must not be modified
   */
  static List<? extends Producer<?>> getParents(ChildProducer<?> producer) {
    return producer.internalAPI().getInputList();
  }

  /**
   * Streams the ancestors of a producer as visited by a breadth-first search (nearer ancestors are always visited
   * before more distal ancestors; e.g. parents before grandparents).  If multiple paths from a producer to an
   * ancestor exist, only one of the shortest paths will be enumerated; ancestors are disambiguated by
   * reference-equality, so it is possible for two enumerated ancestors to be
   * {@link java.util.Objects#equals(Object, Object)} with each other.
   *
   * The enumerated {@link LinkedStack}s contain the ancestor ({@link LinkedStack#peek()} as well as the path
   * through its descendents ending with the provided {@code producer} (the preceding elements in the stack).
   *
   * @param producer the producer whose ancestors are to be streamed
   * @param maxDepth how many generations of parents should be visited; immediate parents == 1, up to grandparents
   *                 == 2, great-grandparents == 3, etc.  Pass {@link Integer#MAX_VALUE} to visit all ancestors.
   * @return a stream containing a shortest path from the producer to each ancestor
   */
  static Stream<LinkedStack<Producer<?>>> ancestors(ChildProducer<?> producer, int maxDepth) {
    return StreamSupport.stream(new AncestorSpliterator(producer, maxDepth, ChildProducer::getParents), false);
  }
}
