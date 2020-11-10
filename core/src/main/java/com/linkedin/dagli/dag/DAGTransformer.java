package com.linkedin.dagli.dag;

import com.linkedin.dagli.generator.Generator;
import com.linkedin.dagli.placeholder.Placeholder;
import com.linkedin.dagli.producer.ChildProducer;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.reducer.Reducer;
import com.linkedin.dagli.transformer.PreparableTransformer;
import com.linkedin.dagli.transformer.Transformer;
import com.linkedin.dagli.transformer.internal.TransformerInternalAPI;
import com.linkedin.dagli.util.collection.LinkedStack;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * Interface that is shared and exclusively implemented by {@code DAGXxY} classes.
 */
public interface DAGTransformer<R, S extends DAGTransformer<R, S>> extends Transformer<R> {

  interface InternalAPI<R, S extends DAGTransformer<R, S>> extends TransformerInternalAPI<R, S> {
    DAGStructure<R> getDAGStructure();

    default Producer<?> getOutputProducer(int index) {
      return getDAGStructure()._outputs.get(index);
    }

    /**
     * Gets the level of reduction that has been applied to this DAG, or null if no reducers have been applied and the
     * DAG is unreduced.
     *
     * @return the level of reduction that has been applied to this DAG
     */
    Reducer.Level getReductionLevel();

    /**
     * @return the executor used by the DAG
     */
    PreparedDAGExecutor getDAGExecutor();

    /**
     * @return the instance corresponding to this internal API
     */
    S getInstance();
  }

  /**
   * @return the internal API for this DAG; not part of the public API and may change at any time
   */
  InternalAPI<R, ?> internalAPI();

  /**
   * Returns a DAG derived from this one that has been reduced to at least the specified level.
   *
   * If level is less than this DAG's current reduction level, {@code this} DAG is returned.
   *
   * Otherwise, this DAG is reduced, and the new, reduced DAG is returned.
   *
   * @param level the level of reduction desired; all reducers at or above this level will be applied
   * @return a DAG that has been reduced to at least the specified level (possibly this same DAG)
   */
  S withReduction(Reducer.Level level);

  /**
   * @return a String that summarizes the structure of this DAG as a table that may be useful for logging or debugging
   */
  default String toGraphSummary() {
    return internalAPI().getDAGStructure().toProducerTable();
  }

  /**
   * Gets the Graph describing this instance.  This method should be minimal cost to allow for code like:
   * <code>instance.graph().doSomething()</code>
   * (i.e. clients are not expected to cache the return value themselves.)
   *
   * Note: graphs in Dagli are often optimized, so the graph returned by this method may be different than the
   * graph you expect, although it will be functionally equivalent.  For example, if you create a DAG like so:
   * <pre>{@code
   *   Placeholder<String> placeholder = new Placeholder<>();
   *   LowerCased<String> lowerCased = new LowerCased().withInput(placeholder);
   *   UpperCased<String> upperCased = new UpperCased().withInput(placeholder);
   *   DAG1x1.Prepared<String, String> dag = DAG.Prepared.withPlaceholder(placeholder).withOutput(lowerCased)
   * }</pre>
   * dag.graph() will not contain the "upperCased" node because it's not needed to compute the DAG's output.
   *
   * @return a {@link Graph} object that describes the graph structure of this instance.
   */
  default Graph<Producer<?>> graph() {
    return internalAPI().getDAGStructure();
  }

  /**
   * Gets the values for all outputs of this DAG that are {@link com.linkedin.dagli.generator.Constant}s; the values for
   * non-{@link com.linkedin.dagli.generator.Constant} outputs will be {@code null}.
   *
   * Often a graph can be reduced such that all outputs that can be determined independently of the values provided by
   * {@link com.linkedin.dagli.placeholder.Placeholder}s are replaced by their pre-computed values in the form of
   * {@link com.linkedin.dagli.generator.Constant}s, but this is dependent upon the level of reduction applied to the
   * DAG; {@link #withReduction(Reducer.Level)} may be used to ensure a sufficient level of reduction before invoking
   * this method.
   *
   * Note that, as a {@link com.linkedin.dagli.generator.Constant} may itself have a null value, it is not possible to
   * determine which outputs are {@link com.linkedin.dagli.generator.Constant} solely from the value returned by this
   * method.
   *
   * @return the constant output values of this DAG
   */
  default R getConstantResult() {
    return internalAPI().getDAGStructure().getConstantOutput();
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
  default Stream<LinkedStack<Producer<?>>> producers() {
    return internalAPI().getDAGStructure().producers();
  }

  /**
   * Returns a stream of the producers in the DAG of a particular class, as discovered by a breadth-first search
   * starting from the outputs (producers with a lower distance to the outputs will be returned first).
   *
   * The producers are provided as {@link LinkedStack}s, each representing a shortest-path from that producer to one of
   * the DAG's outputs (with the top of the stack, accessible via {@link LinkedStack#peek()}, being the producer of
   * interest, and the last/bottom element in the stack being an output node).  Each producer (and path to that
   * producer) will be enumerated only once, even if multiple shortest-paths exist.
   *
   * {@link Placeholder}s that are disconnected from the outputs will not be included in the returned stream.
   *
   * @param producerClass the class of producer to be sought
   * @param <T> the type of the producer to be enumerated
   * @return a stream of {@link LinkedStack}s representing paths to each connected producer in the DAG
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  default <T> Stream<LinkedStack<T>> producers(Class<T> producerClass) {
    return (Stream) internalAPI().getDAGStructure().producers().filter(stack -> producerClass.isInstance(stack.peek()));
  }

  /**
   * Creates a DAG from the provided {@code producer} and its ancestors.  There must be no more than 10 placeholders in
   * {@code producer}'s ancestry.  The graph's sole output will be {@code producer}.  The graph will be preparable
   * if it contains at least one preparable transformer.
   *
   * @param producer the producer from which to construct the DAG
   * @param <R> the type of result of {@code producer}
   * @return a DAG with a single output
   */
  static <R> DAGTransformer<R, ?> withOutput(Producer<R> producer) {
    if (producer instanceof Placeholder) {
      return DAG.withPlaceholder((Placeholder<?>) producer).withOutput(producer);
    } else if (producer instanceof Generator) {
      return DAG.withPlaceholder(new Placeholder<>()).withOutput(producer);
    }

    ChildProducer<R> childProducer = (ChildProducer<R>) producer;

    List<Placeholder<?>> placeholders = ChildProducer.ancestors(childProducer, Integer.MAX_VALUE)
        .filter(p -> p.peek() instanceof Placeholder)
        .map(p -> (Placeholder<?>) p.peek())
        .collect(Collectors.toList());

    boolean prepared =
        !(producer instanceof PreparableTransformer) && ChildProducer.ancestors(childProducer, Integer.MAX_VALUE)
            .noneMatch(p -> p.peek() instanceof PreparableTransformer);

    return prepared ? DAGUtil.createPreparedDAG(placeholders, Collections.singletonList(producer))
        : DAGUtil.createPreparableDAG(placeholders, Collections.singletonList(producer));
  }
}
