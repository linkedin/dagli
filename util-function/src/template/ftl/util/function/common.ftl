<#-- common methods for functions -->

<#assign GenericFunctionIndex = 0 />
<#assign VoidFunctionIndex = 1 />
<#assign BooleanFunctionIndex = 2 />

<#-- arities above this will be non-public and lack certain methods -->
<#assign MaxPublicArity = 10 />

<#-- Empty string means "default" -->
<#-- Format is: Object return type, primitive return type, Java functional interface to extend for interface 0, -->
<#-- Java function interface for arity 0 with type arguments, Java function interface for arity 0's @FI method, -->
<#-- the next six entries are the same as previous three, but for arities 1 and 2 -->
<#assign typeTuples = [
  ["", "", "", "", "", "", "", "", "", "", ""],
  ["Void", "void", "Runnable", "Runnable", "run", "Consumer<A>", "Consumer", "accept", "BiConsumer<A, B>", "BiConsumer", "accept"],
  ["Boolean", "boolean", "BooleanSupplier", "BooleanSupplier", "getAsBoolean" , "Predicate<A>", "Predicate", "test", "BiPredicate<A, B>", "BiPredicate", "test"],
  ["Byte", "byte", "", "", "", "", "", "", "", "", ""],
  ["Character", "char", "", "", "", "", "", "", "", "", ""],
  ["Short", "short", "", "", "", "", "", "", "", "", ""],
  ["Int", "int", "IntSupplier", "IntSupplier", "getAsInt", "ToIntFunction<A>", "ToIntFunction", "applyAsInt", "ToIntBiFunction<A, B>", "ToIntBiFunction", "applyAsInt"],
  ["Long", "long", "LongSupplier", "LongSupplier", "getAsLong", "ToLongFunction<A>", "ToLongFunction", "applyAsLong", "ToLongBiFunction<A, B>", "ToLongBiFunction", "applyAsLong"],
  ["Float", "float", "", "", "", "", "", "", "", "", ""],
  ["Double", "double", "DoubleSupplier", "DoubleSupplier", "getAsDouble", "ToDoubleFunction<A>", "ToDoubleFunction", "applyAsDouble", "ToDoubleBiFunction<A, B>", "ToDoubleBiFunction", "applyAsDouble"]
] />

<#assign defaultValues = ["null", "null", "false", "0", "'\\0'", "0", "0", "0", "0", "0"] />
<#assign defaultValueNames = ["Null", "Null", "False", "Zero", "Zero", "Zero", "Zero", "Zero", "Zero", "Zero"] />

<#function DefaultIfEmpty val def><#if val != ""><#return val><#else><#return def></#if></#function>

<#function Prefix index><#return typeTuples[index][0] /></#function>
<#function ReturnedClassName index><#return DefaultIfEmpty(typeTuples[index][0], "R") /></#function>
<#function ReturnedPrimitiveName index><#return DefaultIfEmpty(typeTuples[index][1], "R") /></#function>
<#function JavaInterface0 index><#return DefaultIfEmpty(typeTuples[index][2], "Supplier<${ReturnedClassName(index)}>") /></#function>
<#function JavaInterface0Dependency index><#return DefaultIfEmpty(typeTuples[index][3], "Supplier") /></#function>
<#function JavaInterface0Method index><#return DefaultIfEmpty(typeTuples[index][4], "get") /></#function>
<#function JavaInterface0IsGeneric index><#return typeTuples[index][4] == "" /></#function>
<#function JavaInterface1 index><#return DefaultIfEmpty(typeTuples[index][5], "Function<A, ${ReturnedClassName(index)}>") /></#function>
<#function JavaInterface1Dependency index><#return DefaultIfEmpty(typeTuples[index][6], "Function") /></#function>
<#function JavaInterface1Method index><#return DefaultIfEmpty(typeTuples[index][7], "apply") /></#function>
<#function JavaInterface1IsGeneric index><#return typeTuples[index][7] == "" /></#function>
<#function JavaInterface2 index><#return DefaultIfEmpty(typeTuples[index][8], "BiFunction<A, B, ${ReturnedClassName(index)}>") /></#function>
<#function JavaInterface2Dependency index><#return DefaultIfEmpty(typeTuples[index][9], "BiFunction") /></#function>
<#function JavaInterface2Method index><#return DefaultIfEmpty(typeTuples[index][10], "apply") /></#function>
<#function JavaInterface2IsGeneric index><#return typeTuples[index][10] == "" /></#function>

<#macro GenericArgs typeIndex arity suffix="" resultType="R">
  <@compress single_line=true>
    <#if (typeIndex > 0 && arity > 0)><<@c.InputGenericArguments arity /><#if suffix != "">, ${suffix}</#if>>
    <#elseif (typeIndex == 0 && arity == 0)><${resultType}<#if suffix != "">, ${suffix}</#if>>
    <#elseif (typeIndex == 0 && arity > 0)><<@c.InputGenericArguments arity />, ${resultType}<#if suffix != "">, ${suffix}</#if>>
    <#elseif suffix != "">
    <${suffix}>
    <#else>
    </#if>
  </@compress>
</#macro>

<#macro JavaInterface typeIndex arity>
  <@compress single_line=true>
  <#if (arity == 0)>
    , ${JavaInterface0(typeIndex)}
  <#elseif (arity == 1)>
    , ${JavaInterface1(typeIndex)}
  <#elseif (arity == 2)>
    , ${JavaInterface2(typeIndex)}
  <#else>
  </#if>
  </@compress>
</#macro>

<#macro JavaInterfaceMethod typeIndex arity>
  <@compress single_line=true>
    <#if (arity == 0)>
    ${JavaInterface0Method(typeIndex)}
    <#elseif (arity == 1)>
    ${JavaInterface1Method(typeIndex)}
    <#elseif (arity == 2)>
    ${JavaInterface2Method(typeIndex)}
    <#else>
    </#if>
  </@compress>
</#macro>

<#function JavaInterfaceIsGeneric typeIndex arity>
  <@compress single_line=true>
    <#if (arity == 0)>
     <#return JavaInterface0IsGeneric(typeIndex)>
    <#elseif (arity == 1)>
      <#return JavaInterface1IsGeneric(typeIndex)>
    <#elseif (arity == 2)>
      <#return JavaInterface2IsGeneric(typeIndex)>
    <#else>
      <#return false>
    </#if>
  </@compress>
</#function>



<#macro JavaInterfaceImport typeIndex arity>
  <@compress single_line=true>
    <#if (arity == 0 && typeIndex != VoidFunctionIndex)>
    import java.util.function.${JavaInterface0Dependency(typeIndex)};
    <#elseif (arity == 1)>
    import java.util.function.${JavaInterface1Dependency(typeIndex)};
    <#elseif (arity == 2)>
    import java.util.function.${JavaInterface2Dependency(typeIndex)};
    <#else>
    </#if>
  </@compress>
</#macro>