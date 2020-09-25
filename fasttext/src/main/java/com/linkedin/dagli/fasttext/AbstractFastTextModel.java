package com.linkedin.dagli.fasttext;

import com.linkedin.dagli.embedding.classification.FastTextInternal;
import com.linkedin.dagli.fasttext.anonymized.Args;
import com.linkedin.dagli.fasttext.anonymized.FastText;
import com.linkedin.dagli.fasttext.anonymized.FastTextOptions;
import com.linkedin.dagli.function.FunctionResult1;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.transformer.AbstractPreparableTransformer2;
import com.linkedin.dagli.transformer.PreparedTransformer2;
import com.linkedin.dagli.util.cryptography.Cryptography;
import com.linkedin.dagli.util.environment.DagliSystemProperties;
import com.linkedin.dagli.util.io.SerializableTempFile;
import com.linkedin.migz.MiGzOutputStream;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;


/**
 * FastText classifiers are simple creatures that expect string "words".
 * These are internally mapped to int32s, but this is not exposed via existing APIs.
 * FastTextClassifier allows you to use any type of label; this is handled internally by assigning the labels to
 * (unique) strings, and is transparent to clients of this class.
 *
 * FastText is a multilabel classifier: an example may have multiple labels.
 *
 * FastText was built with natural text in mind, which brings with it some limitations:
 * - Weights are not supported by FastText at present, although you may repeat tokens and examples at your leisure
 * - Features and labels must be strings.  They are passed to the transformer as string iterables.  However, for
 *   non-text binary features, you may easily pass the name of the feature, the stringified hash, etc.  Note that by
 *   default FastText will compute word ngrams for these strings, which is probably undesirable!
 *
 * In the input tokens, character c == ' ' || c == '\n' || c == '\r' || c == '\t' || c == '\v' || c == '\f' || c == '\0'
 * will be replaced by a "_".  <strong>Token strings must be valid UTF-16 sequences</strong> as they will be converted
 * into UTF-8.
 *
 * Conflicts of tokens with special FastText strings are handled by prepending underscores:
 * The string "&lt;/s&gt;" is special to FastText and will be rewritten into "_&lt;/s&gt;".
 * The prefix "__label__" is also special and will be rewritten to "___label__".
 *
 * This is invisible to clients except that it may cause tokens that would otherwise not collide to collide in
 * specially-constructed corner cases (e.g. if you had both word tokens "__label__" and "___label__" in your input.
 * The labels themselves, even if they are strings, have no restrictions and will not be altered in any way.
 *
 * @param <L> type of the label.  T must (1) have hashCode() and equals() implementations and (2) be serializable.
 */
