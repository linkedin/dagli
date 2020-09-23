package com.linkedin.dagli.text.jflex;

import java.io.IOException;


/**
 * This is the interface that all JFlex-generated lexers implement.
 *
 * @author Juan Bottaro
 * @author Yannick Versley
 * @author Jorge Handl
 */
public interface JFlexLexerInterface {

  /**
   * Scans one more token and returns its type.
   *
   * @return the type of the token, or {@code TokenType.END_OF_FILE} at the end.
   */
  TokenInfo yylex() throws IOException;

  /**
   * Gives the starting position of the last token.
   *
   * @return the index (wrt. the current {@code CharSequence} of the starting position
   */
  int yychar();

  /**
   * Returns the length of the last token.
   *
   * @return the length of the last match
   */
  int yylength();

}
