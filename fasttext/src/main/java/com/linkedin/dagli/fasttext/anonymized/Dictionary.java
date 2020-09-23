package com.linkedin.dagli.fasttext.anonymized;

import com.linkedin.dagli.fasttext.anonymized.io.BufferedLineReader;
import com.linkedin.dagli.fasttext.anonymized.io.LineReader;
import com.linkedin.dagli.embedding.classification.FastTextInternal;
import com.linkedin.dagli.tuple.Tuple2;
import com.linkedin.dagli.util.array.ArraysEx;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongArrays;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.stream.IntStream;

public class Dictionary {

  private static final int MAX_VOCAB_SIZE = 30000000;
  private static final int INITIAL_VOCAB_SIZE = 10000;
  private static final int INITIAL_LABEL_SIZE = 10000; // used to reduce # of resizes for label maps
  private static final int MAX_LINE_SIZE = 1024;
  private static final Integer WORDID_DEFAULT = -1;

  private static final String BOW = "<";
  private static final String EOW = ">";

  private Long2IntOpenHashMap _wordIDMap;
  private Long2IntOpenHashMap _labelIDMap;
  private long _totalWordsRead = 0;
  private long _totalLabelsRead = 0;
  private long[] _labelCounts;
  private String[] _labels;

  private Args args_;

  private String _charsetName = "UTF-8";
  private Class<? extends LineReader> _lineReaderClass = BufferedLineReader.class;

  public Dictionary(Args args) {
    args_ = args;
    _wordIDMap = null;
    _labelIDMap = null;
  }

  public Long2IntOpenHashMap getWordIDMap() {
    return _wordIDMap;
  }

  public long getTotalTokensRead() {
    return _totalLabelsRead + _totalWordsRead;
  }

  public int distinctWordCount() {
    return _wordIDMap.size();
  }

  public int distinctLabelCount() {
    return _labelIDMap.size();
  }

  public void readFromFile(String file) throws IOException {
    LineReader lineReader = null;
    Long2LongOpenHashMap wordCounts = new Long2LongOpenHashMap(INITIAL_VOCAB_SIZE);
    Long2LongOpenHashMap labelCounts = new Long2LongOpenHashMap(INITIAL_LABEL_SIZE);
    Long2ObjectOpenHashMap<String> labelStrings = new Long2ObjectOpenHashMap<>(INITIAL_LABEL_SIZE);

    try {
      try {
        lineReader = _lineReaderClass.getConstructor(String.class, String.class).newInstance(file, _charsetName);
      } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
        // this will never happen unless someone breaks one of the available line readers
        throw new RuntimeException(
            "FastText ecountered an error while trying to create its line reader: " + _lineReaderClass, e);
      }
      long lastUpdateInMillions = 0;
      long minThreshold = 1;
      long linesRead = 0;
      String[] lineTokens;
      while ((lineTokens = lineReader.readLineTokens()) != null) {
        linesRead++;
        for (int i = 0; i <= lineTokens.length; i++) {
          if (i == lineTokens.length) {
            wordCounts.addTo(FastTextInternal.Util.EOS_HASH, 1);
            _totalWordsRead++; // virtual EOS token counts as a word
          } else {
            String token = lineTokens[i];
            if (Utils.isEmpty(token)) {
              continue;
            }
            long hash = FastTextInternal.Util.hash(token);
            if (token.startsWith(args_.label)) {
              labelCounts.addTo(hash, 1);
              _totalLabelsRead++;
              labelStrings.put(hash, token);
            } else {
              wordCounts.addTo(hash, 1);
              _totalWordsRead++;
            }
          }
        }

        if (args_.verbose > 1 && ((_totalLabelsRead + _totalWordsRead) / 1000000) > lastUpdateInMillions) {
          lastUpdateInMillions = (_totalLabelsRead + _totalWordsRead) / 1000000;
          System.err.printf("Read %dM tokens\n", lastUpdateInMillions);
        }
        if (wordCounts.size() > 0.75 * MAX_VOCAB_SIZE) {
          minThreshold++;
          threshold(wordCounts, minThreshold);
        }
      }

      if (args_.verbose > 1) {
        System.err.println("FastText found " + linesRead + " lines in its input file");
      }
    } finally {
      if (lineReader != null) {
        lineReader.close();
      }
    }

    {
      threshold(wordCounts, args_.minCount);
      Tuple2<long[], long[]> sortedWordHashCounts = getParallelArraysReverseSortedByCount(wordCounts);
      _wordIDMap = new Long2IntOpenHashMap(sortedWordHashCounts.get0(),
          IntStream.range(0, sortedWordHashCounts.get0().length).toArray());
      _wordIDMap.defaultReturnValue(-1);
    }

    {
      threshold(labelCounts, args_.minCountLabel);
      Tuple2<long[], long[]> sortedLabelHashCounts = getParallelArraysReverseSortedByCount(labelCounts);
      _labelCounts = sortedLabelHashCounts.get1();
      _labelIDMap = new Long2IntOpenHashMap(sortedLabelHashCounts.get0(),
          IntStream.range(0, sortedLabelHashCounts.get0().length).toArray());
      _labelIDMap.defaultReturnValue(-1);

      _labels = new String[_labelIDMap.size()];
      _labelIDMap.long2IntEntrySet()
          .fastForEach(entry -> _labels[entry.getIntValue()] = labelStrings.get(entry.getLongKey()));
    }


