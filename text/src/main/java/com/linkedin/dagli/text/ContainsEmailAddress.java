package com.linkedin.dagli.text;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.transformer.AbstractPreparedTransformer1WithInput;
import java.util.regex.Pattern;


/**
 * Determines whether the input string contains at least one email address.
 *
 * @author Jeff Pasternack (jepasternack@linkedin.com)
 */
@ValueEquality
public class ContainsEmailAddress
    extends AbstractPreparedTransformer1WithInput<CharSequence, Boolean, ContainsEmailAddress> {
  private static final long serialVersionUID = 1;

  private static final Pattern PATTERN = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}");

  @Override
  public Boolean apply(CharSequence input) {
    return PATTERN.matcher(input).find();
  }
}
