package com.linkedin.dagli.text;

/**
 * Efficiently counts the number of times the input string contains any of a list of substrings.  This is useful for,
 * e.g. creating features around the frequency of certain words.
 *
 * @author Jeff Pasternack (jepasternack@linkedin.com)
 */
public abstract class AbstractSubstringCount<S extends AbstractSubstringCount<S>> extends AbstractSubstringSearch<Integer, S> {
  private static final long serialVersionUID = 1;

  @Override
  protected boolean getSingleResultOnly() {
    return false;
  }

  @Override
  public Integer apply(String value0) {
    return getEmits(value0, 8).size();
  }
}
