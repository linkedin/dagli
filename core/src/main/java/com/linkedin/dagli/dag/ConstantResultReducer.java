package com.linkedin.dagli.dag;

import com.linkedin.dagli.generator.Constant;
import com.linkedin.dagli.generator.Generator;
import com.linkedin.dagli.objectio.ObjectReader;
import com.linkedin.dagli.preparer.Preparer;
import com.linkedin.dagli.preparer.PreparerContext;
import com.linkedin.dagli.preparer.PreparerResultMixed;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.reducer.Reducer;
import com.linkedin.dagli.transformer.ConstantResultTransformation;
import com.linkedin.dagli.transformer.ConstantResultTransformationDynamic;
import com.linkedin.dagli.transformer.PreparableTransformer;
import com.linkedin.dagli.transformer.PreparedTransformer;
import com.linkedin.dagli.transformer.Transformer;
import com.linkedin.dagli.transformer.TriviallyPreparableTransformation;
import com.linkedin.dagli.view.TransformerView;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


/**
 * Replaces producers that are constant-result with {@link com.linkedin.dagli.generator.Constant}s where possible.
 */
class ConstantResultReducer implements Reducer<Producer<?>> {
  static final ConstantResultReducer INSTANCE = new ConstantResultReducer(); // we just need one instance
  private ConstantResultReducer() { } // force use of INSTANCE

  private static boolean isIrreducableConstantResultTransformation(PreparableTransformer<?, ?> preparable) {
    if (preparable instanceof ConstantResultTransformation) {
      ConstantResultTransformation<?, ?> crt = ((ConstantResultTransformation<?, ?>) preparable);
      return !Objects.equals(crt.getResultForNewData(), crt.getResultForPreparationData());
    }
    return false;
  }

  private static boolean isIrreducablePreparableTransformer(Producer<?> target, Context context) {
    if (!(target instanceof PreparableTransformer)) {
      return false;
    }

    PreparableTransformer<?, ?> preparable = (PreparableTransformer<?, ?>) target;

    return !preparable.internalAPI().hasIdempotentPreparer()
        || (preparable instanceof TriviallyPreparableTransformation && context.isViewed(preparable))
        || isIrreducableConstantResultTransformation(preparable);
  }

  @SuppressWarnings("unchecked") // safety enforced by semantics of preparable transformers
  private void reducePreparable(PreparableTransformer<?, ?> preparable, Context context, Object[] inputValues) {
    PreparerResultMixed<?, ?> prepResult = constantPrepared(preparable, inputValues);
    if (context.isViewed(preparable)) { // need to keep a preparable transformer around for the sake of its viewers?
      List<? extends Producer<?>> parents = context.getParents(preparable);
      // we need to make sure the prepared transformer (to be applied to preparation data) has the correct parents
      PreparedTransformer<?> prepForPrepData = prepResult.getPreparedTransformerForPreparationData()
          .internalAPI()
          .withInputsUnsafe(new ArrayList<>(parents));

      // if the prepared transformers for prep/new data are the same, we can (and should) reuse the same instance
      // the same as prepForPrepData if possible/appropriate (this may enable further downstream reductions)
      PreparedTransformer<?> prepForNewData =
          prepResult.hasSamePreparedTransformerForNewAndPreparationData() ? prepForPrepData
              : prepResult.getPreparedTransformerForNewData().internalAPI().withInputsUnsafe(new ArrayList<>(parents));

      context.replace((PreparableTransformer) preparable,
          new TriviallyPreparableTransformation(prepForPrepData, prepForNewData));
    } else {
      Object val1 = apply(prepResult.getPreparedTransformerForNewData(), inputValues);
      if (prepResult.getPreparedTransformerForNewData() != prepResult.getPreparedTransformerForPreparationData()) {
        // need to check other transformer's constant value
        Object val2 = apply(prepResult.getPreparedTransformerForPreparationData(), inputValues);
        if (!Objects.equals(val1, val2)) {
          // the preparable has different values for prep and new data; can't simply replace with a Constant:
          context.replaceUnviewed(preparable,
              new ConstantResultTransformationDynamic().withResultForNewData(val1).withResultForPreparationData(val2));
        }
      }

      context.replaceUnviewed(preparable, new Constant(val1));
    }
  }

