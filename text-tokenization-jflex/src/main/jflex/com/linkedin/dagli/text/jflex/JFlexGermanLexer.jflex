package com.linkedin.dagli.text.jflex;

/**
 * Token lexer with German-specific rules, yielding typed tokens that can
 * then be transformed using postprocessing rules.
 *
 * @author Juan Bottaro
 * @author Yannick Versley
 * @author Jorge Handl
 */
%%

%class JFlexGermanLexer

abbrev = Hr|Fr|Dr|St|Krh|Tel|ca|vgl|bzw|etc
monat = Januar|Februar|März|April|May|Juni|Juli|August|September|October|November|December

%include JFlexTokenizerMacros.inc
%include GermanAbbrev.inc

%%

{abbrev}\.              { return TokenInfo.ABBREV; }
{corpus_abbrev}\.       { return TokenInfo.ABBREV; }
{NUM}\./\s{monat}       { return TokenInfo.ORDINAL; }
{NUM}(te|ste|en|er|es)  { return TokenInfo.ORDINAL; }
{NUM}jährige[rsn]?      { return TokenInfo.WORD; }

// Contractions
{APOSTROPHE}s/\W  { return TokenInfo.WORD; }

%include JFlexTokenizerDefaultRules.inc
