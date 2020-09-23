package com.linkedin.dagli.util.regex;

import java.util.Objects;
import java.util.regex.Pattern;


/**
 * Static utility class for methods relating to {@link java.util.regex.Pattern}.
 */
public abstract class Patterns {
  private Patterns() { }

  /**
   * Calculates a value-based hash code for a {@link Pattern}.
   *
   * Two patterns that were created from the same pattern string with the same flags will have the same hash code.  Note
   * that it is possible for two patterns to be logically equivalent but have different hash codes.
   *
   * @param pattern the pattern whose hash code will be calculated
   * @return a hash code for the provided pattern
   */
  public static int hashCode(Pattern pattern) {
    if (pattern == null) {
      return 0;
    }

    return pattern.pattern().hashCode() + Integer.hashCode(pattern.flags());
  }

  /**
   * Returns true if two Patterns were created from the same pattern string with the same flags.  Note that it is
   * possible for this method to return false even if two patterns are logically equivalent (e.g. the patterns are
   * "..*" and ".+").
   *
   * @param pattern1 the first pattern to be compared
   * @param pattern2 the second pattern to be compared
   * @return true if and only if the two Patterns were created with the same pattern string and flags, or if both are
   *         null
   */
  public static boolean equals(Pattern pattern1, Pattern pattern2) {
    if (pattern1 == null) {
      return pattern2 == null;
    } else if (pattern2 == null) {
      return false;
    }
    return Objects.equals(pattern1.pattern(), pattern2.pattern()) && pattern1.flags() == pattern2.flags();
  }
}
