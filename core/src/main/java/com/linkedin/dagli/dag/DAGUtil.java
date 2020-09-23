package com.linkedin.dagli.dag;

import com.linkedin.dagli.placeholder.Placeholder;
import com.linkedin.dagli.producer.ChildProducer;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.producer.RootProducer;
import com.linkedin.dagli.util.invariant.Arguments;
import com.linkedin.dagli.util.collection.Iterables;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;


/**
 * Utility methods for DAGs used internally by Dagli.
 */
class DAGUtil {
  private DAGUtil() { }

  /**
   * Creates a prepared DAG.
   *
   * @param placeholders the placeholders of the DAG
   * @param outputs the outputs (outputs) of the DAG
   * @param <R> the type of result returned by a DAG (e.g. a Tuple2<String, Integer> for a DAG that returns two outputs,
   *            a String and an Integer.)
   * @return a prepared DAG
   */
  static <R> PreparedDAGTransformer<R, ?> createPreparedDAG(List<? extends Placeholder<?>> placeholders,
      List<? extends Producer<?>> outputs) {
    return DAGMakerUtil.makePreparedDAGTransformer(createDAGStructure(placeholders, outputs));
  }

  /**
   * Creates a preparable DAG.
   *
   * @param placeholders the placeholders of the DAG
   * @param outputs the outputs (outputs) of the DAG
   * @param <R> the type of result returned by a DAG (e.g. a Tuple2<String, Integer> for a DAG that returns two outputs,
   *            a String and an Integer.)
   * @return a preparable DAG
   */
  static <R> PreparableDAGTransformer<R, ?, ?> createPreparableDAG(List<? extends Placeholder<?>> placeholders, List<? extends Producer<?>> outputs) {
    return DAGMakerUtil.makePreparableDAGTransformer(createDAGStructure(placeholders, outputs));
  }

  /**
   * Creates a DAG structure.
   *
   * @param placeholders the placeholders of the DAG
   * @param outputs the outputs (outputs) of the DAG
   * @param <R> the type of result returned by a DAG (e.g. a Tuple2<String, Integer> for a DAG that returns two outputs,
   *            a String and an Integer.)
   * @return a {@link DAGStructure} instance that represents the DAG.
   */
  static <R> DAGStructure<R> createDAGStructure(List<? extends Placeholder<?>> placeholders,
      List<? extends Producer<?>> outputs) {
    DeduplicatedDAG deduplicatedDAG = new DeduplicatedDAG(placeholders, outputs);

    return new DAGStructure<R>(deduplicatedDAG._placeholders, deduplicatedDAG._outputs, deduplicatedDAG._childrenMap);
  }

  /**
   * Creates a producer-to-producer map; this method is used by Dagli to map inputs to corresponding placeholders in
   * the {@link PartialDAG} class.
   *
   * @param producers an alternating list of the form "key, value, key, value, key, value, etc."
   * @return a map from each "key" {@link Producer} to its "value" {@link Producer}
   */
  static IdentityHashMap<Producer<?>, Producer<?>> createInputMap(Producer<?>... producers) {
    Arguments.check(producers.length % 2 == 0, "There must be an even number of inputs");
    IdentityHashMap<Producer<?>, Producer<?>> res = new IdentityHashMap<>(producers.length / 2);
    for (int i = 0; i < producers.length; i += 2) {
      if (res.put(producers[i], producers[i + 1]) != null) {
        throw new IllegalArgumentException("A single input is provided multiple times");
      }
    }
    return res;
  }

  /**
   * Replaces ("remaps") an "child" {@link Producer} node and its ancestors according to an original-to-replacement map.
   * The child and any ancestor of the child that is a key in this map will be replaced with its corresponding value.
   *
   * This is useful for, e.g. creating subgraphs of an existing DAG by replacing some of the nodes with {@link Placeholder}s,
   * making them the new roots of the DAG (and excluding the old node and any of its ancestors that are not also
   * ancestors of the surviving nodes).
   *
   * @param child the node which (along with its ancestors) will be remapped
   * @param mapping the mapping of the original producers (keys) to their replacements (values).  Entries for producers
   *                that don't appear in the subgraph of the child and its ancestors have no effect.
   * @param <R> the type of result produced by the child
   * @return the original child, if no nodes were actually replaced/remapped, or a copy of the child whose ancestors
   *         have been remapped.
   */
  @SuppressWarnings("unchecked") // remappedInputs(...) will return result of the same type as the passed producer
  static <R, T extends Producer<R>> T replaceInputs(T child, Map<Producer<?>, Producer<?>> mapping) {
    // first, try to find the remapped child from the existing mappings of producers that have already had their
    // inputs replaced ("remapped"):
    Producer<?> remamppedOutput = mapping.getOrDefault(child, null);
    if (remamppedOutput != null) {
      // the child is already remapped; just return it
      return (T) remamppedOutput;
    }

    // things without inputs cannot have their inputs remapped, obviously--just return the original
    if (!(child instanceof ChildProducer)) {
      return child;
    }

    return (T) remappedInputs((ChildProducer<R>) child, producer -> replaceInputs(producer, mapping));
  }

