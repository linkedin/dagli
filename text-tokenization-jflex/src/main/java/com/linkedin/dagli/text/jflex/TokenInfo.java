package com.linkedin.dagli.text.jflex;

/**
 * Information returned by a {@code JFlex} rule-based tokenizer.
 *
 * @author Juan Bottaro
 * @author Yannick Versley
 * @author Jorge Handl
 */
enum TokenInfo {

  NOT_A_TOKEN(TokenType.NOT_A_TOKEN, false),
  WORD(TokenType.WORD, false),
  ABBREV(TokenType.ABBREV, false),
  NUMBER(TokenType.NUMBER, false),
  ORDINAL(TokenType.ORDINAL, false),
  PUNCT(TokenType.PUNCT, false),
  APOS(TokenType.APOS, false),
  END_OF_FILE(TokenType.END_OF_FILE, false),

  NOT_A_TOKEN_END_SENTENCE(TokenType.NOT_A_TOKEN, true),
  ABBREV_END_SENTENCE(TokenType.ABBREV, true),
  PUNCT_END_SENTENCE(TokenType.PUNCT, true);

  private final TokenType _type;
  private final boolean _sentenceEnd;

  /**
   * Information about a matched span in a piece of text.
   *
   * @param type the type of the token matched (or {@link TokenType#NOT_A_TOKEN} if it's not a valid token)
   * @param sentenceEnd whether the current sentence should end in this token
   */
  TokenInfo(TokenType type, boolean sentenceEnd) {
    _type = type;
    _sentenceEnd = sentenceEnd;
  }

  public TokenType getType() {
    return _type;
  }

  public boolean isSentenceEnd() {
    return _sentenceEnd;
  }

}
