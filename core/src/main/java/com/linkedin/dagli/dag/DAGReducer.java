package com.linkedin.dagli.dag;

import com.linkedin.dagli.placeholder.Placeholder;
import com.linkedin.dagli.producer.ChildProducer;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.producer.RootProducer;
import com.linkedin.dagli.producer.internal.AncestorSpliterator;
import com.linkedin.dagli.reducer.ClassReducerTable;
import com.linkedin.dagli.reducer.Reducer;
import com.linkedin.dagli.transformer.PreparableTransformer;
import com.linkedin.dagli.transformer.PreparedTransformer;
import com.linkedin.dagli.util.cloneable.AbstractCloneable;
import com.linkedin.dagli.util.collection.Iterables;
import com.linkedin.dagli.util.collection.LinkedStack;
import com.linkedin.dagli.util.invariant.Arguments;
import com.linkedin.dagli.view.TransformerView;
import it.unimi.dsi.fastutil.objects.ReferenceArraySet;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceSet;
import it.unimi.dsi.fastutil.objects.ReferenceSets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;


/**
 * Reduces a DAG, applying available {@link com.linkedin.dagli.reducer.Reducer}s to it to simplify the graph.
 */
abstract class DAGReducer {
  private DAGReducer() { }

  private static class AccumulatedState {
    final ClassReducerTable _classReducerTable;

    // no exact producer instance should be reduced more than once
    final ReferenceOpenHashSet<Producer<?>> _visited = new ReferenceOpenHashSet<>();

    @SuppressWarnings("unchecked") // no direct way to create Class<Producer<?>>
    AccumulatedState() {
      _classReducerTable = new ClassReducerTable();

      // add "global" reducers that should always be considered during DAG reduction:
      _classReducerTable.add(ConstantResultReducer.INSTANCE, (Class<Producer<?>>) (Class) Producer.class);
    }

    /**
     * "Cleans" this aggregated state to forget producer instances that are no longer part of the graph.  Normally,
     * once an instance dropped from the graph, that exact instance will never be added back in.  Of course, a
     * misbehaving reducer may violate this, but the only cost will be having to needlessly re-reduce an already reduced
     * producer.
     *
     * @param childrenMap a map of the current children in the graph
     */
    void clean(IdentityHashMap<Producer<?>, ReferenceSet<ChildProducer<?>>> childrenMap) {
      _visited.removeIf(producer -> !childrenMap.containsKey(producer));
    }
  }

  private static class State {
    final Reducer.Level _minimumImportance;
    final boolean _isCompleteGraphReduction;
    final boolean _isPreparedDAG;
    final IdentityHashMap<Producer<?>, ReferenceSet<ChildProducer<?>>> _childrenMap;
    final IdentityHashMap<ChildProducer<?>, List<Producer<?>>> _parentMap;


    boolean _modified = false;

    // placeholders and outputs may be replaced, but never removed
    final ArrayList<Placeholder<?>> _placeholders;
    final ArrayList<Producer<?>> _outputs;

    final AccumulatedState _accumulated;

    State(List<Placeholder<?>> placeholders, List<Producer<?>> outputs,
        HashMap<Producer<?>, ArrayList<ChildProducer<?>>> childrenMap, Reducer.Level minimumImportance,
        boolean isCompleteGraphReduction, AccumulatedState accumulated) {
      _accumulated = accumulated;
      _minimumImportance = minimumImportance;
      _isCompleteGraphReduction = isCompleteGraphReduction;
      _placeholders = new ArrayList<>(placeholders);
      _outputs = new ArrayList<>(outputs);

      _childrenMap = new IdentityHashMap<>(childrenMap.size());
      childrenMap.forEach((key, value) -> _childrenMap.put(key, new ReferenceOpenHashSet<>(value)));

      _parentMap = new IdentityHashMap<>(childrenMap.size());
      childrenMap.forEach((key, value) -> {
        if (key instanceof ChildProducer) {
          ChildProducer<?> child = (ChildProducer<?>) key;
          _parentMap.put(child, new ArrayList<>(child.internalAPI().getInputList()));
        }
      });

      childrenMap.keySet().stream().map(producer -> producer.internalAPI().getClassReducerTable()).filter(
          Objects::nonNull).forEach(_accumulated._classReducerTable::addAll);

      _isPreparedDAG = childrenMap.keySet().stream().noneMatch(producer -> producer instanceof PreparableTransformer);
    }


    boolean inWorkingGraph(Producer<?> producer) {
      return _childrenMap.containsKey(producer);
    }

