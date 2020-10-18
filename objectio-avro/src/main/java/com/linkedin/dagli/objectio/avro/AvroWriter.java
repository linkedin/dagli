package com.linkedin.dagli.objectio.avro;

import com.linkedin.dagli.objectio.ObjectIterator;
import com.linkedin.dagli.objectio.ObjectReader;
import com.linkedin.dagli.objectio.ObjectWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.avro.Schema;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.specific.SpecificData;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecord;


/**
 * An {@link ObjectWriter} that writes Avro records to a file.
 *
 * @param <T> the type of Avro record to be written.
 */
public class AvroWriter<T extends GenericRecord> implements ObjectWriter<T> {
  private static final String COUNT_FIELD = "count";

  private final Path _path;
  private long _written = 0;
  private final long _predefinedCount;
  private DataFileWriter<T> _dataFileWriter;
  private boolean _initialized = false;
  private final Class<T> _class;

  /**
   * Creates a new instance that will write a record type.
   *
   * Normally, the file is immediately created or opened during the constructor.  However, if the type of record is
   * {@link GenericRecord}, the schema will be taken from the first record written, and only at this time will the file
   * be opened or created.  If no record is written by the time the writer is closed, the file will be created with a
   * NULL schema.
   *
   * @param recordType the class of the record type to write
   * @param path the path of the file where the data will be written.  If the file exists and is non-empty, it will be
   *             appended to.  Note that files that were previously written with a specific recordCount cannot be
   *             appended to.
   * @throws UncheckedIOException if a problem occurs opening or created the file, including if the file is being opened
   *                              to append additional records but a record count has been previously specified
   */
  public AvroWriter(Class<T> recordType, Path path) {
    this(recordType, path, -1);
  }

  /**
   * Creates a new instance that will write a record type.
   *
   * Normally, the file is immediately created or opened during the constructor.  However, if the type of record is
   * {@link GenericRecord}, the schema will be taken from the first record written, and only at this time will the file
   * be opened or created.  If no record is written by the time the writer is closed, the file will be created with a
   * NULL schema.
   *
   * @param recordType the class of the record type to write
   * @param path the path of the file where the data will be written.  If the file exists and is non-empty, it will be
   *             appended to.
   * @param recordCount the exact number of record that will be written to this file if known, or -1 otherwise.
   *                    An exception will be thrown when closing the file if a different number of records are written.
   *                    This count will be stored in the Avro file's metadata, making future AvroReader::size64() calls
   *                    much faster.  A file that is written with a specific record count <b>cannot</b> be appended to
   *                    in the future, and it is also illegal to specify a record count when appending to a file.
   * @throws UncheckedIOException if a problem occurs opening or created the file, including if the file is being opened
   *                              to append additional records but a record count has been previously specified
   * @throws IllegalArgumentException if {@code recordCount >= 0} and the writer is appending to an existing file
   */
  public AvroWriter(Class<T> recordType, Path path, long recordCount) {
    _class = recordType;
    _path = path;
    _predefinedCount = recordCount;

    if (SpecificRecord.class.isAssignableFrom(recordType)) {
      initSpecific(recordType, path);
    }
  }

  /**
   * Checks if the client is trying to append to an Avro file that has a fixed record count (as specified by a "count"
   * metadata field) or is trying to append while specifying a fixed record count (also forbidden).
   *
   * Unfortunately, Avro provides no way to modify metadata fields once a file is created, hence these restrictions.
   *
   * @param recordType the type of the record (specific or generic)
   * @param path the path being appended to
   * @throws IOException if appending to an existing Avro file with a count metadata field
   */
  private void checkForAppendingWithFixedRecordCount(Class<T> recordType, Path path) throws IOException {
    if (_predefinedCount >= 0) {
      throw new IllegalArgumentException("Attempting to append to an existing Avro file while specifying a "
          + "specific number of records.  Pass -1 as the recordCount instead.");
    }


    // check if the file has a specific count stored in its metadata
    try (AvroReader<T> reader = new AvroReader<>(recordType, path)) {
      long existing = reader.size64(true, -1);

      if (existing >= 0) {
        throw new IOException("Attempting to append to an existing Avro file that has a "
            + "'count' metadata field specifying the total number of records in the file.  This is not allowed as it "
            + "would cause the reported size of the file to be incorrect.");
      }
    }
  }

