<#import "../common.ftl" as c />

<#macro PreparedOrPreparableTransformer arity isPrepared preparedType>
  <@compress single_line=true>
    <#if isPrepared><@c.PreparedTransformer arity /><#else><@c.PreparableTransformer arity preparedType /></#if>
  </@compress>
</#macro>

<#macro WithArityMethods arity isPrepared>
  <#if arity < c.maxArity>
    <#assign largerArityPreparedTypeBound>? extends <@c.PreparedTransformer (arity + 1) /></#assign>
    @Override
    default <${c.InputGenericArgument(arity + 1)}>  <@PreparedOrPreparableTransformer (arity + 1) isPrepared largerArityPreparedTypeBound /> withArity${arity + 1}(Producer<? extends ${c.InputGenericArgument(arity + 1)}> newInput${c.InputSuffix(arity + 1)}) {
    <#list 1..arity as index>
      Placeholder<${c.InputGenericArgument(index)}> nestedPlaceholder${c.InputSuffix(index)} = new Placeholder<>("Original Input ${c.InputSuffix(index)}");
    </#list>
      return DAG<#if isPrepared>.Prepared</#if>
          .withPlaceholders(<#list 1..arity as index>nestedPlaceholder${c.InputSuffix(index)}<#sep>, </#list>, new Placeholder<${c.InputGenericArgument(arity + 1)}>("Ignored"))
          .withOutput(withInputs(<#list 1..arity as index>nestedPlaceholder${c.InputSuffix(index)}<#sep>, </#list>))
          .withInputs(<@c.GetInputList arity />, newInput${c.InputSuffix(arity + 1)});
    }

    @Override
    default <N> <#if isPrepared>Prepared<#else>Preparable</#if>Transformer${arity + 1}<N, <@c.InputGenericArguments arity />, R<#if !isPrepared>, ? extends PreparedTransformer${arity + 1}<N, <@c.InputGenericArguments arity />, R></#if>> withPrependedArity${arity + 1}(Producer<? extends N> newInput${c.InputSuffix(1)}) {
    <#list 1..arity as index>
      Placeholder<${c.InputGenericArgument(index)}> nestedPlaceholder${c.InputSuffix(index)} = new Placeholder<>("Original Input ${c.InputSuffix(index)}");
    </#list>
      return DAG<#if isPrepared>.Prepared</#if>
          .withPlaceholders(new Placeholder<N>("Ignored"), <#list 1..arity as index>nestedPlaceholder${c.InputSuffix(index)}<#sep>, </#list>)
          .withOutput(this.withInputs(<#list 1..arity as index>nestedPlaceholder${c.InputSuffix(index)}<#sep>, </#list>))
          .withInputs(newInput${c.InputSuffix(1)}, <@c.GetInputList arity />);
    }
  </#if>
  <#if (arity > 1)>
    @Override
    default <#if isPrepared>Prepared<#else>Preparable</#if>Transformer1<<@c.ValueTupleType arity />, R<#if !isPrepared>, ? extends PreparedTransformer1<<@c.ValueTupleType arity />, R></#if>> withArity1(
      Producer<? extends <@c.ValueTupleType arity />> inputTuple) {
      Placeholder<<@c.ValueTupleType arity />> placeholderTuple = new Placeholder<>("Original Inputs Tuple");

      return DAG<#if isPrepared>.Prepared</#if>
        .withPlaceholder(placeholderTuple)
        .withOutput(withInputs(<#list 0..arity-1 as index>new Value${index}FromTuple<>(placeholderTuple)<#sep>, </#list>))
        .withInput(inputTuple);
    }
  </#if>
</#macro>