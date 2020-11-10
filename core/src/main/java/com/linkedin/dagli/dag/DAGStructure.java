package com.linkedin.dagli.dag;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.generator.Constant;
import com.linkedin.dagli.generator.Generator;
import com.linkedin.dagli.placeholder.Placeholder;
import com.linkedin.dagli.producer.ChildProducer;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.transformer.AbstractPreparedTransformerDynamic;
import com.linkedin.dagli.transformer.PreparableTransformer;
import com.linkedin.dagli.transformer.PreparedTransformer;
import com.linkedin.dagli.transformer.Transformer;
import com.linkedin.dagli.tuple.Tuple;
import com.linkedin.dagli.util.collection.LinkedStack;
import com.linkedin.dagli.view.TransformerView;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * Class used internally by Dagli to store the structure of a DAG.  Note that some information is stored redundantly for
 * efficient execution.
 *
 * @param <R> the type of result produced by this DAG.  For result arity greater than 1, this will be a tuple.  For a
 *            DAG1x2, for example, this would be a Tuple2.
 */
class DAGStructure<R> implements Serializable, Graph<Producer<?>> {
  private static final long serialVersionUID = 1;

  // used to indicate that a node is not present/missing when storing a node index
  private static final int MISSING_NODE_INDEX = -1;

  final List<Placeholder<?>> _placeholders;
  final List<Producer<?>> _outputs;

  // children map will contain duplicate children if a child has more than one input from a single parent
  final HashMap<Producer<?>, ArrayList<ChildProducer<?>>> _childrenMap;

  // easily derived from childrenMap, but we can save time by storing this explicitly:
  final List<Generator<?>> _generators;

  // nodes in DAG order (no node appears before a parent) AND phase order (no node occurs before any of a previous
  // phase.  Placeholders are always first, followed by generators, followed by transformers and views.  In any
  // phase >= 1, nodes are always ordered within the phase as:
  // (1) preparable transformers
  // (2) views
  // (3) prepared transformers
  final Producer<?>[] _nodes;

  private final Object2IntOpenHashMap<Producer<?>> _nodeIndexMap;

  // Phases of the nodes.  These will be in monotonically increasing order, starting at 0.
  final int[] _phases;

  // All parents for a given node, corresponding to its input list.  Will contain duplicates if a node has the same
  // parent as more than one input
  final int[][] _parents;

  // Children, sorted in increasing order of index.  Will contain duplicates for children with multiple inputs from the
  // same parent.
  final int[][] _children;

  // Indices of the outputs in the node list.
  final int[] _outputIndices;

  // True iff all transformers in the DAG are prepared.
  final boolean _isPrepared;

  // The maximum minibatch size of all prepared transformers in the graph; if the graph contains no prepared
  //transformers, the value will be 1.
  final int _maxMinibatchSize;

  // The maximum number of parents possessed by any node in the graph; if the graph contains no child nodes, the value
  // will be 0.
  final int _maxParentCount;

  // A DAG is always constant if all of its outputs are constant (this would be unusual as it implies the placeholder
  // values are ignored entirely, but possible)
  final boolean _isAlwaysConstant;

  // A preparable DAG will have an idempotent preparer iff all its preparable transformers have idempotent preparers.
  // For prepared DAGs, this flag will be trivially true.
  final boolean _hasIdempotentPreparer;

  // a (modified) copy of the DAG that is used exclusively for equality checking (and hashing)
  final EqualityLeaf _equalityDAG;

  /**
   * Creates a new instance from a {@link DeduplicatedDAG}.
   * @param dag
   */
  DAGStructure(DeduplicatedDAG dag) {
    this(dag._placeholders, dag._outputs, dag._childrenMap);
  }

