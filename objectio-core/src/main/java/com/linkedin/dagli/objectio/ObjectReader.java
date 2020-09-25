package com.linkedin.dagli.objectio;

import com.linkedin.dagli.util.invariant.Arguments;
import com.linkedin.dagli.util.stream.AutoClosingStream;
import com.linkedin.dagli.util.stream.BatchSpliterator;
import com.linkedin.dagli.util.closeable.Closeables;
import com.linkedin.dagli.util.collection.UnmodifiableArrayList;
import it.unimi.dsi.fastutil.BigList;
import it.unimi.dsi.fastutil.Size64;
import it.unimi.dsi.fastutil.objects.ObjectBigArrayBigList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;


/**
 * Represents an order-aware container with a 64-bit size and the ability to get a {@link ObjectIterator} starting
 * at any offset.
 *
 * Note that ObjectIterators are AutoCloseable.  It's a good idea to close your iterator when you're
 * finished with it.  Java's for-each method (e.g. "for (T t : ObjectReader) { ... }") will implicitly create an
 * iterator and not close it; for this reason, we recommend using stream()s or the forEach(...) method instead.
 *
 * @param <T> the type of element in the container
 */
public interface ObjectReader<T> extends Iterable<T>, AutoCloseable, Size64 {
  /**
   * Gets the number of elements available from this reader.
   *
   * @return the 64-bit size of the collection
   */
  @Override
  long size64();

  /**
   * Gets a batch iterator over the elements of the iterable.
   *
   * @return a {@link ObjectIterator} that iterates over the items of this iterable
   */
  @Override
  ObjectIterator<T> iterator();

  // Override this method to ensure iterator is closed
  @Override
  default void forEach(Consumer<? super T> action) {
    Objects.requireNonNull(action);
    try (ObjectIterator<T> iter = this.iterator()) {
      while (iter.hasNext()) {
        action.accept(iter.next());
      }
    }
  }

  /**
   * Runs the provided consumer action against the values of this reader, batched into lists.  The list provided to
   * the action must not be stored beyond the lifetime of each call as the underlying data structure will be reused.
   *
   * @param action the action to be run on each batch
   * @param batchSize the batch size; each list (other than possibly the list) will be of this size
   */
  @SuppressWarnings("unchecked") // masquerading Object[] as T[] is safe in this context
  default void forEachBatch(int batchSize, Consumer<? super List<? extends T>> action) {
    Objects.requireNonNull(action);

    T[] buffer = (T[]) new Object[batchSize];
    List<T> batch = new UnmodifiableArrayList<>(buffer, batchSize); // repeatedly reuse this same list

    try (ObjectIterator<T> iter = this.iterator()) {
      while (iter.hasNext()) {
        int copied = iter.next(buffer, 0, batchSize);
        if (copied < batchSize) { // may need to create a shorter list; this should only happen at the end of the reader
          action.accept(new UnmodifiableArrayList<>(buffer, copied));
        } else {
          action.accept(batch);
        }
      }
    }
  }

  /**
   * "Closes" this instance; all further calls have undefined results.
   *
   * close() provides an opportunity to clean up resources, free memory, etc.
   */
  @Override
  void close();

