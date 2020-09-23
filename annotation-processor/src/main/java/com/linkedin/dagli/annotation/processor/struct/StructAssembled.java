package com.linkedin.dagli.annotation.processor.struct;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.annotation.processor.ProcessorConstants;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import com.squareup.javapoet.WildcardTypeName;
import java.io.ObjectStreamException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeParameterElement;


/**
 * Internal, static utility class that supports @Struct annotation processing,
 * specifically generating the inner Assembled Dagli transformer class that combines
 * inputs and constructs a structure from them.
 */
class StructAssembled {
  private StructAssembled() { }

  private static ClassName assembledTypeName(String classPackage, String className) {
    return ClassName.get(classPackage, className, "Assembled");
  }

  private static ClassName assembledDefaultGeneratorTypeName(String classPackage, String className) {
    return assembledTypeName(classPackage, className).nestedClass(StructConstants.DEFAULT_GENERATOR_INTERFACE_NAME);
  }

  private static ClassName assembledBuilderSetterTypeName(String classPackage, String className, StructField field) {
    return StructUtil.helperTypeName(classPackage, className).nestedClass(field.getCoreName()).nestedClass(
        StructConstants.ASSEMBLED_BUILDER_CLASS_NAME);
  }

  private static ClassName completedAssembledBuilderTypeName(String classPackage, String className) {
    return StructUtil.helperTypeName(classPackage, className).nestedClass(
        StructConstants.COMPLETED_ASSEMBLED_BUILDER_INTERFACE_NAME);
  }

  private static TypeSpec getAssembledBuilderType(String classPackage, String className, List<StructField> fields,
      List<? extends TypeParameterElement> typeParameters) {
    List<StructField> mandatoryFields = fields.stream().filter(field -> !field._optional).collect(Collectors.toList());
    HashMap<StructField, StructField> nextItemMap = StructUtil.nextItemMap(mandatoryFields);

    TypeName typeQualifiedAssemblyTypeName =
        StructUtil.typeQualified(assembledTypeName(classPackage, className), typeParameters);

    return TypeSpec.classBuilder(StructConstants.BUILDER_CLASS_NAME)
        .addTypeVariables(typeParameters.stream().map(TypeVariableName::get).collect(Collectors.toList()))
        .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
        .addField(FieldSpec.builder(typeQualifiedAssemblyTypeName, "_instance", Modifier.PRIVATE)
            .initializer("new $T()", typeQualifiedAssemblyTypeName)
            .build())
        .addSuperinterface(
            StructUtil.typeQualified(completedAssembledBuilderTypeName(classPackage, className), typeParameters))
        .addSuperinterfaces(mandatoryFields.stream()
            .map(field -> StructUtil.typeQualified(assembledBuilderSetterTypeName(classPackage, className, field),
                typeParameters))
            .collect(Collectors.toList()))
        .addMethod(MethodSpec.methodBuilder("build")
            .addModifiers(Modifier.PUBLIC)
            .returns(typeQualifiedAssemblyTypeName)
            .addStatement("return _instance")
            .build())
        .addMethods(fields.stream()
            .map(field -> MethodSpec.methodBuilder("set" + field.getCoreName())
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addParameter(ParameterizedTypeName.get(StructConstants.PRODUCER_INTERFACE,
                    WildcardTypeName.subtypeOf(field.getBoxedTypeName())), field.getVariableName())
                .addStatement("_instance." + field.getInternalFieldName() + " = " + field.getVariableName())
                .addStatement("return this")
                .returns(StructUtil.typeQualified(
                    nextItemMap.getOrDefault(field, null) != null ? assembledBuilderSetterTypeName(classPackage,
                        className, nextItemMap.get(field)) : completedAssembledBuilderTypeName(classPackage, className),
                    typeParameters))
                .build())
            .collect(Collectors.toList()))
        .build();
  }

