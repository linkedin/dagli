package com.linkedin.dagli.handle;

import com.linkedin.dagli.producer.Producer;
import java.util.UUID;


/**
 * Handle for a producer.  The handle can be used to pull the producer out of a containing DAG.
 *
 * Internally, a handle contains a UUID and the producer's class name.  All producers must provide a handle with a UUID
 * such that:
 * (1) The handle/UUID remains constant after the object is serialized and then deserialized.
 * (2) Two Producers with different observable properties, method return values, or semantically meaningful side
 *     effects (for the pedantic, throwing an exception or adding to a global counter is a meaningful side effect;
 *     using a certain amount of CPU or RAM is not) must not have the same UUID.  That is, if two instances can be
 *     distinguished from each other by observers outside the class (private fields and methods do not count), the
 *     instances must have different UUIDs.
 *
 * Notice that it is *not* required that two otherwise indistinguishable Producers have the same handle/UUID.
 *
 * @param <T> the type of producer that will be retrieved with this handle.  This may be a supertype of the actual
 *            concrete producer type(s) that will be returned.
 */
public class ProducerHandle<T extends Producer>
  extends AbstractProducerUUIDHandle<T, ProducerHandle<T>> {
  private static final long serialVersionUID = 1;

  /**
   * Creates a new handle to producer.
   *
   * @param producerClass the type of producer that will be addressed by this handle
   * @param uuid the UUID for this handle
   */
  public ProducerHandle(Class<T> producerClass, UUID uuid) {
    super(producerClass.getName(), uuid);
  }

  /**
   * Creates a new handle to producer.
   *
   * @param producerClass the type of producer that will be addressed by this handle
   * @param mostSignificantBits the most significant bits of the UUID
   * @param leastSignificantBits the least significant bits of the UUID
   */
  public ProducerHandle(Class<T> producerClass, long mostSignificantBits, long leastSignificantBits) {
    super(producerClass.getName(), mostSignificantBits, leastSignificantBits);
  }

  /**
   * Creates a new handle to producer.
   *
   * @param producerClassName the type of producer that will be addressed by this handle
   * @param mostSignificantBits the most significant bits of the UUID
   * @param leastSignificantBits the least significant bits of the UUID
   */
  ProducerHandle(String producerClassName, long mostSignificantBits, long leastSignificantBits) {
    super(producerClassName, mostSignificantBits, leastSignificantBits);
  }

  /**
   * Calculates the hash code for a 128-bit UUID as specified by its least and most significant bits.  The returned
   * hash will be the same as that returned by inovking {@link #hashCode()} on a handle created with the same UUID.
   *
   * @param uuidLeastSignificantBits the least significant bits of the UUID
   * @param uuidMostSignificantBits the most significant bits of the UUID
   * @return the hash code for this UUID
   */
  public static int hashCode(long uuidLeastSignificantBits, long uuidMostSignificantBits) {
    return AbstractProducerUUIDHandle.hashCode(uuidLeastSignificantBits, uuidMostSignificantBits);
  }
}