  /**
   * Gets a stream over the items of this reader, batched into lists.
   *
   * The returned {@link Stream} will either not need to be closed or will be a {@link AutoClosingStream} that will
   * close itself when a terminating operation (such as {@link Stream#forEach(Consumer)}) is called.  There are
   * limitations to this approach; please see {@link AutoClosingStream} for more details.
   *
   * In the general case--where a terminating operation might not be called and thus the stream might otherwise be left
   * unclosed--the stream should be {@link Stream#close()}'d using a try-finally block, e.g.:
   * {@code
   *   try (Stream<T> stream = objectReader.stream()) {
   *     // do something with stream
   *   }
   * }
   *
   * The default implementation of this method returns an {@link AutoClosingStream} if {@link #spliterator()} implements
   * {@link AutoCloseable}.
   *
   * @param batchSize the batch size; each enumerated list will be of this size or smaller
   */
  default Stream<List<T>> batchStream(int batchSize) {
    Spliterator<T> spliterator = spliterator();
    if (spliterator instanceof AutoCloseable) {
      // jump through hoops so we can minimize the change the spliterator will be left unclosed...
      int characteristics = BatchSpliterator.characteristics(spliterator.characteristics());
      Closeables.tryClose(spliterator);

      BatchSpliterator<?>[] batchInstance = new BatchSpliterator<?>[1];

      return new AutoClosingStream<>(StreamSupport.stream(() -> {
        BatchSpliterator<T> batchSpliterator = new BatchSpliterator<>(batchSize, spliterator());
        batchInstance[0] = batchSpliterator;
        return batchSpliterator;
      }, characteristics, false).onClose(() -> Closeables.tryClose(batchInstance[0])));
    } else {
      return StreamSupport.stream(new BatchSpliterator<>(batchSize, spliterator), false);
    }
  }

  /**
   * Gets a {@link Stream} over the elements of this ObjectReader.
   *
   * The returned {@link Stream} will either not need to be closed or will be a {@link AutoClosingStream} that will
   * close itself when a terminating operation (such as {@link Stream#forEach(Consumer)}) is called.  There are
   * limitations to this approach; please see {@link AutoClosingStream} for more details.
   *
   * In the general case--where a terminating operation might not be called and thus the stream might otherwise be left
   * unclosed--the stream should be {@link Stream#close()}'d using a try-finally block, e.g.:
   * {@code
   *   try (Stream<T> stream = objectReader.stream()) {
   *     // do something with stream
   *   }
   * }
   *
   * The default implementation of this method returns an {@link AutoClosingStream} if {@link #spliterator()} implements
   * {@link AutoCloseable}.
   *
   * @return a Stream over the elements of this ObjectReader
   */
  default Stream<T> stream() {
    Spliterator<T> spliterator = spliterator();
    if (spliterator instanceof AutoCloseable) {
      // jump through hoops so we can minimize the change the spliterator will be left unclosed...
      int characteristics = spliterator.characteristics();
      Closeables.tryClose(spliterator);

      Spliterator<?>[] instance = new Spliterator<?>[1];

      return new AutoClosingStream<>(StreamSupport.stream(() -> {
        Spliterator<T> splt = spliterator();
        instance[0] = splt;
        return splt;
      }, characteristics, false).onClose(() -> Closeables.tryClose(instance[0])));
    } else {
      return StreamSupport.stream(spliterator, false);
    }
  }

  @Override
  default Spliterator<T> spliterator() {
    class Spliterator extends Spliterators.AbstractSpliterator<T> implements AutoCloseable {
      final ObjectIterator<T> _iterator = iterator();

      public Spliterator() {
        super(size64(), SIZED + ORDERED);
      }

      @Override
      public boolean tryAdvance(Consumer<? super T> action) {
        if (_iterator.hasNext()) {
          action.accept(_iterator.next());
          return true;
        }
        return false;
      }

      @Override
      public void close() {
        _iterator.close();
      }
    }

    return new Spliterator();
  }

  /**
   * Returns a collection that provides a read-only "view" of this ObjectReader.  Changes to the ObjectReader are
   * reflected in this view.
   *
   * The primary utility of this view is interfacing with APIs that require collections; the limitation is that the
   * ObjectReader must have fewer than Integer.MAX_VALUE elements; otherwise an IndexOutOfBoundsException is thrown.
   *
   * @throws IndexOutOfBoundsException if the size of this ObjectReader is greater than Integer.MAX_VALUE
   * @return a collection wrapping this instance
   */
  default Collection<T> toCollection() {
    return new ReaderAsCollection<>(this);
  }

  /**
   * Returns a {@link List} containing the elements of this reader.
   *
   * The default implementation first checks if this {@link ObjectReader} implements {@link List}, and, if so, returns
   * this instance.  Otherwise, it creates an in-memory ArrayList that copies all items in this reader and returns it
   * (this may result in an exception if it contains too many items, e.g. more than approximately 2^31).
   *
   * @return a List that contains this reader's elements
   */
  @SuppressWarnings("unchecked")
  default List<T> toList() {
    if (this instanceof List) {
      return (List<T>) this;
    } else {
      return new ArrayList<>(this.toCollection());
    }
  }

