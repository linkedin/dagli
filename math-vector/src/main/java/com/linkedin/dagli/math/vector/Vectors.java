package com.linkedin.dagli.math.vector;

/**
 * Utility class of static helper methods for using {@link Vector}s.
 */
abstract class Vectors {
  /**
   * Checks if two (non-null) vectors are equal in a general way, without invoking the equals() method on either.
   *
   * @param vector1 the first vector
   * @param vector2 the second vector
   * @return true iff the two vectors have identical elements
   */
  static boolean equals(Vector vector1, Vector vector2) {
    if (vector1 == vector2) {
      return true;
    }

    final long nonZeros = vector1.size64();
    if (nonZeros != vector2.size64()) {
      return false;
    }

    // take advantage of the fact that both iterators are ordered
    VectorElementIterator iter1 = vector1.iterator();
    VectorElementIterator iter2 = vector2.iterator();
    boolean[] match = new boolean[1];
    for (long i = 0; i < nonZeros; i++) {
      iter1.next((index1, value1) -> iter2.next((index2, value2) -> match[0] = index1 == index2 && value1 == value2));
      if (!match[0]) {
        return false;
      }
    }

    return true;
  }

  /**
   * Calculates a hashcode for a vector (without calling the vector's hashCode() method).
   *
   * @param vector the vector whose hash should be calculated
   * @return the calculated hashcode
   */
  static int hashCode(Vector vector) {
    int hashCode = 0;
    VectorElementIterator iter = vector.unorderedIterator(); // we can encounter values in any sequence
    while (iter.hasNext()) {
      hashCode += iter.mapNext(VectorElement::hashCode);
    }
    return hashCode;
  }

  /**
   * Gets a {@link String} representation of the given vector (without calling the vector's toString() method).
   *
   * @param vector the vector whose stringification is sought
   * @return a {@link String} representation of the vector
   */
  static String toString(Vector vector) {
    return "[" + String.join(", ", vector.stream().map(VectorElement::toString).toArray(String[]::new)) + "]";
  }
}