    //initTableDiscard();
    if (args_.verbose > 0) {
      System.out.printf("\rRead %dM tokens\n", (_totalLabelsRead + _totalWordsRead) / 1000000);
      System.out.println("Number of words:  " + distinctWordCount());
      System.out.println("Number of labels: " + distinctLabelCount());
    }
    if (_wordIDMap.isEmpty()) {
      throw new IllegalStateException("Empty vocabulary. Try a smaller -minCount value.");
    }
  }

  /**
   * Dumps the entries of a counts table into two arrays, one of the hashes (keys) and the other of the counts
   * (values) sorted in decreasing order of the counts.
   *
   * @param counts a table of hash -> count pairs.
   * @return a pair of arrays, the (reverse-sorted) hashes and the counts, respectively.
   */
  private static Tuple2<long[], long[]> getParallelArraysReverseSortedByCount(Long2LongOpenHashMap counts) {
    long[] hashArr = new long[counts.size()];
    long[] countArr = new long[counts.size()];
    int next = 0;

    for (Long2LongMap.Entry entry : counts.long2LongEntrySet()) {
      hashArr[next] = entry.getLongKey();
      countArr[next] = entry.getLongValue();
      next++;
    }

    ArraysEx.sort(countArr, hashArr);
    return Tuple2.of(LongArrays.reverse(hashArr), LongArrays.reverse(countArr));
  }

  private static void threshold(Long2LongOpenHashMap counts, long minCount) {
    counts.long2LongEntrySet().removeIf(e -> e.getLongValue() < minCount);
  }

  public long[] getLabelCounts() {
    return _labelCounts;
  }

  public IntArrayList getNgramRowIDs(LongArrayList wordHashes, int maxNgramSize, int wordCount, int ngramBucketCount) {
    if (maxNgramSize <= 1) {
      return new IntArrayList(0);
    }

    // for convenience, use a slight overestimate of the # of ngrams to initialize the list:
    IntArrayList res = new IntArrayList((maxNgramSize - 1) * wordHashes.size());

    for (int i = 0; i < wordHashes.size(); i++) {
      long ngramHash = wordHashes.getLong(i);
      for (int j = i + 1; j < wordHashes.size() && j < i + maxNgramSize; j++) {
        ngramHash = FastTextInternal.Util.hash(ngramHash, wordHashes.getLong(j));
        res.add(FastText.embeddingRowIndexForNgramHash(ngramHash, wordCount, ngramBucketCount));
      }
    }

    return res;
  }

  /**
   * Adds a word to the dictionary if it is not present, and returns the ID (new or existing) of the word.
   *
   * @param word a word, which may be novel
   * @return the ID of the word
   */
  public int addWord(String word) {
    return _wordIDMap.putIfAbsent(FastTextInternal.Util.hash(word), _wordIDMap.size());
  }

  public int getLine(String[] tokens, LongArrayList wordHashes, IntArrayList wordIDs, IntArrayList labelIDs) {
    wordIDs.clear();
    wordHashes.clear();
    labelIDs.clear();
    if (tokens != null) {
      for (int i = 0; i <= tokens.length; i++) {
        if (i < tokens.length && Utils.isEmpty(tokens[i])) {
          continue;
        }
        long hash = (i == tokens.length) ? FastTextInternal.Util.EOS_HASH : FastTextInternal.Util.hash(tokens[i]);

        if (i < tokens.length && tokens[i].startsWith(args_.label)) {
          int id = _labelIDMap.get(hash);
          if (id >= 0) {
            labelIDs.add(id);
          }
        } else {
          int id = _wordIDMap.get(hash);
          if (id >= 0) {
            wordHashes.add(hash);
            wordIDs.add(id);
          }
        }
      }
    }
    return labelIDs.size() + wordIDs.size();
  }

  public String[] getLabels() {
    return _labels;
  }

  public int getWordID(String word) {
    return _wordIDMap.get(FastTextInternal.Util.hash(word));
  }

  public int getLabelID(String label) {
    return _labelIDMap.get(FastTextInternal.Util.hash(label));
  }

  public String getLabel(int lid) {
    return _labels[lid];
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("Dictionary [wordsIDMap_=");
    builder.append(_wordIDMap);
    builder.append(", nlabels_=");
    builder.append(_labels.length);
    builder.append("]");
    return builder.toString();
  }

  public Args getArgs() {
    return args_;
  }

  public String getCharsetName() {
    return _charsetName;
  }

  public Class<? extends LineReader> getLineReaderClass() {
    return _lineReaderClass;
  }

  public void setCharsetName(String charsetName) {
    this._charsetName = charsetName;
  }

  public void setLineReaderClass(Class<? extends LineReader> lineReaderClass) {
    this._lineReaderClass = lineReaderClass;
  }
}