  /**
   * Returns a {@link BigList} containing the reader's elements
   *
   * The default implementation first checks if this {@link ObjectReader} implements {@link BigList} and, if so, returns
   * this instance.  Otherwise,it  copies the elements of this {@link ObjectReader} into a new
   * {@link ObjectBigArrayBigList}.
   *
   * @return a {@link BigList} containing the reader's elements
   */
  @SuppressWarnings("unchecked")
  default BigList<T> toBigList() {
    if (this instanceof BigList) {
      return (BigList<T>) this;
    }

    long size = this.size64();
    try (ObjectIterator<T> iter = this.iterator()) {
      ObjectBigArrayBigList<T> result = new ObjectBigArrayBigList<T>(size);
      for (long i = 0; i < size; i++) {
        result.add(iter.next());
      }
      return result;
    }
  }

  /**
   * Returns a ObjectReader that is sampled from this one.  The default implementation is a lazy sample that keeps
   * a reference to this instance and then effectively filters out the unsampled elements from the returned results.
   *
   * The sampling is performed by mapping every item in the original ObjectReader to a uniformly random real number
   * between 0 (inclusive) and 1 (exclusive).  If this it falls within the range specified for the sample (the
   * "segment") then this item is part of the sample; otherwise it is ignored and will not be part of the list of items
   * contained in the sampled ObjectReader.
   *
   * The size of the range is the expected proportion of the number of items from the original ObjectReader, and two
   * non-overlapping ranges are guaranteed to have mutually exclusive sets of items assuming the same seed is used.  For
   * example, if we have a ObjectReader<T> called "examples" and then set:
   *   ObjectReader<T> trainingData = examples.sample(0.0, 0.8, 1337);
   *   ObjectReader<T> evaluationData = examples.sample(0.8, 1.0, 1337);
   * we now have a set of training data which is approximately 80% of the original data, and a set of evaluation data
   * which consists of the remainder of the data (approximately 20%); each example will be in exactly one of these two
   * sets because the ranges are non-overlapping and exhaustively cover the space of sample values [0, 1).
   *
   * For the same range, the same seed, and the same original ObjectReader, the sample will always be the same, but may
   * change with a subsequent version of the ObjectReader's implementation (i.e. do not rely on samples being identical
   * across different versions of the same ObjectReader class).
   *
   * @param segment a SampleSegment that specifies the range (a subset of the range [0, 1)) and the seed for the sample
   * @return a ObjectReader that contains (only) the sampled elements
   */
  default ObjectReader<T> sample(SampleSegment segment) {
    return new SampleReader<>(this, segment);
  }

  /**
   * Returns a ObjectReader that is sampled from this one.  The default implementation is a lazy sample that keeps
   * a reference to this instance and then effectively filters out the unsampled elements from the returned results.
   *
   * The sampling is performed by mapping every item in the original ObjectReader to a uniformly random real number
   * between 0 (inclusive) and 1 (exclusive).  If this it falls within the range specified for the sample (the
   * "segment") then this item is part of the sample; otherwise it is ignored and will not be part of the list of items
   * contained in the sampled ObjectReader.
   *
   * The size of the range is the expected proportion of the number of items from the original ObjectReader, and two
   * non-overlapping ranges are guaranteed to have mutually exclusive sets of items assuming the same seed is used.  For
   * example, if we have a ObjectReader<T> called "examples" and then set:
   *   ObjectReader<T> trainingData = examples.sample(0.0, 0.8, 1337);
   *   ObjectReader<T> evaluationData = examples.sample(0.8, 1.0, 1337);
   * we now have a set of training data which is approximately 80% of the original data, and a set of evaluation data
   * which consists of the remainder of the data (approximately 20%); each example will be in exactly one of these two
   * sets because the ranges are non-overlapping and exhaustively cover the space of sample values [0, 1).
   *
   * For the same range, the same seed, and the same original ObjectReader, the sample will always be the same, but may
   * change with a subsequent version of the ObjectReader's implementation (i.e. do not rely on samples being identical
   * across different versions of the same ObjectReader class).
   *
   * @param segmentRangeStartInclusive a value in the range [0, 1] where the sampling "segment" begins.
   * @param segmentRangeEndExclusive a value in the range [segmentRangeStartInclusive, 1] where the sampling "segment"
   *                                 ends.  If this value == segmentRangeStartInclusive the sample will be empty.
   * @param seed a seed value for the random sampling
   * @return a ObjectReader that contains (only) the sampled elements
   */
  default ObjectReader<T> sample(double segmentRangeStartInclusive, double segmentRangeEndExclusive, long seed) {
    return sample(new SampleSegment(segmentRangeStartInclusive, segmentRangeEndExclusive, seed));
  }

