package com.linkedin.dagli.transformer;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.preparer.AbstractStreamPreparerDynamic;
import com.linkedin.dagli.preparer.PreparerContext;
import com.linkedin.dagli.preparer.PreparerDynamic;
import com.linkedin.dagli.preparer.PreparerResult;
import com.linkedin.dagli.preparer.PreparerResultMixed;
import java.util.ArrayList;


@ValueEquality
public class TriviallyPreparable<R> extends AbstractPreparableTransformerDynamic<R, PreparedTransformer<R>, TriviallyPreparable<R>> {
  private static final long serialVersionUID = 1;

  private final PreparedTransformer<R> _prepared;

  public TriviallyPreparable(final PreparedTransformer<R> prepared) {
    super(new ArrayList<>(prepared.internalAPI().getInputList())); // adopt wrapped transformer's inputs
    _prepared = prepared;
  }

  @Override
  protected PreparerDynamic<R, PreparedTransformer<R>> getPreparer(PreparerContext context) {
    return new AbstractStreamPreparerDynamic<R, PreparedTransformer<R>>() {
      @Override
      public void processUnsafe(Object[] values) { }

      @Override
      public PreparerResultMixed<? extends PreparedTransformer<R>, PreparedTransformer<R>> finish() {
        return new PreparerResult<>(_prepared);
      }
    };
  }
}
