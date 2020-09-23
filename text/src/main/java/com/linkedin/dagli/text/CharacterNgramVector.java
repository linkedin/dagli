package com.linkedin.dagli.text;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.math.hashing.Murmurish;
import com.linkedin.dagli.math.vector.DenseFloatArrayVector;
import com.linkedin.dagli.math.vector.SparseFloatMapVector;
import com.linkedin.dagli.math.vector.Vector;
import com.linkedin.dagli.transformer.AbstractPreparedTransformer1WithInput;


/**
 * Given a character sequence (such as a {@link String}), extracts character ngrams of sizes within a specified range
 * (e.g. the bigrams of "dog" are "do" and "og", and perhaps "_d" and "g_" if padding is used).  These ngrams are
 * hashed and used as indices in the resultant sparse vector, and the associated values are the counts (i.e. 1 if a
 * character ngram occurs once in the character sequence, 2 if it occurs twice, etc.)
 *
 * The resultant vector can then be used as features in a statistical model.
 *
 * @author Jeff Pasternack (jepasternack@linkedin.com)
 */
@ValueEquality
public class CharacterNgramVector
    extends AbstractPreparedTransformer1WithInput<CharSequence, Vector, CharacterNgramVector> {
  private static final long serialVersionUID = 2;

  /**
   * Determines whether and how the input character sequence is padded, i.e. how many padding characters are prepended
   * and appended to the input before the character ngrams are extracted.
   */
  public enum  Padding {
    /**
     * No padding will be used.
     */
    NONE,

    /**
     * For each ngram size n, n-1 padding characters added before and after the input
     */
    FULL,

    /**
     * A single padding character added before and after the input (except for unigrams)
     */
    SINGLE_CHARACTER,
  }

  /**
   * The character used as padding, an underscore.
   * Protected (not public) because which character is used is actually invisible and irrelevant to clients.
   */
  protected static final char PADDING_CHARACTER = '_';

  protected int _minSize = 1;
  protected int _maxSize = 1;
  protected Padding _padding = Padding.SINGLE_CHARACTER;
  protected String _paddingString;

  /**
   * Creates a new instance with a minimum and maximum ngram size of 1 (character unigrams only) and single-character
   * padding (note that padding is irrelevant for character unigrams, however).
   */
  public CharacterNgramVector() {
    super();
  }

  /**
   * Creates a new instance with the specified configuration.
   *
   * @param minNgramSize the minimum size for the ngrams
   * @param maxNgramSize the maximum size for the ngrams
   * @param padding the type of padding to use
   *
   * @deprecated use the parameterless constructor combined with in-place builder methods withNgramSize(...),
   *             withMaxNgramSize(...), and withPadding(...) instead.
   */
  @Deprecated
  public CharacterNgramVector(int minNgramSize, int maxNgramSize, Padding padding) {
    super();
    setPadding(_padding);
    _minSize = minNgramSize;
    _maxSize = maxNgramSize;
  }

  /**
   * Sets the minimum size of the character ngrams to generate.  The default size of 1 corresponds to unigrams.
   *
   * @param minNgramSize the minimum size of the ngrams that will be extracted.
   * @return a new instance of this CharacterNgramVector with the specified parameter set.
   */
  public CharacterNgramVector withMinSize(int minNgramSize) {
    return clone(c -> c._minSize = minNgramSize);
  }

  /**
   * Sets the maximum size of the character ngrams to generate.  The default size of 1 corresponds to unigrams.
   *
   * @param maxNgramSize the maximum size of the ngrams that will be extracted.
   * @return a new instance of this CharacterNgramVector with the specified parameter set.
   */
  public CharacterNgramVector withMaxSize(int maxNgramSize) {
    return clone(c -> c._maxSize = maxNgramSize);
  }

  /**
   * Sets the padding scheme to use.  Padding helps distinguish character ngrams near the beginning or end of the input
   * from those in the middle, and increases the number of total ngrams extracted.
   *
   * The default padding scheme is "single character".
   *
   * @param padding the padding scheme to use.
   * @return a new instance of this CharacterNgramVector with the specified parameter set.
   */
  public CharacterNgramVector withPadding(Padding padding) {
    return clone(c -> c.setPadding(padding));
  }

  private void setPadding(Padding padding) {
    _padding = padding;
    switch (padding) {
      case NONE:
        _paddingString = null;
        break;
      case FULL:
        int size = _maxSize - 1;
        StringBuilder builder = new StringBuilder(size);
        for (int i = 0; i < size; i++) {
          builder.append(PADDING_CHARACTER);
        }
        _paddingString = builder.toString();
        break;
      case SINGLE_CHARACTER:
        _paddingString = Character.toString(PADDING_CHARACTER);
        break;
      default:
        throw new IllegalArgumentException("Unknown padding scheme");
    }
  }

  private String padString(String str) {
    switch (_padding) {
      case FULL:
      case SINGLE_CHARACTER:
        return _paddingString + str + _paddingString;
      default:
        return str;
    }
  }

  /**
   * Calculates an estimated number of ngrams that will be extracted from a given string, useful for pre-allocating
   * the appropriate space in collections that will store the ngrams (or related statistics).
   *
   * @param stringLength the length of the input string
   * @return an estimate of the number of ngrams that will be extracted
   */
  private int estimateNgramCount(int stringLength) {
    switch (_padding) {
      case FULL:
        return (_maxSize - _minSize + 1) * (stringLength + (_maxSize + _minSize + 1) / 2);
      case SINGLE_CHARACTER:
        stringLength += 2;
      case NONE: // SUPPRESS CHECKSTYLE intentional fallthrough
        if (_minSize > stringLength) {
          return 0;
        }

        int effectiveMaxLength = Math.min(_maxSize, stringLength);
        // calculate the average number of ngrams that will be extracted at each ngram size;
        // this is done by simply averaging the number of ngrams of the largest size and those of the smallest size,
        // rounding up (this is why there's a + 1 at the end of the numerator)
        int avgNgramCount = ((stringLength - effectiveMaxLength + 1) + (stringLength - _minSize + 1) + 1) / 2;
        // return (# of ngram sizes) * (# avg ngrams per size) ~= # ngrams
        return (effectiveMaxLength - _minSize + 1) * avgNgramCount;
      default:
        throw new IllegalStateException("Unknown padding scheme");
    }
  }

  @Override
  public Vector apply(CharSequence input) {
    String str = input.toString();
    if (str.isEmpty()) {
      return DenseFloatArrayVector.wrap();
    }

    SparseFloatMapVector vec = new SparseFloatMapVector(estimateNgramCount(str.length()));
    str = padString(str);

    for (int n = _minSize; n <= _maxSize; n++) {
      final int startIndex;
      final int endIndex;
      switch (_padding) {
        case FULL:
          startIndex = _maxSize - n;
          endIndex = str.length() - _maxSize;
          break;
        case NONE:
          startIndex = 0;
          endIndex = str.length() - n;
          break;
        case SINGLE_CHARACTER:
          startIndex = n == 1 ? 1 : 0;
          endIndex = n == 1 ? str.length() - 2 : str.length() - n;
          break;
        default:
          throw new IllegalStateException("Unknown padding scheme");
      }

      for (int i = startIndex; i <= endIndex; i++) {
        vec.increase(Murmurish.hash(str, i, n, 0), 1.0);
      }
    }

    return vec;
  }
}