    // if it is not already present, adds a producer to the graph, and adds its provided children regardless
    void add(Producer<?> producer, ReferenceSet<ChildProducer<?>> children) {
      _modified = true;

      ReferenceSet<ChildProducer<?>> existingChildren = _childrenMap.get(producer);

      if (existingChildren != null) {
        // existing entry; just add the given children
        existingChildren.addAll(children);
        return;
      }

      producer.validate(); // check that the producer is indeed ready to add to the working graph

      _childrenMap.put(producer, new ReferenceOpenHashSet<>(children));
      if (producer instanceof ChildProducer) {
        ChildProducer<?> childProducer = (ChildProducer<?>) producer;
        List<? extends Producer<?>> parents = childProducer.internalAPI().getInputList();
        _parentMap.put(childProducer, new ArrayList<>(parents));
        parents.forEach(parent -> this.add(parent, ReferenceSets.singleton(childProducer)));
      }
    }

    private void replace(Producer<?> existing, Producer<?> replacement) {
      Objects.requireNonNull(existing);
      Objects.requireNonNull(replacement);

      if (existing == replacement) { // check for reference equality
        return;
      }

      _modified = true;

      if (replacement instanceof Placeholder) {
        // don't allow introduction of new placeholders (it's prohibited by the spec since there's no really any good
        // reason to do so and would thus probably constitute bug in a reducer's implementation).  Our check is overly
        // permissive as it will allow the use of placeholders that are not among the current reduction target's
        // ancestors (if the reducer were to somehow manage to obtain a reference to one):
        Arguments.check(_placeholders.contains(replacement),
            "Attempting to introduce a new placeholder that is not already part of the graph");
      }

      // Is a target being replaced?
      _outputs.replaceAll(output -> output == existing ? replacement : output);

      // add the replacement to the graph if it is not already present
      add(replacement, ReferenceSets.emptySet());

      // unhook existing's children
      ReferenceSet<ChildProducer<?>> existingChildren = _childrenMap.remove(existing);
      for (ChildProducer<?> child : existingChildren) {
        _parentMap.get(child).replaceAll(parent -> parent == existing ? replacement : parent);
      }

      // unhook existing's parents
      if (existing instanceof ChildProducer) {
        List<Producer<?>> existingParents = _parentMap.remove(existing);
        existingParents.forEach(parent -> _childrenMap.get(parent).remove(existing));

        // if we're replacing a TransformerView, we need to reset the "visited" status on the view's parents if they they
        // no longer have any dependent views (this may allow further reductions that previously weren't possible):
        if (existing instanceof TransformerView) {
          for (Producer<?> parent : existingParents) {
            if (_childrenMap.get(parent).stream().noneMatch(child -> child instanceof TransformerView)) {
              _accumulated._visited.remove(parent); // needs to be re-checked if previously visited
            }
          }
        }
      }

      // at this point, there should be no reference to existing in either the parent or children maps...

      // replacement inherits the existing producer's children (but, of course, not its parents!)
      ReferenceSet<ChildProducer<?>> replacementChildren = _childrenMap.get(replacement);
      replacementChildren.addAll(existingChildren);
    }

    List<Producer<?>> getParents(Producer<?> producer) {
      if (producer instanceof ChildProducer) {
        return _parentMap.get(producer);
      } else {
        return Collections.emptyList(); // root node
      }
    }

    /**
     * Returns a (possibly new) producer which will have parents matching those in the working graph.
     *
     * @param producer the producer to resolve
     * @return the producer with parents matching those in the working graph, possibly different than the original
     */
    @SuppressWarnings("unchecked") // correctness guaranteed by semantics of withInputsUnsafe(...)
    <T extends Producer<?>> T withCurrentParents(T producer) {
      if (!(producer instanceof ChildProducer)) {
        return producer; // no parents
      }

      ChildProducer<?> childProducer = (ChildProducer<?>) producer;

      List<Producer<?>> workingGraphParents = getParents(childProducer);
      List<? extends Producer<?>> nominalParents = childProducer.internalAPI().getInputList();

      if (Iterables.elementsAreReferenceEqual(workingGraphParents, nominalParents)) {
        return producer; // no need to create new instance
      }

      // create new instance with the parents specified by the current working graph
      return (T) childProducer.internalAPI().withInputsUnsafe(workingGraphParents);
    }
  }

