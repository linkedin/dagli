package com.linkedin.dagli.embedding.classification;

import com.linkedin.dagli.math.vector.DenseFloatArrayVector;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;


/**
 * Static container for internal-use FastText classes.
 * Dagli clients should not use this class (or its contents) directly.
 */
public abstract class FastTextInternal {
  private FastTextInternal() { }
  /**
   * Common methods shared between inference and training.  This code is intended for shared use by the training
   * and inference modules and should not be relied upon by other parties; method semantics may change at any time.
   *
   * Note to implementors: changes to the methods of this class will likely break any existing serialized models.
   */
  public static class Util {
    private Util() { }

    private final static int LOWER_8_MASK = (1 << 8) - 1;
    private final static long LOWER_63_MASK = Long.MAX_VALUE;
    public final static long EOS_HASH = 1337;

    public static final String LABEL_PREFIX = "__label__";
    public static final String END_OF_LINE_TOKEN = "</s>"; // used internally by FastText, not us
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("[\\s\\x00]");

    /**
     * Quickly (re-)hashes a pair of long hashes "somehow".
     *
     * @param hash1 the first long hash
     * @param hash2 the second long hash
     * @return a new hash of the ordered pair of inputted hashes
     */
    public static long hash(long hash1, long hash2) {
      return 0x2D108DAE83E550E6L + (hash1 << 9) + (hash1 >>> 3) + hash2;
    }

    /**
     * Calculates a modified form of the 64-bit FNV1-a hash.
     * Note that this variant uses chars rather than bytes for efficiency when dealing with Java 8 and earlier strings.
     * If subsequent versions offer direct access to string bytes that should be preferred instead.
     *
     * @param str a character sequence with the string to hash
     * @return a non-cryptographic, non-negative hash value for the string
     */
    public static long hash(CharSequence str) {
      long hash = 0xcbf29ce484222325L;
      for (int i = 0; i < str.length(); i++) {
        hash = (hash ^ (LOWER_8_MASK & str.charAt(i))) * 0x100000001b3L;
        hash = (hash ^ (str.charAt(i) >> 8)) * 0x100000001b3L;
      }
      return hash & LOWER_63_MASK;
    }

    private static String removeWhitespace(CharSequence token) {
      return WHITESPACE_PATTERN.matcher(token).replaceAll("_");
    }

    public static String formatIntegerLabel(int label) {
      return LABEL_PREFIX + Integer.toString(label);
    }

    public static int integerFromLabelString(String label) {
      return Integer.parseInt(label.substring(LABEL_PREFIX.length()));
    }

    public static String formatToken(CharSequence token) {
      if (token.toString().startsWith(LABEL_PREFIX) || token.equals(END_OF_LINE_TOKEN)) {
        token = "_" + token;
      }

      return removeWhitespace(token);
    }
  }

  /**
   * Represents a FastText model.
   *
   * @param <L> the type of label used by the model
   */
  public static class Model<L extends Serializable> extends AbstractEmbeddingClassifier<L, CharSequence> {
    private static final long serialVersionUID = 1;