  /**
   * Returns a ObjectReader that is sampled from this one using a default seed value.  The default implementation is a
   * lazy sample that keeps a reference to this instance and then effectively filters out the unsampled elements from
   * the returned results.
   *
   * The sampling is performed by mapping every item in the original ObjectReader to a uniformly random real number
   * between 0 (inclusive) and 1 (exclusive).  If this it falls within the range specified for the sample (the
   * "segment") then this item is part of the sample; otherwise it is ignored and will not be part of the list of items
   * contained in the sampled ObjectReader.
   *
   * The size of the range is the expected proportion of the number of items from the original ObjectReader, and two
   * non-overlapping ranges are guaranteed to have mutually exclusive sets of items assuming the same seed is used.  For
   * example, if we have a ObjectReader<T> called "examples" and then set:
   *   ObjectReader<T> trainingData = examples.sample(0.0, 0.8, 1337);
   *   ObjectReader<T> evaluationData = examples.sample(0.8, 1.0, 1337);
   * we now have a set of training data which is approximately 80% of the original data, and a set of evaluation data
   * which consists of the remainder of the data (approximately 20%); each example will be in exactly one of these two
   * sets because the ranges are non-overlapping and exhaustively cover the space of sample values [0, 1).
   *
   * For the same range, the same seed, and the same original ObjectReader, the sample will always be the same, but may
   * change with a subsequent version of the ObjectReader's implementation (i.e. do not rely on samples being identical
   * across different versions of the same ObjectReader class).
   *
   * @param segmentRangeStartInclusive a value in the range [0, 1] where the sampling "segment" begins.
   * @param segmentRangeEndExclusive a value in the range [segmentRangeStartInclusive, 1] where the sampling "segment"
   *                                 ends.  If this value == segmentRangeStartInclusive the sample will be empty.
   * @return a ObjectReader that contains (only) the sampled elements
   */
  default ObjectReader<T> sample(double segmentRangeStartInclusive, double segmentRangeEndExclusive) {
    return sample(segmentRangeStartInclusive, segmentRangeEndExclusive, 0);
  }

  /**
   * Returns a ObjectReader whose elements are (lazily) derived from this ObjectReader on-demand using the provided
   * mapper.  The lazily-mapped ObjectReader maintains a reference to this one and does not cache the transformed
   * values, instead (re-)mapping the elements of this ObjectReader as needed.
   *
   * This means that this method is very cheap, and very little additional memory is required for the resulting lazily-
   * mapped ObjectReader, but if the mapping function is expensive and you plan to iterate over the transformed values
   * frequently you're better off explicitly storing the transformed values in, e.g. a ObjectWriterBigArrayList or
   * similar.
   *
   * Closing the returned ObjectReader implicitly closes this one.
   *
   * @param mapper the mapping function that will be applied to each value in this iterable to generate the transformed
   *               values in the lazily-mapped ObjectReader.
   * @param <U> the transformed type of the elements in the returned ObjectReader
   * @return a lazily-mapped ObjectReader whose elements are mapped from this one
   */
  default <U> ObjectReader<U> lazyMap(Function<T, U> mapper) {
    return new LazyMappedReader<U>(this, mapper);
  }

