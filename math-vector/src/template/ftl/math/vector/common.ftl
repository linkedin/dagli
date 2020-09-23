<#assign FloatIndex = 0 />
<#assign DoubleIndex = 1 />

<#assign primitiveTypes = ["float", "double"] />
<#assign boxedTypes = ["Float", "Double"] />
<#assign typeNames = ["Float", "Double"] />

<#function bufferType typeIndex><#return boxedTypes[typeIndex] + "Buffer" /></#function>

<#macro DenseVector typeIndex>Dense${typeNames[typeIndex]}ArrayVector</#macro>
<#macro DenseBufferVector typeIndex>Dense${typeNames[typeIndex]}BufferVector</#macro>
<#macro SparseArrayVector typeIndex>Sparse${typeNames[typeIndex]}ArrayVector</#macro>
<#macro SparseMapVector typeIndex>Sparse${typeNames[typeIndex]}MapVector</#macro>

<#macro CastDoubleToType typeIndex><#if typeIndex != DoubleIndex>(${primitiveTypes[typeIndex]}) </#if><#nested></#macro>
<#macro CastDoubleArrayToType typeIndex><#if typeIndex != DoubleIndex>ArraysEx.to${boxedTypes[typeIndex]}sLossy(<#nested>)<#else><#nested></#if></#macro>