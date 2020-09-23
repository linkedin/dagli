package com.linkedin.dagli.dag;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.preparer.AbstractStreamPreparer1;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.transformer.AbstractPreparedTransformer1;
import com.linkedin.dagli.preparer.PreparerContext;
import com.linkedin.dagli.preparer.PreparerResult;
import com.linkedin.dagli.transformer.AbstractPreparableTransformer1;
import com.linkedin.dagli.transformer.PreparableTransformer;
import com.linkedin.dagli.transformer.Transformer;
import com.linkedin.dagli.view.AbstractTransformerView;


public class DelayedIdentity<T> extends AbstractPreparableTransformer1<T, T, DelayedIdentity.Prepared<T>, DelayedIdentity<T>> {
  private static final long serialVersionUID = 1;

  private final int _delayInMilliseconds;

  public DelayedIdentity<T> withInput(Producer<? extends T> input) {
    return super.withInput1(input);
  }

  @Override
  protected boolean hasIdempotentPreparer() {
    return true;
  }

  @Override
  protected boolean computeEqualsUnsafe(DelayedIdentity<T> other) {
    return Transformer.sameInputs(this, other) && this._delayInMilliseconds == other._delayInMilliseconds;
  }

  @Override
  protected int computeHashCode() {
    return Transformer.hashCodeOfInputs(this) + Integer.hashCode(_delayInMilliseconds);
  }

  public DelayedIdentity(int delayInMilliseconds) {
    _delayInMilliseconds = delayInMilliseconds;
  }

  @Override
  public Preparer<T> getPreparer(PreparerContext context) {
    return new Preparer<>(_delayInMilliseconds);
  }

  public static class Preparer<T> extends AbstractStreamPreparer1<T, T, Prepared<T>> {
    private final int _delayInNanoseconds;
    private long _processedCount = 0;

    public Preparer(int delayInNanoseconds) {
      _delayInNanoseconds = delayInNanoseconds;
    }

    @Override
    public PreparerResult<Prepared<T>> finish() {
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

    public DelayedIdentity.Prepared<T> withInput(Producer<? extends T> input) {
      return super.withInput1(input);
    }

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
