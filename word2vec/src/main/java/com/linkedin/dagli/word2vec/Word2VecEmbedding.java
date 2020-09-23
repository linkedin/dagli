package com.linkedin.dagli.word2vec;

import com.linkedin.dagli.annotation.equality.IgnoredByValueEquality;
import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.math.hashing.Murmurish;
import com.linkedin.dagli.math.vector.DenseFloatBufferVector;
import com.linkedin.dagli.transformer.AbstractPreparedTransformer1WithInput;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

/**
 * Embeds words using previously trained Word2Vec results, returning a list of dense vectors.  Since phrases of multiple
 * tokens are possible and unknown words are omitted, the list of dense vectors will quite possibly be shorter than the
 * list of words.  The Word2Vec data is loaded on the first apply() call.
 *
 * The Word2Vec data must be stored in a resource file that can be read via
 * {@code Word2VecEmbedding.class.getClassLoader().getResourceAsStream(resourcePath)}.  The default
 * {@code resourcePath} is {@code GoogleNews-vectors-negative300.bin.gz}, which matches the file name of the embeddings
 * provided by Google (these may be obtained from https://code.google.com/archive/p/word2vec/ via a link in the section
 * titled "Pre-trained word and phrase vectors").  However, <i>any</i> word/phrase embeddings may be used (regardless
 * of whether they were trained using Word2Vec) so long as they have the same file format.
 *
 * Note that Google's embeddings are fairly substantial in size and will be expensive to read in full (and store in
 * memory).
 */
