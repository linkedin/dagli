package com.linkedin.dagli.text;

import com.linkedin.dagli.transformer.AbstractPreparedTransformer1WithInput;
import com.linkedin.dagli.transformer.Transformer;
import com.linkedin.dagli.util.regex.Patterns;
import java.util.regex.Pattern;


/**
 * Determines whether the input string matches a given regular expression pattern.
 *
 * @author Jeff Pasternack (jepasternack@linkedin.com)
 */
public class MatchesRegex extends AbstractPreparedTransformer1WithInput<CharSequence, Boolean, MatchesRegex> {
  private static final long serialVersionUID = 1;

  private Pattern _pattern;

  @Override
  protected boolean computeEqualsUnsafe(MatchesRegex other) {
    return Patterns.equals(this._pattern, other._pattern);
  }

  @Override
  protected int computeHashCode() {
    return Transformer.hashCodeOfInputs(this) + Patterns.hashCode(_pattern);
  }

  /**
   * Sets the regular expression that will be matched against inputs.
   *
   * @param pattern the regex pattern to use
   * @return a copy of this instance that will use the specified regular expression pattern
   */
  public MatchesRegex withPattern(Pattern pattern) {
    return clone(c -> c._pattern = pattern);
  }

  /**
   * Sets the regular expression that will be matched against inputs.
   *
   * @param pattern the regex pattern to use
   * @return a copy of this instance that will use the specified regular expression pattern
   */
  public MatchesRegex withPattern(String pattern) {
    return clone(c -> c._pattern = Pattern.compile(pattern));
  }

  /**
   * Sets the regular expression that will be matched against inputs.
   *
   * @param pattern the regex pattern to use
   * @param flags the bitwise-union'ed flags to use (e.g. MULTILINE), as defined by the {@link Pattern} class.
   * @return a copy of this instance that will use the specified regular expression pattern
   */
  public MatchesRegex withPattern(String pattern, int flags) {
    return clone(c -> c._pattern = Pattern.compile(pattern, flags));
  }

  /**
   * Creates a new instance with no pattern set.
   */
  public MatchesRegex() {
    _pattern = null;
  }

  @Override
  public Boolean apply(CharSequence input) {
    return _pattern.matcher(input).find();
  }

  @Override
  public void validate() {
    super.validate();
    if (_pattern == null) {
      throw new IllegalStateException("The pattern of MatchesRegex has not been set");
    }
  }
}
