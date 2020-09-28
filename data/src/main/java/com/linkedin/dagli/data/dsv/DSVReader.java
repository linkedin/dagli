package com.linkedin.dagli.data.dsv;

import com.concurrentli.Singleton;
import com.linkedin.dagli.data.schema.RowSchema;
import com.linkedin.dagli.util.cloneable.AbstractCloneable;
import com.linkedin.dagli.objectio.ObjectIterator;
import com.linkedin.dagli.objectio.ObjectReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.BiConsumer;
import java.util.function.IntUnaryOperator;
import java.util.function.Supplier;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

/**
 * Reads one or more fields from a delimiter-separated value (DSV) data.  Delimiter-separated values includes both
 * comma-separated values (CSV) and tab-separated values (TSV), two very common formats for storing records.
 *
 * Field(s) can be read back as @Structs (recommended), as individual fields, as String[] arrays of fields, or arbitrary
 * types by defining your own schema.  To read @Structs, first define the @Struct, create a "schema" for your @Struct
 * via the [YourStruct].Schema.builder(), and finally pass that to DSVReader's withSchema(...) method.
 *
 * {@link DSVReader} is immutable, and implements Iterable<T>.  Results can be read via a "for (value : dsvdata) { ... }"
 * loop, or, e.g. getting the iterator() explicitly.
 *
 * In addition to a schema, you'll also want to provide:
 * (1) A Supplier<Reader> function that provides a Reader that will read the text to be parsed.  A Supplier is provided
 *     because multiple iterations over the data may be required.  Convenience methods for files (a typical use case)
 *     are available.  Specify this using withReaderSupplier(...) or withFile(...)/withResourceFile(...).
 * (2) A {@link CSVFormat} (from the Apache CSV library) that specifies how the DSV file should be parsed, e.g. what
 *     delimiter characters are used, what the headers are (if applicable), what the escape character is, etc.
 *     If unspecified, CSVFormat.DEFAULT will be used (standard comma-seperated value format).
 *
 * @param <T> the type of value that will be read from each row in the DSV data.  This is determined by the schema you
 *           provide via withSchema(...).
 */
public class DSVReader<T> extends AbstractCloneable<DSVReader<T>> implements ObjectReader<T> {
  private CSVFormat _format = CSVFormat.DEFAULT;
  private RowSchema<T, ?> _rowSchema = null;
  private Supplier<Reader> _readerSupplier = null;

  private long _specifiedSize = -1;
  private Singleton<Long> _calculatedSize = null;

  private static class ConstantSingleton extends Singleton<Long> {
    private final long _size;
    public ConstantSingleton(long size) {
      _size = size;
    }

    @Override
    protected Long getValue() {
      return _size;
    }
  }

  private static class SizeSingleton extends Singleton<Long> {
    private final Supplier<Reader> _readerSupplier;
    private final CSVFormat _format;
    public SizeSingleton(CSVFormat format, Supplier<Reader> readerSupplier) {
      _format = format;
      _readerSupplier = readerSupplier;
    }

    @Override
    protected Long getValue() {
      try {
        long size = 0;
        for (Object val : _format.parse(_readerSupplier.get())) {
          size++;
        }
        return size;
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }
  }

  /**
   * Returns a copy of this instance that will use the specified Reader-supplying function to get the Reader that will
   * read the DSV data.
   *
   * @param supplier the supplier function to use
   * @return a copy of this instance that will use the specified supplier.
   */
  public DSVReader<T> withReaderSupplier(Supplier<Reader> supplier) {
    return clone(c -> {
      c._calculatedSize = new DSVReader.SizeSingleton(_format, supplier);
      c._readerSupplier = supplier;
    });
  }

  /**
   * Returns a copy of this instance that will read its data from the specified file.  The file should not be changed
   * while this instance is being used.
   *
   * @param path the path of the file to read
   * @param charset the character set, e.g. StandardCharsets.UTF8
   * @return a copy of this instance that will read from the specified file
   */
  public DSVReader<T> withFile(Path path, Charset charset) {
    return withReaderSupplier(
        () -> {
          try {
            return Files.newBufferedReader(path, charset);
          } catch (IOException e) {
            throw new UncheckedIOException(e);
          }
        }
    );
  }

  /**
   * Returns a copy of this instance that will read its data from the specified file.  The file should not be changed
   * while this instance is being used.
   *
   * @param path the path of the file to read.  This path should *not* start with a "/".
   * @param charset the character set, e.g. StandardCharsets.UTF8
   * @return a copy of this instance that will read from the specified file
   */
  public DSVReader<T> withFile(String path, Charset charset) {
    return withFile(Paths.get(path), charset);
  }

