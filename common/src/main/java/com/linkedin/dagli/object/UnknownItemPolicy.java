package com.linkedin.dagli.object;

/**
 * For indexing transformers (such as {@link Index}, determines what index is used for items not known in the final
 * mapping (this includes items hitherto unseen as well as those that were discarded due to insufficient frequency or
 * lack of mapping capacity).
 */
public enum UnknownItemPolicy {
  /**
   * Unknown items are assigned their own index that is higher than any other mapped item's index (specifically, the
   * unknown item index will be {@code [highest explicitly mapped index] + 1}).
   *
   * This can potentially cause problems if it causes downstream nodes to encounter an index during inference that
   * they never saw during training.
   */
  NEW,

  /**
   * Unknown items will be assigned the index of the item least frequently seen during preparation.  If there are
   * multiple items tied with the lowest frequency, which item's index will be used is arbitrary (but the same index
   * will be used for all unknown items).
   */
  LEAST_FREQUENT,

  /**
   * Unknown items will be assigned the index of the item most frequently seen during preparation (the mode).  If
   * there are multiple items tied with the highest frequency, which item's index will be used is arbitrary (but
   * whichever index is chosen will be used for all unknown items).
   */
  MOST_FREQUENT,

  /**
   * Like {@link #NEW}, unknown items are assigned their own index that is higher than any other mapped
   * item's index (specifically, the unknown item index will be {@code [highest explicitly mapped index] + 1}).
   *
   * However, unlike {@link #NEW}, in this mode the least-frequent item seen during training (preparation) is always
   * dropped and treated as "unknown".  This avoids potential problems with downstream nodes encountering
   * hitherto-unseen indices during inference.
   */
  DISTINCT,

  /**
   * Unknown items are "ignored".  For {@link Indices}, this means that they will be omitted entirely from the list of
   * indices.  For {@link Index}, this means that the resulting index will be {@code null}.
   */
  IGNORE,

  /**
   * Unknown items map to an index of {@code -1}.
   */
  NEGATIVE_ONE
}