abstract class AbstractFastTextModel<
    L extends Serializable,
    R,
    N extends PreparedTransformer2<Iterable<? extends L>, Iterable<? extends CharSequence>, R>,
    S extends AbstractFastTextModel<L, R, N, S>>
  extends AbstractPreparableTransformer2<Iterable<? extends L>, Iterable<? extends CharSequence>, R, N, S> {

  private static final long serialVersionUID = 1;

  protected int _topK = 10;

  protected int _verbosity = 2;
  protected int _minTokenCount = 5;
  protected int _minLabelCount = 0;
  protected int _maxWordNgramLength = 1;
  protected int _bucketCount = 2000000;
  // protected int _minCharacterNgramLength = 3;
  // protected int _maxCharacterNgramLength = 6;
  protected double _samplingThreshold = 0.0001;

  protected double _learningRate = 0.05;

  protected int _learningRateUpdateRate = 100;
  protected int _embeddingLength = 100;
  // protected int contextWindowSize = 5; // not relevant here
  protected int _epochs = 5;
  protected int _sampledNegatives = 5;
  protected FastTextLoss _loss = FastTextLoss.NEGATIVE_SAMPLING;
  protected int _threads = -1;
  protected boolean _isMultilabel = false;
  protected boolean _synchronizedTrainingStart = false;

  protected SerializableTempFile _pretrainedEmbeddings = null;

  protected FastTextDataSerializationMode _dataSerializationMode = FastTextDataSerializationMode.DEFAULT;

  /**
   * Whether or not training will commence only when all threads have reached their equidistant starting points in
   * the training data (FastText multithreads by having each training thread loop over the data in parallel, starting at
   * different positions).  If false, each thread starts as soon as it reaches its predetermined starting position in
   * the data without waiting for any other thread; this makes training faster to some degree (since threads don't spend
   * time waiting for other threads) but also results in the expected number of times examples are considered during
   * training being different depending on the order of the examples.
   *
   * This option is false by default.  This setting matters most when reading the underlying source file is slow and
   * the number of epochs is low relative to the number of threads.
   *
   * @return whether or not synchronized start is enabled
   */
  public boolean getSynchronizedTrainingStart() {
    return _synchronizedTrainingStart;
  }

  /**
   * Determines whether or not training will commence only when all threads have reached their equidistant starting
   * points in the training data (FastText multithreads by having each training thread loop over the data in parallel,
   * starting at different positions).  If false, each thread starts as soon as it reaches its predetermined starting
   * position in the data without waiting for any other thread; this makes training faster to some degree (since threads
   * don't spend time waiting for other threads) but also results in the expected number of times examples are
   * considered during training being different depending on the order of the examples.
   *
   * This option is false by default.  This setting matters most when reading the underlying source file is slow and
   * the number of epochs is low relative to the number of threads.
   *
   * @param synchronizedStart true to synchronize the start of training across threads; this helps ensure a more even
   *                          effective weighting of the examples at the cost of increased training time.
   * @return a copy of this instance with the synchronized start option set as specified
   */
  public S withSynchronizedTrainingStart(boolean synchronizedStart) {
    return clone(c -> c._synchronizedTrainingStart = synchronizedStart);
  }

  /**
   * @return the maximum number of predictions returned by the model
   */
  public int getMaxPredictionCount() {
    return _topK;
  }

  /**
   * @return a verbosity level that reflects how much information will be displayed during training; the default level
   *         is 2.
   */
  public int getVerbosity() {
    return _verbosity;
  }

  /**
   * Tokens occurring fewer than this many times in the training data will be ignored.
   *
   * @return the minimum frequency of any particular token to be considered during training
   */
  public int getMinTokenCount() {
    return _minTokenCount;
  }

  /**
   * Labels appearing fewer than this many times will be ignored.
   *
   * @return the minimum number of examples required for a label for that label to be considered by the model
   */
  public int getMinLabelCount() {
    return _minLabelCount;
  }

  /**
   * The maximum ngram size to be considered.  All ngrams from unigrams to maxWordNgramLength-grams will be extracted
   * from example text and used to calculate the overall embedding for said text.
   *
   * Ngrams other than unigrams are bucketed, so this value does not directly affect memory usage.
   *
   * @return the maximum ngram size
   */
  public int getMaxWordNgramLength() {
    return _maxWordNgramLength;
  }

  /**
   * The number of buckets used to store emeddings for ngrams other than unigrams.  More buckets will reduce the
   * frequency of collisions between differing ngrams at the cost of addition memory and, potentially, greater risk of
   * overfitting.
   *
   * @return the bucket count to be used
   */
  public int getBucketCount() {
    return _bucketCount;
  }

  /**
   * The learning rate acts as a multiple for the updates the embeddings for words/ngrams and labels receive; with
   * higher learning rates these embeddings move more quickly when updated for each training example.
   *
   * This value is the <i>initial</i> learning rate; the actual learning rate used decays linearly over the course of
   * training, eventually reaching ~0 at the end.  The default learning rate is 0.05.
   *
   * @return the initial learning rate
   */
  public double getLearningRate() {
    return _learningRate;
  }

  /**
   * Gets the learning rate update rate, which determines the number of tokens each thread sees before it adds that
   * number to the global number of tokens, which is then used to determine the decay of the learning rate; training
   * stops after (# tokens in corpus) * (# epochs) have been seen across all threads, and the learning rate decays
   * linearly as progress is made.
   *
   * This also has a (more important) side effect of "committing" the updating thread's current view of memory with
   * all other threads when they next read the global token count (which they do before every example).  Note that
   * because FastText depends on a "hog-wild" weight update scheme of massively unsynchronized memory reads and writes
   * execution is at the whim of data races, so we can't really speak of the effect that writing and then reading an
   * atomic would normally have (a happens-before relationship, in Java memory model parlance) because we have no real
   * guarantees.
   *
   * @return the learning rate update rate
   */
  public int getLearningRateUpdateRate() {
    return _learningRateUpdateRate;
  }

  /**
   * Gets the length of the embeddings used for words, ngrams and labels.
   *
   * Longer embeddings give the model more parameters, which may allow it to be capture semantics but also creates a
   * risk of overfitting and, of course, will increase training time.  The default value is 100.
   *
   * @return the embeddign length used by the model
   */
  public int getEmbeddingLength() {
    return _embeddingLength;
  }

  /**
   * Gets the number of epochs training will run for.  As FastText is multithreaded, this in practice means that all
   * threads will collectively examine (# epochs) * (# tokens in corpus) tokens before training stops, which
   * approximates, but is not the same as, making (# epochs) full and complete passes over the training data.  Using
   * {@link AbstractFastTextModel#withSynchronizedTrainingStart(boolean)} can help make this approximation closer in
   * some cases.
   *
   * The default number of epochs is 5.
   *
   * @return the number of epochs to run for
   */
  public int getEpochCount() {
    return _epochs;
  }

  /**
   * Gets the number of negative labels to sample for each example.  The default is 5.
   *
   * This value has no effect if the loss type is set to "softmax".
   *
   * If you have a small number of distinct labels in your problem (e.g. less than a couple dozen), don't use negative
   * sampling.  Instead, set the loss type to softmax so *all* negatives are considered.  If negative sampling is used
   * then the same negative labels may be repeatedly sampled for a single example, which may harm performance.
   *
   * @return the number of negatives to sample per example
   */
  public int getSampledNegativeCount() {
    return _sampledNegatives;
  }

  /**
   * Gets the mechanism used to calculate the loss, e.g. negative sampling (default) or softmax.
   *
   * In softmax, all the embeddings for the negative labels corresponding to a given example are updated.  This is
   * expensive when the number of labels is large.
   *
   * In negative sampling, the negative labels are sampled, so that only [sampledNegatives] are actually updated
   * for each example in each epoch.  This is the default behavior.
   *
   * It's a bad idea to use negative sampling when the number of labels is small (sampling negatives makes the most
   * sense when {@code [actual number of labels] >> [sampledNegatives]}).  Sampling is done with replacement, so in a
   * hypothetical scenario with 4 labels and [sampledNegatives] == 4, for any given example at least one negative will
   * be sampled twice and one or more negative labels may not be sampled at all!  Instead, for relatively small label
   * spaces, just use softmax loss.
   *
   * @return the loss type to be used
   */
  public FastTextLoss getLossType() {
    return _loss;
  }

  /**
   * Gets the number of threads to use during training.  The default value is -1, which means tells FastText to use one
   * thread per logical processor (core) on the machine.
   *
   * @return the number of threads to use, or -1 for "as many logical processors as there are on the machine".
   */
  public int getThreadCount() {
    return _threads;
  }

  /**
   * Gets whether classification will be done via multilabel or multinomial prediction.
   *
   * In multilabel classification, multiple labels (or none) can apply to a given example, and the probabilities over
   * all labels need not sum to 1.
   *
   * In multinomial classification (the default), only one label can apply to a given example, and the probabilities
   * over all labels will sum to 1 (note that, in practice, distributions are often truncated for efficiency and may not
   * provide the probabilities for all labels).
   *
   * In both cases, the examples used for training may still have any number of labels.
   *
   * @return true if multilabel classification is used, false if multinomial
   */
  public boolean isMultilabel() {
    return _isMultilabel;
  }

  //public SerializableTempFile getPretrainedEmbeddings() {
  //  return _pretrainedEmbeddings;
  //}

  /**
   * Gets the serialization mode that determines what encryption and/or compression, if any, is used for temporary data
   * written to disk.
   *
   * @return an {@link FastTextDataSerializationMode}
   */
  public FastTextDataSerializationMode getDataSerializationMode() {
    return _dataSerializationMode;
  }

  /**
   * Sets the input that will provide the labels.  In FastText, an example may have any (non-negative) number
   * of labels.  If you have only a single label for each example, the withLabelInput(...) method may be more
   * convenient.
   *
   * @param input the producer providing zero or more labels for each example
   * @return a copy of this instance with the specified labels input
   */
  public S withLabelsInput(Producer<? extends Iterable<? extends L>> input) {
    return clone(c -> c._input1 = input);
  }

  /**
   * Convenience method that invokes withLabelsInput(...), wrapping the single string input as an iterable of one
   * or zero labels (if the string is not null or null, respectively).
   *
   * @param input something that provides a single label, or provides null when there is no label
   * @return a copy of this instance that will use the specified label input.
   */
  public S withLabelInput(Producer<? extends L> input) {
    return withLabelsInput(
        new FunctionResult1<L, Iterable<L>>().withFunction(Collections::singleton)
            .withInput(input));
  }

  /**
   * Sets the input that will provide the tokens (the text) of the examples.
   *
   * @param input a producer providing the tokenized text of the example
   * @return a copy of this instance that will use the specified tokens input
   */
  public S withTokensInput(Producer<? extends Iterable<? extends CharSequence>> input) {
    return clone(c -> c._input2 = input);
  }

  /**
   * Sets a file containing pretrained embeddings to be used to initialize FastText.  If null, random initialization
   * will be used (this is the default).
   *
   * This file need only exist for the life of the program.  If this instance is serialized, the file will be copied
   * into the serialized object; if you save and later deserialize this instance (or a DAG containing it) the file need
   * not still exist (and will not be used).
   *
   * @param pretrainedEmbeddings the file containing the embeddings to use, or null to use random initialization.
   * @return a copy of this instance that will use the specified pretrained embeddings.
   */
  public S withPretrainedEmbeddings(File pretrainedEmbeddings) {
    return clone(c -> c._pretrainedEmbeddings = pretrainedEmbeddings == null ? null
        : new SerializableTempFile(pretrainedEmbeddings, "FastTextPretrained", ".vec"));
  }

  /**
   * Gets a new FastTextClassifier that will return no more than the specified number of predictions at inference-time.
   * The default is 10.
   *
   * @param topK limit on the number of predictions returned
   * @return a new FastTextClassifier that will return no more than the specified number of predictions at
   *        inference-time.
   */
  public S withMaxPredictionCount(int topK) {
    return clone(c -> c._topK = topK);
  }

  /**
   * Get a new FastTextClassifier with the specified verbosity; the default is 2
   *
   * @param verbosity how vociferous FastText will be
   * @return a new FastTextClassifier with the specified verbosity
   */
  public S withVerbosity(int verbosity) {
    return clone(c -> c._verbosity = verbosity);
  }

  /**
   * Get a new FastTextClassifier with the specified minimum token (feature/string/word) count.  Tokens appearing fewer
   * times than this will be omitted from the dictionary, although their character ngrams may still be present.
   *
   * @param minTokenCount minimum number of times the token must appear to be remembered
   * @return a new FastTextClassifier with the minimum token count
   */
  public S withMinTokenCount(int minTokenCount) {
    return clone(c -> c._minTokenCount = minTokenCount);
  }

  /**
   * Get a new FastTextClassifier with the specified minimum label count.  Labels appearing fewer
   * times than this will be ignored.
   *
   * @param minLabelCount minimum number of times a label must appear to be remembered
   * @return a new FastTextClassifier with the minimum label count
   */
  public S withMinLabelCount(int minLabelCount) {
    return clone(c -> c._minLabelCount = minLabelCount);
  }

  /**
   * Get a new FastTextClassifier with the specified maximum token ngram length.  Ngrams from 1 to this length will
   * be generated from the sequence of tokens provided.  If your sequence does not have a meaningful order you should
   * leave this at 1 so only unigrams are generated.
   *
   * @param maxWordNgramLength maximum token ngram size to generate
   * @return a new FastTextClassifier with the maximum token ngram size
   */
  public S withMaxWordNgramLength(int maxWordNgramLength) {
    return clone(c -> c._maxWordNgramLength = maxWordNgramLength);
  }

  /**
   * Get a new FastTextClassifier with the specified bucket count.  FastText saves space by mapping all hashed ngrams
   * into a fixed number of buckets (as opposed to maintaining a comprehensive dictionary).  Collisions mean that
   * multiple distinct ngrams may map to the same feature as far as the model is concerned.
   *
   * More buckets means more memory consumption (i.e. more embeddings must be stored) but fewer collisions.
   *
   * @param bucketCount number of buckets to use
   * @return a new FastTextClassifier with the specified number of buckets
   */
  public S withBucketCount(int bucketCount) {
    return clone(c -> c._bucketCount = bucketCount);
  }

  /**
   * Set the minimum character length for sub-word character ngrams.
   *
   * This is currently not supported for classification, but may be in the future.
   *
   * @param minCharacterNgramLength
   * @return
   */
  //public FastTextClassification withMinCharacterNgramLength(int minCharacterNgramLength) {
  //  return clone(c -> c._minCharacterNgramLength = minCharacterNgramLength);
  //}

  /**
   * Set the maximum character length for sub-word character ngrams.
   *
   * This is currently not supported for classification, but may be in the future.
   *
   * @param maxCharacterNgramLength
   * @return
   */
  //public FastTextClassification withMaxCharacterNgramLength(int maxCharacterNgramLength) {
  //  return clone(c -> c._maxCharacterNgramLength = maxCharacterNgramLength);
  //}



  /**
   * The learning rate acts as a multiple for the updates that the embeddings for words/ngrams and labels receive; with
   * higher learning rates these embeddings move more quickly when updated for each training example.
   *
   * This value is the <i>initial</i> learning rate; the actual learning rate used decays linearly over the course of
   * training, eventually reaching approximately 0 at the end.  The default learning rate is 0.05.
   *
   * @param learningRate the initial learning rate to use
   * @return a copy of this instance with the specified initial learning rate.
   */
  public S withLearningRate(double learningRate) {
    return clone(c -> c._learningRate = learningRate);
  }

  /**
   * Except in very rare corner cases, you should leave this property at its default value of 100.  Read on for more
   * detail.
   *
   * Determines the number of tokens each thread sees before it adds that number to the global number of tokens, which
   * is then used to determine the decay of the learning rate; training stops after (# tokens in corpus) * (# epochs)
   * have been seen across all threads, and the learning rate decays linearly as progress is made.
   *
   * This also has a (more important) side effect of "committing" the updating thread's current view of memory with
   * all other threads when they next read the global token count (which they do before every example).  Note that
   * because FastText depends on a "hog-wild" weight update scheme of massively unsynchronized memory reads and writes
   * execution is at the whim of data races, so we can't really speak of the effect that writing and then reading an
   * atomic would normally have (a happens-before relationship, in Java memory model parlance) because we have no real
   * guarantees.
   *
   * Increasing this value <b>might</b> make your training faster at the expense of more stale weights and learning
   * rates.
   *
   * @param learningRateUpdateRate the number of tokens the thread will see before updating the global token count.
   * @return a copy of this instance with the specified learning rate update rate.
   */
  public S withLearningRateUpdateRate(int learningRateUpdateRate) {
    return clone(c -> c._learningRateUpdateRate = learningRateUpdateRate);
  }

  /**
   * Sets the length of the embeddings used for words, ngrams and labels.
   *
   * Longer embeddings give the model more parameters, which may allow it to be capture semantics but also creates a
   * risk of overfitting and, of course, will increase training time.  The default value is 100.
   *
   * @param embeddingLength the length of embeddings to use
   * @return a copy of this instance that will use the specified embedding length
   */
  public S withEmbeddingLength(int embeddingLength) {
    return clone(c -> c._embeddingLength = embeddingLength);
  }

  /**
   * Sets the number of epochs training will run for.  As FastText is multithreaded, this in practice means that all
   * threads will collectively examine (# epochs) * (# tokens in corpus) tokens before training stops, which
   * approximates, but is not the same as, making (# epochs) full and complete passes over the training data.  Using
   * {@link AbstractFastTextModel#withSynchronizedTrainingStart(boolean)} can help make this approximation closer in
   * some cases.
   *
   * The default number of epochs is 5.
   *
   * @param epochs roughly speaking, the number of passes over the data training will make
   * @return a copy of this instance that will use the specified number of epochs
   */
  public S withEpochCount(int epochs) {
    return clone(c -> c._epochs = epochs);
  }

  /**
   * The number of negative labels to sample for each example.  The default is 5.
   *
   * This value has no effect if the loss type is set to "softmax".
   *
   * If you have a small number of distinct labels in your problem (e.g. less than a couple dozen), don't use negative
   * sampling.  Instead, set the loss type to softmax so *all* negatives are considered.  If negative sampling is used
   * then the same negative labels may be repeatedly sampled for a single example, which may harm performance.
   *
   * @param sampledNegatives the number of negative labels to sample for each example.
   * @return a copy of this instance that will use the specified number of sampled negatives.
   */
  public S withSampledNegativeCount(int sampledNegatives) {
    return clone(c -> c._sampledNegatives = sampledNegatives);
  }

  /**
   * Sets the mechanism used to calculate the loss, e.g. negative sampling (default) or softmax.
   *
   * In softmax, all the embeddings for the negative labels corresponding to a given example are updated.  This is
   * expensive when the number of labels is large.
   *
   * In negative sampling, the negative labels are sampled, so that only [sampledNegatives] are actually updated
   * for each example in each epoch.  This is the default behavior.
   *
   * It's a bad idea to use negative sampling when the number of labels is small (sampling negatives makes the most
   * sense when {@code [actual number of labels] >> [sampledNegatives]}).  Sampling is done with replacement, so in a
   * hypothetical scenario with 4 labels and [sampledNegatives] == 4, for any given example at least one negative will
   * be sampled twice and one or more negative labels may not be sampled at all!  Instead, for relatively small label
   * spaces, just use softmax loss.
   *
   * @param loss the type of mechanism used to calculate the loss
   * @return a copy of this instance that will use the specified loss type
   */
  public S withLossType(FastTextLoss loss) {
    return clone(c -> c._loss = loss);
  }

  /**
   * Get a new FastTextClassifier that will use the specified number of threads.  The default value is -1, which means
   * "number of processors this machine has".
   *
   * @param threads number of threads to use
   * @return a new FastTextClassifier with the specified number of threads
   */
  public S withThreadCount(int threads) {
    return clone(c -> c._threads = threads);
  }

  /**
   * Determines whether classification will be done via multilabel or multinomial prediction.
   *
   * In multilabel classification, multiple labels (or none) can apply to a given example, and the probabilities over
   * all labels need not sum to 1.
   *
   * In multinomial classification (the default), only one label can apply to a given example, and the probabilities
   * over all labels will sum to 1 (note that, in practice, distributions are often truncated for efficiency and may not
   * provide the probabilities for all labels).
   *
   * In both cases, the examples used for training may still have any number of labels.
   *
   * @param isMultilabel if true, multilabel classification will be used, otherwise multinomial
   * @return a new instance like this one but with the specified multilabel or multinomial prediction mode
   */
  public S withMultilabel(boolean isMultilabel) {
    return clone(c -> c._isMultilabel = isMultilabel);
  }

  /**
   * Determines whether temporary data files created for the FastText model will be encrypted and/or compressed.
   *
   * Compression is useful when you have a very large data set, a spinning disk, and many cores, as it effectively
   * trades CPU for I/O and can alleviate an I/O bottleneck.
   *
   * Encryption adds computational cost and is only warranted in circumstances where training examples being written to
   * disk in plaintext poses a security concern.  A random encryption key specific to this JVM instance will be used
   * to encrypt, so there's no need to provide your own key.
   *
   * @param mode the {@link FastTextDataSerializationMode} to use
   * @return a new instance like this one but using the specified serialization mode
   */
  public S withDataSerializationMode(FastTextDataSerializationMode mode) {
    return clone(c -> c._dataSerializationMode = mode);
  }

  protected Producer<? extends Iterable<? extends L>> getLabelsInput() {
    return _input1;
  }

  protected Producer<? extends Iterable<? extends CharSequence>> getTokensInput() {
    return _input2;
  }

  protected static class Trainer<T extends Serializable> {
    private final OutputStreamWriter _exampleWriter;
    private final Path _tempFile;
    private long _exampleCount = 0;

    private final AbstractFastTextModel<T, ?, ?, ?> _owner;
    private final Object2IntOpenHashMap<T> _labelMap = new Object2IntOpenHashMap<>();

    public Trainer(AbstractFastTextModel<T, ?, ?, ?> owner) {
      _owner = owner;

      try {
        _tempFile = Files.createTempFile(Paths.get(DagliSystemProperties.getTempDirectory()), "FastText", ".dat");
        _tempFile.toFile().deleteOnExit();

        OutputStream outputStream = Files.newOutputStream(_tempFile);

        if (owner.getDataSerializationMode().isEncrypted()) {
          outputStream = Cryptography.getOutputStream(outputStream);
        }

        if (owner.getDataSerializationMode().isCompressed()) {
          outputStream = new MiGzOutputStream(outputStream,
              _owner._threads >= 1 ? _owner._threads : Runtime.getRuntime().availableProcessors(),
              MiGzOutputStream.DEFAULT_BLOCK_SIZE);
        }

        _exampleWriter = new OutputStreamWriter(outputStream);
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      } catch (NoSuchAlgorithmException e) {
        throw new UnsupportedOperationException("The requisite encryption algorithm is not supported by this JVM", e);
      }
    }

    private int getLabelIndex(T label) {
      return _labelMap.computeIfAbsent(label, k -> _labelMap.size());
    }

    public FastTextInternal.Model<T> finish() {
      try {
        _exampleWriter.close();
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }

      Args args = new Args();
      args.hashBits = 63;

      args.parseArgs(new String[]{
          "supervised",

          "-input", _tempFile.toAbsolutePath().toString(),

          "-verbose", String.valueOf(_owner._verbosity),
          "-minCount", String.valueOf(_owner._minTokenCount),
          "-minCountLabel", String.valueOf(_owner._minLabelCount),
          "-wordNgrams", String.valueOf(_owner._maxWordNgramLength),
          "-bucket", String.valueOf(_owner._bucketCount),
          "-t", String.valueOf(_owner._samplingThreshold),

          "-lr", String.valueOf(_owner._learningRate),
          "-lrUpdateRate", String.valueOf(_owner._learningRateUpdateRate),
          "-dim", String.valueOf(_owner._embeddingLength),
          "-epoch", String.valueOf(_owner._epochs),
          "-neg", String.valueOf(_owner._sampledNegatives),
          "-loss", _owner._loss.getArgumentName(),
          "-pretrainedVectors",
          _owner._pretrainedEmbeddings == null ? "" : _owner._pretrainedEmbeddings.getFile().getAbsolutePath(),
          "-thread",
          String.valueOf((_owner._threads >= 1 ? _owner._threads : Runtime.getRuntime().availableProcessors()))
      });

      Serializable[] inverseLabelMap = new Serializable[_labelMap.size()];
      _labelMap.object2IntEntrySet().fastForEach(entry -> inverseLabelMap[entry.getIntValue()] = entry.getKey());

      try {
        FastText fastText = new FastText();
        fastText.setLineReaderClass(_owner.getDataSerializationMode().getLineReaderClass());

        FastTextOptions options = FastTextOptions.Builder
            .setArgs(args)
            .setMultilabel(_owner._isMultilabel)
            .setExampleCount(_exampleCount)
            .setSynchronizedStart(_owner._synchronizedTrainingStart)
            .build();

        return fastText.train(options)
            .remapLabels(stringLabel -> (T) inverseLabelMap[FastTextInternal.Util.integerFromLabelString(stringLabel)]);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    public void process(Iterable<? extends T> labelsInput, Iterable<? extends CharSequence> tokensInput) {
      _exampleCount++;
      try {
        for (T label : labelsInput) {
          _exampleWriter.write(FastTextInternal.Util.formatIntegerLabel(getLabelIndex(label)) + " ");
        }
        for (CharSequence token : tokensInput) {
          _exampleWriter.write(FastTextInternal.Util.formatToken(token) + " ");
        }

        _exampleWriter.write('\n');
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }
  }
}
