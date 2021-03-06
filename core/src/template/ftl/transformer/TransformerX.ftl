<#import "../common.ftl" as c />
<@c.AutoGeneratedWarning />
package com.linkedin.dagli.transformer;

import com.linkedin.dagli.transformer.internal.Transformer${arity}InternalAPI;

<#assign subclass>? extends <@c.Transformer arity /></#assign>
public interface <@c.Transformer arity /> extends Transformer<R>, TransformerWithInputBound<<#if arity == 1>A<#else>Object</#if>, R> {

  @Override
  <@c.TransformerInternalAPI arity subclass /> internalAPI();
}
