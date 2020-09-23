package com.linkedin.dagli.reducer;

import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.producer.RootProducer;
import com.linkedin.dagli.transformer.PreparableTransformer;
import com.linkedin.dagli.transformer.PreparedTransformer;
import com.linkedin.dagli.util.cloneable.AbstractCloneable;
import com.linkedin.dagli.util.collection.LinkedNode;
import com.linkedin.dagli.view.TransformerView;
import it.unimi.dsi.fastutil.objects.ReferenceSet;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;


/**
 * Interface for all types of graph reducers, which optimize the graph by simplifying or rewriting it.
 *
 * The "reduction" nomenclature is used to avoid confusion with the loss optimization that may occur while a graph is
 * being prepared/trained.  Reduction occurs before the graph is executed, simplifying the graph by rewriting it (such
 * as by removing or replacing its nodes).  The resulting graph will be more efficient but maintain the same inputs and
 * outputs.
 *
 * The reduction of a DAG proceeds by repeatedly applying the available {@link Reducer}s until the graph ceases to
 * change.  No particular ordering of the application of the reducers is strictly guaranteed, but generally reductions
 * are approximately topographically ordered (i.e. the roots are reduced first) in each of one or more "passes" through
 * the graph.
 *
 * <strong>For efficiency, a "working graph" is used during the reduction of a DAG that keeps track of the producers
 * parents and children independently, such that the parents (inputs) reported by producer instances themselves are
 * irrelevant.</strong>  This means that special care must be taken when a reducer is traversing or modifying the graph:
 * (1) Methods on the {@link Context} that accept a {@link Producer} are sensitive to reference-equality; it is possible
 *     for two producers that are {@link java.util.Objects#equals(Object, Object)} to be distinct with different parents
 *     and children in the working graph.
 * (2) To get a copy of a producer with the "correct" parents (as reflected by the working graph), use
 *     {@link Context#withCurrentParents(Producer)}.
 * (3) To traverse the ancestors of a producer, use the methods provided by {@link Context}, such as
 *     {@link Context#getParents(Producer)}.
 * (4) Make sure you read and understand the description of the {@code replace(...)} methods like
 *     {@link Context#replace(PreparedTransformer, Producer)}.  If the replacement (or one of its ancestors) is not
 *     already reference-equal to an existing producer in the working graph, it will be added as a new node in the
 *     graph.
 * (5) Any method that relies on the correctness of a {@link Producer}'s ancestors will yield incorrect results relative
 *     to the conceptual state of the producer as currently situated in the working graph.  This includes
 *     {@link Producer#hasConstantResult()}, {@link Producer#equals(Object)}, etc.
 *
 * Graph reducers should be immutable (i.e. their externally visible methods and fields should remain consistent once
 * they are constructed).  They may be invoked in multiple threads simultaneously.  They do not need to be serializable.
 *
 * @param <T> the type of the producer being reduced (this type will usually--but does not strictly need to--derived
 *            from {@link Producer}; however, the actual instance passed to {@link #reduce(Object, Context)} will always
 *            be a producer, and hence derive from {@link Producer})
 */
@FunctionalInterface
public interface Reducer<T> {
  /**
   * Gets the importance level of the reducer (used to decide which reducers should be retained when the total cost of
   * reducing the DAG needs to be limited).  The default implementation returns {@link Level#NORMAL}, which is
   * appropriate for the vast majority of reducers.
   *
   * @return the importance level of this reducer
   */
  default Level getLevel() {
    return Level.NORMAL;
  }

  /**
   * Applies this reducer to the given {@code target}, possibly replacing it or one its ancestors with a different
   * {@link Producer} in the DAG.
   *
   * {@code target}'s parents (its inputs) <strong>may not be correct</strong>.  This is because it would be too
   * expensive to keep producers' parents up-to-date while the DAG is being reduced, and the graph structure is instead
   * stored separately.
   *
   * @param target the target producer of this reduction (guaranteed to derive from {@link Producer}); only the target
   *               and its ancestors may be accessed and replaced via the provided {@code context}
   * @param context the context of the {@code target}, providing methods for inspecting and replacing the {@code target}
   *                and its ancestors
   */
  void reduce(T target, Context context);

