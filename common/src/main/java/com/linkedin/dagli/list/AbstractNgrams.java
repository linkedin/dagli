package com.linkedin.dagli.list;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.math.hashing.DoubleXorShift;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.transformer.AbstractPreparedTransformer1WithInput;
import com.linkedin.dagli.util.function.LongFunction1;
import com.linkedin.dagli.util.invariant.Arguments;
import java.util.List;
import java.util.function.LongConsumer;


/**
 * Creates a sparse vector of ngram counts from a provided list items (from which the ngrams are computed and counted).
 *
 * Each ngram maps to a vector index via hashing.  The per-item hashing function (which is used to compute the hash of
 * the entire ngram) may be provided, but otherwise defaults to {@link Object#hashCode()}.
 *
 * @param <R> the type of result produced by this transformer
 * @param <S> the type of the derived class
 */
@ValueEquality
abstract class AbstractNgrams<R, S extends AbstractNgrams<R, S>>
    extends AbstractPreparedTransformer1WithInput<List<?>, R, S> {
  private static final long serialVersionUID = 1;
  private static final long PADDING_HASH = 0x1337DEADBEEF9000L;
  private static final long NULL_HASH =    0xABC123CD1337BABEL;

  /**
   * Determines the type of padding used around the input sequence when extracting ngrams.
   */
  public enum Padding {
    /**
     * No padding is used.
     */
    NONE,
    /**
     * For each ngram size n, n-1 padding elements added before and after the list.  This allows ngrams to capture their
     * position at the beginning or end of a sequence, e.g. a [START, START, "Call", "me", "Ishmael"] 5-gram.
     */
    FULL,

    /**
     * A single padding element added before and after the list (except for unigrams).  This allows ngrams to capture
     * their position at the beginning or end of the sequence, although only one ngram will contain contain this
     * information, e.g. a [START, "It", "was", "the", "worst"] 5-gram.
     */
    SINGLE,
  }

  private int _minLength = 1;
  private int _maxLength = 1;
  private Padding _padding = Padding.SINGLE;
  private LongFunction1.Serializable<Object> _hashFunction = null;

  /**
   * Create a new NgramVector that will (by default) calculate unigram counts only.
   *
   * The default padding is SINGLE, although padding is irrelevant for unigrams regardless.
   */
  public AbstractNgrams() {
    super();
  }

  /**
   * Creates a copy of this instance with the specified minimum ngram length.
   *
   * Ngrams of size n \in [minSize, maxSize] will be extracted.
   *
   * The default value is 1.
   *
   * @param length the new minimum ngram length.
   * @return a copy of this instance with the specified minimum size
   */
  public S withMinSize(int length) {
    Arguments.check(length >= 1, "Min ngram size must be at least 1");
    return clone(c -> ((AbstractNgrams<?, ?>) c)._minLength = length);
  }

  /**
   * Creates a copy of this instance with the specified maximum ngram length.
   *
   * Ngrams of size n \in [minSize, maxSize] will be extracted.
   *
   * The default value is 1.
   *
   * @param length the new maximum ngram length.
   * @return a copy of this instance with the specified maximum size
   */
  public S withMaxSize(int length) {
    Arguments.check(length >= 1, "Max ngram size must be at least 1");
    return clone(c -> ((AbstractNgrams<?, ?>) c)._maxLength = length);
  }

  @Override
  public void validate() {
    super.validate();
    Arguments.check(_maxLength >= _minLength, "Maximum ngram length must be >= minimum ngram length");
  }

  /**
   * Returns a copy of this instance with the specified padding to be used around the input sequence when extracting
   * ngrams.  The default is SINGLE, where a single START/END symbol is implicitly prepended and appended to the
   * sequence (when calculating bigrams or larger).
   *
   * @param padding the type of padding to use
   * @return a copy of this instance with the specified padding
   */
  public S withPadding(Padding padding) {
    return clone(c -> ((AbstractNgrams<?, ?>) c)._padding = padding);
  }

  /**
   * Returns a copy of this instance with the specified input producer providing lists of items and an accompanying hash
   * function used to hash the items in those lists.  This may be used to provide a better, 64-bit hash function than
   * the default {@link Object#hashCode()}.
   *
   * @param hashFunction the hash function to use.  This function must either "safely-serializable", i.e. a method
   *                     reference or a function object; an exception will be thrown if an anonymous lambda is passed,
   *                     as anonymous lambdas are not safely serializable.  This hash function will <strong>not</strong>
   *                     be called against null items; rather, a pre-defined constant hash value is used for these.
   * @return copy of this instance that will use the specified input and accompanying hash function
   */
  @SuppressWarnings("unchecked")
  public <T> S withInput(Producer<? extends List<? extends T>> input,
      LongFunction1.Serializable<? super T> hashFunction) {
    S res = super.withInput1(input);
    ((AbstractNgrams<?, ?>) res)._hashFunction = (LongFunction1.Serializable) hashFunction.safelySerializable();
    return res;
  }

  public S withInput(Producer<? extends List<?>> input) {
    S res = super.withInput1(input);
    ((AbstractNgrams<?, ?>) res)._hashFunction = null; // clear any previously-stored hash function
    return res;
  }

  protected int estimateNgramCount(int sequenceLength) {
    switch (_padding) {
      case FULL:
        return (_maxLength - _minLength + 1) * (sequenceLength + (_maxLength + _minLength + 1) / 2);
      case SINGLE:
        sequenceLength += 2;
      case NONE: // SUPPRESS CHECKSTYLE intentional fallthrough
        if (_minLength > sequenceLength) {
          return 0;
        }

        int effectiveMaxLength = Math.min(_maxLength, sequenceLength);
        int avgNgramCount = ((sequenceLength - effectiveMaxLength + 1) + (sequenceLength - _minLength + 1) + 1) / 2;
        return (effectiveMaxLength - _minLength + 1) * avgNgramCount;
      default:
        throw new IllegalStateException("Unknown padding scheme");
    }
  }

  protected void apply(List<?> sequence, LongConsumer ngramHashConsumer) {
    int pad = 0;
    if (_maxLength > 1) {
      switch (_padding) {
        case NONE:
          break;
        case FULL:
          pad = _maxLength - 1;
          break;
        case SINGLE:
          pad = 1;
          break;
        default:
          throw new IllegalStateException("Unknown padding scheme");
      }
    }

    // the highest index at which an ngram can start + 1
    int size = Math.min(sequence.size(), sequence.size() + pad - _minLength + 1);

    for (int i = -pad; i < size; i++) {
      long runningHash = 0;
      for (int n = 0; n < _maxLength; n++) {
        int offset = i + n;
        if (offset >= sequence.size() + pad) { // we've gone past the hallucinated padding
          break;
        }

        // figure out the hash for the current token
        long elementHash;
        if (offset < 0 || offset >= sequence.size()) {
          elementHash = PADDING_HASH;
        } else {
          Object obj = sequence.get(offset);
          if (obj == null) {
            elementHash = NULL_HASH;
          } else if (_hashFunction == null) {
            elementHash = obj.hashCode();
          } else {
            elementHash = _hashFunction.applyAsLong(obj);
          }
        }

        runningHash = DoubleXorShift.hashWithDefaultSeed(elementHash + runningHash);
        if (offset >= 0 && n >= _minLength - 1) {
          // this is a valid ngram sequence
          ngramHashConsumer.accept(runningHash);
        }
      }
    }
  }
}
