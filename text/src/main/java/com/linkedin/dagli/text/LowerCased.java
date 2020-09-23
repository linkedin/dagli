package com.linkedin.dagli.text;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.producer.MissingInput;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.transformer.AbstractPreparedTransformer1WithInput;


/**
 * Produces a lower-case version of the character sequence input.
 *
 * @author Jeff Pasternack (jepasternack@linkedin.com)
 */
@ValueEquality
public class LowerCased extends AbstractPreparedTransformer1WithInput<CharSequence, String, LowerCased> {
  private static final long serialVersionUID = 1;

  /**
   * Create a new instance of the transformer.
   */
  public LowerCased() {
    super(MissingInput.get());
  }

  /**
   * Create a new instance of the transformer that will use the specified input.
   *
   * @param stringInput the input to lower-case
   */
  public LowerCased(Producer<? extends CharSequence> stringInput) {
    super(stringInput);
  }

  @Override
  public String apply(CharSequence value0) {
    return value0.toString().toLowerCase();
  }
}
