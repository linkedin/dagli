package com.linkedin.dagli.preparer;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.annotation.struct.HasStructField;
import com.linkedin.dagli.dag.DAGExecutor;
import com.linkedin.dagli.data.schema.RowSchema;
import com.linkedin.dagli.generator.AbstractGenerator;
import com.linkedin.dagli.objectio.ObjectIterator;
import com.linkedin.dagli.objectio.ObjectReader;
import com.linkedin.dagli.producer.MissingInput;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.reducer.InverseClassReducer;
import com.linkedin.dagli.reducer.Reducer;
import com.linkedin.dagli.struct.Struct;
import com.linkedin.dagli.transformer.AbstractPreparedTransformer1;
import com.linkedin.dagli.transformer.AbstractPreparedTransformerDynamic;
import com.linkedin.dagli.transformer.Transformer;
import com.linkedin.dagli.util.cloneable.AbstractCloneable;
import com.linkedin.dagli.util.function.Function1;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.CharSequence;
import java.lang.CloneNotSupportedException;
import java.lang.Cloneable;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.BiFunction;

@HasStructField(
    optional = false,
    name = "estimatedExampleCount",
    type = long.class
)
@HasStructField(
    optional = false,
    name = "exampleCountLowerBound",
    type = long.class
)
@HasStructField(
    optional = false,
    name = "exampleCountUpperBound",
    type = long.class
)
@HasStructField(
    optional = false,
    name = "executor",
    type = DAGExecutor.class
)
public class PreparerContext extends PreparerContextBase implements Cloneable, Struct {
  private static final long serialVersionUID = 1L;

  protected PreparerContext() {
    super();
  }

  @Override
  public String toString() {
    return "PreparerContext(EstimatedExampleCount = " + _estimatedExampleCount + ", " + "ExampleCountLowerBound = " + _exampleCountLowerBound + ", " + "ExampleCountUpperBound = " + _exampleCountUpperBound + ", " + "Executor = " + _executor + ")";
  }

