package com.linkedin.dagli.text;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import java.util.ArrayList;
import java.util.Collection;


/**
 * Efficiently counts the number of times the input string contains any of a list of substrings.  This is useful for,
 * e.g. creating features around the frequency of certain words.
 *
 * @author Jeff Pasternack (jepasternack@linkedin.com)
 */
@ValueEquality
public class SubstringCount extends AbstractSubstringCount<SubstringCount> {
  private static final long serialVersionUID = 1;

  private Collection<String> _substrings = null;
  private boolean _allowOverlaps = false;

  /**
   * Sets the substrings that will be sought; the search in the input text will be case-sensitive.
   *
   * @param substrings the strings to search for in the input
   * @return a copy of this instance that will search for the specified substrings
   */
  public SubstringCount withSubstrings(Collection<String> substrings) {
    return withSubstrings(substrings, true);
  }

  /**
   * Sets the substrings that will be sought.
   *
   * @param substrings the strings to search for in the input
   * @param caseSensitive whether the search for the substrings will be case-sensitive
   * @return a copy of this instance that will search for the specified substrings
   */
  public SubstringCount withSubstrings(Collection<String> substrings, boolean caseSensitive) {
    return clone(c -> {
      c._substrings = new ArrayList<>(substrings);
      c._caseSensitivity = caseSensitive;
    });
  }

  /**
   * Sets whether or not overlapping substrings will be counted.  If true, each appearance of each sought substring
   * is counted, even if those appearances overlap each other.  If false, the leftmost, longest substring is preferred.
   * The default is false.
   *
   * Example: if overlaps are allowed, the sought substrings are "dog", "dogg", and "oggy" and the text is
   * "hello doggy", the count will be 3.  If overlaps are not allowed, the count will be 1 ("dogg").
   *
   * @param allowOverlaps whether or not overlaps will be counted
   * @return a copy of this instance with the specified overlapping substring policy
   */
  public SubstringCount withOverlappingSubstringsCounted(boolean allowOverlaps) {
    return clone(c -> c._allowOverlaps = allowOverlaps);
  }

  @Override
  protected Collection<String> getSubstrings() {
    return _substrings;
  }

  @Override
  protected boolean getAllowOverlaps() {
    return _allowOverlaps;
  }

  @Override
  public void validate() {
    super.validate();
    if (_substrings == null) {
      throw new IllegalStateException("No substrings have been provided via withSubstrings(...)");
    }
  }
}