  /**
   * The context of an ongoing DAG reduction relative to the "target" producer that is currently being reduced.
   *
   * {@link Context} instances are valid only for the duration of the {@link Reducer#reduce(Object, Context)} call
   * and should not be persisted beyond this.
   *
   * If a {@link Context} method throws an exception, the working graph may be left in an invalid state.  Reducers must
   * consequently not suppress these exceptions in such a way that reduction is allowed to continue; they should be
   * allowed to "bubble".  In particular, it is not correct to wrap, e.g. a {@code reduce(...)} call in a
   * {code try...catch} block to attempt a potentially-invalid replacement that may throw an exception.
   */
  interface Context {
    /**
     * @return the minimum importance of reductions that will be used when reducing the DAG; this will be constant
     *         throughout the reduction of the DAG
     */
    Level getMinimumImportance();

    /**
     * Checks whether or not the current DAG reduction is guaranteed to run exhaustively, until no reducers result in
     * further changes to the DAG.
     *
     * This is needed when deciding whether a reduction on a producer that is dependent on the execution of other
     * reductions can safely proceed (if the return value is false, these other reductions are not guaranteed to
     * execute).
     *
     * Note that, even if the reduction proceeds to completion, it may not use all possible reductions; these are
     * limited by the importance floor used when reducing the DAG, as reported by {@link #getMinimumImportance()}.
     *
     * @return whether or not the reduction of the DAG is guaranteed to run exhaustively until completion
     */
    boolean isCompleteGraphReduction();

    /**
     * @return true iff the DAG being reduced was prepared (containing no preparable transformers) when reduction begin;
     *         when this is the case, preparable transformers may not be introduced to the graph via calls to
     *         {@code replace(...)}.
     */
    boolean isPreparedDAG();

    /**
     * Checks whether the given preparable transformer has {@link TransformerView} children.  This method will trivially
     * return false if the provided {@code producer} is not a {@link PreparableTransformer}, as only preparable
     * transformers may have dependent views
     *
     * @param producer the {@link Producer} to examine
     * @return true if the provided transformer is being viewed by one or more {@link TransformerView}s, false otherwise
     */
    boolean isViewed(Producer<?> producer);

    /**
     * Checks to see if the current DAG reduction will apply a particular reducer to all instances of a given class for
     * the current target producer and all its graph descendants.  Only reducers provided by {@link ClassReducerTable}s
     * are queried; reducers from other sources are considered to apply to specific <i>instances</i> and are thus
     * excluded here).
     *
     * @param clazz the class whose set of available reducers is to be examined
     * @param reducer the reducer whose existence is being queried
     * @param <T> the class whose set of available reducers is to be examined
     * @return true if the given reducer will be applied to all instances of the given class for the target producer
     *         currently being reduced and all graph descendants; false otherwise
     */
    <T> boolean hasReducer(Class<T> clazz, Reducer<? super T> reducer);

    /**
     * Replaces the target producer currently being reduced, or one of its ancestors, with another producer of the
     * exact same type.  The replacement may itself be the target or any of its ancestors, or a new
     * producer.
     *
     * This method requires that the existing and replacement producers derive from {@link AbstractCloneable}; this is
     * done to help ensure that both producers have exactly the same type, but {@link AbstractCloneable} has no special
     * significance beyond being a ubiquitous base class whose generic parameter is the concrete (non-wildcard) type of
     * its extending class. Technically, it is possible to break the same-type constraint by, e.g. declaring a class as
     * {@code MyType<T> extends AbstractCloneable<MyType<?>>}; however, this would be a bug as it violates the
     * specification for {@code AbstractCloneable}.
     *
     * The replacement must not be or have as an ancestor a {@link com.linkedin.dagli.placeholder.Placeholder} that is
     * not an existing ancestor of the target producer (i.e. new placeholders may not be introduced into the graph).
     * Additionally, replacing a {@link com.linkedin.dagli.placeholder.Placeholder} with something that is not a
     * {@link com.linkedin.dagli.placeholder.Placeholder} will not affect the arity of the DAG (a corresponding input
     * will still be required as one of the inputs to the DAG, it just won't be used for anything).
     *
     * <strong>After replacement, the replaced producer will be disconnected from the graph.</strong>  Attempting to
     * inspect or manipulate the replaced the replaced producer via this {@link Context} will result in undefined
     * behavior and should be avoided.
     *
     * @param existing the producer being replaced
     * @param replacement a replacement of the producer
     * @param <T> the type of producer being replaced; this method is guaranteed to be safe if T is a concrete type
     *            (with no wildcards)
     * @throws IllegalArgumentException if {@code replacement} has an ancestor which is a placeholder not already in the
     *                                  working graph
     */
    <T extends AbstractCloneable<T> & Producer<?>> void replaceWithSameClass(T existing, T replacement);

