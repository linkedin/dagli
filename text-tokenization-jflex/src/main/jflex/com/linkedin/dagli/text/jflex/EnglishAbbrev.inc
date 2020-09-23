
abbrev_corpus = (acc|Adm|Agr|Assn|awt|Cir|defs|Dist|dlvr|Eds|elit|equiv|Esq|Est|ibid|Mh|Mich|Minn|Ont|Ow|Pg|Supp|tek|usc|Util|Vt|Wisc|Wyo)

abbrev_legal = ([Ii]nc|[Ee]xt|[Cc]orp|[Ll][Tt][Dd]|[Ll][Ll][Cc])
abbrev_honorifics = ([Mm]r|[Mm]rs|[Mm]s|[Dd]r|[Ss]r|[Jj]r|[Rr]ep|[Ss]en|[Ss]t|[Pp]h\.[Dd])
abbrev_months = (Jan|Feb|Mar|Apr|Jun|Jul|Aug|Sep|Sept|Oct|Nov|Dec)
abbrev_others = ([Ee]tc)

abbrev = ({abbrev_corpus}|{abbrev_legal}|{abbrev_honorifics}|{abbrev_months}|{abbrev_others})