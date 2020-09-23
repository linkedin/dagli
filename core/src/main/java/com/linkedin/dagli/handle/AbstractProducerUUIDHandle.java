package com.linkedin.dagli.handle;

import com.linkedin.dagli.producer.Producer;
import java.util.UUID;


/**
 * Base class for handles to producers where the handle contains a universally unique identifier to a producer.
 *
 * Note that derived handle types are not necessarily a handle to the uniquely identified producer!  An example would be
 * a handle to a prepared producer inside a prepared DAG that was prepared from the identified producer.
 *
 * All producers must provide a UUID such that:
 * (1) The UUID remains constant after the object is serialized and then deserialized.
 * (2) Two Producers with different observable properties, method return values, or semantically meaningful side
 *     effects (for the pedantic, throwing an exception or adding to a global counter is a meaningful side effect;
 *     using a certain amount of CPU or RAM is not) must not have the same UUID.  That is, if two instances can be
 *     distinguished from each other by observers outside the class (private fields and methods do not count), the
 *     instances must have different UUIDs.
 *
 * Notice that it is *not* required that two otherwise indistinguishable Producers have the same UUID.
 *
 * @param <T> the type of producer that will be retrieved with this handle.  This may be a supertype of the actual
 *            concrete producer type(s) that will be returned.
 * @param <S> the type of the derived handle
 */
abstract class AbstractProducerUUIDHandle<T extends Producer, S extends AbstractProducerUUIDHandle<T, S>>
  extends AbstractProducerHandle<T, S> implements Comparable<AbstractProducerUUIDHandle> {
  private static final long serialVersionUID = 1;

  // to avoid allocating an extra object on the heap, we store the bits of the UUID directly, rather than a UUID object:
  private final long _uuidLeastSignificantBits;
  private final long _uuidMostSignificantBits;

  /**
   * Creates a new handle to the specified class based on the specified UUID.
   *
   * @param producerClassName the type of producer that will be addressed by this handle
   * @param uuid the UUID for this handle
   */
  protected AbstractProducerUUIDHandle(String producerClassName, UUID uuid) {
    this(producerClassName, uuid.getMostSignificantBits(), uuid.getLeastSignificantBits());
  }

  protected AbstractProducerUUIDHandle(String producerClassName, long mostSignificantBits, long leastSignificantBits) {
    super(producerClassName);
    _uuidLeastSignificantBits = leastSignificantBits;
    _uuidMostSignificantBits = mostSignificantBits;
  }

  protected long getUUIDLeastSignificantBits() {
    return _uuidLeastSignificantBits;
  }

  protected long getUUIDMostSignificantBits() {
    return _uuidMostSignificantBits;
  }

  public UUID getUUID() {
    return new UUID(_uuidMostSignificantBits, _uuidLeastSignificantBits);
  }

  // note that, as always, having the same hashcode does not guarantee equals() == true
  @Override
  public int hashCode() {
    return hashCode(_uuidLeastSignificantBits, _uuidMostSignificantBits);
  }

  /**
   * Calculates the hash code for a 128-bit UUID as specified by its least and most significant bits.  The returned
   * hash will be the same as that returned by inovking {@link #hashCode()} on a handle created with the same UUID.
   *
   * @param uuidLeastSignificantBits the least significant bits of the UUID
   * @param uuidMostSignificantBits the most significant bits of the UUID
   * @return the hash code for this UUID
   */
  protected static int hashCode(long uuidLeastSignificantBits, long uuidMostSignificantBits) {
    return Long.hashCode(uuidLeastSignificantBits) + Long.hashCode(Long.rotateLeft(uuidMostSignificantBits, 3));
  }

  /**
   * Two UUID handles are considered equal if they have the same UUIDs *and* either one is derived from the other
   * or both have the same type.  Note that the class associated with the handle is not considered.
   *
   * @param obj the object to compare to
   * @return true if this handle is considered equal to obj, false otherwise
   */
  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof AbstractProducerHandle)) {
      return false;
    }

    AbstractProducerUUIDHandle<?, ?> other = (AbstractProducerUUIDHandle<?, ?>) obj;
    if (this._uuidLeastSignificantBits != other._uuidLeastSignificantBits
        || this._uuidMostSignificantBits != other._uuidMostSignificantBits) {
      return false;
    }

    return compareClass(other) == 0;
  }

  /**
   * "Compares" this instance's class with the class of another AbstractProducerUUIDHandle.
   *
   * The method returns 0 if the classes are the same or one derives from the other.
   * Otherwise, it returns a value obtained by comparing the objects' class names.
   *
   * @param other the object to compare against
   * @return 0 if the classes are equal, an arbitrary-but-consistent non-zero value derived from comparing class names
   * otherwise.
   */
  private int compareClass(AbstractProducerUUIDHandle other) {
    Class<? extends AbstractProducerUUIDHandle> myClass = this.getClass();
    Class<? extends AbstractProducerUUIDHandle> otherClass = other.getClass();

    if (myClass.isAssignableFrom(otherClass) || otherClass.isAssignableFrom(myClass)) {
      return 0;
    } else {
      return myClass.getName().compareTo(otherClass.getName());
    }
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName() + " targeting " + getTargetClassName() + " with UUID "
        + getUUID().toString();
  }

  @Override
  public int compareTo(AbstractProducerUUIDHandle o) {
    // first, check that o has a suitable class.  If it doesn't, these two instances are not equal and their order
    // depends on the names of the classes
    int classComparison = compareClass(o);
    if (classComparison != 0) {
      return classComparison;
    }

    if (this._uuidMostSignificantBits != o._uuidMostSignificantBits) {
      return Long.compare(this._uuidMostSignificantBits, o._uuidMostSignificantBits);
    }
    return Long.compare(this._uuidLeastSignificantBits, o._uuidLeastSignificantBits);
  }
}