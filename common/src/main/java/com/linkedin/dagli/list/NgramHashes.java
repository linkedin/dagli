package com.linkedin.dagli.list;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.longs.LongLists;
import java.util.List;


/**
 * Creates a list of (64-bit) ngram hashes from a provided list items (from which the ngrams are computed).
 *
 * The list will include duplicate hashes if an ngram occurs multiple times in the list (or in the very unlikely event
 * of a hash collision).  The ordering of the list is arbitrary.
 *
 * The per-item hashing function (which is used to compute the hash of the entire ngram) may be provided, but otherwise
 * defaults to {@link Object#hashCode()}.
 */
@ValueEquality
public class NgramHashes extends AbstractNgrams<LongList, NgramHashes> {

  private static final long serialVersionUID = 1;

  /**
   * Create a new instance that will (by default) calculate unigram hashes only.
   *
   * The default padding is SINGLE, although padding is irrelevant for unigrams regardless.
   */
  public NgramHashes() {
    super();
  }

  @Override
  public LongList apply(List<?> sequence) {
    if (sequence.isEmpty()) {
      return LongLists.EMPTY_LIST;
    }

    LongArrayList res = new LongArrayList(estimateNgramCount(sequence.size()));
    apply(sequence, res::add);
    return res;
  }
}
