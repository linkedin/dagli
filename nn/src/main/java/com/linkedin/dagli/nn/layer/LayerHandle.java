package com.linkedin.dagli.nn.layer;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;


/**
 * A handle to a {@link NNLayer}.  Handles are useful for referring to a layer without needing the actual layer itself.
 *
 * Layer handles know their corresponding layer class, but their hash and equals() methods compare their UUIDs only; it
 * is an invariant that no two handles with the same UUID but different layer classes should exist (this is enforced by
 * how the handles are created).
 *
 * @param <L> the type of the layer to which this handle points
 */
public final class LayerHandle<L extends NNLayer<?, L>> implements Serializable, Comparable<LayerHandle<?>> {
  private static final long serialVersionUID = 1;

  // storing the canonical class name as a string (rather than a class) avoids potentially loading the class
  private final String _className;

  // to avoid allocating an extra object on the heap, we store the bits of the UUID directly, rather than a UUID object:
  private final long _uuidLeastSignificantBits;
  private final long _uuidMostSignificantBits;

  // private no-arg constructor for Kryo
  private LayerHandle() {
    _className = null;
    _uuidLeastSignificantBits = 0;
    _uuidMostSignificantBits = 0;
  }

  /**
   * Creates a new handle to the specified class with a random, unique UUID
   *
   * @param layerClass the class of the layer this handle will point to
   */
  LayerHandle(Class<L> layerClass) {
    UUID newUUID = UUID.randomUUID();
    _uuidLeastSignificantBits = newUUID.getLeastSignificantBits();
    _uuidMostSignificantBits = newUUID.getMostSignificantBits();
    _className = layerClass.getCanonicalName();
  }

  long getUUIDLeastSignificantBits() {
    return _uuidLeastSignificantBits;
  }

  long getUUIDMostSignificantBits() {
    return _uuidMostSignificantBits;
  }

  // as always, having the same hashcode does not guarantee equals() == true
  @Override
  public int hashCode() {
    return hashCode(_uuidLeastSignificantBits, _uuidMostSignificantBits);
  }

  /**
   * Calculates the hash code for a 128-bit UUID as specified by its least and most significant bits.  The returned
   * hash will be the same as that returned by invoking {@link #hashCode()} on a handle created with the same UUID.
   *
   * @param uuidLeastSignificantBits the least significant bits of the UUID
   * @param uuidMostSignificantBits the most significant bits of the UUID
   * @return the hash code for this UUID
   */
  static int hashCode(long uuidLeastSignificantBits, long uuidMostSignificantBits) {
    return Long.hashCode(uuidLeastSignificantBits) + Long.hashCode(Long.rotateLeft(uuidMostSignificantBits, 3));
  }

  /**
   * Two layer handles are considered equal if they have the same UUIDs.
   *
   * @param obj the object to compare to
   * @return true if this handle is considered equal to obj, false otherwise
   */
  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof LayerHandle)) {
      return false;
    }

    LayerHandle<?> other = (LayerHandle<?>) obj;
    if (this._uuidLeastSignificantBits != other._uuidLeastSignificantBits
        || this._uuidMostSignificantBits != other._uuidMostSignificantBits) {
      return false;
    }

    // Having the same UUID also implies they have the same class name; this is checked by an assert:
    assert Objects.equals(this._className, other._className);

    return true;
  }

  @Override
  public String toString() {
    return "Handle to " + _className + " with UUID " + new UUID(_uuidMostSignificantBits,
        _uuidLeastSignificantBits).toString();
  }

  @Override
  public int compareTo(LayerHandle<?> o) {
    if (this._uuidMostSignificantBits != o._uuidMostSignificantBits) {
      return Long.compare(this._uuidMostSignificantBits, o._uuidMostSignificantBits);
    }
    return Long.compare(this._uuidLeastSignificantBits, o._uuidLeastSignificantBits);
  }
}