  /**
   * Returns a copy of this instance that will read its data from the specified resource file using the
   * default/system/bootstrap loader.
   *
   * @param path the path of the resource file to read; it may optionally start with "/".
   * @param charset the character set, e.g. StandardCharsets.UTF8
   * @return a copy of this instance that will read from the specified file
   */
  public DSVReader<T> withResourceFile(String path, Charset charset) {
    return withResourceFile(null, path, charset);
  }

  /**
   * Returns a copy of this instance that will read its data from the specified resource file using the specified
   * {@link ClassLoader}.
   *
   * @param classLoader the {@link ClassLoader} to use.  If null, the default/system/bootstrap loader (via
   *                    ClassLoader.getSystemResourceAsStream(...)) is used.
   * @param path the path of the resource file to read; it may optionally start with "/".
   * @param charset the character set, e.g. StandardCharsets.UTF8
   * @return a copy of this instance that will read from the specified file
   */
  public DSVReader<T> withResourceFile(ClassLoader classLoader, String path, Charset charset) {
    if (classLoader == null) {
      return withReaderSupplier(
          () -> new InputStreamReader(ClassLoader.getSystemResourceAsStream(formatResourcePath(path)), charset));
    }
    return withReaderSupplier(
        () -> new InputStreamReader(classLoader.getResourceAsStream(formatResourcePath(path)), charset));
  }

  private static String formatResourcePath(String original) {
    if (original.startsWith("/")) {
      return original.substring(1);
    } else {
      return original;
    }
  }

  /**
   * Optional setting.
   *
   * Returns a copy of this instance that knows a priori how many rows/lines it will read.  This size can be less than
   * the true size (in which case, only this many rows will be read and the remainder ignored) but it MUST NOT BE
   * LARGER than the true size.  If you do not know the true size, do not specify this value; if and when the size is
   * requested via the size64() method it will be automatically calculated by scanning the stream.
   *
   * @param size the true size (or smaller), in rows.
   * @return a copy of this instance that will use the specified size
   */
  public DSVReader<T> withSizeInRows(long size) {
    return clone(c -> c._specifiedSize = size);
  }

  /**
   * Returns a copy of this instance that will use the specified {@link CSVFormat} when parsing the DSV data.
   *
   * @param format the format to use
   * @return a copy of this instance that will use the specified format.
   */
  public DSVReader<T> withFormat(CSVFormat format) {
    return clone(c -> {
      c._calculatedSize = new DSVReader.SizeSingleton(format, _readerSupplier);
      c._format = format;
    });
  }

  /**
   * Gets the {@link CSVFormat} used to parse the DSV data.
   *
   * @return the format
   */
  public CSVFormat getFormat() {
    return _format;
  }

  @Override
  public long size64() {
    return _specifiedSize >= 0 ? _specifiedSize : _calculatedSize.get();
  }

  // returns the size set by the user (which acts as a limit), or MAX_VALUE
  private long sizeLimit() {
    return _specifiedSize >= 0 ? _specifiedSize : Long.MAX_VALUE;
  }

