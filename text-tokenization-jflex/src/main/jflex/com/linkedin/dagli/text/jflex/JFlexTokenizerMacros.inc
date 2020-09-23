%type TokenInfo
%implements JFlexLexerInterface
%caseless
%unicode
%char

%{

  /**
   * Returns the start offset of the matched text region.
   *
   * @return the start offset of the matched text region
   */
  public int yychar() {
    return yychar;
  }

%}

ALPHANUM = ([:letter:]|[:digit:])
WORD = {ALPHANUM}+
PNAME = [:uppercase:]{ALPHANUM}*([\&@]{ALPHANUM}+)*
INITIAL = [:letter:]
CAPITAL = [:uppercase:]
ABBREV = ({INITIAL}\.)+
ABBREV_NO_END_PUNCT = {ABBREV}{INITIAL}
NUM = [:digit:]+
APOSTROPHE = ['’`´\u0091\u2018\u201B]
DASH = [-\u05BE\u2010\u2011\u2012\u2013\u2014\u2015\uFE63\uFF0D]
ONAME = [Oo]{APOSTROPHE}{ALPHANUM}+

DATE_YYYYMMDD = \d{4}-\d{2}-\d{2}
DATE_MMDDYYYY = \d{1,2}\/\d{1,2}\/\d{4}
DATE_MMDDYY = \d{1,2}\/\d{1,2}\/\d{2}
DATE_MMYY = \d{1,2}\/\d{2}
DATE = ({DATE_YYYYMMDD}|{DATE_MMDDYYYY}|{DATE_MMDDYY}|{DATE_MMYY})

TIME_HHMM = \d{1,2}:\d{2}
TIME_HHMMSS = \d{1,2}:\d{1,2}:\d{2}
TIME = ({TIME_HHMM}|{TIME_HHMMSS})

PHONE_COUNTRY = "+"? \d{1,3} [\-\s]?
PHONE_AREA = "(" \d{2,4} ")" [\-\s]?
PHONE_SEGMENT = \d{2,5}
PHONE = {PHONE_COUNTRY}? {PHONE_AREA}? {PHONE_SEGMENT} ([\-\s] {PHONE_SEGMENT})+

DOT_NAME = "." {WORD}
CPP = [Cc] "++"
E_WORD = [Ee] "-" {WORD}
FILENAME = [\w\_]+\.\w{2,5}

// the context that suggests a dot is not part of an abbreviation
NEXT_IS_LOWER = (\s+[:lowercase:])
// the context that suggests a dot after an abbreviation is the end of a sentence
NEXT_IS_UPPER = (\s+[:uppercase:])

// Horizontal lines of symbols
HOR_ELEM = [=\+_\*\~\$\#\@\/] | {DASH}
HOR_LINE = {HOR_ELEM} {HOR_ELEM} {HOR_ELEM}+ | \.\.\.+
DBL_SYM = ".." | "--" | "++" | "$$" | "<<" | ">>" | "==" | {APOSTROPHE}{2}

// Emoticons
BROW = ">" | "<" | "|" | "}" | "{"
EYES = ":" | ";" | "=" | "8" | "B"
NOSE = "o" | "-" | "^"
MOUTH = ")" | "D" | "]" | "}" | ">" | "(" | "[" | "{" | "<" | "p" | "P" | "d" | "O" | "3" | "/" | "|" | "*" | "$" | "@"
CHIN = "}" | ">" | "]"
EMOTICON = {BROW}? {EYES} {NOSE}? {MOUTH} {CHIN}?

// URL and email matchers (taken from Lucene 6.0.0)
%include JFlexUrlEmail.inc

// Internet references (twitter, etc)
TWTR_HANDLE = \@\w+
TWTR_HASHTAG = \#\w+
AT_ADDR = \@{DOMAIN}

// Sentence splitting for lists
BULLET = [:digit:]*[\*\-\u2022\u25CF\u26AB\u2B24\u2219\u22C5]
SPACES_WITH_NEWLINE = \s*\n\s*
DOUBLE_NEWLINE = \s*\n\n+
