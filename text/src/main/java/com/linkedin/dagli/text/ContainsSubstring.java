package com.linkedin.dagli.text;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import java.util.ArrayList;
import java.util.Collection;


/**
 * Efficiently determines whether the input string contains any of a list of substrings.  This is useful for, e.g.
 * blacklisting or whitelisting certain specific phrases.
 *
 * @author Jeff Pasternack (jepasternack@linkedin.com)
 */
@ValueEquality
public class ContainsSubstring extends AbstractSubstringSearch<Boolean, ContainsSubstring> {
  private static final long serialVersionUID = 1;

  private Collection<String> _substrings = null;

  /**
   * Sets the substrings that will be sought; the search in the input text will be case-sensitive.
   *
   * @param substrings the strings to search for in the input
   * @return a copy of this instance that will search for the specified substrings
   */
  public ContainsSubstring withSubstrings(Collection<String> substrings) {
    return withSubstrings(substrings, true);
  }

  /**
   * Sets the substrings that will be sought.
   *
   * @param substrings the strings to search for in the input
   * @param caseSensitive whether the search for the substrings will be case-sensitive
   * @return a copy of this instance that will search for the specified substrings
   */
  public ContainsSubstring withSubstrings(Collection<String> substrings, boolean caseSensitive) {
    return clone(c -> {
      c._substrings = new ArrayList<>(substrings);
      c._caseSensitivity = caseSensitive;
    });
  }

  @Override
  protected Collection<String> getSubstrings() {
    return _substrings;
  }

  @Override
  protected boolean getSingleResultOnly() {
    return true;
  }

  @Override
  protected boolean getAllowOverlaps() {
    return true;
  }

  @Override
  public Boolean apply(String value0) {
    return !getEmits(value0, 1).isEmpty();
  }

  @Override
  public void validate() {
    super.validate();
    if (_substrings == null) {
      throw new IllegalStateException("No substrings have been provided via withSubstrings(...)");
    }
  }
}
