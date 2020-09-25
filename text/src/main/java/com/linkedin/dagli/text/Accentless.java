package com.linkedin.dagli.text;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.transformer.AbstractPreparedTransformer1WithInput;
import java.text.Normalizer;
import java.util.regex.Pattern;


/**
 * Removes all accent marks from the input text.  This can be useful when normalizing text in languages with accents
 * where the accents are inconsistently used and not typically essential to meaning.
 *
 * Please note that in the current implementation, when stripping accents, the Unicode is normalized via NFD
 * normalization.  You may wish to re-normalize the result via {@link NormalizedUnicode} if this is not your preferred
 * normalization form.
 *
 * E.g. {@code Cómo estás -> Como estas}
 *
 * @author Jeff Pasternack (jepasternack@linkedin.com)
 */
@ValueEquality
public class Accentless extends AbstractPreparedTransformer1WithInput<CharSequence, String, Accentless> {
  private static final long serialVersionUID = 1;
  private static final Pattern ACCENT_MATCHER = Pattern.compile("\\p{M}");

  @Override
  public String apply(CharSequence text) {
    return ACCENT_MATCHER.matcher(Normalizer.normalize(text, Normalizer.Form.NFD)).replaceAll("");
  }
}
