package com.linkedin.dagli.dag;

import com.linkedin.dagli.generator.Generator;
import com.linkedin.dagli.placeholder.Placeholder;
import com.linkedin.dagli.producer.ChildProducer;
import com.linkedin.dagli.producer.MissingInput;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.producer.RootProducer;
import com.linkedin.dagli.transformer.Transformer;
import com.linkedin.dagli.util.invariant.Arguments;
import com.linkedin.dagli.view.TransformerView;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * A basic deduplicated producer graph.  Unlike {@link DAGStructure}, does not precompute extensive graph
 * metadata.  Typically used as an intermediate graph representation during processing.
 */
class DeduplicatedDAG {
  final List<Placeholder<?>> _placeholders;
  final List<Producer<?>> _outputs;
  final HashMap<Producer<?>, ArrayList<ChildProducer<?>>> _childrenMap;

  /**
   * Creates a deduplicated DAG "view" of an existing {@link DAGStructure}.  This is a very inexpensive operation as the
   * {@link DAGStructure} will already be deduplicated.
   *
   * @param dag the source DAG that will be "viewed" by this instance
   */
  DeduplicatedDAG(DAGStructure<?> dag) {
    _placeholders = dag._placeholders;
    _outputs = dag._outputs;
    _childrenMap = dag._childrenMap;
  }

  /**
   * Creates a deduplicated DAG.
   *
   * @param placeholders the placeholders of the DAG
   * @param outputs the outputs (outputs) of the DAG
   */
  DeduplicatedDAG(List<? extends Placeholder<?>> placeholders, List<? extends Producer<?>> outputs) {
    Objects.requireNonNull(placeholders, "Inputs may not be null");
    Objects.requireNonNull(outputs, "Outputs may not be null");
    Arguments.check(placeholders.size() > 0, "Must have at least one input");
    Arguments.check(outputs.size() > 0, "Must have at least one output");
    Arguments.check(placeholders.stream().distinct().count() == placeholders.size(),
        "The list of placeholders contains duplicates");

    // find the edges of the DAG (parent-to-child relationships) by walking upwards, starting from the outputs
    IdentityHashMap<Producer<?>, ArrayList<ChildProducer<?>>> childrenMap =
        parentToChildrenMap(placeholders, outputs);

    // this is a good time to validate everything in the DAG (doing this before deduplication means we might catch
    // misconfigured nodes whose equality/hashing logic would otherwise fail without explanation)
    validate(childrenMap.keySet());

    // compute the deduplication map
    IdentityHashMap<Producer<?>, Producer<?>> deduplicationMap = deduplicationMap(childrenMap);

    List<Producer<?>> deduplicatedOutputs = outputs.stream().map(deduplicationMap::get).collect(Collectors.toList());
    // (note that placeholders never need to be deduplicated--we explicitly prohibited duplicates above)

    // now create a new parent-to-children map from the deduplicated outputs
    IdentityHashMap<Producer<?>, ArrayList<ChildProducer<?>>> deduplicatedChildrenIdentityMap =
        parentToChildrenMap(placeholders, deduplicatedOutputs);
    HashMap<Producer<?>, ArrayList<ChildProducer<?>>> deduplicatedChildrenMap =
        new HashMap<>(deduplicatedChildrenIdentityMap);
    if (deduplicatedChildrenIdentityMap.size() != deduplicatedChildrenMap.size()) {
      // this should never happen--if it does, it's a bug (probably, though not necessarily, in this class)
      throw new IllegalStateException("Failed to correctly deduplicate nodes while building DAG");
    }

    _placeholders = new ArrayList<>(placeholders);
    _outputs = deduplicatedOutputs;
    _childrenMap = deduplicatedChildrenMap;
  }