  /**
   * Creates a new instance.
   *
   * @param placeholders the placeholders of the DAG
   * @param outputs the outputs of the DAG
   * @param childrenMap the map of children from nodes to their children
   */
  DAGStructure(List<Placeholder<?>> placeholders, List<Producer<?>> outputs,
      HashMap<Producer<?>, ArrayList<ChildProducer<?>>> childrenMap) {

    // validate all extant
    childrenMap.keySet().forEach(Producer::validate);

    _placeholders = placeholders;
    _outputs = outputs;
    _childrenMap = childrenMap;

    _generators =
        (List) childrenMap.keySet().stream().filter(p -> p instanceof Generator<?>).collect(Collectors.toList());

    _nodes = new Producer[_childrenMap.size()];
    _nodeIndexMap = new Object2IntOpenHashMap<>(_nodes.length);
    _nodeIndexMap.defaultReturnValue(MISSING_NODE_INDEX);
    _phases = new int[_nodes.length];
    _parents = new int[_nodes.length][];
    _children = new int[_nodes.length][];

    LinkedList<PreparableTransformer<?, ?>> preparableQueue = new LinkedList<>();
    LinkedList<PreparedTransformer<?>> preparedQueue = new LinkedList<>();
    LinkedList<TransformerView<?, ?>> viewQueue = new LinkedList<>();

    // get a map of child producers to a set of their unsatisfied dependencies
    IdentityHashMap<ChildProducer<?>, Set<Producer<?>>> unsatisfiedDependencies =
        DAGUtil.producerToInputSetMap(_childrenMap.keySet());

    for (Producer<?> root : _placeholders) {
      addNode(root, 0, unsatisfiedDependencies, preparableQueue, preparedQueue, viewQueue);
    }
    for (Producer<?> root : _generators) {
      addNode(root, 0, unsatisfiedDependencies, preparableQueue, preparedQueue, viewQueue);
    }

    int phase = 0;
    while (_nodeIndexMap.size() < _nodes.length) {
      // add as many non-preparable nodes as possible; those nodes that are added will be those who dependencies are
      // satisfied in this or previous phases

      // note that views always have a single preparable dependency; adding prepared transformers in the next loop will
      // *not* possibly allow more views to be added to this phase (which would otherwise create a bug)
      while (!viewQueue.isEmpty()) {
        addNode(viewQueue.remove(), phase, unsatisfiedDependencies, preparableQueue, preparedQueue, viewQueue);
      }

      while (!preparedQueue.isEmpty()) {
        addNode(preparedQueue.remove(), phase, unsatisfiedDependencies, preparableQueue, preparedQueue, viewQueue);
      }

      phase++;

      LinkedList<PreparableTransformer<?, ?>> phasePreparables = preparableQueue;
      preparableQueue = new LinkedList<>();
      for (PreparableTransformer<?, ?> preparable : phasePreparables) {
        addNode(preparable, phase, unsatisfiedDependencies, preparableQueue, preparedQueue, viewQueue);
      }
    }

    // fill in children info
    for (int i = 0; i < _nodes.length; i++) {
      ArrayList<ChildProducer<?>> children = _childrenMap.get(_nodes[i]);
      int[] childrenArray = new int[children.size()];
      for (int j = 0; j < children.size(); j++) {
        childrenArray[j] = getNodeIndex(children.get(j));
      }
      Arrays.sort(childrenArray);
      _children[i] = childrenArray;
    }

    // store output indices to avoid having to do a lookup later
    _outputIndices = new int[_outputs.size()];
    for (int i = 0; i < _outputIndices.length; i++) {
      _outputIndices[i] = _nodeIndexMap.getInt(_outputs.get(i));
    }

    boolean isPrepared = true;
    for (int i = _placeholders.size() + _generators.size(); i < _nodes.length; i++) {
      if (!(_nodes[i] instanceof PreparedTransformer)) {
        isPrepared = false;
        break;
      }
    }
    _isPrepared = isPrepared;

    // a DAG transformer is constant-result if all its outputs have a constant result
    _isAlwaysConstant = _outputs.stream().allMatch(Producer::hasConstantResult);

    // a DAG is marked idempotent if all its preprable transformers are idempotent
    _hasIdempotentPreparer = isPrepared || Arrays.stream(_nodes)
        .allMatch(producer -> !(producer instanceof PreparableTransformer)
            || ((PreparableTransformer<?, ?>) producer).internalAPI().hasIdempotentPreparer());

    _maxMinibatchSize = Arrays.stream(_nodes)
        .filter(node -> node instanceof PreparedTransformer)
        .map(node -> (PreparedTransformer<?>) node)
        .mapToInt(prepared -> prepared.internalAPI().getPreferredMinibatchSize())
        .max()
        .orElse(1);

    _maxParentCount = Arrays.stream(_parents).mapToInt(arr -> arr.length).max().orElse(0);

    _equalityDAG = createEqualityDAG();
  }

  /**
   * @return the number of inputs the DAG accepts
   */
  int getInputArity() {
    return _placeholders.size();
  }

  /**
   * @return the number of outputs the DAG produces
   */
  int getOutputArity() {
    return _outputIndices.length;
  }

  /**
   * @param node the node whose index is sought
   * @return the 0-based index of the node as assigned by this DAGStructure
   */
  int getNodeIndex(Producer<?> node) {
    return _nodeIndexMap.getInt(node);
  }