  /**
   * Returns a copy of this instance that will use the specified {@link RowSchema} to read values from rows.
   *
   * @param schema the schema to use
   * @return a copy of this instance that will use the specified schema.
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  public <U> DSVReader<U> withSchema(RowSchema<U, ?> schema) {
    return (DSVReader) clone(c -> c._rowSchema = (RowSchema) schema);
  }

  private static class Iterator<T> implements ObjectIterator<T> {
    private final RowSchema<T, Object> _schema;
    private final CSVParser _parser;
    private final java.util.Iterator<CSVRecord> _iterator;
    private final long _size;
    private long _readSoFar = 0;

    private final ArrayList<BiConsumer<CSVRecord, Object>> _consumers;

    @SuppressWarnings({"unchecked", "rawtypes"})
    public Iterator(DSVReader<T> owner) {
      if (owner._rowSchema == null) {
        throw new NullPointerException("No row schema has been provided for this DSVReader instance.  Please obtain a "
            + "DSVReader instance with a schema by calling .withSchema(...).  Common schemas can be obtained from "
            + "methods on the Schemas class");
      } else if (owner._readerSupplier == null) {
        throw new NullPointerException("No underlying data source or reader has been provided for this DSVReader "
            + "instance.  Please obtain an instance with a data source by using on the relevant methods, e.g. "
            + ".withFile(...)");
      }

      _schema = (RowSchema) owner._rowSchema;

      // if user set a size < true size we might need to stop early
      _size = owner.sizeLimit();

      try {
        _parser = owner._format.parse(owner._readerSupplier.get());
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
      final String[] headers;
      Map<String, Integer> headerMap = _parser.getHeaderMap();
      if (headerMap != null) {
        headers = new String[
            headerMap.values().stream().mapToInt(IntUnaryOperator.identity()::applyAsInt).max().orElse(-1) + 1];
        for (Map.Entry<String, Integer> entry : headerMap.entrySet()) {
          headers[entry.getValue()] = entry.getKey();
        }
      } else {
        headers = null;
      }

      _iterator = _parser.iterator();

      Collection<? extends RowSchema.FieldSchema<?>> fields = _schema.getFields();
      _consumers = new ArrayList<>(fields.size());

      for (RowSchema.FieldSchema<?> field : fields) {
        boolean required = field.isRequired();

        if (field instanceof RowSchema.Field.Indexed) {
          RowSchema.Field.Indexed<Object> indexedField = (RowSchema.Field.Indexed<Object>) field;
          _consumers.add((record, acc) -> {
            if (indexedField.getIndex() >= record.size()) {
              if (required) {
                throw new IndexOutOfBoundsException();
              }
            } else {
              indexedField.read(acc, record.get(indexedField.getIndex()));
            }
          });
        } else if (field instanceof RowSchema.Field.Named) {
          RowSchema.Field.Named<Object> namedField = (RowSchema.Field.Named<Object>) field;
          _consumers.add((record, acc) -> {
            if (!record.isSet(namedField.getName())) {
              if (required) {
                throw new StringIndexOutOfBoundsException();
              }
            } else {
              namedField.read(acc, record.get(namedField.getName()));
            }
          });
        } else if (field instanceof RowSchema.AllFields) {
          RowSchema.AllFields<Object> allFields = (RowSchema.AllFields<Object>) field;
          _consumers.add((record, acc) -> allFields.read(acc, headers, recordToStringArray(record)));
        } else if (field instanceof RowSchema.MultiField.Indexed) {
          RowSchema.MultiField.Indexed<Object> indexedFields = (RowSchema.MultiField.Indexed<Object>) field;
          _consumers.add((record, acc) -> {
            int[] indices = indexedFields.getIndices();
            String[] values = new String[indices.length];

            for (int i = 0; i < values.length; i++) {
              if (indices[i] >= record.size()) {
                if (required) {
                  throw new IndexOutOfBoundsException();
                }
                return;
              }
              values[i] = record.get(indices[i]);
            }
            indexedFields.read(acc, values);
          });
        } else if (field instanceof RowSchema.MultiField.Named) {
          RowSchema.MultiField.Named<Object> namedFields = (RowSchema.MultiField.Named<Object>) field;
          _consumers.add((record, acc) -> {
            String[] names = namedFields.getNames();
            String[] values = new String[names.length];

            for (int i = 0; i < values.length; i++) {
              if (!record.isSet(names[i])) {
                if (required) {
                  throw new StringIndexOutOfBoundsException();
                }
                return;
              }
              values[i] = record.get(names[i]);
            }
            namedFields.read(acc, values);
          });
        } else {
          throw new IllegalArgumentException("Unknown type of field: " + field);
        }
      }
    }

    private static String[] recordToStringArray(CSVRecord record) {
      String[] res = new String[record.size()];
      java.util.Iterator<String> iter = record.iterator();
      for (int i = 0; i < res.length; i++) {
        res[i] = iter.next();
      }
      return res;
    }

    @Override
    public boolean hasNext() {
      if (_readSoFar >= _size) {
        return false;
      }
      return _iterator.hasNext();
    }

    @Override
    public T next() {
      if (_readSoFar >= _size) {
        throw new NoSuchElementException();
      }

      CSVRecord record = _iterator.next();
      _readSoFar++;

      Object accumulator = _schema.createAccumulator();
      for (BiConsumer<CSVRecord, Object> consumer : _consumers) {
        consumer.accept(record, accumulator);
      }
      return _schema.finish(accumulator);
    }

    @Override
    public void close() {
      try {
        _parser.close();
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }
  }

  @Override
  public ObjectIterator<T> iterator() {
    return new Iterator<>(this);
  }

  @Override
  public void close() { }
}