  private void reduceView(TransformerView viewTarget, Context context) {
    List<? extends Producer<?>> parents = context.getParents(viewTarget);

    assert parents.size() == 1;
    if (parents.get(0) instanceof TriviallyPreparableTransformation) {
      TriviallyPreparableTransformation<?, ?> trivialParent =
          (TriviallyPreparableTransformation<?, ?>) parents.get(0);

      // in principle, the view might produce different values for preparation and new data
      Object value1 = viewTarget.internalAPI().prepare(trivialParent.getPreparedForNewData());
      Object value2 = viewTarget.internalAPI()
          .prepareForPreparationData(trivialParent.getPreparedForPreparationData(),
              trivialParent.getPreparedForNewData());

      if (Objects.equals(value1, value2)) {
        context.replace(viewTarget, new Constant<>(value1));
      } else {
        // best we can do is replace it with a ConstantResultTransformation
        context.replace(viewTarget, new ConstantResultTransformationDynamic().withResultForNewData(value1)
            .withResultForPreparationData(value2));
      }
    }
  }

  @Override
  public void reduce(Producer<?> target, Context context) {
    if (isIrreducablePreparableTransformer(target, context) || target instanceof Constant) {
      return; // can't reduce this target at this time
    }

    if (target instanceof Generator) {
      if (target.internalAPI().hasAlwaysConstantResult()) {
        context.replace((Generator<Object>) target, new Constant<>((((Generator<Object>) target).generate(0))));
      }
    } else if (target instanceof TransformerView) {
      reduceView((TransformerView<?, ?>) target, context);
    } else if (target instanceof PreparedTransformer && target.internalAPI().hasAlwaysConstantResult()) {
      // intrinsically constant prepared transformers can be fed nulls to generate their constant result:
      PreparedTransformer<?> prepared = (PreparedTransformer<?>) target;
      context.replace(prepared, new Constant(apply(prepared, new Object[context.getParents(prepared).size()])));
    } else if (target instanceof ConstantResultTransformation
        && !context.isViewed((PreparableTransformer<?, ?>) target)) {
      // The isIrreducablePreparableTransformer check above filtered out the case where the constant result for new and
      // prepared data are different; thus, we can now safely substitute with Constant.  We are ignoring the case where
      // there is a view on the ConstantResultTransformation because there is no sensible reason to create a view on a
      // ConstantResultTransformation.
      context.replaceUnviewed((PreparableTransformer<?, ?>) target,
          new Constant(((ConstantResultTransformation<?, ?>) target).getResultForNewData()));
    } else if (target instanceof Transformer) {
      // can only be reduced if all parents are Constant
      List<? extends Producer<?>> parents = context.getParents(target);
      if (parents.stream().allMatch(parent -> parent instanceof Constant)) {
        Object[] inputValues = parents.stream().map(parent -> ((Constant<Object>) parent).getValue()).toArray();

        if (target instanceof PreparableTransformer) {
          reducePreparable((PreparableTransformer<?, ?>) target, context, inputValues);
        } else {
          PreparedTransformer<Object> preparedTarget = (PreparedTransformer<Object>) target;
          context.replace(preparedTarget, new Constant<>(apply(preparedTarget, inputValues)));
        }
      }
    }
  }

  private static Object apply(PreparedTransformer<?> prepared, Object[] inputValues) {
    return prepared.internalAPI().applyUnsafe(prepared.internalAPI().createExecutionCache(1), inputValues);
  }

  private static PreparerResultMixed<?, ?> constantPrepared(PreparableTransformer<?, ?> target, Object[] inputs) {
    Preparer<?, ?> preparer = target.internalAPI().getPreparer(PreparerContext.builder(1).setExecutor(new LocalDAGExecutor()).build());
    preparer.processUnsafe(inputs);
    return preparer.finishUnsafe(ObjectReader.singleton(inputs));
  }
}