    private final Long2ObjectOpenHashMap<DenseFloatArrayVector> _wordEmbeddings;
    private final DenseFloatArrayVector[] _ngramBucketEmbeddings;
    private final boolean _multilabel;
    private final DenseFloatArrayVector _eosEmbedding;
    private final int _maxNgramSize;

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof Model)) {
        return false;
      }
      if (!super.equals(o)) {
        return false;
      }
      Model<?> model = (Model<?>) o;
      return _multilabel == model._multilabel && _maxNgramSize == model._maxNgramSize && Objects.equals(_wordEmbeddings,
          model._wordEmbeddings) && Arrays.equals(_ngramBucketEmbeddings, model._ngramBucketEmbeddings)
          && Objects.equals(_eosEmbedding, model._eosEmbedding);
    }

    @Override
    public int hashCode() {
      int result = Objects.hash(super.hashCode(), _wordEmbeddings, _multilabel, _eosEmbedding, _maxNgramSize);
      result = 31 * result + Arrays.hashCode(_ngramBucketEmbeddings);
      return result;
    }

    public Model(L[] labels, DenseFloatArrayVector[] labelEmbeddings, Long2ObjectOpenHashMap<DenseFloatArrayVector> wordEmbeddings,
        DenseFloatArrayVector[] ngramBucketEmbeddings, boolean multilabel, int maxNgramSize) {
      super(labels, labelEmbeddings);
      _wordEmbeddings = wordEmbeddings;
      _ngramBucketEmbeddings = ngramBucketEmbeddings;
      _multilabel = multilabel;
      _maxNgramSize = maxNgramSize;
      _eosEmbedding = _wordEmbeddings.get(Util.EOS_HASH);
    }

    public <M extends Serializable> Model<M> remapLabels(Function<L, M> remapper) {
      return new Model<>((M[]) Arrays.stream(getLabels()).map(remapper).toArray(Serializable[]::new),
          getLabelEmbeddings(), _wordEmbeddings, _ngramBucketEmbeddings, _multilabel, _maxNgramSize);
    }

    /**
     * Add (bucketed) ngram embeddings to the target vector.  Only bigrams and larger are considered;
     * if maxNgramSize <= 1 this call is a no-op.
     * @param target the vector to which the embeddings will be added
     * @param tokenHashes the hashes of the original tokens; ngrams will be formed over these
     * @param maxNgramSize ngrams of size 2...maxNgramSize will be included
     *
     * @return the number of embeddings that were added to the target
     */
    int addBucketedNgrams(DenseFloatArrayVector target, LongArrayList tokenHashes, int maxNgramSize) {
      if (maxNgramSize <= 1) {
        return 0; // no-op; we only care about bigrams and larger
      }
      int count = 0;
      for (int i = 0; i < tokenHashes.size(); i++) {
        long ngramHash = tokenHashes.getLong(i);
        for (int j = i + 1; j < tokenHashes.size() && j < i + maxNgramSize; j++) {
          ngramHash = Util.hash(ngramHash, tokenHashes.getLong(j));
          target.addInPlace(_ngramBucketEmbeddings[(int) (Math.abs(ngramHash) % _ngramBucketEmbeddings.length)]);
          count++;
        }
      }

      return count;
    }

    @Override
    protected FeaturesEmbeddingResult embedFeatures(Iterable<? extends CharSequence> features) {
      DenseFloatArrayVector res = DenseFloatArrayVector.wrap(new float[getLabelEmbeddingDimensions()]);

      LongArrayList tokenHashes;

      if (features instanceof List) {
        tokenHashes = new LongArrayList(((List) features).size() + 1);
      } else {
        tokenHashes = new LongArrayList(16);
      }

      for (CharSequence rawToken : features) {
        String token = FastTextInternal.Util.formatToken(rawToken);
        long wordHash = Util.hash(token);
        DenseFloatArrayVector tokenEmbedding = _wordEmbeddings.get(wordHash);
        if (tokenEmbedding != null) {
          // note: this results in [word1] [unk word] [word 2] essentially becoming [word1] [word2];
          // discarding unknown words and finding ngrams over what remains is intentional, if questionable
          res.addInPlace(tokenEmbedding);
          tokenHashes.add(wordHash);
        }
      }

      int featuresInVocabulary = tokenHashes.size(); // keep track of how many words were in vocabulary

      if (_eosEmbedding != null) { // there are perverse corner cases where no EOS embedding exists
        res.addInPlace(_eosEmbedding);
        tokenHashes.add(Util.EOS_HASH);
      }

      int summedEmbeddingCount = tokenHashes.size();
      summedEmbeddingCount += addBucketedNgrams(res, tokenHashes, _maxNgramSize);

      res.multiplyInPlace(1.0 / summedEmbeddingCount);
      return new FeaturesEmbeddingResult(res, featuresInVocabulary);
    }

    private static double sigmoid(double x) {
      return 1.0 / (1.0 + Math.exp(-x));
    }

    @Override
    protected void distributionalize(float[] labelProbabilities) {
      if (_multilabel) {
        for (int i = 0; i < labelProbabilities.length; i++) {
          labelProbabilities[i] = (float) sigmoid(labelProbabilities[i]);
        }
      } else {
        // note: finding the max and subtracting it away is a mathematical noop, but it helps avoid numeric precision/
        // overflow issues:
        float max = labelProbabilities[0];
        for (int i = 1; i < labelProbabilities.length; i++) {
          max = Math.max(max, labelProbabilities[i]);
        }

        // softmax:
        float sum = 0;
        for (int i = 0; i < labelProbabilities.length; i++) {
          float exp = (float) Math.exp(labelProbabilities[i] - max);
          labelProbabilities[i] = exp;
          sum += exp;
        }
        for (int i = 0; i < labelProbabilities.length; i++) {
          labelProbabilities[i] /= sum;
        }
      }
    }
  }
}