    /**
     * Replaces the target producer currently being reduced, or one of its ancestors, with another producer that
     * produces the same type of result.  The replacement may itself be the target or any of its ancestors, or a new
     * producer.
     *
     * The location of the replacement within the current DAG reduction's working graph is determined by the parents
     * reported by that instance, unless that <strong>exact</strong> producer instance, as determined by reference
     * equality, is already in the working graph (in which case its parents will remain unchanged).  If you wish to
     * replace a producer with a modified copy of a producer that already exists in the DAG, use
     * {@link #withCurrentParents(Producer)} to get a copy of the existing producer with up-to-date parents and then
     * create modified copies (e.g. through {@code producer.withSomeProperty(...)}) to use as replacements.
     *
     * The replacement must not be or have as an ancestor a {@link com.linkedin.dagli.placeholder.Placeholder} that is
     * not an existing ancestor of the target producer (i.e. new placeholders may not be introduced into the graph).
     * Additionally, replacing a {@link com.linkedin.dagli.placeholder.Placeholder} with something that is not a
     * {@link com.linkedin.dagli.placeholder.Placeholder} will not affect the arity of the DAG (a corresponding input
     * will still be required as one of the inputs to the DAG, it just won't be used for anything).
     *
     * <strong>After replacement, the replaced producer will be disconnected from the graph.</strong>  Attempting to
     * inspect or manipulate the replaced the replaced producer via this {@link Context} will result in undefined
     * behavior and should be avoided.
     *
     * @param existing the producer being replaced
     * @param replacement a replacement of the producer
     * @param <R> the type of result of the producers
     * @throws IllegalArgumentException if the replacement is or has an ancestor that is a {@link PreparableTransformer}
     *                                  when {@link #isPreparedDAG()} is {@code true}; replacements may not make a
     *                                  prepared DAG preparable.
     * @throws IllegalArgumentException if {@code replacement} is a placeholder not already in the working graph and
     *                                  {@code existing} is not itself a placeholder, or {@code replacement} has an
     *                                  ancestor which is a placeholder not already in the working graph
     */
    <R> void replace(RootProducer<R> existing, Producer<? extends R> replacement);

    /**
     * Replaces the target producer currently being reduced, or one of its ancestors, with another producer that
     * produces the same type of result.  The replacement may itself be the target or any of its ancestors, or a new
     * producer.
     *
     * The location of the replacement within the current DAG reduction's working graph is determined by the parents
     * reported by that instance, unless that <strong>exact</strong> producer instance, as determined by reference
     * equality, is already in the working graph (in which case its parents will remain unchanged).  If you wish to
     * replace a producer with a modified copy of a producer that already exists in the DAG, use
     * {@link #withCurrentParents(Producer)} to get a copy of the existing producer with up-to-date parents and then
     * create modified copies (e.g. through {@code producer.withSomeProperty(...)}) to use as replacements.
     *
     * The replacement must not be or have as an ancestor a {@link com.linkedin.dagli.placeholder.Placeholder} that is
     * not an existing ancestor of the target producer (i.e. new placeholders may not be introduced into the graph).
     *
     * <strong>After replacement, the replaced producer will be disconnected from the graph, and some or all of its
     * ancestors may not longer remain ancestors of the target producer currently being reduced</strong> (it is further
     * possible that these ancestors may be disconnected from the DAG entirely).  Attempting to inspect or manipulate
     * producers that are no longer ancestors of the current target producer via this {@link Context} will result in
     * undefined behavior and should be avoided.
     *
     * @param existing the producer being replaced
     * @param replacement a replacement of the producer
     * @param <R> the type of result of the producers
     * @throws IllegalArgumentException if {@code replacement} is a placeholder not already in the working graph, or
     *                                  {@code replacement} has an ancestor which is a placeholder not already in the
     *                                  working graph
     */
    <R> void replace(TransformerView<R, ?> existing, Producer<? extends R> replacement);

