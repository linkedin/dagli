package com.linkedin.dagli.text;

import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.transformer.AbstractPreparedTransformer2;
import com.linkedin.dagli.transformer.Transformer;
import com.linkedin.dagli.util.regex.Patterns;
import java.util.regex.Pattern;


/**
 * Replaces regular expression matches in the first input string with a replacement provided by the second input string.
 *
 * @author Jeff Pasternack (jepasternack@linkedin.com)
 */
public class ReplacedRegex extends AbstractPreparedTransformer2<CharSequence, CharSequence, String, ReplacedRegex> {
  private static final long serialVersionUID = 1;

  private Pattern _pattern;

  @Override
  protected boolean computeEqualsUnsafe(ReplacedRegex other) {
    return Patterns.equals(this._pattern, other._pattern);
  }

  @Override
  protected int computeHashCode() {
    return Transformer.hashCodeOfInputs(this) + Patterns.hashCode(_pattern);
  }

  /**
   * Sets the regular expression that will be replaced with an inputted replacement string.
   *
   * @param pattern the regex pattern to use
   * @return a copy of this instance that will use the specified regular expression pattern
   */
  public ReplacedRegex withPattern(Pattern pattern) {
    return clone(c -> c._pattern = pattern);
  }

  /**
   * Sets the regular expression that will be replaced with an inputted replacement string.
   *
   * @param pattern the regex pattern to use
   * @return a copy of this instance that will use the specified regular expression pattern
   */
  public ReplacedRegex withPattern(String pattern) {
    return clone(c -> c._pattern = Pattern.compile(pattern));
  }

  /**
   * Sets the regular expression that will be replaced with an inputted replacement string.
   *
   * @param pattern the regex pattern to use
   * @param flags the bitwise-union'ed flags to use (e.g. MULTILINE), as defined by the {@link Pattern} class.
   * @return a copy of this instance that will use the specified regular expression pattern
   */
  public ReplacedRegex withPattern(String pattern, int flags) {
    return clone(c -> c._pattern = Pattern.compile(pattern, flags));
  }

  /**
   * Sets the input that will provide the target string.  All matches of the pattern in this inputted target string
   * will be replaced with an inputted replacement string to generate the result.
   *
   * For example, if the pattern is "cat", the inputted target string is "catfish", and the inputted replacement is
   * "dog", the result of the transformation will be "dogfish".
   *
   * @param targetInput the input that provides the target string in which all matches of the pattern will be replaced
   * @return a copy of this instance that will use the specified input
   */
  public ReplacedRegex withTargetInput(Producer<? extends CharSequence> targetInput) {
    return clone(c -> c._input1 = targetInput);
  }

  /**
   * Sets the input that will provide the replacement string.  All matches of the pattern in the inputted target string
   * will be replaced with this string to generate the result.
   *
   * For example, if the pattern is "cat", the inputted target string is "catfish", and the inputted replacement is
   * "dog", the result of the transformation will be "dogfish".
   *
   * @param replacementInput the input that provides the string that will replace all matches of the pattern
   * @return a copy of this instance that will use the specified input
   */
  public ReplacedRegex withReplacementInput(Producer<? extends CharSequence> replacementInput) {
    return clone(c -> c._input2 = replacementInput);
  }

  /**
   * Creates a new instance with no pattern set.
   */
  public ReplacedRegex() {
    _pattern = null;
  }

  @Override
  public String apply(CharSequence target, CharSequence replacement) {
    return _pattern.matcher(target).replaceAll(replacement.toString());
  }

  @Override
  public void validate() {
    super.validate();
    if (_pattern == null) {
      throw new IllegalStateException("The pattern of ReplacedRegex has not been set");
    }
  }
}
