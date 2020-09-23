package com.linkedin.dagli.transformer;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.producer.Producer;


/**
 * This is a convenience base class that simply extends {@link AbstractPreparableTransformer1} and adds a public
 * {@link #withInput(Producer)} method that allows clients to set the (sole) input to the transformer.
 *
 * The reason this class exists is that, in the vast majority of cases, the semantics of the (sole) input to a
 * single-input transformer are unambiguous and a generic {@code withInput(...)} method is the standard nomenclature
 * for specifying it.  Using this class is of course equivalent to (but easier than) adding the same
 * {@code withInput(...)} implementation yourself.
 *
 * An equivalent of this class does not exist for higher-arity transformers because, at higher arities, the inputs
 * should generally be named, e.g. {@code withLabelInput(...)}, {@code withFeaturesInput(...)}, etc.
 *
 * @param <A> the type of the input values accepte by this transformer
 * @param <R> the type of result produced by this transformer
 * @param <N> the type of the prepared transformer replacing this preparable transformer after the DAG is prepared
 * @param <S> the derived transformer class
 */
@ValueEquality
public abstract class AbstractPreparableTransformer1WithInput<
    A, R, N extends PreparedTransformer1<A, R>, S extends AbstractPreparableTransformer1WithInput<A, R, N, S>>
    extends AbstractPreparableTransformer1<A, R, N, S> {
  private static final long serialVersionUID = 1;

  /**
   * Creates a new input with a {@link com.linkedin.dagli.producer.MissingInput} input.
   */
  public AbstractPreparableTransformer1WithInput() {
    super();
  }

  /**
   * Creates a new instance with the specified input.
   *
   * @param input the input to the new transformer
   */
  public AbstractPreparableTransformer1WithInput(Producer<? extends A> input) {
    super(input);
  }

  /**
   * Returns a copy of this transformer that will accept input values from the provided {@link Producer}.
   *
   * @param input the {@link Producer} that will provide the input values to the returned transformer
   * @return a copy of this transformer that will accept input values from the provided {@link Producer}
   */
  public S withInput(Producer<? extends A> input) {
    return super.withInput1(input);
  }
}