  /**
   * Checks if a particular node is an output (i.e. its result is an output of the DAG)
   * @param nodeIndex the index of the node to check
   * @return whether or not the node is a output
   */
  boolean isOutput(int nodeIndex) {
    for (int outputIndex : _outputIndices) {
      if (nodeIndex == outputIndex) {
        return true;
      }
    }
    return false;
  }

  /**
   * Checks if a node is a root of the DAG (a generator or placeholder)
   * @param nodeIndex the index of the node to check
   * @return whether or not the node is a root
   */
  boolean isRoot(int nodeIndex) {
    return nodeIndex < _placeholders.size() + _generators.size();
  }

  private void addNode(Producer<?> node, int phase,
      IdentityHashMap<ChildProducer<?>, Set<Producer<?>>> unsatisfiedDependencies,
      LinkedList<PreparableTransformer<?, ?>> preparableQueue, LinkedList<PreparedTransformer<?>> preparedQueue,
      LinkedList<TransformerView<?, ?>> viewQueue) {
    int index = _nodeIndexMap.size();
    _nodes[index] = node;
    _nodeIndexMap.put(node, index);
    _phases[index] = phase;

    if (node instanceof Transformer<?>) {
      Transformer<?> transformer = (Transformer<?>) node;
      List<? extends Producer<?>> parentList = transformer.internalAPI().getInputList();
      int[] parents = new int[parentList.size()];
      for (int i = 0; i < parents.length; i++) {
        parents[i] = getNodeIndex(parentList.get(i));
      }
      _parents[index] = parents;
    } else if (node instanceof TransformerView<?, ?>) {
      TransformerView<?, ?> transformerView = (TransformerView<?, ?>) node;
      int[] parents = new int[] { getNodeIndex(transformerView.internalAPI().getViewed()) };
      _parents[index] = parents;
    } else {
      assert phase == 0;
      _parents[index] = new int[0];
    }

    for (ChildProducer<?> child : _childrenMap.get(node)) {
      Set<Producer<?>> dependencies = unsatisfiedDependencies.get(child);
      // Could be empty/added already if this child appears multiple times in children list and we've already seen it:
      if (!dependencies.isEmpty()) {
        dependencies.remove(node);
        if (dependencies.isEmpty()) {
          if (child instanceof PreparedTransformer<?>) {
            preparedQueue.add((PreparedTransformer<?>) child);
          } else if (child instanceof PreparableTransformer<?, ?>) {
            preparableQueue.add((PreparableTransformer<?, ?>) child);
          } else if (child instanceof TransformerView<?, ?>) {
            viewQueue.add((TransformerView<?, ?>) child);
          } else {
            throw new IllegalArgumentException("Unknown dependency type");
          }
        }
      }
    }
  }

  /**
   * Gets the highest phase of any node in the DAG.
   *
   * @return the highest phase of any node in the DAG
   */
  public int getLastPhase() {
    return _phases[_phases.length - 1];
  }

  /**
   * Checks if a node is in the last phase
   *
   * @param nodeIndex the node to check
   * @return true iff the node is in the last phase
   */
  public boolean isLastPhase(int nodeIndex) {
    return _phases[nodeIndex] == getLastPhase();
  }

  /**
   * Returns the index of the first node in a given phase
   *
   * @param phase the phase to look for
   * @return the index of the first node with the specified phase
   */
  public int firstNodeInPhase(int phase) {
    if (phase == 0) {
      return 0;
    }

    int firstIndex = Arrays.binarySearch(_phases, phase);
    while (_phases[firstIndex - 1] == phase) {
      firstIndex--;
    }
    return firstIndex;
  }

  /**
   * Returns the index of the first prepared transformer in a given phase (prepared transformers are always last within
   * a phase).  If there are no prepared transformers in a phase, the index of the first node in the *next* phase is
   * returned, or, if the requested phase is the last phase, the total number of nodes is returned instead.
   *
   * @param phase the phase in which to search
   * @return the index of the first prepared transformer in the phase, or the index of the last node in the phase + 1 if
   *         there are not prepared transformers in the phase
   */
  public int firstPreparedTransformerInPhase(int phase) {
    int index = firstNodeInPhase(phase);
    while (index < _phases.length && _phases[index] == phase && !(_nodes[index] instanceof PreparedTransformer)) {
      index++;
    }
    return index;
  }

  @Override
  public Set<Producer<?>> nodes() {
    return _childrenMap.keySet();
  }

  @Override
  public List<? extends ChildProducer<?>> children(Producer<?> vertex) {
    return _childrenMap.get(vertex);
  }

  @Override
  public List<? extends Producer<?>> parents(Producer<?> vertex) {
    assert _childrenMap.containsKey(vertex);
    return vertex instanceof ChildProducer ? ((ChildProducer<?>) vertex).internalAPI().getInputList()
        : Collections.emptyList();
  }