  /**
   * Finds the edges of the DAG (parent-to-child relationships) by walking upwards, starting from the outputs.
   *
   * @param placeholders the list of placeholders rooting the DAG; placeholders not reachable from the outputs will
   *                     still be included in the resulting map because the DAG needs to remember all the placeholders
   *                     specified by the client, even the unnecessary/irrelevant ones
   * @param outputs the list of outputs that will be produced by the DAG
   * @return a map of each parent node in the DAG to its children; the list of children will include duplicates if a
   *         parent acts as an input to the child multiple times
   */
  private static IdentityHashMap<Producer<?>, ArrayList<ChildProducer<?>>> parentToChildrenMap(
      List<? extends Placeholder<?>> placeholders, List<? extends Producer<?>> outputs) {
    // we must be careful to use an IdentityHashMap here because we're not yet in a position to de-duplicate nodes in
    // the graph--that requires us to explore the graph from the placeholders down, and will be done in a later step
    final IdentityHashMap<Producer<?>, ArrayList<ChildProducer<?>>> childrenMap =
        new IdentityHashMap<>(placeholders.size() + outputs.size());

    for (Placeholder<?> placeholder : placeholders) {
      childrenMap.put(placeholder, new ArrayList<>());
    }

    LinkedList<ChildProducer<?>> queue = new LinkedList<>();

    for (Producer<?> output : new ReferenceOpenHashSet<>(outputs)) { // don't process the exact same output twice
      if (output instanceof Placeholder<?>) {
        Arguments.check(childrenMap.containsKey(output),
            "Outputs list includes Placeholder not present in the placeholders list");
        continue;
      } else if (output instanceof ChildProducer<?>) {
        queue.add((ChildProducer<?>) output);
      } else if (!(output instanceof Generator<?>)) {
        // everything we know about is a ChildProducer, Placeholder or Generator
        throw new IllegalArgumentException(
            "Outputs list contains an object that is an unsupported type of Producer: " + output);
      }

      childrenMap.put(output, new ArrayList<>());
    }

    while (!queue.isEmpty()) {
      ChildProducer<?> child = queue.pop();
      int index = -1;

      for (Producer<?> parent : child.internalAPI().getInputList()) {
        index++;
        if (parent instanceof Placeholder<?>) {
          Arguments.check(childrenMap.containsKey(parent),
              "The outputs list requires a Placeholder that was not provided: " + parent.toString()
                  + "; proximate dependent" + " child is " + child.toString());
          childrenMap.get(parent).add(child);
        } else if (parent instanceof Generator<?>) {
          childrenMap.computeIfAbsent(parent, k -> new ArrayList<>()).add(child);
        } else if (parent instanceof Transformer<?> || parent instanceof TransformerView<?, ?>) {
          if (childrenMap.containsKey(parent)) {
            childrenMap.get(parent).add(child);
          } else {
            queue.add((ChildProducer<?>) parent);
            ArrayList<ChildProducer<?>> children = new ArrayList<>(1);
            children.add(child);
            childrenMap.put(parent, children);
          }
        } else if (parent instanceof MissingInput) {
          throw new IllegalArgumentException(
              "The transformer " + child + " has an MissingInput at input number " + index
                  + ".  This probably means you forgot to set an input on this transformer, e.g. using withInput(...).");
        } else {
          throw new IllegalArgumentException("Outputs list has ancestor that is not a supported Producer type");
        }
      }
    }

    return childrenMap;
  }