  @SuppressWarnings("unchecked") // applicability of graph reducers assured by API
  static void reduce(Producer<?> producer, State state) {
    assert state.inWorkingGraph(producer);

    if (!state._accumulated._visited.add(producer)) {
      return; // have already reduced this producer--done
    }

    // note that reducing any of this producer's parents could change its parent list
    int visitedThisIteration;
    do {
      visitedThisIteration = 0;
      List<Producer<?>> parents = new ArrayList<>(state.getParents(producer));
      for (Producer<?> parent : parents) {
        if (!state._accumulated._visited.contains(parent) && state.inWorkingGraph(parent)) {
          // if the parent hasn't been seen this reduction round and is still in the graph...
          reduce(parent, state);
          visitedThisIteration++;
        }
      }
    } while (visitedThisIteration > 0);

    // reduce this producer
    ReducerContext context = new ReducerContext(state);
    for (Reducer<?> reducer : producer.internalAPI().getGraphReducers()) {
      if (reducer.getLevel().compareTo(state._minimumImportance) >= 0) {
        ((Reducer) reducer).reduce(producer, context);
        // each reducer has the potential to disconnect the current producer; if so, stop immediately:
        if (!state.inWorkingGraph(producer)) {
          return;
        }
      }
    }
    for (Reducer<?> reducer : Iterables.<Reducer>lazyConcatenate(() -> producer.internalAPI().getGraphReducers(),
        () -> state._accumulated._classReducerTable.getReducers(producer.getClass()))) {
      ((Reducer) reducer).reduce(producer, context);
      // each reducer has the potential to disconnect the current producer; if so, stop immediately:
      if (!state.inWorkingGraph(producer)) {
        return;
      }
    }
  }

  static Producer<?> instantiateFromWorkingGraph(final State state, final Producer<?> producer,
      final IdentityHashMap<Producer<?>, Producer<?>> instantiated) {
    if (producer instanceof RootProducer) {
      return producer;
    }

    Producer<?> extant = instantiated.get(producer);
    if (extant != null) {
      return extant;
    }

    ChildProducer<?> childProducer = (ChildProducer<?>) producer;
    List<Producer<?>> newParents = state.getParents(childProducer)
        .stream()
        .map(parent -> instantiateFromWorkingGraph(state, parent, instantiated))
        .collect(Collectors.toList());

    // we should only create a new producer instance if the parents have changed
    ChildProducer<?> newProducer =
        Iterables.elementsAreReferenceEqual(newParents, childProducer.internalAPI().getInputList()) ? childProducer
            : childProducer.internalAPI().withInputsUnsafe(newParents);

    instantiated.put(producer, newProducer);
    return newProducer;
  }

  /**
   * Reduces a DAG, returning the new, reduced DAG.
   *
   * @param placeholders the placeholders list of the unreduced DAG
   * @param outputs the outputs list of the unreduced DAG
   * @param minimumLevel the minimum level of reducers to run
   * @return the reduced DAG
   */
  public static DeduplicatedDAG reduce(List<Placeholder<?>> placeholders, List<Producer<?>> outputs,
      Reducer.Level minimumLevel) {
    return reduce(new DeduplicatedDAG(placeholders, outputs), minimumLevel);
  }

  /**
   * Reduces a DAG with reducers of the  and returns the new, reduced DAG.
   *
   * @param dag the (deduplicated) DAG to reduce
   * @param minimumLevel the minimum level of reducers to run; if null, this method simply returns the original DAG
   * @return the reduced DAG
   */
  static DeduplicatedDAG reduce(DeduplicatedDAG dag, Reducer.Level minimumLevel) {
    if (minimumLevel == null) {
      return dag;
    }

    AccumulatedState accumulated = new AccumulatedState();

    while (true) {
      State state = new State(dag._placeholders, dag._outputs, dag._childrenMap, minimumLevel, true, accumulated);

      // do the reduction, starting at each of the graph's outputs
      int visitedThisIteration;
      do {
        visitedThisIteration = 0;
        // create a copy since state._outputs may change as we do our reductions
        ArrayList<Producer<?>> currentOutputs = new ArrayList<>(state._outputs);
        for (Producer<?> output : currentOutputs) {
          if (!state._accumulated._visited.contains(output) && state.inWorkingGraph(output)) {
            // if the output hasn't been seen this reduction round and is still in the graph...
            reduce(output, state);
            visitedThisIteration++;
          }
        }
      } while (visitedThisIteration > 0);

      if (!state._modified) {
        return dag;
      }

      // clean the accumulated state to avoid keep references to producers that are no longer part of the working graph:
      accumulated.clean(state._childrenMap);

      // rebuild the graph with the correct parents on each node
      IdentityHashMap<Producer<?>, Producer<?>> instantiated = new IdentityHashMap<>();
      ArrayList<Producer<?>> outputs = state._outputs;
      outputs.replaceAll(output -> instantiateFromWorkingGraph(state, output, instantiated));

      dag = new DeduplicatedDAG(state._placeholders, outputs);
    }
  }

  static class ReducerContext implements Reducer.Context {
    final State _state;

    ReducerContext(State state) {
      _state = state;
    }

    @Override
    public Reducer.Level getMinimumImportance() {
      return _state._minimumImportance;
    }

