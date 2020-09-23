package com.linkedin.dagli.transformer;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.objectio.ObjectReader;
import com.linkedin.dagli.preparer.AbstractBatchPreparerDynamic;
import com.linkedin.dagli.preparer.AbstractStreamPreparerDynamic;
import com.linkedin.dagli.preparer.Preparer1;
import com.linkedin.dagli.preparer.PreparerContext;
import com.linkedin.dagli.preparer.PreparerDynamic;
import com.linkedin.dagli.preparer.PreparerMode;
import com.linkedin.dagli.preparer.PreparerResult;
import com.linkedin.dagli.preparer.PreparerResultMixed;
import com.linkedin.dagli.producer.MissingInput;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


/**
 * Wraps a transformer that accepts a single object array input as a dynamic transformer with an input for each array
 * element.
 *
 * @param <R> the type of result produced by the transformer
 */
@ValueEquality
class ArrayTransformerAsDynamic<R>
    extends AbstractPreparableTransformerDynamic<R, PreparedTransformerDynamic<R>, ArrayTransformerAsDynamic<R>> {
  private static final long serialVersionUID = 1;

  private final PreparableTransformer1<Object[], R, ? extends PreparedTransformer1<Object[], R>> _wrapped;

  public ArrayTransformerAsDynamic(int arity,
      PreparableTransformer1<Object[], R, ? extends PreparedTransformer1<Object[], R>> preparableTransformer) {
    super(IntStream.range(0, arity).mapToObj(i -> MissingInput.get()).collect(Collectors.toList()));
    _wrapped = preparableTransformer;
  }

  @Override
  protected PreparerDynamic<R, PreparedTransformerDynamic<R>> getPreparer(PreparerContext context) {
    Preparer1<Object[], R, ? extends PreparedTransformer1<Object[], R>> preparer =
        _wrapped.internalAPI().getPreparer(context);
    return preparer.getMode() == PreparerMode.STREAM ? new StreamPreparer<>(getInputList().size(), preparer)
        : new BatchPreparer<>(getInputList().size(), preparer);
  }

  /**
   * Given the preparation result from a wrapped preparer, wraps each prepared transformer with a {@link Prepared}
   * instance to lift it to dynamic arity.
   *
   * @param arity the arity of the dynamic transformer wrapper to be created
   * @param prepResult the result of preparing the preparer wrapped by this class
   * @param <R> the result type of this transformer
   * @return a {@link PreparerResult} that effectively converts the original prepared transformers to dynamic arity
   */
  private static <R> PreparerResult<PreparedTransformerDynamic<R>> makePrepared(
      int arity,
      PreparerResultMixed<? extends PreparedTransformer1<? super Object[], ? extends R>, ? extends PreparedTransformer1<Object[], R>> prepResult) {
    return new PreparerResult.Builder<PreparedTransformerDynamic<R>>()
        .withTransformerForNewData(new Prepared<>(arity, prepResult.getPreparedTransformerForNewData()))
        .withTransformerForPreparationData(new Prepared<R>(arity, prepResult.getPreparedTransformerForPreparationData()))
        .build();
  }

  /**
   * Prepares a batch-mode wrapped preparer.
   *
   * @param <R> the result type of the resulting prepared transformer
   */
  private static class BatchPreparer<R> extends AbstractBatchPreparerDynamic<R, PreparedTransformerDynamic<R>> {
    private Preparer1<? super Object[], R, ? extends PreparedTransformer1<Object[], R>> _preparer;
    private int _arity;

    BatchPreparer(int arity, Preparer1<? super Object[], R, ? extends PreparedTransformer1<Object[], R>> preparer) {
      _preparer = preparer;
      _arity = arity;
    }

    @Override
    public void processUnsafe(Object[] values) {
      _preparer.process(values);
    }

    @Override
    public PreparerResult<PreparedTransformerDynamic<R>> finishUnsafe(
        ObjectReader<Object[]> inputs) {
      return makePrepared(_arity,  _preparer.finish(ObjectReader.cast(inputs)));
    }
  }

  /**
   * Prepares a stream-mode wrapped preparer.
   *
   * @param <R> the result type of the resulting prepared transformer
   */
  private static class StreamPreparer<R> extends AbstractStreamPreparerDynamic<R, PreparedTransformerDynamic<R>> {
    private Preparer1<? super Object[], ? extends R, ? extends PreparedTransformer1<Object[], R>> _preparer;
    private int _arity;

    StreamPreparer(int arity, Preparer1<? super Object[], ? extends R, ? extends PreparedTransformer1<Object[], R>> preparer) {
      _preparer = preparer;
      _arity = arity;
    }

    @Override
    public void processUnsafe(Object[] values) {
      _preparer.process(values);
    }

    @Override
    public PreparerResult<PreparedTransformerDynamic<R>> finish() {
      return makePrepared(_arity,  _preparer.finish(null));
    }
  }

  /**
   * Wraps a prepared transformer that accepts an array of objects as a dynamic-arity prepared transformer.
   *
   * @param <R>
   */
  @ValueEquality
  static class Prepared<R> extends AbstractPreparedTransformerDynamic<R, Prepared<R>> {
    private static final long serialVersionUID = 1;

    final PreparedTransformer1<? super Object[], ? extends R> _wrapped;

    Prepared(int arity, PreparedTransformer1<? super Object[], ? extends R> prepared) {
      super(IntStream.range(0, arity).mapToObj(i -> MissingInput.get()).collect(Collectors.toList()));
      _wrapped = prepared;
    }

    @Override
    protected R apply(List<?> values) {
      return _wrapped.apply(values.toArray());
    }
  }
}
