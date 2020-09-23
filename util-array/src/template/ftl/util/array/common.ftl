<#assign objectGroup = 0 />
<#assign booleanGroup = 1 />
<#assign integerGroup = 2 />
<#assign floatGroup = 3 />

<#-- Compendium of primitive types
     Format is: Boxed type name, primitive type name, primitive "group" index (as defined above), default value,
     width in bits (not necessarily related to actual RAM consumption; determines if conversion widening/narrowing)
     All types within a "group" must be listed in order of increasing width -->
<#assign typeTuples = [
["Object", "Object", objectGroup, "null", 64], <#-- width depends on architecture, but not used regardless -->
["Boolean", "boolean", booleanGroup, "false", 1],
["Byte", "byte", integerGroup, "0", 8],
["Character", "char", integerGroup, "'\\0'", 16],
["Short", "short", integerGroup, "0", 16],
["Integer", "int", integerGroup, "0", 32],
["Long", "long", integerGroup, "0", 64],
["Float", "float", floatGroup, "0", 32],
["Double", "double", floatGroup, "0", 64]
] />

<#assign maxType = typeTuples?size - 1 />

<#-- Gets the column of a "table" (an array of array "rows") -->
<#function tableColumn table columnIndex>
  <#assign result = [] />
  <#list 0..(table?size-1) as rowIndex>
    <#assign result += [table[rowIndex][columnIndex]] />
  </#list>
  <#return result />
</#function>

<#assign boxedTypes = tableColumn(typeTuples, 0) />

<#function typeFromBoxedName boxedName><#return boxedTypes?seq_index_of(boxedName) /></#function>

<#assign objectType = typeFromBoxedName("Object") />
<#assign shortType = typeFromBoxedName("Short") />
<#assign characterType = typeFromBoxedName("Character") />

<#function boxedType typeIndex><#return typeTuples[typeIndex][0] /></#function>
<#function primitiveType typeIndex><#return typeTuples[typeIndex][1] /></#function>
<#function typeGroup typeIndex><#return typeTuples[typeIndex][2] /></#function>
<#function typeDefaultValue typeIndex><#return typeTuples[typeIndex][3] /></#function>
<#function typeWidth typeIndex><#return typeTuples[typeIndex][4] /></#function>

<#-- Gets the "composite type name" for the type as it is used is composite names, like IntList -->
<#-- This is mostly the same as the boxed type name, with exceptions -->
<#function compositeTypeName typeIndex>
    <#if boxedType(typeIndex) == "Integer"><#return "Int" />
    <#elseif boxedType(typeIndex) == "Character"><#return "Char" />
    <#else><#return boxedType(typeIndex) /></#if>
</#function>

<#-- Want to check that value can fit within float type's mantissa; we use float bit width / 2 because it's a
     convenient underestimate of mantissa size and being exact wouldn't change the result -->
<#function canLosslesslyConvertToFloat from toFloatType><#return typeWidth(from) <= (typeWidth(toFloatType)/2) /></#function>

<#function typeIsNumeric typeIndex><#return typeGroup(typeIndex) == integerGroup || typeGroup(typeIndex) == floatGroup /></#function>
<#function typeIsNumber typeIndex><#return typeIsNumeric(typeIndex) && primitiveType(typeIndex) != "char" /></#function>

<#function canConvert from to><#return from == to || (typeIsNumeric(from) && typeIsNumeric(to)) /></#function>
<#function canLosslesslyConvert from to><#return (typeGroup(from) == typeGroup(to) && typeWidth(from) <= typeWidth(to)) ||
  (typeGroup(to) == floatGroup && canLosslesslyConvertToFloat(from to)) /></#function>

<#-- Any conversion to char (that is not a no-op conversion from char to char) will either be narrowing or,
     in the bizarre case of byte -> char, widening (to int) and then narrowing, per Java spec.  We also check if
     the types have the same width to catch the case of short -> char, which is also considered "narrowing" -->
<#function needsExplicitCast from to>
  <#return !canLosslesslyConvert(from to) || (from != to && (typeWidth(from) == typeWidth(to) || to == characterType)) />
</#function>