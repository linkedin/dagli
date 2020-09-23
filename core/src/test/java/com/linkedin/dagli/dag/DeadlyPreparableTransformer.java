package com.linkedin.dagli.dag;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.preparer.Preparer1;
import com.linkedin.dagli.preparer.PreparerContext;
import com.linkedin.dagli.preparer.TrivialPreparer1;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.transformer.AbstractPreparableTransformer1;
import com.linkedin.dagli.transformer.AbstractPreparedTransformer1;
import com.linkedin.dagli.transformer.PreparableTransformer;
import com.linkedin.dagli.view.AbstractTransformerView;

@ValueEquality
public class DeadlyPreparableTransformer extends
                                  AbstractPreparableTransformer1<Integer, Integer, DeadlyPreparableTransformer.Prepared, DeadlyPreparableTransformer> {
  private static final long serialVersionUID = 1;

  public DeadlyPreparableTransformer withInput(Producer<? extends Integer> input1) {
    return super.withInput1(input1);
  }

  @Override
  protected Preparer1<Integer, Integer, Prepared> getPreparer(PreparerContext context) {
    return new TrivialPreparer1<>(new Prepared());
  }

  @ValueEquality
  public static class Prepared extends AbstractPreparedTransformer1<Integer, Integer, Prepared> {
    private static final long serialVersionUID = 1;

    public Prepared withInput(Producer<? extends Integer> input1) {
      return super.withInput1(input1);
    }

    @Override
    public Integer apply(Integer value0) {
      throw new UnsupportedOperationException();
    }
  }

  @ValueEquality
  public static class View extends AbstractTransformerView<Integer, Prepared, View> {
    private static final long serialVersionUID = 1;

    public View(PreparableTransformer<?, ? extends Prepared> viewedTransformer) {
      super(viewedTransformer);
    }

    @Override
    protected Integer prepare(Prepared preparedTransformerForNewData) {
      return 7;
    }
  }
}
