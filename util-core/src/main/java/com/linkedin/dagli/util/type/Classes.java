package com.linkedin.dagli.util.type;

import java.util.Arrays;
import java.util.stream.Stream;


public class Classes {
  private Classes() { }

  /**
   * Gets a Class from a canonical name.  Class.getClass(...) will not work correctly for nested class names that
   * do not use the "$" character to separate the top-level and nested class names, but canonical names use dots.
   *
   * @param canonicalName the canonical name of the class, e.g. "java.lang.String".
   * @return a Class instance for the specified class
   */
  public static Class forCanonicalName(String canonicalName) throws ClassNotFoundException {
    try {
      return Class.forName(canonicalName);
    } catch (ClassNotFoundException e) {
      int lastDot = canonicalName.lastIndexOf('.');
      if (lastDot < 0) {
        throw e;
      }

      StringBuilder builder = new StringBuilder(canonicalName);
      builder.setCharAt(lastDot, '$');
      try {
        return forCanonicalName(builder.toString());
      } catch (ClassNotFoundException f) {
        throw e; // throw the original (top-level) exception
      }
    }
  }

  /**
   * Returns a stream containing all the interfaces directly implemented by the specified class (or extended by the
   * specified interface), as well as all the interfaces that <strong>those</strong> interfaces extend (directly or
   * transitively).
   *
   * If a class is provided, the returned stream does not include the interfaces of its superclass(es).  If an interface
   * is provided the returned stream will not include this interface (only its ancestors).
   *
   * It is possible for the same interface to occur multiple times in the interface tree; these duplicates will be
   * included in the returned stream.
   *
   * No particular ordering of the interfaces in the resulting stream is guaranteed.
   *
   * @param clazz the class/interface whose ancestor interfaces should be walked
   * @return the stream of walked ancestor interfaces
   */
  private static Stream<Class<?>> walkInterfaces(Class<?> clazz) {
    return Stream.concat(Arrays.stream(clazz.getInterfaces()),
        Arrays.stream(clazz.getInterfaces()).flatMap(Classes::walkInterfaces));
  }

  /**
   * Returns a stream that contains both the provided class/interface and all of its ancestor clases and interfaces.
   *
   * Interfaces may exist multiple times among a class/interface's ancestors; the returned stream may thus accordingly
   * contain a given interface multiple times.
   *
   * No particular ordering of the returned stream is guaranteed.
   *
   * @param clazz the class/interface whose hierarchy should be walked by the returned stream
   * @return a stream that contains the provided class/interface and all its ancestor classes and interfaces
   */
  public static Stream<Class<?>> walkHierarchy(Class<?> clazz) {
    Stream<Class<?>> res = Stream.concat(
        Stream.of(clazz),
        Stream.of(clazz).flatMap(Classes::walkInterfaces)); // don't call walkInterfaces unless/until we have to

    Class<?> superclass = clazz.getSuperclass();
    return superclass == null ? res : Stream.concat(res, Stream.of(superclass).flatMap(Classes::walkHierarchy));
  }
}