  /**
   * Initialize the writer when writing a specific record type.
   *
   * @param specificRecordType the record type being written
   * @param path the path being written to
   */
  private void initSpecific(Class<T> specificRecordType, Path path) {
    _initialized = true;
    _dataFileWriter = new DataFileWriter<>(new SpecificDatumWriter<>(specificRecordType));
    try {
      if (Files.exists(path) && Files.size(path) > 0) {
        checkForAppendingWithFixedRecordCount(specificRecordType, path);

        _dataFileWriter.appendTo(path.toFile());
      } else {
        // write the count field if necessary
        if (_predefinedCount >= 0) {
          _dataFileWriter.setMeta(COUNT_FIELD, _predefinedCount);
        }
        _dataFileWriter.create(SpecificData.get().getSchema(specificRecordType), path.toFile());
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * Initialize the writer when writing a generic record type.  Only called when T = GenericRecord.
   *
   * @param schema the schema being written
   * @param path the path being written to
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  private void initGeneric(Schema schema, Path path) {
    _initialized = true;
    _dataFileWriter = new DataFileWriter<>(new GenericDatumWriter<T>(schema));
    try {
      if (Files.exists(path) && Files.size(path) > 0) {
        // use (Class) because we know T = GenericRecord here and this is perfectly safe:
        checkForAppendingWithFixedRecordCount((Class) GenericRecord.class, path);

        _dataFileWriter.appendTo(path.toFile());
      } else {
        // write the count field if necessary
        if (_predefinedCount >= 0) {
          _dataFileWriter.setMeta(COUNT_FIELD, _predefinedCount);
        }
        _dataFileWriter.create(schema, path.toFile());
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public AvroReader<T> createReader() {
    return new AvroReader<>(_class, _path);
  }

  @Override
  public long size64() {
    return _written;
  }

  @Override
  public void write(T appended) {
    if (!_initialized) {
      initGeneric(appended.getSchema(), _path);
    }

    try {
      _dataFileWriter.append(appended);
      _written++;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public void close() {
    if (!_initialized) {
      initGeneric(Schema.create(Schema.Type.NULL), _path);
    }
    try {
      _dataFileWriter.close();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    if (_predefinedCount >= 0 && _predefinedCount != _written) {
      throw new IllegalStateException("Expected " + _predefinedCount + " records to be written, but only " + _written
          + " were.  The count field for the generated file is consequently incorrect.");
    }
  }

  /**
   * Writes Avro records contained in an arbitrary {@link ObjectReader} to a specified path.
   *
   * The type of the record and its schema is determined from the first record in the in supplied reader.  If the reader
   * is empty, an Avro file with a NULL schema and no records will be created.
   *
   * @param records the records to be written to an Avro file
   * @param path the Avro file that will be written.  If the file exists, it will be appended to.
   * @param <T> the type of the Avro record
   */
  @SuppressWarnings("unchecked")
  public static <T extends GenericRecord> void toAvroFile(ObjectReader<T> records, Path path) {
    try (ObjectIterator<T> iterator = records.iterator()) {
      if (iterator.hasNext()) {
        long size = records.size64();
        T firstRecord = iterator.next();
        try (AvroWriter<T> writer = new AvroWriter<T>((Class<T>) firstRecord.getClass(), path, size)) {
          writer.write(firstRecord);
          writer.write(iterator, size - 1);
        }
      } else {
        try (AvroWriter<GenericRecord> writer = new AvroWriter<>(GenericRecord.class, path, 0)) { }
      }
    }
  }
}