  static TypeSpec getAssembledBuilderSetterInterface(String classPackage, String className, StructField field,
      HashMap<StructField, StructField> nextFieldMap, List<? extends TypeParameterElement> typeParameters) {
    return TypeSpec.interfaceBuilder(StructConstants.ASSEMBLED_BUILDER_CLASS_NAME)
        .addModifiers(Modifier.PUBLIC)
        .addTypeVariables(typeParameters.stream().map(TypeVariableName::get).collect(Collectors.toList()))
        .addMethod(MethodSpec.methodBuilder("set" + field.getCoreName())
            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
            .addParameter(ParameterizedTypeName.get(StructConstants.PRODUCER_INTERFACE,
                WildcardTypeName.subtypeOf(field.getBoxedTypeName())), field.getVariableName())
            .returns(StructUtil.typeQualified(
                nextFieldMap.get(field) == null ? completedAssembledBuilderTypeName(classPackage, className)
                    : assembledBuilderSetterTypeName(classPackage, className, nextFieldMap.get(field)), typeParameters))
            .build())
        .build();
  }

  static TypeSpec getCompletedAssembledBuilderType(String classPackage, String className, List<StructField> fields,
      List<? extends TypeParameterElement> typeParameters) {
    return TypeSpec.interfaceBuilder(StructConstants.COMPLETED_ASSEMBLED_BUILDER_INTERFACE_NAME)
        .addModifiers(Modifier.PUBLIC)
        .addTypeVariables(typeParameters.stream().map(TypeVariableName::get).collect(Collectors.toList()))
        .addMethod(MethodSpec.methodBuilder("build")
            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
            .returns(StructUtil.typeQualified(assembledTypeName(classPackage, className), typeParameters))
            .build())
        .addMethods(fields.stream()
            .filter(field -> field._optional)
            .map(field -> MethodSpec.methodBuilder("set" + field.getCoreName())
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(ParameterizedTypeName.get(StructConstants.PRODUCER_INTERFACE,
                    WildcardTypeName.subtypeOf(field.getBoxedTypeName())), field.getVariableName())
                .returns(StructUtil.typeQualified(completedAssembledBuilderTypeName(classPackage, className),
                    typeParameters))
                .build())
            .collect(Collectors.toList()))
        .build();
  }

  private static MethodSpec getAssembledApplyUnsafe(String classPackage, String className, List<StructField> fields,
      List<? extends TypeParameterElement> typeParameters) {
    final ClassName structTypeName = ClassName.get(classPackage, className);
    final TypeName typeQualifiedStructName = StructUtil.typeQualified(structTypeName, typeParameters);

    MethodSpec.Builder spec = MethodSpec.methodBuilder("apply")
        .addModifiers(Modifier.PROTECTED)
        .addAnnotation(Override.class)
        .returns(typeQualifiedStructName)
        .addParameter(ParameterizedTypeName.get(ClassName.get(List.class), WildcardTypeName.subtypeOf(Object.class)),
            "values")
        .addStatement("$T res = new $T()", typeQualifiedStructName, typeQualifiedStructName);

    for (int i = 0; i < fields.size(); i++) {
      StructField field = fields.get(i);
      if (field._optional) {
        spec.beginControlFlow("if (!(_$L instanceof $T))", field.getVariableName(),
            assembledDefaultGeneratorTypeName(classPackage, className));
      }
      spec.addStatement("res.$L = ($T) values.get($L)", field.getFieldName(), field.getBoxedTypeName(), i);
      if (field._optional) {
        spec.endControlFlow();
      }
    }

    spec.addStatement("return res");
    return spec.build();
  }