  @Override
  public boolean equals(Object other) {
    if (other == null || other.getClass() != this.getClass()) {
      return false;
    }
    PreparerContext o = (PreparerContext) other;
    return _estimatedExampleCount == o._estimatedExampleCount && _exampleCountLowerBound == o._exampleCountLowerBound && _exampleCountUpperBound == o._exampleCountUpperBound && java.util.Objects.equals(_executor, o._executor);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_estimatedExampleCount, _exampleCountLowerBound, _exampleCountUpperBound, _executor);
  }

  /**
   *  The <i>estimated</i> number of examples in this execution; estimates may be arbitrarily inaccurate but
   *  should not grossly exceed the true number of examples (i.e. this value will be suitable for pre-allocating
   *  space in data structures).
   */
  public long getEstimatedExampleCount() {
    return _estimatedExampleCount;
  }

  /**
   *  A lower bound on the number of examples in this execution, or {@code 0} if no better lower bound is known.
   */
  public long getExampleCountLowerBound() {
    return _exampleCountLowerBound;
  }

  /**
   *  An upper bound on the number of examples in this execution, or {@link Long#MAX_VALUE} if no upper bound
   *  can be established (e.g. an indefinitely long stream of exmaples).
   */
  public long getExampleCountUpperBound() {
    return _exampleCountUpperBound;
  }

  /**
   *  The DAGExecutor that is being used to prepare the DAG.  This may be useful if, e.g. a {@link Preparer} wants to use
   *  this same executor to prepare another DAG internally.
   */
  public DAGExecutor getExecutor() {
    return _executor;
  }

  public static PreparerContext fromMap(Map<? extends CharSequence, Object> map) {
    com.linkedin.dagli.preparer.PreparerContext res = new com.linkedin.dagli.preparer.PreparerContext();
    if (!map.containsKey("estimatedExampleCount") && !map.containsKey("EstimatedExampleCount")) { throw new NoSuchElementException("estimatedExampleCount"); };
    if (!map.containsKey("exampleCountLowerBound") && !map.containsKey("ExampleCountLowerBound")) { throw new NoSuchElementException("exampleCountLowerBound"); };
    if (!map.containsKey("exampleCountUpperBound") && !map.containsKey("ExampleCountUpperBound")) { throw new NoSuchElementException("exampleCountUpperBound"); };
    if (!map.containsKey("executor") && !map.containsKey("Executor")) { throw new NoSuchElementException("executor"); };
    res._estimatedExampleCount = (long) map.getOrDefault("estimatedExampleCount", map.getOrDefault("EstimatedExampleCount", res._estimatedExampleCount));
    res._exampleCountLowerBound = (long) map.getOrDefault("exampleCountLowerBound", map.getOrDefault("ExampleCountLowerBound", res._exampleCountLowerBound));
    res._exampleCountUpperBound = (long) map.getOrDefault("exampleCountUpperBound", map.getOrDefault("ExampleCountUpperBound", res._exampleCountUpperBound));
    res._executor = (DAGExecutor) map.getOrDefault("executor", map.getOrDefault("Executor", res._executor));
    return res;
  }

  @Override
  public Map<String, Object> toMap() {
    HashMap<String, Object> map = new HashMap<String, Object>(4);
    map.put("estimatedExampleCount", _estimatedExampleCount);
    map.put("exampleCountLowerBound", _exampleCountLowerBound);
    map.put("exampleCountUpperBound", _exampleCountUpperBound);
    map.put("executor", _executor);
    return map;
  }

  /**
   *  The <i>estimated</i> number of examples in this execution; estimates may be arbitrarily inaccurate but
   *  should not grossly exceed the true number of examples (i.e. this value will be suitable for pre-allocating
   *  space in data structures).
   */
  public PreparerContext withEstimatedExampleCount(long estimatedExampleCount) {
    try {
      PreparerContext res = (PreparerContext) this.clone();
      res._estimatedExampleCount = estimatedExampleCount;
      return res;
    } catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   *  A lower bound on the number of examples in this execution, or {@code 0} if no better lower bound is known.
   */
  public PreparerContext withExampleCountLowerBound(long exampleCountLowerBound) {
    try {
      PreparerContext res = (PreparerContext) this.clone();
      res._exampleCountLowerBound = exampleCountLowerBound;
      return res;
    } catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   *  An upper bound on the number of examples in this execution, or {@link Long#MAX_VALUE} if no upper bound
   *  can be established (e.g. an indefinitely long stream of exmaples).
   */
  public PreparerContext withExampleCountUpperBound(long exampleCountUpperBound) {
    try {
      PreparerContext res = (PreparerContext) this.clone();
      res._exampleCountUpperBound = exampleCountUpperBound;
      return res;
    } catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   *  The DAGExecutor that is being used to prepare the DAG.  This may be useful if, e.g. a {@link Preparer} wants to use
   *  this same executor to prepare another DAG internally.
   */
  public PreparerContext withExecutor(DAGExecutor executor) {
    try {
      PreparerContext res = (PreparerContext) this.clone();
      res._executor = executor;
      return res;
    } catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
  }

  public interface Builder {
    /**
     *  The <i>estimated</i> number of examples in this execution; estimates may be arbitrarily inaccurate but
     *  should not grossly exceed the true number of examples (i.e. this value will be suitable for pre-allocating
     *  space in data structures).
     */
    static Helper.ExampleCountLowerBound.Builder setEstimatedExampleCount(long estimatedExampleCount) {
      return new BuilderImpl().setEstimatedExampleCount(estimatedExampleCount);
    }
  }

  public static class Placeholder extends com.linkedin.dagli.placeholder.Placeholder<PreparerContext> {
    private static final long serialVersionUID = 1L;

    private transient EstimatedExampleCount _estimatedExampleCount = null;

    private transient ExampleCountLowerBound _exampleCountLowerBound = null;

    private transient ExampleCountUpperBound _exampleCountUpperBound = null;

    private transient Executor _executor = null;

    public EstimatedExampleCount asEstimatedExampleCount() {
      if (_estimatedExampleCount == null) {
        _estimatedExampleCount = new EstimatedExampleCount().withInput(this);
      }
      return _estimatedExampleCount;
    }

    public ExampleCountLowerBound asExampleCountLowerBound() {
      if (_exampleCountLowerBound == null) {
        _exampleCountLowerBound = new ExampleCountLowerBound().withInput(this);
      }
      return _exampleCountLowerBound;
    }

    public ExampleCountUpperBound asExampleCountUpperBound() {
      if (_exampleCountUpperBound == null) {
        _exampleCountUpperBound = new ExampleCountUpperBound().withInput(this);
      }
      return _exampleCountUpperBound;
    }

    public Executor asExecutor() {
      if (_executor == null) {
        _executor = new Executor().withInput(this);
      }
      return _executor;
    }
  }

  private static class BuilderImpl implements Helper.CompletedBuilder, Helper.EstimatedExampleCount.Builder, Helper.ExampleCountLowerBound.Builder, Helper.ExampleCountUpperBound.Builder, Helper.Executor.Builder {
    private PreparerContext _instance = new PreparerContext();

    public PreparerContext build() {
      return _instance;
    }

    @Override
    public Helper.ExampleCountLowerBound.Builder setEstimatedExampleCount(long estimatedExampleCount) {
      _instance._estimatedExampleCount = estimatedExampleCount;
      return this;
    }

    @Override
    public Helper.ExampleCountUpperBound.Builder setExampleCountLowerBound(long exampleCountLowerBound) {
      _instance._exampleCountLowerBound = exampleCountLowerBound;
      return this;
    }

    @Override
    public Helper.Executor.Builder setExampleCountUpperBound(long exampleCountUpperBound) {
      _instance._exampleCountUpperBound = exampleCountUpperBound;
      return this;
    }

    @Override
    public Helper.CompletedBuilder setExecutor(DAGExecutor executor) {
      _instance._executor = executor;
      return this;
    }
  }

  public static class EstimatedExampleCount extends AbstractPreparedTransformer1<PreparerContext, Long, EstimatedExampleCount> {
    private static final long serialVersionUID = 1L;

    public EstimatedExampleCount withInput(Producer<? extends PreparerContext> input) {
      return withInput1(input);
    }

    @Override
    public int computeHashCode() {
      return Transformer.hashCodeOfInputs(this);
    }

    @Override
    public boolean computeEqualsUnsafe(EstimatedExampleCount other) {
      return Transformer.sameInputs(this, other);
    }

    @Override
    protected Collection<? extends Reducer<? super EstimatedExampleCount>> getGraphReducers() {
      return Collections.singleton(new InverseClassReducer(0, Assembled.class));
    }

    @Override
    public Long apply(PreparerContext struct) {
      return struct._estimatedExampleCount;
    }
  }

  public static class ExampleCountLowerBound extends AbstractPreparedTransformer1<PreparerContext, Long, ExampleCountLowerBound> {
    private static final long serialVersionUID = 1L;

    public ExampleCountLowerBound withInput(Producer<? extends PreparerContext> input) {
      return withInput1(input);
    }

    @Override
    public int computeHashCode() {
      return Transformer.hashCodeOfInputs(this);
    }

    @Override
    public boolean computeEqualsUnsafe(ExampleCountLowerBound other) {
      return Transformer.sameInputs(this, other);
    }

    @Override
    protected Collection<? extends Reducer<? super ExampleCountLowerBound>> getGraphReducers() {
      return Collections.singleton(new InverseClassReducer(1, Assembled.class));
    }

    @Override
    public Long apply(PreparerContext struct) {
      return struct._exampleCountLowerBound;
    }
  }

  public static class ExampleCountUpperBound extends AbstractPreparedTransformer1<PreparerContext, Long, ExampleCountUpperBound> {
    private static final long serialVersionUID = 1L;

    public ExampleCountUpperBound withInput(Producer<? extends PreparerContext> input) {
      return withInput1(input);
    }

    @Override
    public int computeHashCode() {
      return Transformer.hashCodeOfInputs(this);
    }

    @Override
    public boolean computeEqualsUnsafe(ExampleCountUpperBound other) {
      return Transformer.sameInputs(this, other);
    }

    @Override
    protected Collection<? extends Reducer<? super ExampleCountUpperBound>> getGraphReducers() {
      return Collections.singleton(new InverseClassReducer(2, Assembled.class));
    }

    @Override
    public Long apply(PreparerContext struct) {
      return struct._exampleCountUpperBound;
    }
  }

  public static class Executor extends AbstractPreparedTransformer1<PreparerContext, DAGExecutor, Executor> {
    private static final long serialVersionUID = 1L;

    public Executor withInput(Producer<? extends PreparerContext> input) {
      return withInput1(input);
    }

    @Override
    public int computeHashCode() {
      return Transformer.hashCodeOfInputs(this);
    }

    @Override
    public boolean computeEqualsUnsafe(Executor other) {
      return Transformer.sameInputs(this, other);
    }

    @Override
    protected Collection<? extends Reducer<? super Executor>> getGraphReducers() {
      return Collections.singleton(new InverseClassReducer(3, Assembled.class));
    }

    @Override
    public DAGExecutor apply(PreparerContext struct) {
      return struct._executor;
    }
  }

  public static class Helper {
    public interface CompletedBuilder {
      PreparerContext build();
    }

    public interface CompletedAssembledBuilder {
      Assembled build();
    }

    public interface CompletedReaderBuilder {
      Reader build();
    }

    public interface CompletedSchemaBuilder {
      Schema build();
    }

    public static class EstimatedExampleCount {
      public interface Builder {
        /**
         *  The <i>estimated</i> number of examples in this execution; estimates may be arbitrarily inaccurate but
         *  should not grossly exceed the true number of examples (i.e. this value will be suitable for pre-allocating
         *  space in data structures).
         */
        ExampleCountLowerBound.Builder setEstimatedExampleCount(long estimatedExampleCount);
      }

      public interface AssembledBuilder {
        ExampleCountLowerBound.AssembledBuilder setEstimatedExampleCount(Producer<? extends Long> estimatedExampleCount);
      }

      public interface ReaderBuilder {
        ExampleCountLowerBound.ReaderBuilder setEstimatedExampleCount(ObjectReader<? extends Long> estimatedExampleCount);
      }

      public interface SchemaBuilder {
        ExampleCountLowerBound.SchemaBuilder setEstimatedExampleCountColumnIndex(int estimatedExampleCountColumnIndex,
            Function1<String, Long> parser);

        ExampleCountLowerBound.SchemaBuilder setEstimatedExampleCountColumnName(String estimatedExampleCountColumnName,
            Function1<String, Long> parser);

        ExampleCountLowerBound.SchemaBuilder setEstimatedExampleCountParser(BiFunction<String[], String[], Long> parser);

        ExampleCountLowerBound.SchemaBuilder setEstimatedExampleCountColumnIndex(int estimatedExampleCountColumnIndex);

        ExampleCountLowerBound.SchemaBuilder setEstimatedExampleCountColumnName(String estimatedExampleCountColumnName);
      }
    }

    public static class ExampleCountLowerBound {
      public interface Builder {
        /**
         *  A lower bound on the number of examples in this execution, or {@code 0} if no better lower bound is known.
         */
        ExampleCountUpperBound.Builder setExampleCountLowerBound(long exampleCountLowerBound);
      }

      public interface AssembledBuilder {
        ExampleCountUpperBound.AssembledBuilder setExampleCountLowerBound(Producer<? extends Long> exampleCountLowerBound);
      }

      public interface ReaderBuilder {
        ExampleCountUpperBound.ReaderBuilder setExampleCountLowerBound(ObjectReader<? extends Long> exampleCountLowerBound);
      }

      public interface SchemaBuilder {
        ExampleCountUpperBound.SchemaBuilder setExampleCountLowerBoundColumnIndex(int exampleCountLowerBoundColumnIndex,
            Function1<String, Long> parser);

        ExampleCountUpperBound.SchemaBuilder setExampleCountLowerBoundColumnName(String exampleCountLowerBoundColumnName,
            Function1<String, Long> parser);

        ExampleCountUpperBound.SchemaBuilder setExampleCountLowerBoundParser(BiFunction<String[], String[], Long> parser);

        ExampleCountUpperBound.SchemaBuilder setExampleCountLowerBoundColumnIndex(int exampleCountLowerBoundColumnIndex);

        ExampleCountUpperBound.SchemaBuilder setExampleCountLowerBoundColumnName(String exampleCountLowerBoundColumnName);
      }
    }

    public static class ExampleCountUpperBound {
      public interface Builder {
        /**
         *  An upper bound on the number of examples in this execution, or {@link Long#MAX_VALUE} if no upper bound
         *  can be established (e.g. an indefinitely long stream of exmaples).
         */
        Executor.Builder setExampleCountUpperBound(long exampleCountUpperBound);
      }

      public interface AssembledBuilder {
        Executor.AssembledBuilder setExampleCountUpperBound(Producer<? extends Long> exampleCountUpperBound);
      }

      public interface ReaderBuilder {
        Executor.ReaderBuilder setExampleCountUpperBound(ObjectReader<? extends Long> exampleCountUpperBound);
      }

      public interface SchemaBuilder {
        Executor.SchemaBuilder setExampleCountUpperBoundColumnIndex(int exampleCountUpperBoundColumnIndex,
            Function1<String, Long> parser);

        Executor.SchemaBuilder setExampleCountUpperBoundColumnName(String exampleCountUpperBoundColumnName,
            Function1<String, Long> parser);

        Executor.SchemaBuilder setExampleCountUpperBoundParser(BiFunction<String[], String[], Long> parser);

        Executor.SchemaBuilder setExampleCountUpperBoundColumnIndex(int exampleCountUpperBoundColumnIndex);

        Executor.SchemaBuilder setExampleCountUpperBoundColumnName(String exampleCountUpperBoundColumnName);
      }
    }

    public static class Executor {
      public interface Builder {
        /**
         *  The DAGExecutor that is being used to prepare the DAG.  This may be useful if, e.g. a {@link Preparer} wants to use
         *  this same executor to prepare another DAG internally.
         */
        CompletedBuilder setExecutor(DAGExecutor executor);
      }

      public interface AssembledBuilder {
        CompletedAssembledBuilder setExecutor(Producer<? extends DAGExecutor> executor);
      }

      public interface ReaderBuilder {
        CompletedReaderBuilder setExecutor(ObjectReader<? extends DAGExecutor> executor);
      }

      public interface SchemaBuilder {
        CompletedSchemaBuilder setExecutorColumnIndex(int executorColumnIndex,
            Function1<String, DAGExecutor> parser);

        CompletedSchemaBuilder setExecutorColumnName(String executorColumnName,
            Function1<String, DAGExecutor> parser);

        CompletedSchemaBuilder setExecutorParser(BiFunction<String[], String[], DAGExecutor> parser);
      }
    }
  }

  public static class Assembled extends AbstractPreparedTransformerDynamic<PreparerContext, Assembled> {
    private static final long serialVersionUID = 1L;

    private Producer<? extends Long> _estimatedExampleCount = MissingInput.get();

    private Producer<? extends Long> _exampleCountLowerBound = MissingInput.get();

    private Producer<? extends Long> _exampleCountUpperBound = MissingInput.get();

    private Producer<? extends DAGExecutor> _executor = MissingInput.get();

    private Assembled() {
    }

    public static Helper.EstimatedExampleCount.AssembledBuilder builder() {
      return new Builder();
    }

    public Assembled withEstimatedExampleCount(Producer<? extends Long> estimatedExampleCountInput) {
      return clone(c -> c._estimatedExampleCount = estimatedExampleCountInput);
    }

    public Assembled withExampleCountLowerBound(Producer<? extends Long> exampleCountLowerBoundInput) {
      return clone(c -> c._exampleCountLowerBound = exampleCountLowerBoundInput);
    }

    public Assembled withExampleCountUpperBound(Producer<? extends Long> exampleCountUpperBoundInput) {
      return clone(c -> c._exampleCountUpperBound = exampleCountUpperBoundInput);
    }

    public Assembled withExecutor(Producer<? extends DAGExecutor> executorInput) {
      return clone(c -> c._executor = executorInput);
    }

    @Override
    protected PreparerContext apply(List<?> values) {
      PreparerContext res = new PreparerContext();
      res._estimatedExampleCount = (Long) values.get(0);
      res._exampleCountLowerBound = (Long) values.get(1);
      res._exampleCountUpperBound = (Long) values.get(2);
      res._executor = (DAGExecutor) values.get(3);
      return res;
    }

    @Override
    protected Assembled withInputsUnsafe(List<? extends Producer<?>> inputs) {
      return clone(c ->  {
        c._estimatedExampleCount = (Producer<? extends Long>) inputs.get(0);
        c._exampleCountLowerBound = (Producer<? extends Long>) inputs.get(1);
        c._exampleCountUpperBound = (Producer<? extends Long>) inputs.get(2);
        c._executor = (Producer<? extends DAGExecutor>) inputs.get(3);
      } );
    }

    @Override
    protected int computeHashCode() {
      return Transformer.hashCodeOfInputs(this);
    }

    @Override
    protected boolean computeEqualsUnsafe(Assembled other) {
      return Transformer.sameInputs(this, other);
    }

    @Override
    protected List<Producer<?>> getInputList() {
      return Arrays.asList(new Producer<?>[] {_estimatedExampleCount, _exampleCountLowerBound, _exampleCountUpperBound, _executor});
    }

    @ValueEquality
    private static final class DefaultGenerator<R> extends AbstractGenerator<R, DefaultGenerator<R>> {
      private static final long serialVersionUID = 0L;

      private static DefaultGenerator _singleton = new DefaultGenerator();

      DefaultGenerator() {
        super(0xe327508a79df43b0L, 0xfb0af9e419745536L);
      }

      public static <R> DefaultGenerator<R> get() {
        return _singleton;
      }

      @Override
      public R generate(long index) {
        return null;
      }

      private Object readResolve() throws ObjectStreamException {
        return _singleton;
      }
    }

    private static class Builder implements Helper.CompletedAssembledBuilder, Helper.EstimatedExampleCount.AssembledBuilder, Helper.ExampleCountLowerBound.AssembledBuilder, Helper.ExampleCountUpperBound.AssembledBuilder, Helper.Executor.AssembledBuilder {
      private Assembled _instance = new Assembled();

      public Assembled build() {
        return _instance;
      }

      @Override
      public Helper.ExampleCountLowerBound.AssembledBuilder setEstimatedExampleCount(Producer<? extends Long> estimatedExampleCount) {
        _instance._estimatedExampleCount = estimatedExampleCount;
        return this;
      }

      @Override
      public Helper.ExampleCountUpperBound.AssembledBuilder setExampleCountLowerBound(Producer<? extends Long> exampleCountLowerBound) {
        _instance._exampleCountLowerBound = exampleCountLowerBound;
        return this;
      }

      @Override
      public Helper.Executor.AssembledBuilder setExampleCountUpperBound(Producer<? extends Long> exampleCountUpperBound) {
        _instance._exampleCountUpperBound = exampleCountUpperBound;
        return this;
      }

      @Override
      public Helper.CompletedAssembledBuilder setExecutor(Producer<? extends DAGExecutor> executor) {
        _instance._executor = executor;
        return this;
      }
    }
  }

  public static class Reader extends AbstractCloneable<Reader> implements ObjectReader<PreparerContext>, Serializable {
    private static final long serialVersionUID = 1L;

    private ObjectReader<? extends Long> _estimatedExampleCount = null;

    private ObjectReader<? extends Long> _exampleCountLowerBound = null;

    private ObjectReader<? extends Long> _exampleCountUpperBound = null;

    private ObjectReader<? extends DAGExecutor> _executor = null;

    private Reader() {
    }

    public ObjectReader<? extends Long> getEstimatedExampleCountReader() {
      return _estimatedExampleCount;
    }

    public ObjectReader<? extends Long> getExampleCountLowerBoundReader() {
      return _exampleCountLowerBound;
    }

    public ObjectReader<? extends Long> getExampleCountUpperBoundReader() {
      return _exampleCountUpperBound;
    }

    public ObjectReader<? extends DAGExecutor> getExecutorReader() {
      return _executor;
    }

    public static Helper.EstimatedExampleCount.ReaderBuilder builder() {
      return new Builder();
    }

    public Reader withEstimatedExampleCount(ObjectReader<? extends Long> estimatedExampleCountInput) {
      return clone(c -> c._estimatedExampleCount = estimatedExampleCountInput);
    }

    public Reader withExampleCountLowerBound(ObjectReader<? extends Long> exampleCountLowerBoundInput) {
      return clone(c -> c._exampleCountLowerBound = exampleCountLowerBoundInput);
    }

    public Reader withExampleCountUpperBound(ObjectReader<? extends Long> exampleCountUpperBoundInput) {
      return clone(c -> c._exampleCountUpperBound = exampleCountUpperBoundInput);
    }

    public Reader withExecutor(ObjectReader<? extends DAGExecutor> executorInput) {
      return clone(c -> c._executor = executorInput);
    }

    @Override
    public long size64() {
      return _estimatedExampleCount.size64();
    }

    @Override
    public Iterator iterator() {
      return new Iterator(this);
    }

    @Override
    public void close() {
      _estimatedExampleCount.close();
      _exampleCountLowerBound.close();
      _exampleCountUpperBound.close();
      _executor.close();
    }

    private static class Builder implements Helper.CompletedReaderBuilder, Helper.EstimatedExampleCount.ReaderBuilder, Helper.ExampleCountLowerBound.ReaderBuilder, Helper.ExampleCountUpperBound.ReaderBuilder, Helper.Executor.ReaderBuilder {
      private Reader _instance = new Reader();

      public Reader build() {
        return _instance;
      }

      @Override
      public Helper.ExampleCountLowerBound.ReaderBuilder setEstimatedExampleCount(ObjectReader<? extends Long> estimatedExampleCount) {
        _instance._estimatedExampleCount = estimatedExampleCount;
        return this;
      }

      @Override
      public Helper.ExampleCountUpperBound.ReaderBuilder setExampleCountLowerBound(ObjectReader<? extends Long> exampleCountLowerBound) {
        _instance._exampleCountLowerBound = exampleCountLowerBound;
        return this;
      }

      @Override
      public Helper.Executor.ReaderBuilder setExampleCountUpperBound(ObjectReader<? extends Long> exampleCountUpperBound) {
        _instance._exampleCountUpperBound = exampleCountUpperBound;
        return this;
      }

      @Override
      public Helper.CompletedReaderBuilder setExecutor(ObjectReader<? extends DAGExecutor> executor) {
        _instance._executor = executor;
        return this;
      }
    }

    public static class Iterator implements ObjectIterator<PreparerContext> {
      private ObjectIterator<? extends Long> _estimatedExampleCount;

      private ObjectIterator<? extends Long> _exampleCountLowerBound;

      private ObjectIterator<? extends Long> _exampleCountUpperBound;

      private ObjectIterator<? extends DAGExecutor> _executor;

      public Iterator(Reader owner) {
        _estimatedExampleCount = owner._estimatedExampleCount.iterator();
        _exampleCountLowerBound = owner._exampleCountLowerBound.iterator();
        _exampleCountUpperBound = owner._exampleCountUpperBound.iterator();
        _executor = owner._executor.iterator();
      }

      @Override
      public boolean hasNext() {
        return _estimatedExampleCount.hasNext();
      }

      @Override
      public PreparerContext next() {
        PreparerContext res = new PreparerContext();
        res._estimatedExampleCount = _estimatedExampleCount.next();
        res._exampleCountLowerBound = _exampleCountLowerBound.next();
        res._exampleCountUpperBound = _exampleCountUpperBound.next();
        res._executor = _executor.next();
        return res;
      }

      @Override
      public void close() {
        _estimatedExampleCount.close();
        _exampleCountLowerBound.close();
        _exampleCountUpperBound.close();
        _executor.close();
      }
    }
  }

  public static class Schema implements RowSchema<PreparerContext, PreparerContext> {
    private final ArrayList<RowSchema.FieldSchema<PreparerContext>> _fields = new ArrayList<RowSchema.FieldSchema<PreparerContext>>(4);

    public static Helper.EstimatedExampleCount.SchemaBuilder builder() {
      return new Builder();
    }

    @Override
    public PreparerContext createAccumulator() {
      return new PreparerContext();
    }

    @Override
    public PreparerContext finish(PreparerContext accumulator) {
      return accumulator;
    }

    @Override
    public Collection<RowSchema.FieldSchema<PreparerContext>> getFields() {
      return _fields;
    }

    private static class Builder implements Helper.CompletedSchemaBuilder, Helper.EstimatedExampleCount.SchemaBuilder, Helper.ExampleCountLowerBound.SchemaBuilder, Helper.ExampleCountUpperBound.SchemaBuilder, Helper.Executor.SchemaBuilder {
      private Schema _instance = new Schema();

      public Schema build() {
        return _instance;
      }

      @Override
      public Helper.ExampleCountLowerBound.SchemaBuilder setEstimatedExampleCountColumnIndex(int estimatedExampleCountColumnIndex) {
        return setEstimatedExampleCountColumnIndex(estimatedExampleCountColumnIndex, Long::parseLong);
      }

      @Override
      public Helper.ExampleCountUpperBound.SchemaBuilder setExampleCountLowerBoundColumnIndex(int exampleCountLowerBoundColumnIndex) {
        return setExampleCountLowerBoundColumnIndex(exampleCountLowerBoundColumnIndex, Long::parseLong);
      }

      @Override
      public Helper.Executor.SchemaBuilder setExampleCountUpperBoundColumnIndex(int exampleCountUpperBoundColumnIndex) {
        return setExampleCountUpperBoundColumnIndex(exampleCountUpperBoundColumnIndex, Long::parseLong);
      }

      @Override
      public Helper.ExampleCountLowerBound.SchemaBuilder setEstimatedExampleCountColumnIndex(int estimatedExampleCountColumnIndex,
          Function1<String, Long> parser) {
        _instance._fields.add(new RowSchema.Field.Indexed<PreparerContext>() {
          @Override
          public boolean isRequired() {
            return true;
          }

          @Override
          public void read(PreparerContext accumulator, String fieldText) {
            accumulator._estimatedExampleCount = parser.apply(fieldText);
          }

          @Override
          public int getIndex() {
            return estimatedExampleCountColumnIndex;
          }
        });
        return this;
      }

      @Override
      public Helper.ExampleCountUpperBound.SchemaBuilder setExampleCountLowerBoundColumnIndex(int exampleCountLowerBoundColumnIndex,
          Function1<String, Long> parser) {
        _instance._fields.add(new RowSchema.Field.Indexed<PreparerContext>() {
          @Override
          public boolean isRequired() {
            return true;
          }

          @Override
          public void read(PreparerContext accumulator, String fieldText) {
            accumulator._exampleCountLowerBound = parser.apply(fieldText);
          }

          @Override
          public int getIndex() {
            return exampleCountLowerBoundColumnIndex;
          }
        });
        return this;
      }

      @Override
      public Helper.Executor.SchemaBuilder setExampleCountUpperBoundColumnIndex(int exampleCountUpperBoundColumnIndex,
          Function1<String, Long> parser) {
        _instance._fields.add(new RowSchema.Field.Indexed<PreparerContext>() {
          @Override
          public boolean isRequired() {
            return true;
          }

          @Override
          public void read(PreparerContext accumulator, String fieldText) {
            accumulator._exampleCountUpperBound = parser.apply(fieldText);
          }

          @Override
          public int getIndex() {
            return exampleCountUpperBoundColumnIndex;
          }
        });
        return this;
      }

      @Override
      public Helper.CompletedSchemaBuilder setExecutorColumnIndex(int executorColumnIndex,
          Function1<String, DAGExecutor> parser) {
        _instance._fields.add(new RowSchema.Field.Indexed<PreparerContext>() {
          @Override
          public boolean isRequired() {
            return true;
          }

          @Override
          public void read(PreparerContext accumulator, String fieldText) {
            accumulator._executor = parser.apply(fieldText);
          }

          @Override
          public int getIndex() {
            return executorColumnIndex;
          }
        });
        return this;
      }

      @Override
      public Helper.ExampleCountLowerBound.SchemaBuilder setEstimatedExampleCountColumnName(String estimatedExampleCountColumnName) {
        return setEstimatedExampleCountColumnName(estimatedExampleCountColumnName, Long::parseLong);
      }

      @Override
      public Helper.ExampleCountUpperBound.SchemaBuilder setExampleCountLowerBoundColumnName(String exampleCountLowerBoundColumnName) {
        return setExampleCountLowerBoundColumnName(exampleCountLowerBoundColumnName, Long::parseLong);
      }

      @Override
      public Helper.Executor.SchemaBuilder setExampleCountUpperBoundColumnName(String exampleCountUpperBoundColumnName) {
        return setExampleCountUpperBoundColumnName(exampleCountUpperBoundColumnName, Long::parseLong);
      }

      @Override
      public Helper.ExampleCountLowerBound.SchemaBuilder setEstimatedExampleCountColumnName(String estimatedExampleCountColumnName,
          Function1<String, Long> parser) {
        _instance._fields.add(new RowSchema.Field.Named<PreparerContext>() {
          @Override
          public boolean isRequired() {
            return true;
          }

          @Override
          public void read(PreparerContext accumulator, String fieldText) {
            accumulator._estimatedExampleCount = parser.apply(fieldText);
          }

          @Override
          public String getName() {
            return estimatedExampleCountColumnName;
          }
        });
        return this;
      }

      @Override
      public Helper.ExampleCountUpperBound.SchemaBuilder setExampleCountLowerBoundColumnName(String exampleCountLowerBoundColumnName,
          Function1<String, Long> parser) {
        _instance._fields.add(new RowSchema.Field.Named<PreparerContext>() {
          @Override
          public boolean isRequired() {
            return true;
          }

          @Override
          public void read(PreparerContext accumulator, String fieldText) {
            accumulator._exampleCountLowerBound = parser.apply(fieldText);
          }

          @Override
          public String getName() {
            return exampleCountLowerBoundColumnName;
          }
        });
        return this;
      }

      @Override
      public Helper.Executor.SchemaBuilder setExampleCountUpperBoundColumnName(String exampleCountUpperBoundColumnName,
          Function1<String, Long> parser) {
        _instance._fields.add(new RowSchema.Field.Named<PreparerContext>() {
          @Override
          public boolean isRequired() {
            return true;
          }

          @Override
          public void read(PreparerContext accumulator, String fieldText) {
            accumulator._exampleCountUpperBound = parser.apply(fieldText);
          }

          @Override
          public String getName() {
            return exampleCountUpperBoundColumnName;
          }
        });
        return this;
      }

      @Override
      public Helper.CompletedSchemaBuilder setExecutorColumnName(String executorColumnName,
          Function1<String, DAGExecutor> parser) {
        _instance._fields.add(new RowSchema.Field.Named<PreparerContext>() {
          @Override
          public boolean isRequired() {
            return true;
          }

          @Override
          public void read(PreparerContext accumulator, String fieldText) {
            accumulator._executor = parser.apply(fieldText);
          }

          @Override
          public String getName() {
            return executorColumnName;
          }
        });
        return this;
      }

      @Override
      public Helper.ExampleCountLowerBound.SchemaBuilder setEstimatedExampleCountParser(BiFunction<String[], String[], Long> parser) {
        _instance._fields.add(new RowSchema.AllFields<PreparerContext>() {
          @Override
          public boolean isRequired() {
            return true;
          }

          @Override
          public void read(PreparerContext accumulator, String[] fieldNames, String[] fieldText) {
            accumulator._estimatedExampleCount = parser.apply(fieldNames, fieldText);
          }
        });
        return this;
      }

      @Override
      public Helper.ExampleCountUpperBound.SchemaBuilder setExampleCountLowerBoundParser(BiFunction<String[], String[], Long> parser) {
        _instance._fields.add(new RowSchema.AllFields<PreparerContext>() {
          @Override
          public boolean isRequired() {
            return true;
          }

          @Override
          public void read(PreparerContext accumulator, String[] fieldNames, String[] fieldText) {
            accumulator._exampleCountLowerBound = parser.apply(fieldNames, fieldText);
          }
        });
        return this;
      }

      @Override
      public Helper.Executor.SchemaBuilder setExampleCountUpperBoundParser(BiFunction<String[], String[], Long> parser) {
        _instance._fields.add(new RowSchema.AllFields<PreparerContext>() {
          @Override
          public boolean isRequired() {
            return true;
          }

          @Override
          public void read(PreparerContext accumulator, String[] fieldNames, String[] fieldText) {
            accumulator._exampleCountUpperBound = parser.apply(fieldNames, fieldText);
          }
        });
        return this;
      }

      @Override
      public Helper.CompletedSchemaBuilder setExecutorParser(BiFunction<String[], String[], DAGExecutor> parser) {
        _instance._fields.add(new RowSchema.AllFields<PreparerContext>() {
          @Override
          public boolean isRequired() {
            return true;
          }

          @Override
          public void read(PreparerContext accumulator, String[] fieldNames, String[] fieldText) {
            accumulator._executor = parser.apply(fieldNames, fieldText);
          }
        });
        return this;
      }
    }
  }
}