  /**
   * Returns a ObjectReader whose elements are (lazily) derived from this ObjectReader on-demand using the provided
   * mapper, which transforms the original elements into an Iterable of zero or more derived elements; the returned
   * ObjectReader's elements are a concatenation of all of the elements of these iterables, ordered by the
   * corresponding original mapped items.  So if the original ObjectReader is [A, B, C], and the mapping function is:
   * <pre>{@code
   * A -> [1, 2]
   * B -> [3, 4]
   * C -> [5, 6]
   * }</pre>
   * the flatMapped result will be: [1, 2, 3, 4, 5, 6]
   *
   * Notes:
   * (1) The lazily-mapped ObjectReader maintains a reference to this one and does not cache the transformed values,
   *     instead (re-)mapping the elements of this ObjectReader as needed.
   * (2) Computing the size64() of the lazy-flatmapped ObjectReader is relatively expensive, as all values must
   *     be mapped to determine the number of flatmapped values; this size *is* cached, so it is only computed once.
   * (3) For efficiency, it is assumed that the Iterables produced by mapper are "lightweight"; i.e. that neither they
   *     nor their iterators require close()'ing.
   * (4) This method itself is very cheap, and very little additional memory is required for the resulting lazily-
   *     mapped ObjectReader, but if the mapping function is expensive and you plan to iterate over the transformed
   *     values frequently you're better off explicitly storing the transformed values in, e.g. a
   *     ObjectWriterBigArrayList or similar.
   * (5) Closing the returned ObjectReader implicitly closes this one.
   *
   * @param mapper the mapping function that will be applied to each value in this iterable to generate an Iterable of
   *               transformed values in the lazily-mapped ObjectReader.
   * @param <U> the transformed type of the elements in the returned ObjectReader
   * @return a lazily-flatmapped ObjectReader whose elements are mapped from this one
   */
  default <U> ObjectReader<U> lazyFlatMap(Function<T, Iterable<? extends U>> mapper) {
    return new LazyFlatMappedReader<U>(this, mapper);
  }

  /**
   * Returns a ObjectReader whose elements are lazily filtered from this one by applying inclusionTest.
   *
   * The filtered ObjectReader maintains a reference to this one and computes its elements on demand; no copy of the
   * data is made.
   *
   * @param inclusionTest a predicate that returns true for the elements that should appear in the filtered
   *                      ObjectReader
   * @return a filtered ObjectReader containing those elements of this ObjectReader passing inclusionTest
   */
  default ObjectReader<T> lazyFilter(Predicate<T> inclusionTest) {
    return new LazyFilteredReader<>(this, inclusionTest);
  }

  /**
   * Lazily shuffles this reader.  This is *not* a true, uniform shuffle where every element has an equal chance of
   * being in every position.  Instead, a finite, K-sized buffer of elements in memory is used.  Once the first K
   * elements of this reader are in the buffer, the shuffled reader iterates elements via the following procedure:
   * (1) Get the next element from the original reader
   * (2) Assign it a random location in the buffer
   * (3) Return (as the next shuffled element) the element currently in that location
   *
   * Once the last element from the underlying reader is read, the elements remaining in the buffer are returned in
   * random order.
   *
   * This is very different than a uniform shuffle; e.g. the first K elements returned are guaranteed to be among the
   * first 2*K elements in the original reader.  It is more akin to minibatch shuffling, albeit with greater
   * randomization: the last element returned can in principle be any element, but is much more likely to be an element
   * near the end of the original reader.
   *
   * The advantage over a true, uniform shuffle (e.g. Fisher-Yates) is that the memory requirement is O(K) rather than
   * O(n), where n is the number of elements in the wrapped reader.  If {@code K >= n} this class performs a true
   * shuffle.  For machine learning purposes a partial shuffle can achieve much of the benefit of a full shuffle without
   * a potentially high shuffling cost.
   *
   * Note that the buffer is not allocated when this method is called.  Instead, it is created when iterating over the
   * shuffled data.  The returned reader is "lazy", meaning that it stores no data (modulo the aforementioned buffer)
   * and is instead backed by the original reader reader.
   *
   * @param seed the random seed to use.  Shuffled order is deterministically dependent on this seed.
   * @param bufferSize the size of the buffer; a larger buffer makes the shuffle more "thorough" at the expense of
   *                   greater memory usage.
   */
  default ObjectReader<T> lazyShuffle(long seed, int bufferSize) {
    return new LazyShuffledBufferReader<>(this, seed, bufferSize);
  }

