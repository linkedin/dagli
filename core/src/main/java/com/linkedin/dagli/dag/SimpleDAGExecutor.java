package com.linkedin.dagli.dag;

import com.linkedin.dagli.generator.Constant;
import com.linkedin.dagli.generator.Generator;
import com.linkedin.dagli.objectio.biglist.BigListWriter;
import com.linkedin.dagli.objectio.ConcatenatedReader;
import com.linkedin.dagli.objectio.ConstantReader;
import com.linkedin.dagli.objectio.ObjectIterator;
import com.linkedin.dagli.objectio.ObjectReader;
import com.linkedin.dagli.objectio.ObjectWriter;
import com.linkedin.dagli.placeholder.Placeholder;
import com.linkedin.dagli.preparer.Preparer;
import com.linkedin.dagli.preparer.PreparerContext;
import com.linkedin.dagli.preparer.PreparerResult;
import com.linkedin.dagli.preparer.PreparerResultMixed;
import com.linkedin.dagli.producer.ChildProducer;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.transformer.PreparableTransformer;
import com.linkedin.dagli.transformer.PreparedTransformer;
import com.linkedin.dagli.transformer.Transformer;
import com.linkedin.dagli.util.invariant.Arguments;
import com.linkedin.dagli.view.TransformerView;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;


/**
 * A Simple(r) DAG executor for training and inference on DAGs.  The {@link SimpleDAGExecutor} is effectively a
 * reference implementation and can be used for debugging and checking correctness relative to other executors.
 *
 * A consequence of its simplicity is that it's single-threaded and thus relatively slow vs.
 * {@link MultithreadedDAGExecutor} when the machine has multiple logical processors.  Normally this executor would not
 * be used outside of testing.
 */
public final class SimpleDAGExecutor extends AbstractDAGExecutor<SimpleDAGExecutor> implements DAGExecutor {
  private static final long serialVersionUID = 1L;

  @Override
  public int hashCode() {
    return SimpleDAGExecutor.class.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof SimpleDAGExecutor;
  }

  private static ObjectReader<Object> generateIterable(long inputSize, Generator<?> generator) {
    if (inputSize == 0) {
      return ObjectReader.empty();
    }

    BigListWriter<Object> bbal = new BigListWriter<>(inputSize);

    Object[] buffer = new Object[(int) Math.min(4096, inputSize)];
    for (long i = 0; i < inputSize; i++) {
      int bufferIndex = (int) (i % buffer.length);
      buffer[bufferIndex] = generator.generate(i);
      if (bufferIndex == buffer.length - 1) {
        bbal.write(buffer, 0, buffer.length);
      }
    }

    bbal.write(buffer, 0, (int) (inputSize % buffer.length));

    assert bbal.size64() == inputSize;
    return bbal.createReader();
  }

  private <T> PreparedTransformer<?> transformerWithNewInputs(List<? extends Producer<?>> transformerInputs,
      PreparedTransformer<T> preparedTransformer, HashMap<Producer<?>, Producer<?>> producerMap) {

    //PreparedTransformer<T> preparedTransformer =
    //    originalPreparedTransformer.internalAPI().withProgenitorHandleUnsafe(originalPreparedTransformer.getHandle());

    if (transformerInputs.stream().anyMatch(input -> producerMap.get(input) != input)) {
      PreparedTransformer<T> res = preparedTransformer.internalAPI().withInputsUnsafe(
          transformerInputs.stream().map(producerMap::get).collect(Collectors.toList()));

      LogManager.getLogger()
          .trace(() -> "Pre-prepared transformer " + preparedTransformer.toString()
              + " requires new, prepared inputs, became " + res.toString());

      return res;
    } else {
      return preparedTransformer;
    }
  }

  @Override
  protected <R, T extends PreparedDAGTransformer<R, T>> ObjectReader<?>[] applyUnsafeImpl(T dag,
      ObjectReader<Object>[] inputValueLists) {
    return prepareAndApply(dag, inputValueLists).getOutputs();
  }

  @Override
  protected <R, N extends PreparedDAGTransformer<R, N>, T extends PreparableDAGTransformer<R, N, T>> DAGExecutionResult<R, N>
  prepareAndApplyUnsafeImpl(T dag, ObjectReader<Object>[] inputValueLists) {
    return (DAGExecutionResult<R, N>) prepareAndApply(dag, inputValueLists);
  }