    /**
     * Replaces the target producer currently being reduced, or one of its ancestors, with another producer that
     * produces the same type of result.  The replacement may itself be the target or any of its ancestors, or a new
     * producer.
     *
     * The location of the replacement within the current DAG reduction's working graph is determined by the parents
     * reported by that instance, unless that <strong>exact</strong> producer instance, as determined by reference
     * equality, is already in the working graph (in which case its parents will remain unchanged).  If you wish to
     * replace a producer with a modified copy of a producer that already exists in the DAG, use
     * {@link #withCurrentParents(Producer)} to get a copy of the existing producer with up-to-date parents and then
     * create modified copies (e.g. through {@code producer.withSomeProperty(...)}) to use as replacements.
     *
     * The replacement must not be or have as an ancestor a {@link com.linkedin.dagli.placeholder.Placeholder} that is
     * not an existing ancestor of the target producer (i.e. new placeholders may not be introduced into the graph).
     *
     * <strong>After replacement, the replaced producer will be disconnected from the graph, and some or all of its
     * ancestors may not longer remain ancestors of the target producer currently being reduced</strong> (it is further
     * possible that these ancestors may be disconnected from the DAG entirely).  Attempting to inspect or manipulate
     * producers that are no longer ancestors of the current target producer via this {@link Context} will result in
     * undefined behavior and should be avoided.
     *
     * @param existing the producer being replaced
     * @param replacement a replacement of the producer
     * @param <R> the type of result of the producers
     * @throws IllegalArgumentException if the replacement is or has an ancestor that is a {@link PreparableTransformer}
     *                                  when {@link #isPreparedDAG()} is {@code true}; replacements may not make a
     *                                  prepared DAG preparable.
     * @throws IllegalArgumentException if {@code replacement} is a placeholder not already in the working graph, or
     *                                  {@code replacement} has an ancestor which is a placeholder not already in the
     *                                  working graph
     */
    <R> void replace(PreparedTransformer<R> existing, Producer<? extends R> replacement);

    /**
     * Replaces the target producer currently being reduced, or one of its ancestors, with another producer that
     * produces the same type of result.  The replacement may itself be the target or any of its ancestors, or a new
     * producer.
     *
     * The location of the replacement within the current DAG reduction's working graph is determined by the parents
     * reported by that instance, unless that <strong>exact</strong> producer instance, as determined by reference
     * equality, is already in the working graph (in which case its parents will remain unchanged).  If you wish to
     * replace a producer with a modified copy of a producer that already exists in the DAG, use
     * {@link #withCurrentParents(Producer)} to get a copy of the existing producer with up-to-date parents and then
     * create modified copies (e.g. through {@code producer.withSomeProperty(...)}) to use as replacements.
     *
     * The replacement must not have a {@link com.linkedin.dagli.placeholder.Placeholder} ancestor that is not also an
     * ancestor of the target producer (i.e. new placeholders may not be introduced into the graph).
     *
     * <strong>After replacement, the replaced producer will be disconnected from the graph, and some or all of its
     * ancestors may not longer remain ancestors of the target producer currently being reduced</strong> (it is further
     * possible that these ancestors may be disconnected from the DAG entirely).  Attempting to inspect or manipulate
     * producers that are no longer ancestors of the current target producer via this {@link Context} will result in
     * undefined behavior and should be avoided.
     *
     * @param existing the producer being replaced
     * @param replacement a replacement of the producer
     * @param <R> the type of result of the producers
     * @throws IllegalArgumentException if {@code replacement} is a placeholder not already in the working graph, or
     *                                  {@code replacement} has an ancestor which is a placeholder not already in the
     *                                  working graph
     */
    <R, N extends PreparedTransformer<? extends R>> void replace(PreparableTransformer<R, N> existing,
        PreparableTransformer<? extends R, ? extends N> replacement);