  /**
   * Lazily shuffles this reader.  This is *not* a true, uniform shuffle where every element has an equal chance of
   * being in every position.  Instead, a finite, K-sized buffer of elements in memory is used.  Once the first K
   * elements of this reader are in the buffer, the shuffled reader iterates elements via the following procedure:
   * (1) Get the next element from the original reader
   * (2) Assign it a random location in the buffer
   * (3) Return (as the next shuffled element) the element currently in that location
   *
   * Once the last element from the underlying reader is read, the elements remaining in the buffer are returned in
   * random order.
   *
   * This is very different than a uniform shuffle; e.g. the first K elements returned are guaranteed to be among the
   * first 2*K elements in the original reader.  It is more akin to minibatch shuffling, albeit with greater
   * randomization: the last element returned can in principle be any element, but is much more likely to be an element
   * near the end of the original reader.
   *
   * The advantage over a true, uniform shuffle (e.g. Fisher-Yates) is that the memory requirement is O(K) rather than
   * O(n), where n is the number of elements in the wrapped reader.  If {@code K >= n} this class performs a true
   * shuffle.  For machine learning purposes a partial shuffle can achieve much of the benefit of a full shuffle without
   * a potentially high shuffling cost.
   *
   * Note that the buffer is not allocated when this method is called.  Instead, it is created when iterating over the
   * shuffled data.  The returned reader is "lazy", meaning that it stores no data (modulo the aforementioned buffer)
   * and is instead backed by the original reader reader.
   *
   * This method overload used a fixed seed value of 0.  It is equivalent to calling lazyShuffle(0, bufferSize).
   *
   * @param bufferSize the size of the buffer; a larger buffer makes the shuffle more "thorough" at the expense of
   *                   greater memory usage
   */
  default ObjectReader<T> lazyShuffle(int bufferSize) {
    return new LazyShuffledBufferReader<>(this, 0, bufferSize);
  }

  /**
   * Casts an instance to an effective "supertype" interface.  The semantics of {@link ObjectReader} guarantee that the
   * returned type is valid for the instance.
   *
   * Note that although this and other {@code cast(...)} methods are safe, this safety extends only to the interfaces
   * for which they are implemented.  The covariance and contravariance relationships existing for these interfaces do
   * not necessarily hold for their implementing classes.
   *
   * @param reader the instance to cast
   * @param <R> the type of item read by the returned reader
   * @return the passed reader, typed to a new "supertype" interface of the original
   */
  @SuppressWarnings("unchecked")
  static <R> ObjectReader<R> cast(ObjectReader<? extends R> reader) {
    return (ObjectReader<R>) reader;
  }

  /**
   * Gets a canonical reader containing no elements.
   *
   * @param <T> the type of element read by this reader (though only in principle, since it's empty)
   * @return a reader containing no elements
   */
  @SuppressWarnings("unchecked")
  static <T> ObjectReader<T> empty() {
    return EmptyReader.INSTANCE;
  }

  /**
   * If iterable is a ObjectReader, returns iterable.
   *
   * Otherwise, creates a ObjectReader wrapper around iterable and returns the wrapper.
   *
   * @param iterable an iterable to wrap
   * @param <T> the type of element in the iterable
   * @return a ObjectReader containing the elements of the iterable
   */
  @SuppressWarnings("unchecked")
  static <T> ObjectReader<T> wrap(Iterable<? extends T> iterable) {
    if (iterable instanceof ObjectReader) {
      return (ObjectReader<T>) iterable;
    }

    return new IterableReader<>(iterable);
  }

