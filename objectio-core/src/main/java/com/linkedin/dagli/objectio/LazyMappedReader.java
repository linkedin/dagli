package com.linkedin.dagli.objectio;

import com.linkedin.dagli.util.array.ArraysEx;
import java.util.function.Function;


/**
 * A ObjectReader that represents a collection of values derived from the values of another ObjectReader, as lazily
 * transformed by a given mapping function.  The transformed values are not saved; rather, values are re-transformed
 * every time they are accessed.
 *
 * @param <T> the type of the (mapped) elements in this ObjectReader
 */
class LazyMappedReader<T> implements ObjectReader<T> {
  private final ObjectReader<?> _wrapped;
  private final Function<Object, T> _mapper;

  /**
   * Creates a new instance.
   *
   * @param wrapped the reader that will be wrapped and whose elements will be mapped
   * @param mapper the mapper that will transform the elements of the wrapped reader
   * @param <S> the type of element contained by the wrapped reader
   */
  public <S> LazyMappedReader(ObjectReader<S> wrapped, Function<S, T> mapper) {
    if (wrapped instanceof LazyMappedReader) {
      // avoid inefficiently nesting LazyMappedReader instances
      LazyMappedReader<S> lazyWrapped = (LazyMappedReader<S>) wrapped;
      _wrapped = lazyWrapped._wrapped;
      _mapper = lazyWrapped._mapper.andThen(mapper);
    } else {
      _wrapped = wrapped;
      _mapper = (Function) mapper; // correctness is enforced by the parameter typing on this constructor
    }
  }

  @Override
  public long size64() {
    return _wrapped.size64();
  }

  /**
   * An {@link ObjectIterator} that transforms the elements of another, wrapped iterator.
   */
  private class Iterator implements ObjectIterator<T> {
    @SuppressWarnings("unchecked") // safe because the iterator is "read only"--we only pull values out of it
    private final ObjectIterator<Object> _wrappedIterator = (ObjectIterator<Object>) _wrapped.iterator();
    private Object[] _buffer = ArraysEx.EMPTY_OBJECT_ARRAY; // expanded as needed

    @Override
    public long skip(long toSkip) {
      return _wrappedIterator.skip(toSkip);
    }

    @Override
    public boolean hasNext() {
      return _wrappedIterator.hasNext();
    }

    @Override
    public T next() {
      return _mapper.apply(_wrappedIterator.next());
    }

    private int transformBuffer(Object[] destination, int offset, int count) {
      for (int i = 0; i < count; i++) {
        destination[offset + i] = _mapper.apply(_buffer[i]);
      }
      return count;
    }

    private void ensureBufferSize(int size) {
      if (_buffer.length < size) {
        _buffer = new Object[size];
      }
    }

    private int next(ObjectIteratorNext<Object> nextMethod, Object[] destination, int offset, int count) {
      ensureBufferSize(count);
      int read = nextMethod.next(_wrappedIterator, _buffer, offset, count);
      return transformBuffer(destination, offset, read);
    }

    @Override
    public int tryNextAvailable(Object[] destination, int offset, int count) {
      return next(ObjectIterator::tryNextAvailable, destination, offset, count);
    }

    @Override
    public int next(Object[] destination, int offset, int count) {
      return next(ObjectIterator::next, destination, offset, count);
    }

    @Override
    public int nextAvailable(Object[] destination, int offset, int count) {
      return next(ObjectIterator::nextAvailable, destination, offset, count);
    }

    @Override
    public void close() {
      _wrappedIterator.close();
    }
  }

  @Override
  public ObjectIterator<T> iterator() {
    return new Iterator();
  }

  @Override
  public void close() {
    _wrapped.close();
  }
}