    /**
     * Replaces the target producer currently being reduced, or one of its ancestors, with another producer that
     * produces the same type of result.  The replacement may be the target, any of its ancestors, or a new producer.
     * The replaced producer must not have any dependent {@link TransformerView}s; this is trivially true for anything
     * other than {@link PreparableTransformer}s, but this method accepts all types of producers for convenience.
     *
     * The location of the replacement within the current DAG reduction's working graph is determined by the parents
     * reported by that instance, unless that <strong>exact</strong> producer instance, as determined by reference
     * equality, is already in the working graph (in which case its parents will remain unchanged).  If you wish to
     * replace a producer with a modified copy of a producer that already exists in the DAG, use
     * {@link #withCurrentParents(Producer)} to get a copy of the existing producer with up-to-date parents and then
     * create modified copies (e.g. through {@code producer.withSomeProperty(...)}) to use as replacements.
     *
     * The replacement must not be or have as an ancestor a {@link com.linkedin.dagli.placeholder.Placeholder} that is
     * not an existing ancestor of the target producer (i.e. new placeholders may not be introduced into the graph).
     * Additionally, the preparable transformer <strong>must</strong> have no {@link TransformerView} children
     * ({@link #isViewed(Producer)} must return false).
     *
     * <strong>After replacement, the replaced producer will be disconnected from the graph, and some or all of its
     * ancestors may not longer remain ancestors of the target producer currently being reduced</strong> (it is further
     * possible that these ancestors may be disconnected from the DAG entirely).  Attempting to inspect or manipulate
     * producers that are no longer ancestors of the current target producer via this {@link Context} will result in
     * undefined behavior and should be avoided.
     *
     * @param existing the producer being replaced
     * @param replacement a replacement of the producer
     * @param <R> the type of result of the producers
     * @throws IllegalArgumentException if {@code isViewed(existing)} returns true (only possible if {@code existing}
     *                                  is a {@link PreparableTransformer}
     * @throws IllegalArgumentException if {@code replacement} is a placeholder not already in the working graph and
     *                                  {@code existing} is not itself a placeholder, or {@code replacement} has an
     *                                  ancestor which is a placeholder not already in the working graph
     */
    <R> void replaceUnviewed(Producer<R> existing, Producer<? extends R> replacement);

    /**
     * Replaces the target producer currently being reduced, or one of its ancestors, with another producer that
     * produces the same type of result.  The replacement may be the target, any of its ancestors, or a new producer.
     * If the replaced producer is a {@link PreparableTransformer} with a dependent {@link TransformerView}, the
     * replacement will not occur and this method will return false; otherwise, the replacement will proceed and this
     * method will return true (assuming the replacement producer is otherwise valid).
     *
     * The location of the replacement within the current DAG reduction's working graph is determined by the parents
     * reported by that instance, unless that <strong>exact</strong> producer instance, as determined by reference
     * equality, is already in the working graph (in which case its parents will remain unchanged).  If you wish to
     * replace a producer with a modified copy of a producer that already exists in the DAG, use
     * {@link #withCurrentParents(Producer)} to get a copy of the existing producer with up-to-date parents and then
     * create modified copies (e.g. through {@code producer.withSomeProperty(...)}) to use as replacements.
     *
     * The replacement must not be or have as an ancestor a {@link com.linkedin.dagli.placeholder.Placeholder} that is
     * not an existing ancestor of the target producer (i.e. new placeholders may not be introduced into the graph).
     * Additionally, replacing a {@link com.linkedin.dagli.placeholder.Placeholder} with something that is not a
     * {@link com.linkedin.dagli.placeholder.Placeholder} will not affect the arity of the DAG (a corresponding input
     * will still be required as one of the inputs to the DAG, it just won't be used for anything).
     *
     * <strong>After replacement, the replaced producer will be disconnected from the graph.</strong>  Attempting to
     * inspect or manipulate the replaced the replaced producer via this {@link Context} will result in undefined
     * behavior and should be avoided.
     *
     * @param existing the producer being replaced
     * @param replacementSupplier a supplier providing a replacement for the producer; will not be called if this method
     *                            returns false
     * @param <R> the type of result of the producers
     * @return true if the replacement succeeded, false if {@code existing} is a {@link PreparableTransformer} with a
     *         dependent {@link TransformerView} and the DAG is unchanged
     * @throws IllegalArgumentException if {@code replacement} is a placeholder not already in the working graph and
     *                                  {@code existing} is not itself a placeholder, or {@code replacement} has an
     *                                  ancestor which is a placeholder not already in the working graph
     */
    <R> boolean tryReplaceUnviewed(Producer<R> existing, Supplier<Producer<? extends R>> replacementSupplier);