  /**
   * Returns an {@link ObjectReader} containing the specified sequence of values.
   *
   * @param values the values enumerated by the returned reader
   * @param <T> the type of the values
   * @return a {@link ObjectReader} that will read the provided values
   */
  @SafeVarargs
  static <T> ObjectReader<T> of(T... values) {
    return wrap(Arrays.asList(values));
  }

  /**
   * Gets an iterable over a single provided element
   *
   * @param element the element that will be contained by the iterable
   * @param <T> the type of the element
   * @return a ObjectReader of size 1 containing (only) the specified element
   */
  static <T> ObjectReader<T> singleton(T element) {
    return new ConstantReader<>(element, 1);
  }

  /**
   * Convenience method.  Concatenates the results of one or more parallel ObjectReaders (that must have the same
   * size) into a single ObjectReader that produces arrays.
   *
   * @param arrayGenerator a method that generates arrays for the concatenated results; typically an array constructor
   * @param objectReaders one or more ObjectReaders whose results should be concatenated
   * @param <T> the type of element contained in the ObjectReaders
   * @return a new ObjectReader whose elements are arrays of the parallel (same ordinal position) elements of its
   *         underlying ObjectReaders
   */
  @SafeVarargs
  static <T> ConcatenatedReader<T> concatenate(IntFunction<T[]> arrayGenerator,
      ObjectReader<? extends T>... objectReaders) {
    return new ConcatenatedReader<>(arrayGenerator, objectReaders);
  }

  /**
   * "Splits" a ObjectReader that contains arrays of length "size", returning "size" new ObjectReaders whose
   * values each correspond to one element original ObjectReader's arrays.  If the ObjectReader is a type known to be
   * cheaply splittable (i.e. a ConcatenatedReader), this will be exploited; otherwise each resultant
   * ObjectReader will be a lazy map of the original--this may be expensive, as reading each of the resultant split
   * ObjectReaders will essentially cost as much as reading the original.
   *
   * For example, if the original has values: [1, 2], [3, 4] then it would be split into two new ObjectReaders:
   * 1, 3 (1st indexed item in the original arrays) and 2, 4 (2nd indexed item).
   *
   * @param size the size of the arrays in the original ObjectReader; it is assumed that all arrays it contains have
   *             this same fixed size
   * @param objectReader the original ObjectReader to split
   * @param <T> the type of elements in the original ObjectReader's arrays
   * @return size new ObjectReaders, each of which contain items from a particular index in the original's arrays
   */
  static <T> ObjectReader<T>[] split(int size, ObjectReader<T[]> objectReader) {
    if (objectReader instanceof ConcatenatedReader) {
      ObjectReader<T>[] objReaders = ((ConcatenatedReader<T>) objectReader).getObjectReaders();
      Arguments.check(objReaders.length == size,
          "ConcatenatedReader produces arrays of a different size than expected");
      return objReaders;
    }

    ObjectReader<T>[] result = new ObjectReader[size];
    for (int i = 0; i < size; i++) {
      final int index = i;
      result[i] = objectReader.lazyMap(arr -> arr[index]);
    }
    return result;
  }

  /**
   * Checks if the elements of two ObjectReaders are equal (with the same order) or both readers are null.
   *
   * @param a the first reader
   * @param b the second reader
   * @param <T> the type of element contained by the readers
   * @return true iff the readers have the same elements (as determined by Objects.equals()) in the same order, or are
   *         both null.
   */
  static <T> boolean equals(ObjectReader<T> a, ObjectReader<T> b) {
    if (a == b) {
      return true; // a reader adhering to the ObjectReader interface should always have the same elements as itself
    } else if (a == null) {
      return false;
    } else if (b == null) {
      return false;
    }

    try (ObjectIterator<T> ai = a.iterator(); ObjectIterator<T> bi = b.iterator()) {
      while (ai.hasNext()) {
        if (!bi.hasNext()) {
          return false;
        }
        if (!Objects.equals(ai.next(), bi.next())) {
          return false;
        }
      }
      return !bi.hasNext();
    }
  }
}
