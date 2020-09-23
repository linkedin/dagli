package com.linkedin.dagli.text.jflex;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * JFlex implementation of a tokenizer.
 *
 * @author Juan Bottaro
 * @author Yannick Versley
 * @author Jorge Handl
 */
public class JFlexTokenizer {
  private final Function<Reader, JFlexLexerInterface> _lexerFactory;

  /**
   * Creates a rule-based tokenizer.
   *
   * @param locale the Locale defining the language for the tokenization
   */
  public JFlexTokenizer(Locale locale) {
    if (sameLanguage(locale, Locale.ENGLISH)) {
      _lexerFactory = JFlexEnglishLexer::new;
    } else if (sameLanguage(locale, Locale.GERMAN)) {
      _lexerFactory = JFlexGermanLexer::new;
    } else {
      _lexerFactory = JFlexGenericLexer::new;
    }
  }

  /**
   * Checks if two locales have the same language.
   *
   * @param locale1 the first locale
   * @param locale2 the second locale
   * @return true if the locales have the same language as determined by {@link Locale#getLanguage()}, false otherwise.
   */
  private static boolean sameLanguage(Locale locale1, Locale locale2) {
    return locale1.getLanguage().equals(locale2.getLanguage());
  }

  /**
   * Tokenizes text, producing a sequence of String tokens.
   *
   * @param text the text to tokenize
   * @return a list of tokens
   */
  public List<String> tokenize(CharSequence text) {
    return splitAndTokenize(text).stream()
        .flatMap(Collection::stream)
        .collect(Collectors.toList());
  }

  /**
   * Tokenizes text, return a list of lists of String tokens.  Each (inner) list of String tokens corresponds to a
   * sentence in the text.
   *
   * @param text the text to tokenize
   * @return a list of lists of String tokens
   */
  private List<List<String>> splitAndTokenize(CharSequence text) {
    String originalText = text.toString();
    JFlexLexerInterface tokenizer = _lexerFactory.apply(new StringReader(originalText));

    List<String> tokens = new ArrayList<>();
    List<List<String>> sentences = new ArrayList<>();
    try {
      TokenInfo info = tokenizer.yylex();
      while (info.getType() != TokenType.END_OF_FILE) {

        if (info.getType() != TokenType.NOT_A_TOKEN) {
          int startOffset = tokenizer.yychar();
          int endOffset = startOffset + tokenizer.yylength();
          tokens.add(originalText.substring(startOffset, endOffset));
        }

        if (info.isSentenceEnd() && !tokens.isEmpty()) {
          sentences.add(tokens);
          tokens = new ArrayList<>();
        }

        info = tokenizer.yylex();
      }
    } catch (IOException e) {
      // This should never happen, as the reader is a string
      throw new RuntimeException("Error while trying to process StringReader", e);
    }

    if (!tokens.isEmpty()) {
      sentences.add(tokens);
    }

    return sentences;
  }
}