    /**
     * During reduction, the DAG is conceptually independent of the producer instances' parents and instead stored in a
     * <i>working graph</i> maintained by the DAG reducer.  This method returns a copy of the provided {@code producer}
     * that has up-to-date parents (according to the working graph); this may be the {@code producer} itself if the
     * parents stored within {@code producer} already exactly (reference-equals) match those in the working graph.
     *
     * Root producers ({@link com.linkedin.dagli.placeholder.Placeholder}s and
     * {@link com.linkedin.dagli.generator.Generator}s) have no parents and are thus always "up-to-date"; passing
     * a root producer thus trivially returns that same producer.
     *
     * Unless the provided producer was itself returned, the returned producer will be a new instance that will not
     * (yet) be part of the working graph.
     *
     * This method may be used if you wish to access the inputs to a producer "in situ" using accessors provided by
     * that producer (e.g. {@code MyProducer::getLabelInput()}), but note that the <i>grandparents</i> of the returned
     * producer (the parents reported by the returned producer's parents) may still not be up-to-date; if your intent
     * is to traverse the working graph, use {@link #getParents(Producer)} instead (it's also far more efficient).
     *
     * This method should always be used to get the latest version of a producer already in the working graph if it (or
     * a modified copy) is to be used replace another producer.  This is because the {@code replace(...)} method will
     * use the parents reported by the replacement producer to determine its edges in the updated working graph.
     *
     * @param producer a producer for which a copy should be made with its current parents in the working graph
     * @param <T> the type of the producer
     * @return a copy of the provided producer whose parents (as reflected by the instance) will match the parents as
     *         recorded in the DAG reduction's working graph
     */
    <T extends Producer<?>> T withCurrentParents(T producer);

    /**
     * Gets the current list of parents (inputs) of a producer in the current DAG reduction's working graph.  This may
     * include duplicates if the producer accepts the same input at more than one position in its input list.  For
     * producers that are not {@link com.linkedin.dagli.producer.ChildProducer}s and have no parents, the returned list
     * will be empty.
     *
     * This method may be used to "walk the graph" during reduction; the parents as stored in the producer instances
     * themselves may be inaccurate and not reflect the current state of the working graph.
     *
     * Note that producers are free to determine their parents arbitrarily, and these parents may not be the same as the
     * input producers provided via their public API.  The type and number of a producer's parents should be regarded as
     * an implementation detail and not relied upon for correctness of your reducer, unless you also own the producer's
     * implementation and can guarantee that it accords with your expectations).
     *
     * @param producer the producer whose parents are sought
     * @return a list of parents of the provided producer; this list must not be modified
     */
    List<? extends Producer<?>> getParents(Producer<?> producer);

    /**
     * Gets the parents of a given producer that are (or derive from) the given class or interface.
     *
     * @param producer the producer whose parents are sought; must be the producer currently being reduced or one of its
     *                 ancestors
     * @param producerClass the class or interface of the returned subset of parents
     * @param <T> the type of parent sought
     * @return a set of the subset of parents of the given producer with the given class (or interface); this set must
     *         not be modified
     */
    <T> ReferenceSet<T> getParentsByClass(Producer<?> producer, Class<T> producerClass);

    /**
     * Gets a parent of a given producer that is (or derives from) the given class or interface.
     *
     * If there are multiple matching parents, which parent is returned is arbitrary.  If there are no matching parents,
     * {@code null} is returned.
     *
     * @param producer the producer whose parents are sought; must be the producer currently being reduced or one of its
     *                 ancestors
     * @param producerClass the class or interface of the returned subset of parents
     * @param <T> the type of parent sought
     * @return a parent of the given producer with or deriving from the given class (or interface), or {@code null} if
     *         {@code producer} has no such parent
     */
    default <T> T getParentByClass(Producer<?> producer, Class<T> producerClass) {
      ReferenceSet<T> set = getParentsByClass(producer, producerClass);
      return set.isEmpty() ? null : set.iterator().next();
    }

