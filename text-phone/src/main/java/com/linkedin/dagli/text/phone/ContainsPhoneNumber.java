package com.linkedin.dagli.text.phone;

import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.transformer.AbstractPreparedTransformer1WithInput;


/**
 * Checks whether or not the input string contains a phone number.
 *
 * @author Jeff Pasternack (jepasternack@linkedin.com)
 */
@ValueEquality
public class ContainsPhoneNumber
    extends AbstractPreparedTransformer1WithInput<CharSequence, Boolean, ContainsPhoneNumber> {
  private static final long serialVersionUID = 1;

  private static final PhoneNumberUtil INSTANCE = PhoneNumberUtil.getInstance();
  private PhoneNumberUtil.Leniency _leniency = PhoneNumberUtil.Leniency.VALID;

  /**
   * Sets the leniency used to detect the number.  The default is Leniency.VALID, which, e.g. will bar local numbers.
   * A looser, more permissive option is Leniency.POSSIBLE.
   *
   * @param leniency the strictness used when finding phone numbers, effectively trading precision for recall, or vice-
   *                 versa.
   * @return a copy of this instance with the desired leniency.
   */
  public ContainsPhoneNumber withLeniency(PhoneNumberUtil.Leniency leniency) {
    return clone(c -> c._leniency = leniency);
  }

  @Override
  public Boolean apply(CharSequence input) {
    return INSTANCE.findNumbers(input, "US", _leniency, Long.MAX_VALUE).iterator().hasNext();
  }
}
