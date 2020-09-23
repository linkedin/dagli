package com.linkedin.dagli.dag;

import com.linkedin.dagli.objectio.biglist.BigListWriter;
import com.linkedin.dagli.objectio.ObjectWriter;
import com.linkedin.dagli.util.kryo.KryoWriters;
import it.unimi.dsi.fastutil.objects.ObjectBigArrayBigList;
import java.util.function.LongFunction;

/**
 * Determines how intermediate results are stored on the local machine.
 */
public enum LocalStorage {
  /**
   * Values are stored on the JVM's heap.  This consumes the most RAM, but provides the fastest storage and retrieval
   * times (the same as any other Java objects)
   */
  MEMORY_HEAP(c -> new BigListWriter<>(new ObjectBigArrayBigList<>(c))),

  /**
   * Values are written and read from files in the dagli.tmpdir directory in Kryo format.
   * Please note that Kryo is a fast way to serialize values, but may not be compatible with all objects.
   */
  DISK_KRYO(c -> KryoWriters.kryo()),

  /**
   * Values are written and read from files in the dagli.tmpdir directory in Kryo format.
   * All data will be both compressed and encrypted, which will likely slow writing and reading.
   * Please note that Kryo is a fast way to serialize values, but may not be compatible with all objects.
   */
  DISK_KRYO_COMPRESSED_AND_ENCRYPTED(c -> KryoWriters.kryoFrom(null, true, true)),

  /**
   * Values are written and read from files in the dagli.tmpdir directory in Kryo format.
   * All data will be compressed, which will save space and may speed up reading and writing when using slow I/O
   * devices, particularly spinning disks or slower network storage.
   * Please note that Kryo is a fast way to serialize values, but may not be compatible with all objects.
   */
  DISK_KRYO_COMPRESSED(c -> KryoWriters.kryoFrom(null, true, false)),

  /**
   * Values are written and read from files in the dagli.tmpdir directory in Kryo format.
   * All data will be encrypted, which will likely slow writing and reading.
   * Please note that Kryo is a fast way to serialize values, but may not be compatible with all objects.
   */
  DISK_KRYO_ENCRYPTED(c -> KryoWriters.kryoFrom(null, false, true));

  final LongFunction<ObjectWriter<Object>> _objectWriterGenerator;

  LocalStorage(LongFunction<ObjectWriter<Object>> generator) {
    _objectWriterGenerator = generator;
  }
}
