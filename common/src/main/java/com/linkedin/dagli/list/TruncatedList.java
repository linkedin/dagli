package com.linkedin.dagli.list;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.generator.Constant;
import com.linkedin.dagli.producer.MissingInput;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.reducer.Reducer;
import com.linkedin.dagli.transformer.AbstractPreparedTransformer2;
import com.linkedin.dagli.util.invariant.Arguments;
import java.util.Collection;
import java.util.Collections;
import java.util.List;


/**
 * Truncates an inputted list (via {@link List#subList(int, int)}) if it exceeds a specified maximum size, or, if not,
 * simply returns the inputted list, unmodified.
 *
 * @param <T> the type of element in the list
 */
@ValueEquality
public class TruncatedList<T>
    extends AbstractPreparedTransformer2<List<? extends T>, Integer, List<? extends T>, TruncatedList<T>> {
  private static final long serialVersionUID = 1;

  public TruncatedList() {
    super(MissingInput.get(), new Constant<>(Integer.MAX_VALUE));
  }

  @Override
  @SuppressWarnings("unchecked")
  protected Collection<? extends Reducer<? super TruncatedList<T>>> getGraphReducers() {
    // if our parent is also a TruncatedList, we can replace ourself with a new transformer that just truncates to
    // the minimum of the maximum sizes of ourself and our parent and accepts our grandparent as input
    return Collections.singleton((target, context) -> {
      Integer targetMaxSize = (Integer) Constant.tryGetValue(context.getParents(target).get(1));
      if (targetMaxSize != null) {
        TruncatedList truncatedParent = context.getParentByClass(target, TruncatedList.class);
        if (truncatedParent != null) {
          Integer parentMaxSize = (Integer) Constant.tryGetValue(context.getParents(truncatedParent).get(1));
          if (parentMaxSize != null) {
            context.replace(target, new TruncatedList<T>().withMaxSize(Math.min(targetMaxSize, parentMaxSize))
                .withListInput((Producer<List<T>>) context.getParents(truncatedParent).get(0)));
          }
        }
      }
    });
  }

  /**
   * @param listInput the producer that will provide lists to truncate
   * @return a copy of this instance that will truncate the lists provided by the given input producer
   */
  public TruncatedList<T> withListInput(Producer<? extends List<? extends T>> listInput) {
    return withInput1(listInput);
  }

  /**
   * Returns a copy of this instance that will truncate lists to the specified maximum size.
   *
   * The default maximum size is {@link Integer#MAX_VALUE}, which will cause this transformer to simply produce the
   * inputted list untruncated.
   *
   * @param maxSize the maximum size of the truncated list (must be {@code >= 0})
   * @return a copy of this instance that will truncate lists to the specified maximum size
   */
  public TruncatedList<T> withMaxSize(int maxSize) {
    return withMaxSizeInput(new Constant<>(Arguments.inInclusiveRange(maxSize, 0, Integer.MAX_VALUE)));
  }

  /**
   * Returns a copy of this instance that will truncate lists to the maximum size provided by the given input.
   *
   * The input {@link Number} values should be non-negative.
   *
   * If no input is specified, the default maximum size is {@link Integer#MAX_VALUE}, which will cause this transformer
   * to simply produce the inputted list untruncated.
   *
   * @param maxSize the maximum size of the truncated list (must be {@code >= 0})
   * @return a copy of this instance that will truncate lists to the specified maximum size
   */
  public TruncatedList<T> withMaxSizeInput(Producer<? extends Integer> maxSize) {
    return withInput2(maxSize);
  }

  @Override
  public List<? extends T> apply(List<? extends T> list, Integer maxSize) {
    return maxSize >= list.size() ? list : list.subList(0, maxSize);
  }
}
