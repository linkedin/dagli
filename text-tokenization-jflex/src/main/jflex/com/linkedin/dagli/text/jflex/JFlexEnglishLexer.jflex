package com.linkedin.dagli.text.jflex;

/**
 * Token lexer with English-specific rules, yielding typed tokens that can
 * then be transformed using postprocessing rules.
 *
 * @author Juan Bottaro
 * @author Yannick Versley
 * @author Jorge Handl
 */
%%

%class JFlexEnglishLexer

prefix = ([Mm]is|[Aa]nti|[Cc]ounter|[Oo]ver|[Uu]nder|[Nn]on|[Cc]o|[Mm]eta|[Mm]ulti|[Pp]re|[Oo]ff|[Ss]ub)

%include JFlexTokenizerMacros.inc
%include EnglishAbbrev.inc

%%

// The following rule should be included in every language-specific jflex file
// except for languages where capitalized words inside sentences are common, e.g. German.
// NOTE: Disabled as it breaks examples like "This is the U.S. Department of Security."
//{ABBREV}/{NEXT_IS_UPPER}              { return TokenInfo.ABBREV_END_SENTENCE; }

{abbrev}\.                            { return TokenInfo.ABBREV; }

// Contractions
e-{WORD}                              { return TokenInfo.WORD; }
{APOSTROPHE}(m|ve|d|s|ll|re|tis|twas) { return TokenInfo.WORD; }
{WORD}/n{APOSTROPHE}t                 { return TokenInfo.WORD; }
n{APOSTROPHE}t                        { return TokenInfo.WORD; }

// Hyphenated words
{prefix}-{WORD}                       { return TokenInfo.WORD; }

// Ordinals
{NUM}(st|nd|rd|th)                    { return TokenInfo.ORDINAL; }


%include JFlexTokenizerDefaultRules.inc
