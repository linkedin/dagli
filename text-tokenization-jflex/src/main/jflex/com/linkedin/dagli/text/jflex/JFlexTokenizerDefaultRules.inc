// some common rules that should always apply

{WORD}                          { return TokenInfo.WORD; }
{PNAME}                         { return TokenInfo.WORD; }
{ONAME}                         { return TokenInfo.WORD; }
{DOT_NAME}                      { return TokenInfo.WORD; }
{CPP}                           { return TokenInfo.WORD; }
{E_WORD}                        { return TokenInfo.WORD; }
{FILENAME}                      { return TokenInfo.WORD; }
{DATE}                          { return TokenInfo.WORD; }
{TIME}                          { return TokenInfo.WORD; }
{PHONE}                         { return TokenInfo.WORD; }
{EMAIL}                         { return TokenInfo.WORD; }
{URL}                           { return TokenInfo.WORD; }
{TWTR_HANDLE}                   { return TokenInfo.WORD; }
{TWTR_HASHTAG}                  { return TokenInfo.WORD; }
{AT_ADDR}                       { return TokenInfo.WORD; }
{NUM}                           { return TokenInfo.NUMBER; }
{NUM}\./{NEXT_IS_LOWER}         { return TokenInfo.ORDINAL; }
{CAPITAL}\./{NEXT_IS_UPPER}     { return TokenInfo.ABBREV; }
{ABBREV}/{NEXT_IS_LOWER}        { return TokenInfo.ABBREV; }
{ABBREV_NO_END_PUNCT}           { return TokenInfo.ABBREV; }
{ABBREV}                        { return TokenInfo.ABBREV; }

// fairly general: sequences of numbers separated by dots or commas
{NUM}([,\.]{NUM})+              { return TokenInfo.NUMBER; }

// exclamation marks and general punctuation
\. | [?!]+                      { return TokenInfo.PUNCT_END_SENTENCE; }
{DBL_SYM}                       { return TokenInfo.PUNCT; }
{HOR_LINE}                      { return TokenInfo.PUNCT; }
{EMOTICON}                      { return TokenInfo.PUNCT; }

// apostrophe by itself
{APOSTROPHE}                    { return TokenInfo.APOS; }

// sentence splitting for lists
{SPACES_WITH_NEWLINE}/{BULLET}  { return TokenInfo.NOT_A_TOKEN_END_SENTENCE; }
{DOUBLE_NEWLINE}/\s*{CAPITAL}   { return TokenInfo.NOT_A_TOKEN_END_SENTENCE; }

// spaces are ignored, any non-space non-letter token is treated as punctuation
\s+                             { /* ignore */ }
\S                              { return TokenInfo.PUNCT; }
<<EOF>>                         { return TokenInfo.END_OF_FILE; }

// Error fallback, any tokens not considered will throw exception
// TODO (jbottaro): make sure this never happens, as tokenization shouldn't fail at any point
[^]                             { throw new Error("Illegal character <" + yytext() + ">"); }
