package com.linkedin.dagli.text.jflex;

/**
 * In the rule-based tokenizer, tokens have a type that
 * can serve to identify rules that should be used for
 * postprocessing.
 *
 * @author Juan Bottaro
 * @author Yannick Versley
 * @author Jorge Handl
 */
enum TokenType {
  NOT_A_TOKEN, // instances that should not be considered a token (e.g. whitespaces signaling end of sentence)
  WORD,        // "normal" words, e.g. John, R2D2, goin'
  ABBREV,      // abbreviations, can be sentence-ending
  NUMBER,      // numbers
  ORDINAL,     // ordinal numbers, can be sentence-ending
  PUNCT,       // normal punctuation (commas, quotes, parentheses)
  APOS,        // apostrophe
  END_OF_FILE  // should only occur on EOF
}
