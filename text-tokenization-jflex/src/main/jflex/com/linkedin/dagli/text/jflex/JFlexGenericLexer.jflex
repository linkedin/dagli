package com.linkedin.dagli.text.jflex;

/**
 * Token lexer with rules targeting a broad range of languages, yielding typed tokens that can
 * then be transformed using postprocessing rules.
 *
 * @author Juan Bottaro
 * @author Yannick Versley
 * @author Jorge Handl
 */
%%

%class JFlexGenericLexer

%include JFlexTokenizerMacros.inc

%%

%include JFlexTokenizerDefaultRules.inc
