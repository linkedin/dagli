<#-- utilities that may be useful in the future (or were useful in the past), but not currently used -->

<#function Concatenate l>
  <#local res = []>
  <#list 0..(l?size - 1) as index>
    <#local res += l[index]>
  </#list>
  <#return res>
</#function>

<#function MaterializeSequence seq depth=2>
  <#local res = pp.newWritableSequence() />
  <#list seq as it>
    <#if it?is_sequence && (depth > 0)>
      <@pp.add seq=res value=MaterializeSequence(it, depth-1) />
    <#else>
      <@pp.add seq=res value=it />
    </#if>
  </#list>
  <#return res />
</#function>

<#-- Gets a sequence of sequences that are all possible arities of roots in a DAG -->
<#-- e.g. [1], [2], [1, 1], [2, 1], [4, 4, 2], [-1, 2, 3] etc. -->
<#-- (-1 is used to denote where a Placeholder should be used) -->
<#function GetArgumentAritiesList maxArity maxLength>
  <#local cache = [] />
  <#list 1..maxArity as index>
    <#local cache += [GetArgumentAritiesListWithCache(index, cache, maxLength)] />
  </#list>
  <#return Concatenate(cache)>
</#function>

<#function GetArgumentAritiesListWithCache maxArity cache maxLength>
  <#local res = [] />
  <#if (maxArity > 1)>
    <#list ([-1] + (1..(maxArity-1))) as argArity>
      <#list cache[maxArity - argArity?abs - 1] as remainingArity>
        <#if remainingArity?size < maxLength>
          <#local res += [[argArity] + remainingArity] />
        </#if>
      </#list>
    </#list>
  <#else>
    <#local res += [[-1]] />
  </#if>
  <#local res += [[maxArity]] />
  <#return res />
</#function>

<#function max val1 val2>
  <#if val1 < val2>
    <#return val2>
  </#if>
  <#return val1>
</#function>

<#function SumSequence seq>
  <#local res = 0 />
  <#list seq as val>
    <#local res += val />
  </#list>
  <#return res />
</#function>

<#macro RootsParameters isPrepared arities>
  <#local sum = 0>
  <@compress single_line=true>
    <#list arities as arity>
      <#if arity = 0>
      Placeholder<${c.InputGenericArgument(sum)}> root${arity?index}
      <#else>
        <#if isPrepared>Prepared<#else>Preparable</#if>Transformer${arity}<<#list 0..arity-1 as index>${c.InputGenericArgument(index + sum)}<#sep>, </#list>, ?>
      </#if>
      <#local sum += max(arity, 1)>
      <#sep>, </#list>
  </@compress>
</#macro>

<#macro WithRootsMethod isPrepared arities placeholderArity>
public static <<@c.InputGenericArguments placeholderArity />> PartialDAG.<#if isPrepared>Prepared.</#if><@WithPlaceholders placeholderArity /> withRoot<@c.s arities?size />() {
</#macro>

<#macro RootSubgraphMethods isPrepared>
  <#list GetArgumentAritiesList(c.maxArity) as arities>
    <@WithRootsMethod isPrepared arities SumSequence(arities) />
  </#list>
</#macro>