  /**
   * Remaps the inputs to a given {@link ChildProducer} according to some mapping function.  If the mapping function
   * does not change any of the inputs (as determined by reference equality) then the original {@link ChildProducer}
   * is returned; otherwise, a copy of the original with the new, remapped inputs is created and returned.
   *
   * @param target the {@link ChildProducer} whose inputs should be remapped
   * @param mapper a function that maps each original input to a new, remapped input
   * @param <R> the type of value produced by target
   * @return if no inputs are different after remapping, target; otherwise, a copy of target but with remapped inputs
   */
  @SuppressWarnings("unchecked") // withInputsUnsafe(...) guaranteed to return same type as original
  static <R, T extends ChildProducer<R>> T remappedInputs(T target, UnaryOperator<Producer<?>> mapper) {
    List<? extends Producer<?>> originalInputs = target.internalAPI().getInputList();
    List<Producer<?>> remappedInputs = originalInputs.stream().map(mapper).collect(Collectors.toList());

    // if the original list doesn't contain null, the remapped list also should not (in fact, neither list should ever
    // contain null, but that's a more general invariant than we want to check here!)
    assert originalInputs.contains(null) || !remappedInputs.contains(null);

    // return a new copy of the target with remapped inputs only if it is necessary (i.e. only if the remapped inputs
    // are different)
    return Iterables.elementsAreReferenceEqual(originalInputs, remappedInputs) ? target
        : (T) target.internalAPI().withInputsUnsafe(remappedInputs);
  }

  /**
   * Creates an {@link IdentityHashMap} (distinguishing keys by reference equality rather than equals()) mapping from
   * the {@link ChildProducer}s in a provided collection of {@link Producer}s to "identity hash sets" ({@link Set}s
   * where also distinguished by reference equality rather than equals()) of each {@link ChildProducer}'s inputs.
   *
   * @param nodes a list of {@link Producer}s from which to construct the map
   * @return a map of child producers to their inputs
   */
  static IdentityHashMap<ChildProducer<?>, Set<Producer<?>>> producerToInputSetMap(Collection<Producer<?>> nodes) {
    IdentityHashMap<ChildProducer<?>, Set<Producer<?>>> result =
        new IdentityHashMap<>(Math.toIntExact(Iterables.size64(nodes)));

    nodes.stream()
        .filter(node -> node instanceof ChildProducer)
        .map(node -> (ChildProducer<?>) node)
        .forEach(node -> result.put(node, new ReferenceOpenHashSet<>(node.internalAPI().getInputList())));

    return result;
  }

  /**
   * Finds all the producers in a subgraph that are "out of bounds" and do not have as an ancestor one of the
   * {@code boundingAncestors} and are not in the {@code boundingAncestors} set themselves.
   *
   * @param leaf the leaf of the subgraph under consideration
   * @param outOfBounds used to accrue the discovered out-of-bounds producers in the subgraph
   * @param boundingAncestors establish the boundary of the subgraph
   * @return true if {@code leaf} is not in and has no ancestor in the {@code ancestorBounds} set, false otherwise
   */
  static boolean findOutOfBounds(Producer<?> leaf, HashSet<Producer<?>> outOfBounds,
      Set<Producer<?>> boundingAncestors) {
    if (outOfBounds.contains(leaf)) {
      return true;
    } else if (boundingAncestors.contains(leaf)) {
      return false;
    } else if (leaf instanceof RootProducer) {
      outOfBounds.add(leaf);
      return true;
    } else { // ChildProducer
      List<? extends Producer<?>> parents = ChildProducer.getParents((ChildProducer<?>) leaf);
      boolean allParentsOutOfBounds = true;
      for (Producer<?> parent : parents) {
        // note we deliberately avoid && here--we need to recurse on *all* parents
        allParentsOutOfBounds &= findOutOfBounds(parent, outOfBounds, boundingAncestors);
      }
      // this leaf is out-of-bounds if all its parents are out-of-bounds
      if (allParentsOutOfBounds) {
        outOfBounds.add(leaf);
      }
      return allParentsOutOfBounds;
    }
  }

  /**
   * Finds all the inputs of a minimal subgraph such that the inputs are within the {@code outOfBounds} set.
   *
   * @param leaf the leaf of the subgraph under consideration
   * @param minimalInputs used to accrue the discovered inputs of the minimal subgraph, which must be within
   *                      {@code outOfBounds}
   * @param outOfBounds establish the boundary of the subgraph
   */
  static void findMinimalInputs(Producer<?> leaf, HashSet<Producer<?>> minimalInputs,
      HashSet<Producer<?>> outOfBounds) {
    if (outOfBounds.contains(leaf)) {
      minimalInputs.add(leaf);
    } else if (leaf instanceof RootProducer) {
      throw new IllegalArgumentException("The minimal DAG is invalid, since its bounds imply that a root producer is "
          + "in-bounds and must therefore have out-of-bounds ancestors (which is trivially untrue for a producer "
          + "lacking parents)");
    } else { // ChildProducer
      List<? extends Producer<?>> parents = ChildProducer.getParents((ChildProducer<?>) leaf);
      parents.forEach(parent -> findMinimalInputs(parent, minimalInputs, outOfBounds));
    }
  }
}