  private <R, T extends DAGTransformer<R, T>> DAGExecutionResult<R, ?>
  prepareAndApply(T dag, ObjectReader<Object>[] inputValueLists) {
    HashMap<Producer<?>, ObjectReader<?>> cache = new HashMap<>();
    HashMap<Producer<?>, Producer<?>> preparedForNewDataProducerMap = new HashMap<>();
    HashMap<Producer<?>, Producer<?>> preparedForPreparationDataProducerMap = new HashMap<>();

    long inputSize = inputValueLists[0].size64();
    DAGStructure<R> dagStructure = dag.internalAPI().getDAGStructure();

    for (int i = 0; i < inputValueLists.length; i++) {
      Arguments.check(inputValueLists[i].size64() == inputSize);
      cache.put(dagStructure._placeholders.get(i), inputValueLists[i]);
    }

    // placeholders are intrinsically "prepared"
    for (Placeholder<?> placeholder : dagStructure._placeholders) {
      preparedForNewDataProducerMap.put(placeholder, placeholder);
      preparedForPreparationDataProducerMap.put(placeholder, placeholder);
    }

    HashMap<ChildProducer<?>, Set<ChildProducer<?>>> unsatisfiedDependencies =
        new HashMap<>(dagStructure._childrenMap.size());
    LinkedList<ChildProducer<?>> queue = new LinkedList<>();

    for (Producer<?> producer : dagStructure._childrenMap.keySet()) {
      if (producer instanceof Generator<?>) {
        Generator<?> generator = (Generator<?>) producer;

        // like placeholders, generators are intrinsically "prepared"
        preparedForNewDataProducerMap.put(generator, generator);
        preparedForPreparationDataProducerMap.put(generator, generator);

        // and their values can be generated immediately
        cache.put(generator, generateIterable(inputSize, generator));
      } else if (producer instanceof ChildProducer<?>) {
        ChildProducer<?> child = (ChildProducer<?>) producer;
        Set<ChildProducer<?>> dependencies = child.internalAPI().getInputList()
            .stream()
            .filter(p -> p instanceof ChildProducer<?>)
            .map(p -> (ChildProducer<?>) p)
            .collect(Collectors.toSet());
        if (dependencies.isEmpty()) {
          queue.push(child);
        } else {
          unsatisfiedDependencies.put(child, dependencies);
        }
      }
    }

    while (!queue.isEmpty()) {
      ChildProducer<?> producer = queue.pop();

      List<? extends Producer<?>> parents = producer.internalAPI().getInputList();
      List<ObjectReader<?>> args =
          parents.stream().map(cache::get).collect(Collectors.toList());


      final ObjectReader<Object> results;

      if (producer instanceof Transformer<?>) {
        final PreparedTransformer<?> preparedForNewData;
        final PreparedTransformer<?> preparedForPreparationData;

        if (producer instanceof PreparedTransformer<?>) {
          PreparedTransformer<?> preparedTransformer = (PreparedTransformer<?>) producer;

          preparedForNewData = transformerWithNewInputs(parents, preparedTransformer, preparedForNewDataProducerMap);

          preparedForPreparationData =
              transformerWithNewInputs(parents, preparedTransformer, preparedForPreparationDataProducerMap);
        } else if (producer instanceof PreparableTransformer<?, ?>) {
          PreparableTransformer<?, ?> preparableTransformer = (PreparableTransformer<?, ?>) producer;
          Preparer<?, ?> transformerPreparer = preparableTransformer.internalAPI()
              .getPreparer(PreparerContext.builder(inputSize).setExecutor(this).build());

          ObjectIterator<?>[] iterators = args.stream().map(ObjectReader::iterator).toArray(ObjectIterator[]::new);
          for (long i = 0; i < inputSize; i++) {
            Object[] objs = new Object[args.size()];
            for (int j = 0; j < parents.size(); j++) {
              objs[j] = iterators[j].next();
            }
            transformerPreparer.processUnsafe(objs);
          }

          PreparerResultMixed<? extends PreparedTransformer<?>, ? extends PreparedTransformer<?>> preparerResult =
              transformerPreparer.finishUnsafe(
                  new ConcatenatedReader<>(Object[]::new, args.toArray(new ObjectReader[0])));

          List<Producer<?>> preparedInputsForNewData =
              parents.stream().map(preparedForNewDataProducerMap::get).collect(Collectors.toList());
          List<Producer<?>> preparedInputsForPreparationData =
              parents.stream().map(preparedForPreparationDataProducerMap::get).collect(Collectors.toList());

          preparedForNewData = preparerResult.getPreparedTransformerForNewData()
              .internalAPI()
              .withInputsUnsafe(preparedInputsForNewData);

          preparedForPreparationData = preparerResult.getPreparedTransformerForPreparationData()
              .internalAPI()
              .withInputsUnsafe(preparedInputsForPreparationData);

          if (LogManager.getLogger().getLevel().equals(Level.TRACE)) {
            assert (preparedForNewData.internalAPI().getInputList().size() == preparedInputsForNewData.size());
            for (int i = 0; i < preparedForNewData.internalAPI().getInputList().size(); i++) {
              if (preparedForNewData.internalAPI().getInputList().get(i) != preparedInputsForNewData.get(i)) {
                throw new IllegalStateException("Input mismatch while processing transformer " + producer.toString());
              }
            }
          }
        } else {
          throw new IllegalArgumentException("Unknown transformer type");
        }

        ObjectWriter<Object> resultsAccumulator = new BigListWriter<>(inputSize);

        ObjectIterator<Object>[] iterators = args.stream().map(ObjectReader::iterator).toArray(ObjectIterator[]::new);

        Object executionState = preparedForPreparationData.internalAPI().createExecutionCache(inputSize);

        long remaining = inputSize;

        while (remaining > 0) {
          int batchSize = (int) Math.min(remaining, Integer.MAX_VALUE - 8); // limit to safe-ish max array size
          remaining -= batchSize;

          // yes, we could reuse arrays across batches, but it is *highly* unlikely this executor will ever be applied
          // with more than a single batch worth of examples
          Object[][] objs = new Object[iterators.length][batchSize];
          for (int j = 0; j < parents.size(); j++) {
            iterators[j].next(objs[j], 0, batchSize);
          }
          Object[] resultArray = new Object[batchSize];
          preparedForPreparationData.internalAPI().applyAllUnsafe(executionState, batchSize, objs, resultArray);
          resultsAccumulator.writeAll(resultArray);
        }

        results = resultsAccumulator.createReader();

        // check all ancestors for unprepared transformers
        if (LogManager.getLogger().getLevel().equals(Level.TRACE)) {
          HashSet<PreparedTransformer<?>> seen = new HashSet<>();
          LinkedList<PreparedTransformer> toCheck = new LinkedList<>();
          toCheck.add(preparedForNewData);
          seen.add(preparedForNewData);

          while (!toCheck.isEmpty()) {
            PreparedTransformer next = toCheck.pop();
            for (Object parent : next.internalAPI().getInputList()) {
              if (parent instanceof PreparableTransformer) {
                LogManager.getLogger().error(
                    "ERROR!: " + preparedForNewData.toString() + " has non-prepared ancestor: " + parent.toString());
              } else if (parent instanceof PreparedTransformer) {
                if (!seen.contains(parent)) {
                  toCheck.add((PreparedTransformer) parent);
                  seen.add((PreparedTransformer) parent);
                }
              }
            }
          }
        }

        preparedForNewDataProducerMap.put(producer, preparedForNewData);
        preparedForPreparationDataProducerMap.put(producer, preparedForPreparationData);
      } else if (producer instanceof TransformerView<?, ?>) {
        TransformerView view = (TransformerView<?, ?>) producer;
        assert parents.size() == 1;
        PreparedTransformer<?> parentPreparedForNewData =
            (PreparedTransformer<?>) preparedForNewDataProducerMap.get(parents.get(0));
        PreparedTransformer<?> parentPreparedForPreparationData =
            (PreparedTransformer<?>) preparedForPreparationDataProducerMap.get(parents.get(0));
        Object valueForNewData = view.internalAPI().prepare(parentPreparedForNewData);
        Object valueForPreparationData =
            view.internalAPI().prepareForPreparationData(parentPreparedForPreparationData, parentPreparedForNewData);
        preparedForNewDataProducerMap.put(view, new Constant<>(valueForNewData));
        preparedForPreparationDataProducerMap.put(view, new Constant<>(valueForPreparationData));
        results = new ConstantReader<Object>(valueForPreparationData, inputSize);
      } else {
        throw new IllegalArgumentException("Unknown ChildProducer type");
      }

      cache.put(producer, results);

      for (ChildProducer<?> child : dagStructure._childrenMap.get(producer)) {
        Set<ChildProducer<?>> dependencies = unsatisfiedDependencies.get(child);
        dependencies.remove(producer);
        if (dependencies.isEmpty()) {
          queue.add(child);
        }
      }
    }

    PreparedTransformer<R> preparedForNewDataDAG;
    PreparedTransformer<R> preparedForPreparationDataDAG;
    if (dag instanceof PreparedDAGTransformer) {
      preparedForNewDataDAG = (PreparedDAGTransformer<R, ?>) dag;
      preparedForPreparationDataDAG = (PreparedDAGTransformer<R, ?>) dag;
    } else {
      PreparableDAGTransformer<R, ?, ?> preparableDAG = (PreparableDAGTransformer<R, ?, ?>) dag;
      preparedForNewDataDAG = preparableDAG.internalAPI().createPreparedDAG(dagStructure._placeholders,
          dagStructure._outputs.stream().map(preparedForNewDataProducerMap::get).collect(Collectors.toList()));
      preparedForPreparationDataDAG = preparableDAG.internalAPI().createPreparedDAG(dagStructure._placeholders,
          dagStructure._outputs.stream().map(preparedForPreparationDataProducerMap::get).collect(Collectors.toList()));
    }

    ObjectReader<?>[] resList = dagStructure._outputs.stream().map(cache::get).toArray(ObjectReader[]::new);
    return new DAGExecutionResult(
        new PreparerResult.Builder<>().withTransformerForNewData(preparedForNewDataDAG)
            .withTransformerForPreparationData(preparedForPreparationDataDAG)
            .build(), resList);
  }
}