  /**
   * No-op prepared transformer that is used to help determine the equality and hash codes of DAGStructures.
   */
  @ValueEquality
  private static class EqualityLeaf extends AbstractPreparedTransformerDynamic<Void, EqualityLeaf> {
    private static final long serialVersionUID = 1L;

    public EqualityLeaf(List<? extends Producer<?>> inputs) {
      super(inputs);
    }

    @Override
    protected Void apply(List values) {
      return null;
    }
  }

  private EqualityLeaf createEqualityDAG() {
    EqualityLeaf equalityDAG = new EqualityLeaf(_outputs);
    IdentityHashMap<Producer<?>, Producer<?>> placeholderMap = new IdentityHashMap<>(_placeholders.size());
    for (int i = 0; i < _placeholders.size(); i++) {
      placeholderMap.put(_placeholders.get(i), new PositionPlaceholder<>(i));
    }
    return DAGUtil.replaceInputs(equalityDAG, placeholderMap);
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof DAGStructure)) {
      return false;
    }
    DAGStructure<?> other = (DAGStructure<?>) obj;

    // A DAGStructure is equal to another if their graphs and numbers of placeholders are equal
    return this._placeholders.size() == other._placeholders.size()
        && this._equalityDAG.equals(other._equalityDAG);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this._placeholders.size(), _equalityDAG);
  }

  public Object[] createExecutionStateArray(long count) {
    Object[] states = new Object[_nodes.length];
    for (int i = _placeholders.size() + _generators.size(); i < states.length; i++) {
      states[i] = ((PreparedTransformer<?>) _nodes[i]).internalAPI().createExecutionCache(count);
    }
    return states;
  }

  private static String intSequenceString(int[] vals) {
    return Arrays.stream(vals).mapToObj(Integer::toString).collect(Collectors.joining(","));
  }

  public String toProducerTable() {
    final String format = "%-5s%-35s%-25s%-25s\n";
    StringBuilder builder = new StringBuilder();
    builder.append(String.format(format, "ID", "Name", "Children", "Parents"));

    for (int i = 0; i < 85; i++) {
      builder.append('-');
    }
    builder.append('\n');

    for (int i = 0; i < _nodes.length; i++) {
      builder.append(String.format(format, i, _nodes[i].getName(), intSequenceString(_children[i]),
          intSequenceString(_parents[i])));
    }

    return builder.toString();
  }

  /**
   * Returns a stream of the producers in the DAG as discovered by a breadth-first search starting from the outputs
   * (producers with a lower distance to the outputs will be returned first).
   *
   * The producers are provided as {@link LinkedStack}s, each representing a shortest-path from that producer to one of
   * the DAG's outputs (with the top of the stack, accessible via {@link LinkedStack#peek()}, being the producer of
   * interest, and the last/bottom element in the stack being an output node).  Each producer (and path to that
   * producer) will be enumerated only once, even if multiple shortest-paths exist.
   *
   * {@link Placeholder}s that are disconnected from the outputs will not be included in the returned stream.
   *
   * @return a stream of {@link LinkedStack}s representing paths to each connected producer in the DAG
   */
  public Stream<LinkedStack<Producer<?>>> producers() {
    return Producer.subgraphProducers(_outputs);
  }

  /**
   * Gets the values for all outputs of this DAG that are {@link Constant}s; the values for non-{@link Constant} outputs
   * will be {@code null}.
   *
   * Often a graph can be reduced such that all outputs that can be determined independently of the values provided by
   * {@link com.linkedin.dagli.placeholder.Placeholder}s are replaced by their pre-computed values in the form of
   * {@link Constant}s, but this is dependent upon the level of reduction applied to the DAG.
   *
   * Note that, as a {@link Constant} may itself have a null value, it is not possible to determine which outputs are
   * {@link Constant} solely from the value returned by this method.
   *
   * @return the constant output values of this DAG
   */
  @SuppressWarnings("unchecked") // R guaranteed to be the right output type or tuple-of-outputs type by DAG semantics
  public R getConstantOutput() {
    // some compilers will object if we don't cast Producer<?> to Producer<Object> prior to Constant.tryGetValue(...),
    // although it should not be necessary:
    Iterator<?> constantOutput = _outputs.stream().map(p -> Constant.tryGetValue((Producer<Object>) p)).iterator();
    if (_outputs.size() == 1) {
      return (R) constantOutput.next();
    } else {
      return (R) Tuple.generator(_outputs.size()).fromIterator(constantOutput);
    }
  }
}