  /**
   * Deduplicates nodes in the DAG that are {@link Object#equals(Object)}, creating a semantically equivalent DAG that
   * eliminates duplicated work.
   *
   * Note that it is possible for a node in the original graph to not {@link Object#equals(Object)} its replacement in
   * the new, deduplicated graph.  This will happen if, e.g. the producer uses handle-equality (rather than the typical
   * value-equality): when the deduplication process modifies an ancestor of the node, we replace the node itself with a
   * copy that will have different (but semantically equivalent) inputs; a handle-equality implementation of equals()
   * may thus assert the copy is not equal to the original (since they will have different handles).
   *
   * @param childrenMap the original DAG, expressed as a map from parents to children, also including childless root
   *                    nodes that are explicit outputs of the DAG
   * @return a mapping from the original producers to their deduplicated equivalents (possibly the same producer) in the
   *         new DAG
   */
  private static IdentityHashMap<Producer<?>, Producer<?>> deduplicationMap(
      IdentityHashMap<Producer<?>, ArrayList<ChildProducer<?>>> childrenMap) {

    IdentityHashMap<Producer<?>, Producer<?>> deduplicationMap = new IdentityHashMap<>(childrenMap.size());

    IdentityHashMap<ChildProducer<?>, Set<Producer<?>>> unsatisfiedDependencies = DAGUtil.producerToInputSetMap(childrenMap.keySet());

    // Maps one Producer to another Producer (possibly the same one used to query the table) that is equals() to it.
    // This determines which specific instance of a set of equivalent Producers is "interned" and will actually be
    // included in the final graph.
    HashMap<Producer<?>, Producer<?>> producerInternTable = new HashMap<>();

    // A queue keeps track of everything that has no remaining unsatisfied dependencies and can therefore be processed.
    // Initially the queue should contain (only) the root nodes, since these they have no parents (and thus no
    // unsatisfied dependencies).
    //
    // We use a priority queue such that the producers with the highest "class depth" (most ancestors) will be
    // processed first.  This is because we need to make sure that, of a set of equals() producers, the "canonical"
    // instance from that set that we use in the final DAG is the one with the most derived class (recall that, to be
    // equals(), one Producer must have the same class as another or a have a class that is a superclass of another;
    // consequently, ensuring that our canonical instance is of the most derived type guarantees that it will have a
    // superset of the methods and fields of the others--since we might need to hand the client this instance when
    // they hand us a handle to one of the other equals() Producers, this is important!)
    PriorityQueue<Producer<?>> queue = childrenMap.keySet()
        .stream()
        .filter(producer -> producer instanceof RootProducer)
        .collect(Collectors.toCollection(
            () -> new PriorityQueue<>(Comparator.comparing(producer -> classDepth(producer.getClass())).reversed())));

    while (!queue.isEmpty()) {
      Producer<?> original = queue.poll();

      // first, remap the original's inputs (if a ChildProducer); this ensures that "remapped" refers to a producer
      // whose ancestors have all already been de-duplicated
      Producer<?> remapped =
          original instanceof ChildProducer ? DAGUtil.remappedInputs((ChildProducer<?>) original, deduplicationMap::get)
              : original;

      // now we find the "canonical" instance corresponding to this one from our intern table (which will be the same
      // instance if we haven't seen anything equivalent to this one before)
      Producer<?> interned = producerInternTable.computeIfAbsent(remapped, Function.identity());

      // add a mapping from the original node to its interned equivalent
      deduplicationMap.put(original, interned);

      // Update the unsatisfied dependencies of everyone depending on this node.  We use referenceEqualitySet() because
      // a child can be listed multiple times if it the parent fills multiple of its input slots
      new ReferenceOpenHashSet<>(childrenMap.get(original)).forEach(child -> {
        Set<Producer<?>> unsafisfiedSet = unsatisfiedDependencies.get(child);

        // sanity check; this should never happen
        if (!unsafisfiedSet.remove(original)) {
          throw new IllegalStateException("A Producer's child does not have the expected dependency on that Producer");
        }

        if (unsafisfiedSet.isEmpty()) {
          queue.add(child);
        }
      });
    }

    return deduplicationMap;
  }

  /**
   * Calculates the "depth" of a class, defined as the number of classes that are in its ancestry, including itself.
   * "null" has a depth of 0, Object has a depth of 1, String has a depth of 2 (itself and Object), etc.
   * @param clazz the class whose depth should be checked (can be null)
   * @return the depth of the provided class
   */
  private static int classDepth(Class<?> clazz) {
    int depth = 0;
    while (clazz != null) {
      clazz = clazz.getSuperclass();
      depth++;
    }

    return depth;
  }

  private static void validate(Iterable<Producer<?>> producers) {
    for (Producer<?> producer : producers) {
      try {
        producer.validate();
      } catch (RuntimeException e) {
        throw new IllegalStateException(
            "While building a DAG, encountered an exception validating node of type " + producer.getClass() + ", "
                + producer.getName() + " (" + producer + "): " + e.getMessage(), e);
      }
    }
  }
}