  private static MethodSpec getAssembledWithInputsUnsafe(String classPackage, String className,
      List<StructField> fields, List<? extends TypeParameterElement> typeParameters) {
    final ParameterizedTypeName producerListType =
        ParameterizedTypeName.get(ClassName.get(List.class), WildcardTypeName.subtypeOf(StructConstants.PRODUCER_INTERFACE_WILDCARD));

    CodeBlock.Builder codeBuilder = CodeBlock.builder();
    for (int i = 0; i < fields.size(); i++) {
      codeBuilder.addStatement("c._$L = ($T) inputs.get($L)", fields.get(i).getVariableName(),
          ParameterizedTypeName.get(StructConstants.PRODUCER_INTERFACE,
              WildcardTypeName.subtypeOf(fields.get(i).getBoxedTypeName())), i);
    }

    return MethodSpec.methodBuilder("withInputsUnsafe")
        .addModifiers(Modifier.PROTECTED)
        .addAnnotation(Override.class)
        .returns(StructUtil.typeQualified(assembledTypeName(classPackage, className), typeParameters))
        .addParameter(producerListType, "inputs")
        .beginControlFlow("return clone(c -> ")
        .addCode(codeBuilder.build())
        .endControlFlow(")")
        .build();
  }

  private static TypeSpec getAssembledDefaultGenerator(String classPackage, String className,
      List<StructField> fields) {

    final ParameterizedTypeName producerHandleType = ParameterizedTypeName.get(StructConstants.PRODUCER_HANDLE,
        WildcardTypeName.subtypeOf(ParameterizedTypeName.get(assembledDefaultGeneratorTypeName(classPackage, className),
            TypeVariableName.get("R"))));

    final ClassName generatorTypeName = assembledDefaultGeneratorTypeName(classPackage, className);

    return TypeSpec.classBuilder(StructConstants.DEFAULT_GENERATOR_INTERFACE_NAME)
        .addAnnotation(ValueEquality.class) // HandleEquality would have the same behavior in practice
        .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
        .addTypeVariable(TypeVariableName.get("R"))
        .superclass(ParameterizedTypeName.get(StructConstants.ABSTRACT_GENERATOR, TypeVariableName.get("R"),
            ParameterizedTypeName.get(generatorTypeName, TypeVariableName.get("R"))))
        .addField(StructProcessor.getSerialVersionUIDField(0))
        .addField(FieldSpec.builder(generatorTypeName, "_singleton",
            Modifier.PRIVATE, Modifier.STATIC)
            .initializer("new $T()", generatorTypeName)
            .build())
        .addMethod(MethodSpec.methodBuilder("get")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addTypeVariable(TypeVariableName.get("R"))
            .returns(ParameterizedTypeName.get(generatorTypeName, TypeVariableName.get("R")))
            .addStatement("return _singleton")
            .build())
        .addMethod(MethodSpec.constructorBuilder().addStatement(
            "super(0xe327508a79df43b0L, 0xfb0af9e419745536L)"
        ).build())
        .addMethod(MethodSpec.methodBuilder("generate")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override.class)
            .addParameter(long.class, "index")
            .returns(TypeVariableName.get("R"))
            .addStatement("return null")
            .build())
        .addMethod(MethodSpec.methodBuilder("readResolve")
            .addModifiers(Modifier.PRIVATE)
            .returns(Object.class)
            .addException(ObjectStreamException.class)
            .addStatement("return _singleton")
            .build())
        .build();
  }

  static TypeSpec getAssembledType(String classPackage, String className, List<StructField> fields, long version,
      List<? extends TypeParameterElement> typeParameters) {
    final ArrayTypeName producerArrayType =
        ArrayTypeName.of(StructConstants.PRODUCER_INTERFACE_WILDCARD);

    final ParameterizedTypeName producerListType =
        ParameterizedTypeName.get(ClassName.get(List.class), StructConstants.PRODUCER_INTERFACE_WILDCARD);

    final ClassName structTypeName = ClassName.get(classPackage, className);
    final TypeName typeQualifiedStructTypeName = StructUtil.typeQualified(structTypeName, typeParameters);

    return TypeSpec.classBuilder("Assembled")
        .superclass(ParameterizedTypeName.get(StructConstants.ABSTRACT_PREPARED_TRANSFORMER_DYNAMIC,
            typeQualifiedStructTypeName,
            StructUtil.typeQualified(assembledTypeName(classPackage, className), typeParameters)))
        .addTypeVariables(typeParameters.stream().map(TypeVariableName::get).collect(Collectors.toList()))
        .addField(StructProcessor.getSerialVersionUIDField(version))
        .addType(getAssembledDefaultGenerator(classPackage, className, fields))
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
        .addFields(fields.stream()
            .map(field -> FieldSpec.builder(ParameterizedTypeName.get(StructConstants.PRODUCER_INTERFACE,
                WildcardTypeName.subtypeOf(field.getBoxedTypeName())), field.getInternalFieldName(), Modifier.PRIVATE)
                .initializer(field._optional ? CodeBlock.of("$T.get()",
                    assembledDefaultGeneratorTypeName(classPackage, className))
                    : CodeBlock.of("$T.get()", StructConstants.MISSING_INPUT))
                .build())
            .collect(Collectors.toList()))
        .addMethod(MethodSpec.constructorBuilder()
            .addModifiers(Modifier.PRIVATE)
            .build())
        .addType(getAssembledBuilderType(classPackage, className, fields, typeParameters))
        .addMethod(MethodSpec.methodBuilder("builder")
            .addTypeVariables(typeParameters.stream().map(TypeVariableName::get).collect(Collectors.toList()))
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .returns(StructUtil.typeQualified(fields.stream()
                .filter(field -> !field._optional)
                .map(field -> assembledBuilderSetterTypeName(classPackage, className, field))
                .findFirst()
                .orElse(completedAssembledBuilderTypeName(classPackage, className)), typeParameters))
            .addStatement("return new Builder$L()", typeParameters.isEmpty() ? "" : "<>")
            .build())
        .addMethods(IntStream.range(0, fields.size()).mapToObj(i -> {
          StructField field = fields.get(i);
          String inputParam = field.getVariableName() + "Input";

          return MethodSpec.methodBuilder("with" + field.getCoreName())
              .addModifiers(Modifier.PUBLIC)
              .addParameter(ParameterizedTypeName.get(StructConstants.PRODUCER_INTERFACE,
                  WildcardTypeName.subtypeOf(field.getBoxedTypeName())), inputParam)
              .returns(StructUtil.typeQualified(assembledTypeName(classPackage, className), typeParameters))
              .addStatement("return clone(c -> c._$L = $L)", field.getVariableName(), inputParam)
              .build();
        }).collect(Collectors.toList()))
        .addMethod(getAssembledApplyUnsafe(classPackage, className, fields, typeParameters))
        .addMethod(getAssembledWithInputsUnsafe(classPackage, className, fields, typeParameters))
        .addMethod(MethodSpec.methodBuilder(ProcessorConstants.PRODUCER_HASH_CODE_METHOD_NAME)
            .addModifiers(Modifier.PROTECTED)
            .addAnnotation(Override.class)
            .returns(TypeName.INT)
            .addStatement("return $T.hashCodeOfInputs(this)", StructConstants.TRANSFORMER_CLASS)
            .build())
        .addMethod(MethodSpec.methodBuilder(ProcessorConstants.PRODUCER_EQUALS_METHOD_NAME)
            .addModifiers(Modifier.PROTECTED)
            .addAnnotation(Override.class)
            .addParameter(ClassName.get(classPackage, className, "Assembled"), "other")
            .returns(TypeName.BOOLEAN)
            .addStatement("return $T.sameInputs(this, other)", StructConstants.TRANSFORMER_CLASS)
            .build())
        .addMethod(MethodSpec.methodBuilder("getInputList")
            .addModifiers(Modifier.PROTECTED)
            .addAnnotation(Override.class)
            .returns(producerListType)
            .addStatement("return $T.asList(new $T {$L})", Arrays.class, producerArrayType, String.join(", ",
                fields.stream().map(field -> "_" + field.getVariableName()).collect(Collectors.toList())))
            .build())
        .build();
  }
}