@ValueEquality
public class Word2VecEmbedding
    extends AbstractPreparedTransformer1WithInput<List<String>, List<DenseFloatBufferVector>, Word2VecEmbedding> {
  private static final long serialVersionUID = 1;

  private static final int MAX_STRING_LENGTH = 1000; // this is just a conservative overestimate
  private static final int BUFFER_SIZE = 8 * 1024 * 1024; // 8MB
  private static final int MISSING_VECTOR_INDEX = -1;
  private static final int MARKER_VECTOR_INDEX = -2;

  @IgnoredByValueEquality
  private transient Long2IntOpenHashMap _embeddingIndex = null;

  @IgnoredByValueEquality
  private transient int _vectorLength = 0;

  @IgnoredByValueEquality
  private transient int _vectorsPerBuffer = 0;

  @IgnoredByValueEquality
  private transient FloatBuffer[] _floatBuffers = null;

  protected String _embeddingResourcePath = "GoogleNews-vectors-negative300.bin.gz";
  protected boolean _isEmbeddingResourceFileGZipped = true;

  protected int _maxDictionarySize = 100000;
  protected CaseSensitivity _caseSensitivity = CaseSensitivity.FALLBACK_TO_LOWER_CASE;

  protected EmbeddingListFormat _resultListFormat = EmbeddingListFormat.STANDARD;

  public enum CaseSensitivity {
    /**
     * Ignore all casing information.  In the case of duplicate phrases, the first to appear in the file is used.
     */
    IGNORE_CASE,

    /**
     * Try to find an exact match for a token, but, if that fails, check for the lower-cased version of the token.
     */
    FALLBACK_TO_LOWER_CASE,

    /**
     * Only look for exact matches for the text.
     */
    EXACT_CASE,
  }

  public enum EmbeddingListFormat {
    /**
     * The list including the sequence of embeddings that could be discovered for tokens and phrases in the input.  When
     * a phrase is embedded, the embedding for that phrase occurs in the result only once (and not once per constituent
     * token).  If an embedding cannot be found for a token, that token is simply ignored and no embedding for it will
     * be included in the result.
     */
    STANDARD,

    /**
     * Like STANDARD, except that if an embedding cannot be found for a token, null is added to the list of results
     * where that token's embedding would otherwise have been.  This allows you to, e.g. process the list of results to
     * discover how many out-of-dictionary tokens there were.
     */
    USE_NULL_FOR_UNKNOWN_TOKENS,

    /**
     * The result list will have the same length as the input.  Every input token has a corresponding vector or null at
     * the same index in the result list.  Here's how it works:
     * (1) If the token was not part of a phrase and has an embedding, that embedding vector appears in the result list
     * (2) If the token was part of a phrase and that phrase has an embedding, all tokens with that phrase get the same
     *     embedding vector (and thus this embedding vector will occur multiple times in the result).
     * (3) If an embedding could not be found for the token, a null is placed in the result list.
     */
    ONE_VECTOR_PER_TOKEN,
  }

  private void clearTransientState() {
    _embeddingIndex = null;
    _vectorLength = 0;
    _vectorsPerBuffer = 0;
    _floatBuffers = null;
  }

  /**
   * Returns a copy of this instance that will load embeddings from the specified, possibly-GZipped, resource file.
   *
   * Note that since resource files are normally compressed within JARs anyway, it is usually disadvantageous to use
   * additional GZip compression.  However, gzipping will substantially reduce the file size as stored in your
   * repository.
   *
   * @param path the resource path of the embeddings file
   * @param isGZipped true if the file is gzip-compressed, false otherwise
   * @return a copy of this instance that will load embeddings from the specified, possibly-GZipped, resource file
   */
  public Word2VecEmbedding withResourcePath(String path, boolean isGZipped) {
    return clone(c -> {
      c.clearTransientState();
      c._embeddingResourcePath = path;
      c._isEmbeddingResourceFileGZipped = isGZipped;
    });
  }

  /**
   * Returns a copy of this instance that will load no more than the number of embeddings specified from the underlying
   * data file.  Reading all embeddings is somewhat expensive (in both time and, especially, RAM) so it may be prudent
   *
   * Embeddings are loaded in the order they occur in the file, which should be most-common-phrase-first.
   *
   * @param maxDictionarySize the maximum number of embeddings that will be loaded.  Pass Integer.MAX_VALUE to load
   *                          all the embeddings.
   * @return a copy of this instance that will load the specified number of embeddings.
   */
  public Word2VecEmbedding withMaxDictionarySize(int maxDictionarySize) {
    return clone(c -> {
      c.clearTransientState();
      c._maxDictionarySize = maxDictionarySize;
    });
  }

  /**
   * Sets how embeddings are looked up with respect to the case of the provided tokens.  The default is to fall back
   * to lower case: if an exact case match cannot be found (e.g. for "Run"), the lower-cased version (e.g. "run") is
   * sought instead.
   *
   * @param caseSensitivity how the embeddings will be sought with respect to the casing of the tokens
   * @return a copy of this instance that will use the specified case sensitivity scheme
   */
  public Word2VecEmbedding withCaseSensitivity(CaseSensitivity caseSensitivity) {
    return clone(c -> {
      c.clearTransientState();
      c._caseSensitivity = caseSensitivity;
    });
  }

  /**
   * Since the transformer accepts a list of tokens, it also produces a list of embeddings.  However, exactly how that
   * list is constructed (e.g. using null placeholders for unknown tokens or not) is configurable; please see
   * {@link EmbeddingListFormat} for more information on the various options.  The default is "STANDARD": unknown tokens
   * will simply be ignored and omitted from the resultant list of embeddings.
   *
   * @param format how the list of the embeddings will be constructed
   * @return a copy of this instance that will use the specified embedding list format
   */
  public Word2VecEmbedding withEmbeddingListFormat(EmbeddingListFormat format) {
    return clone(c -> c._resultListFormat = format);
  }

  private FloatBuffer getFloatBufferForVectorIndex(int index) {
    return _floatBuffers[index / _vectorsPerBuffer];
  }

  private int getFloatBufferOffsetForVectorIndex(int index) {
    return (index % _vectorsPerBuffer) * _vectorLength;
  }

  // should only be invoked when this instance is locked, to avoid multiple threads modifying the embedding table
  // simultaneously
  private void readEmbeddings() {
    try (InputStream gzModelIS = Word2VecEmbedding.class.getClassLoader().getResourceAsStream(_embeddingResourcePath);
        InputStream modelIS = _isEmbeddingResourceFileGZipped ? new GZIPInputStream(gzModelIS) : gzModelIS;
        ReadableByteChannel channel = Channels.newChannel(modelIS);) {
      ByteBuffer b = ByteBuffer.allocateDirect(BUFFER_SIZE);
      b.order(ByteOrder.LITTLE_ENDIAN);

      int nextVectorIndex = 0;
      int numberRead = 0;
      int lastRead = 0;
      int maxRecordLength = 0;
      byte[] phraseBytes = new byte[MAX_STRING_LENGTH + 1];
      byte next;

      do {
        while (b.hasRemaining() && lastRead >= 0) {
          lastRead = channel.read(b);
        }

        b.flip(); // reset position to 0, limit = # bytes read into buffer
        if (_vectorLength == 0) { // we're reading the header
          StringBuilder strBuff = new StringBuilder(20);

          do {
            next = b.get();
            strBuff.append((char) next);
          } while (next != ' ');
          strBuff.setLength(strBuff.length() - 1);
          int embeddingCount = Math.min(_maxDictionarySize, Integer.valueOf(strBuff.toString()));
          _embeddingIndex = new Long2IntOpenHashMap(embeddingCount);
          _embeddingIndex.defaultReturnValue(MISSING_VECTOR_INDEX);

          strBuff.setLength(0);

          do {
            next = b.get();
            strBuff.append((char) next);
          } while (next != '\n');
          strBuff.setLength(strBuff.length() - 1);

          _vectorLength = Integer.valueOf(strBuff.toString());
          maxRecordLength = MAX_STRING_LENGTH + _vectorLength * 4;


          int vectorBytes = _vectorLength * 4;
          _vectorsPerBuffer = Integer.MAX_VALUE / vectorBytes;
          int maxBufferBytes = _vectorsPerBuffer * vectorBytes;
          long bytesNeeded = ((long) embeddingCount) * vectorBytes;

          _floatBuffers = new FloatBuffer[(int) ((bytesNeeded + maxBufferBytes - 1) / maxBufferBytes)];
          for (int i = 0; i < _floatBuffers.length - 1; i++) {
            _floatBuffers[i] = ByteBuffer.allocateDirect(maxBufferBytes).asFloatBuffer();
          }
          _floatBuffers[_floatBuffers.length - 1] =
              ByteBuffer.allocateDirect((int) (bytesNeeded % maxBufferBytes)).asFloatBuffer();
        }

        // now we're at the start of a record
        while (b.remaining() >= maxRecordLength || (lastRead < 0 && b.remaining() > 0)) {
          // read the word/phrase
          int byteOffset = 0;
          do {
            next = b.get();
            phraseBytes[byteOffset++] = next;
          } while (next != ' ');
          String phrase = new String(phraseBytes, 0, byteOffset - 1, StandardCharsets.UTF_8);
          if (_caseSensitivity == CaseSensitivity.IGNORE_CASE) {
            phrase = phrase.toLowerCase();
          }

          // now read the vector
          FloatBuffer bufferToRead = b.asFloatBuffer();
          bufferToRead.limit(_vectorLength);

          FloatBuffer buffer = getFloatBufferForVectorIndex(nextVectorIndex);
          buffer.position(getFloatBufferOffsetForVectorIndex(nextVectorIndex));
          buffer.put(bufferToRead);

          b.position(b.position() + _vectorLength * 4);

          // now update the embedding table
          String[] tokens = phrase.split("_");
          long hash = 0;
          for (int i = 0; i < tokens.length; i++) {
            hash = Murmurish.hash(tokens[i], (hash + (hash >> 31)));

            int existing = _embeddingIndex.get(hash);
            if (i == tokens.length - 1 && (existing == MISSING_VECTOR_INDEX || existing == MARKER_VECTOR_INDEX)) {
              _embeddingIndex.put(hash, nextVectorIndex);
              nextVectorIndex++;
            } else if (existing == MISSING_VECTOR_INDEX) {
              // make it known that a string with this prefix hash exists
              _embeddingIndex.put(hash, MARKER_VECTOR_INDEX);
            }
          }

          if (++numberRead >= _maxDictionarySize) {
            break;
          }
        }

        if (numberRead >= _maxDictionarySize) {
          break;
        }

        // move remaining data to the front of the buffer and prepare for next buffer fill
        b.compact();
      } while (lastRead >= 0); // stop on EOF
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
  }

  private int greedyMatch(List<String> tokens, int start, boolean lowercase, List<DenseFloatBufferVector> result) {
    long hash = 0;
    int bestVecIndex = -1;
    int bestLength = 1;
    for (int j = start; j < tokens.size(); j++) {
      String token = lowercase ? tokens.get(j).toLowerCase() : tokens.get(j);
      hash = Murmurish.hash(token, hash + (hash >> 31));

      int vectorIndex = _embeddingIndex.get(hash);
      if (vectorIndex == MISSING_VECTOR_INDEX) {
        break;
      } else if (vectorIndex != MARKER_VECTOR_INDEX) {
        bestLength = j - start + 1;
        bestVecIndex = vectorIndex;
      } // else we have a marker which indicates nothing usable...yet; keep going
    }

    if (bestVecIndex < 0) {
      // no match found
      if (!lowercase && _caseSensitivity == CaseSensitivity.FALLBACK_TO_LOWER_CASE) {
        return greedyMatch(tokens, start, true, result);
      } else {
        if (_resultListFormat == EmbeddingListFormat.USE_NULL_FOR_UNKNOWN_TOKENS
            || _resultListFormat == EmbeddingListFormat.ONE_VECTOR_PER_TOKEN) {
          result.add(null);
        }
        return 1;
      }
    } else {
      DenseFloatBufferVector vec = getVector(bestVecIndex);
      if (_resultListFormat == EmbeddingListFormat.ONE_VECTOR_PER_TOKEN) {
        // add the vector repeatedly, corresponding to the number of tokens in the corresponding phrase
        for (int i = 0; i < bestLength; i++) {
          result.add(vec);
        }
      } else {
        // just add the vector to the results once
        result.add(vec);
      }
      return bestLength;
    }
  }

  private DenseFloatBufferVector getVector(int vectorIndex) {
    return new DenseFloatBufferVector(getFloatBufferForVectorIndex(vectorIndex),
        getFloatBufferOffsetForVectorIndex(vectorIndex), _vectorLength);
  }

  @Override
  public List<DenseFloatBufferVector> apply(List<String> value0) {
    if (_embeddingIndex == null) {
      synchronized (this) {
        if (_embeddingIndex == null) {
          readEmbeddings();
        }
      }
    }

    List<DenseFloatBufferVector> result = new ArrayList<>();

    for (int i = 0; i < value0.size();) {
      i += greedyMatch(value0, i, _caseSensitivity == CaseSensitivity.IGNORE_CASE, result);
    }

    return result;
  }
}
