package com.linkedin.dagli.util.named;

import java.util.Objects;


/**
 * Interface for objects that have human-friendly names.
 *
 * Names are human-friendly identifiers of an object that may be distinct from the String produced by
 * {@link Object#toString()}; in particular, the idea is to disambiguate the instance from others rather than provide a
 * more comprehensive description or representation.  However, names are not intended to serve as unique or even
 * consistent identifiers: two objects may share the same name, a deserialized object need not have the same name as
 * its progenitor, and a mutable object's name may change.  Furthermore, different names in and of themselves do not
 * disqualify two objects from comparing as {@link Object#equals(Object)}.
 *
 * Names should not be {@code null}.
 *
 * The static {@link #getName(Object)} and {@link #getShortName(Object)} methods provide a convenient way to get the
 * names of objects, falling back to {@link Object#toString()} when this interface is not implemented.
 */
public interface Named {
  /**
   * @param obj the object whose name is sought
   * @return the name of the object if it is instance of {@link Named}, or the string representation of the object as
   *         determined by {@link Objects#toString(Object)} otherwise
   */
  static String getName(Object obj) {
    if (obj instanceof Named) {
      return ((Named) obj).getName();
    }
    return Objects.toString(obj);
  }

  /**
   * @param obj the object whose short name is sought
   * @return the short name of the object if it is instance of {@link Named}, or the string representation of the object
   *         as determined by {@link Objects#toString(Object)} otherwise
   */
  static String getShortName(Object obj) {
    if (obj instanceof Named) {
      return ((Named) obj).getShortName();
    }
    return Objects.toString(obj);
  }

  /**
   * Gets the (non-null) name of an instance.
   *
   * When computing the name of an object which itself will contain the names of other objects (e.g. those in its
   * fields), implementations should generally use {@link #getShortName()} to find the names of these other
   * objects to avoid both the potential for infinite recursion (if there is a cycle in the object graph) and to
   * avoid creating an excessively long name.
   *
   * The default implementation simply calls {@link Objects#toString(Object)}.
   *
   * @return a (non-null) name of an instance.
   */
  default String getName() {
    return Objects.toString(this);
  }

  /**
   * Gets the (non-null) short name of an instance.
   *
   * The "short name" is intended to provide a very succinct, very inexpensively calculated name for this instance that
   * will generally be no longer than the name returned by {@link #getName()}.
   *
   * In particular, if the name returned by {@link #getName()} nests the names of other objects (e.g. fields), an
   * implementation of this method should generally also be provided to supply a shorter name that does not incorporate
   * other names.  This both helps avoid a potential source of infinite recursion and keep names reasonably concise.
   *
   * The default implementation simply calls {@link #getName()}.
   *
   * @return the (non-null) short name of an instance
   */
  default String getShortName() {
    return getName();
  }
}
