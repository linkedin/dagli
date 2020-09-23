package com.linkedin.dagli.text;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.transformer.AbstractPreparedTransformer1WithInput;
import java.text.Normalizer;


/**
 * Normalizes the Unicode of the inputted text, which can avoid issues where two texts are semantically equivalent but
 * have different Unicode representations.
 *
 * E.g. "ﬀ" -> "ff" (under NKFC/D normalization forms)
 *
 * @author Jeff Pasternack (jepasternack@linkedin.com)
 */
@ValueEquality
public class NormalizedUnicode extends AbstractPreparedTransformer1WithInput<CharSequence, String, NormalizedUnicode> {
  private static final long serialVersionUID = 1;

  private Normalizer.Form _form = Normalizer.Form.NFKC;

  /**
   * Gets the form of the normalization to be used, e.g. NFC
   *
   * @return the normalization form
   */
  public Normalizer.Form getForm() {
    return _form;
  }

  /**
   * Sets the normalization form to use.  See {@link Normalizer.Form} for more information.
   *
   * The default normalization form is NFKC, which is recommended for use in machine learning applications.
   *
   * @param form the normalization form to use
   * @return a copy of this instance that will use the specified form
   */
  public NormalizedUnicode withForm(Normalizer.Form form) {
    return clone(c -> c._form = form);
  }

  @Override
  public String apply(CharSequence value0) {
    return Normalizer.normalize(value0, _form);
  }
}