    @Override
    public boolean isCompleteGraphReduction() {
      return _state._isCompleteGraphReduction;
    }

    @Override
    public boolean isPreparedDAG() {
      return _state._isPreparedDAG;
    }

    @Override
    public boolean isViewed(Producer<?> producer) {
      return producer instanceof PreparableTransformer && _state._childrenMap.get(producer)
          .stream()
          .anyMatch(child -> child instanceof TransformerView);
    }

    private void replaceUnconditionally(Producer<?> existing, Producer<?> replacement) {
      _state.replace(existing, replacement);
    }

    private void checkForIllegalPreparable(Producer<?> replacement) {
      if (isPreparedDAG() && replacement instanceof PreparableTransformer) {
        throw new IllegalArgumentException("Cannot add PreparableTransformer to prepared DAG");
      }
    }

    @Override
    public <T> boolean hasReducer(Class<T> clazz, Reducer<? super T> reducer) {
      return _state._accumulated._classReducerTable.hasReducer(clazz, reducer);
    }

    @Override
    public <T extends AbstractCloneable<T> & Producer<?>> void replaceWithSameClass(T existing, T replacement) {
      // Checking the runtime class cannot guarantee that existing and replacement have the same generic parameters
      // (if applicable), but it's a fallback defense--the AbstractCloneable<T> typing ensures existing and replacement
      // have the exact same type (including generic parameters) when AbstractCloneable is extended correctly.
      Arguments.check(existing.getClass().equals(replacement.getClass()),
          "Existing and replacement producers must be of the same type");

      // if existing and replacement are indeed the exact same type, we can safely substitute one for the other
      replaceUnconditionally(existing, replacement);
    }

    @Override
    public <R> void replace(RootProducer<R> existing, Producer<? extends R> replacement) {
      checkForIllegalPreparable(replacement);
      replaceUnconditionally(existing, replacement);
    }

    @Override
    public <R> void replace(TransformerView<R, ?> existing, Producer<? extends R> replacement) {
      replaceUnconditionally(existing, replacement);
    }

    @Override
    public <R> void replace(PreparedTransformer<R> existing, Producer<? extends R> replacement) {
      checkForIllegalPreparable(replacement);
      replaceUnconditionally(existing, replacement);
    }

    @Override
    public <R, N extends PreparedTransformer<? extends R>> void replace(PreparableTransformer<R, N> existing,
        PreparableTransformer<? extends R, ? extends N> replacement) {
      replaceUnconditionally(existing, replacement);
    }

    @Override
    public <R> void replaceUnviewed(Producer<R> existing, Producer<? extends R> replacement) {
      if (!tryReplaceUnviewed(existing, () -> replacement)) {
        throw new IllegalArgumentException("Attempt to use replaceUnviewedPreparable(...) on a preparable transformer "
            + "with one or more TransformerView children");
      }
    }

    @Override
    public <R> boolean tryReplaceUnviewed(Producer<R> existing, Supplier<Producer<? extends R>> replacementSupplier) {
      if (isViewed(existing)) {
        return false;
      }
      replaceUnconditionally(existing, replacementSupplier.get());
      return true;
    }

    @Override
    public <T extends Producer<?>> T withCurrentParents(T producer) {
      return _state.withCurrentParents(producer);
    }

    @Override
    public List<? extends Producer<?>> getParents(Producer<?> producer) {
      return _state.getParents(producer);
    }

    @Override
    @SuppressWarnings("unchecked") // cast vetted by isInstance(...)
    public <T> ReferenceSet<T> getParentsByClass(Producer<?> producer, Class<T> producerClass) {
      List<? extends Producer<?>> parents = getParents(producer);
      ReferenceArraySet<T> result = new ReferenceArraySet<>(parents.size());
      for (Producer<?> parent : parents) {
        if (producerClass.isInstance(parent)) {
          result.add((T) parent);
        }
      }

      return result;
    }

    @Override
    public <T> ReferenceSet<T> getAncestorsByClass(Producer<?> producer, Class<T> producerClass, int maxDepth) {
      ReferenceOpenHashSet<T> result = new ReferenceOpenHashSet<>();
      ancestors(producer, maxDepth).map(LinkedStack::peek).filter(producerClass::isInstance)
          .forEach(ancestor -> result.add(producerClass.cast(ancestor)));
      return result;
    }

    @Override
    public Stream<LinkedStack<Producer<?>>> ancestors(Producer<?> producer, int maxDepth) {
      if (!(producer instanceof ChildProducer)) {
        return Stream.empty();
      }

      return StreamSupport.stream(new AncestorSpliterator((ChildProducer<?>) producer, maxDepth, _state::getParents),
          false);
    }
  }


}
