package com.linkedin.dagli.dag;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.objectio.ObjectReader;
import com.linkedin.dagli.preparer.AbstractBatchPreparer1;
import com.linkedin.dagli.preparer.PreparerContext;
import com.linkedin.dagli.preparer.PreparerResult;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.transformer.AbstractPreparableTransformer1;
import com.linkedin.dagli.transformer.AbstractPreparedTransformer1;
import com.linkedin.dagli.transformer.PreparableTransformer;
import com.linkedin.dagli.util.invariant.Arguments;
import com.linkedin.dagli.view.AbstractTransformerView;
import java.util.Objects;


// Copy of DelayedIdentity, but with a Batch-type preparer
@ValueEquality
public class DelayedIdentityBatchPrepared<T> extends
    AbstractPreparableTransformer1<T, T, DelayedIdentityBatchPrepared.Prepared<T>, DelayedIdentityBatchPrepared<T>> {
  private static final long serialVersionUID = 1;

  private final int _delayInMilliseconds;

  public DelayedIdentityBatchPrepared(int delayInMilliseconds) {
    _delayInMilliseconds = delayInMilliseconds;
  }

  public DelayedIdentityBatchPrepared<T> withInput(Producer<? extends T> input) {
    return super.withInput1(input);
  }

  @Override
  public Preparer<T> getPreparer(PreparerContext context) {
    return new Preparer<>(_delayInMilliseconds);
  }

  public static class Preparer<T> extends AbstractBatchPreparer1<T, T, Prepared<T>> {
    private final int _delayInNanoseconds;
    private long _processedCount = 0;

    public Preparer(int delayInNanoseconds) {
      _delayInNanoseconds = delayInNanoseconds;
    }

    @Override
    public PreparerResult<Prepared<T>> finish(ObjectReader<T> items) {
      Objects.requireNonNull(items);
      Arguments.check(items.stream().findFirst().isPresent());
      //try {
        //Thread.sleep(0, _delayInNanoseconds);
      //} catch (InterruptedException e) { }
      return new PreparerResult<>(new Prepared<T>(_delayInNanoseconds, _processedCount));
    }

    @Override
    public void process(T value0) {
      _processedCount++;
      //try {
        //Thread.sleep(0, _delayInNanoseconds);
      //} catch (InterruptedException e) { }
    }
  }
  @ValueEquality
  public static class Prepared<T> extends AbstractPreparedTransformer1<T, T, Prepared<T>> {
    private static final long serialVersionUID = 1;

    public int getDelayInMilliseconds() {
      return _delayInMilliseconds;
    }

    public long getProcessedCount() {
      return _processedCount;
    }

    private final int _delayInMilliseconds;
    private final long _processedCount;

    public Prepared(int delayInMilliseconds, long processedCount) {
      _delayInMilliseconds = delayInMilliseconds;
      _processedCount = processedCount;
    }

    public Prepared(int delayInMilliseconds) {
      this(delayInMilliseconds, 0);
    }

    @Override
    public T apply(T value0) {
      //try {
        //Thread.sleep(0, _delayInNanoseconds);
      //} catch (InterruptedException e) { }
      return value0;
    }
  }

  @ValueEquality
  public static class ProcessedCountView extends AbstractTransformerView<Long, Prepared<?>, ProcessedCountView> {
    private static final long serialVersionUID = 1;

    public <T> ProcessedCountView(PreparableTransformer<T, Prepared<T>> di) {
      super(di);
    }

    @Override
    protected Long prepare(Prepared<?> preparedTransformerForNewData) {
      return preparedTransformerForNewData._processedCount;
    }
  }
}