    /**
     * Gets the ancestors of a given producer that are (or derive from) the given class or interface.
     *
     * @param producer the producer whose ancestors are sought; must be the producer currently being reduced or one of
     *                 its ancestors
     * @param producerClass the class or interface of the returned subset of ancestors
     * @param maxDepth how many generations of parents should be searched (immediate parents == 1, up to grandparents
     *                 == 2, great-grandparents == 3, etc.
     * @param <T> the type of ancestor sought
     * @return a set of the subset of ancestors of the given producer with the given class (or interface); this list
     *         must not be modified
     */
    <T> ReferenceSet<T> getAncestorsByClass(Producer<?> producer, Class<T> producerClass, int maxDepth);

    /**
     * Streams the ancestors of a node as visited by a breadth-first search (nearer ancestors are always visited
     * before more distal ancestors; e.g. parents before grandparents).  If multiple paths from a producer to an
     * ancestor exist, only one of the shortest paths will be enumerated.
     *
     * The enumerated {@link LinkedNode}'s contain the ancestor ({@link LinkedNode#getItem()} as well as the path
     * through its descendents ending with the provided producer (via {@link LinkedNode#getPreviousNode()}).
     *
     * @param producer the producer whose ancestors are to be streamed
     * @param maxDepth how many generations of parents should be visited; immediate parents == 1, up to grandparents
     *                 == 2, great-grandparents == 3, etc.
     * @return a stream containing the shortest path from the producer to each ancestor
     */
    Stream<LinkedNode<Producer<?>>> ancestors(Producer<?> producer, int maxDepth);
  }

  /**
   * Denotes the "importance" of a reducer (a subjective function of its computational cost relative to its benefit).
   *
   * Level may be used to limit the total cost of reducing a DAG by restricting the set of reducers used to only
   * those of sufficient value compared to their cost.
   *
   * The natural ordering of {@link Level}s correspond to their increasing degree of importance (alternatively,
   * increasingly better cost/benefit).
   */
  enum Level {
    /**
     * The reducer is expensive (e.g. it changes distant ancestors of the target node, which forces a re-examination
     * of a large part of the graph) and provides very limited improvement to the efficiency of the graph.
     */
    EXPENSIVE,

    /**
     * The reducer is not especially expensive or, if it is, provides commensurate benefit to the efficiency of the
     * graph.  This importance level is appropriate for the vast majority of reducers.
     */
    NORMAL,

    /**
     * The reducer provides critical benefit to the efficiency of the graph.  The graph may be impractical to execute
     * if the reducer is not employed.
     */
    ESSENTIAL;

    /**
     * Convenience method that wraps a reducer (if necessary) to give it this {@link Level}.
     *
     * @param reducer the reducer to wrap
     * @param <T> the type of thing the reducer targets
     * @return the provided {@code reducer} if it is already this level, otherwise a new wrapper that has this level
     */
    public <T> Reducer<T> with(Reducer<T> reducer) {
      if (reducer.getLevel() == this) {
        return reducer;
      } else {
        return new Reducer<T>() {
          @Override
          public void reduce(T target, Context context) {
            reducer.reduce(target, context);
          }

          @Override
          public Level getLevel() {
            return Level.this;
          }
        };
      }
    }

    /**
     * Compares two levels.  A {@code null} level is later in the ordering than all non-{@code null} levels.  The
     * ordering (among non-null levels) corresponds with the importance (or, equivalently, the cost-benefit) of reducers
     * at each level.
     *
     * @param first the first {@link Level} to compare, may be null
     * @param second the second {@link Level} to compare, may be null
     * @return a value < 0 if the first level is ordered before the second, 0 if they are equal, a value > 0 if the
     *         first level is ordered after the second.
     */
    public static int compare(Level first, Level second) {
      if (first == second) {
        return 0;
      } else if (first == null) {
        return 1; // nulls are always higher/later in comparison order
      } else if (second == null) {
        return -1;
      } else {
        return first.compareTo(second);
      }
    }
  }
}
