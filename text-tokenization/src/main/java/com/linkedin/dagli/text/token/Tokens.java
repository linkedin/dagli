package com.linkedin.dagli.text.token;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.generator.Constant;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.text.jflex.JFlexTokenizer;
import com.linkedin.dagli.transformer.AbstractPreparedStatefulTransformer2;
import com.linkedin.dagli.util.function.Function1;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Tokenizes a string of natural text into its constituent tokens (words, punctuation, emoticons, etc.) using the best
 * available tokenizer for each locale provided.
 *
 * The locale may be given for each example using {@link Tokens#withLocale(Locale)} or
 * {@link Tokens#withLocaleInput(Producer)}.  The default is US English (en-US) unless configured otherwise.
 *
 * E.g. a tokenizer created with {@code new Tokens().withInput(texts).withLocale(Locale.FRENCH)} will tokenize all
 * the "texts" inputs under the assumption that they are all in French.
 *
 * Alternatively, {@code new Tokens().withInput(texts).withLocaleInput(locales)} will tokenize each text input using the
 * tokenizer appropriate for its corresponding locale input; this allows a single Tokenizer to tokenize inputs in
 * multiple, dynamically-determined languages.
 *
 * Please note that it is possible for the underlying tokenizer to change in future versions (be replaced, be retrained,
 * etc.)
 *
 * <strong>Current language support:</strong>
 * English and German are considered to be well supported.
 * Other Latin-charset languages should also tokenize well.
 * Non-Latin languages such as Hindi and Russian may have poor results.
 * Logographic languages such as Chinese or Japanese will perform very poorly.
 *
 * @author Jeff Pasternack (jepasternack@linkedin.com)
 */
@ValueEquality
public class Tokens extends AbstractPreparedStatefulTransformer2<
    Locale, CharSequence, List<String>, ConcurrentHashMap<Locale, Function1<CharSequence, List<String>>>, Tokens> {
  private static final long serialVersionUID = 1;

  /**
   * Creates a new tokenizer with the default locale of US English.
   */
  public Tokens() {
    _input1 = new Constant<>(Locale.US);
  }

  /**
   * Sets the input that will provide the locale of the text.  Different tokenizers may be used for different
   * locales.  If no locale input is specified, the default locale is {@link Locale#US}.
   *
   * @param localeInput the input that will provide the locale for the text
   * @return a copy of this instance that will use the specified input
   */
  public Tokens withLocaleInput(Producer<? extends Locale> localeInput) {
    return clone(c -> c._input1 = localeInput);
  }

  /**
   * Sets the locale of the text.  Different tokenizers may be used for different locales.  If no locale is specified,
   * the default locale is {@link Locale#US}.
   *
   * @param locale locale of the text
   * @return a copy of this instance that will use the specified locale
   */
  public Tokens withLocale(Locale locale) {
    return withLocaleInput(new Constant<>(Locale.US));
  }

  /**
   * Sets the input that will provide the text to be tokenized.
   *
   * @param textInput the input that will provide the text
   * @return a copy of this instance that will use the specified input
   */
  public Tokens withTextInput(Producer<? extends CharSequence> textInput) {
    return clone(c -> c._input2 = textInput);
  }

  @Override
  protected ConcurrentHashMap<Locale, Function1<CharSequence, List<String>>> createExecutionCache(
      long exampleCountGuess) {
    return new ConcurrentHashMap<>(); // used to cache tokenizers
  }

  @Override
  protected List<String> apply(ConcurrentHashMap<Locale, Function1<CharSequence, List<String>>> executionCache,
      Locale locale, CharSequence text) {
    return executionCache.computeIfAbsent(locale, l -> new JFlexTokenizer(l)::tokenize).apply(text);
  }
}
