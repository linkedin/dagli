package com.linkedin.dagli.annotation.processor.struct;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeParameterElement;

/**
 * Internal, static utility class that supports @Struct annotation processing,
 * specifically generating the inner Builder class that constructs the (immutable) struct.
 */
class StructBuilder {
  private static final String BUILDER_IMPLEMENTATION_CLASS_NAME = StructConstants.BUILDER_CLASS_NAME + "Impl";

  private StructBuilder() { }

  static ClassName builderSetterTypeName(String classPackage, String className, StructField field) {
    return StructUtil.helperTypeName(classPackage, className).nestedClass(field.getCoreName()).nestedClass(
        StructConstants.BUILDER_SETTER_INTERFACE_NAME);
  }

  static ClassName completedBuilderTypeName(String classPackage, String className) {
    return StructUtil.helperTypeName(classPackage, className).nestedClass(StructConstants.COMPLETED_BUILDER_INTERFACE_NAME);
  }

  static TypeSpec getBuilderType(String classPackage, String className, List<StructField> fields,
      List<? extends TypeParameterElement> typeParameters) {
    List<StructField> mandatoryFields = fields.stream().filter(field -> !field._optional).collect(Collectors.toList());

    final TypeName
        typeQualifiedStructName = StructUtil.typeQualified(ClassName.get(classPackage, className), typeParameters);

    return TypeSpec.classBuilder(BUILDER_IMPLEMENTATION_CLASS_NAME)
        .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
        .addTypeVariables(typeParameters.stream().map(TypeVariableName::get).collect(Collectors.toList()))
        .addField(FieldSpec.builder(typeQualifiedStructName, "_instance", Modifier.PRIVATE)
            .initializer("new $T()", typeQualifiedStructName)
            .build())
        .addSuperinterface(StructUtil.typeQualified(completedBuilderTypeName(classPackage, className), typeParameters))
        .addSuperinterfaces(mandatoryFields.stream()
            .map(field -> StructUtil.typeQualified(builderSetterTypeName(classPackage, className, field), typeParameters))
            .collect(Collectors.toList()))
        .addMethod(MethodSpec.methodBuilder("build")
            .addModifiers(Modifier.PUBLIC)
            .returns(typeQualifiedStructName)
            .addStatement("return _instance")
            .build())
        .addMethods(IntStream.range(0, mandatoryFields.size()).mapToObj(fieldIndex -> {
          StructField field = mandatoryFields.get(fieldIndex);
          return MethodSpec.methodBuilder("set" + field.getCoreName())
              .addAnnotation(Override.class)
              .addModifiers(Modifier.PUBLIC)
              .addParameter(TypeName.get(field._type), field.getVariableName())
              .addStatement("_instance." + field.getFieldName() + " = " + field.getVariableName())
              .addStatement("return this")
              .returns(StructUtil.typeQualified(
                  fieldIndex < mandatoryFields.size() - 1 ? builderSetterTypeName(classPackage, className,
                      mandatoryFields.get(fieldIndex + 1)) : completedBuilderTypeName(classPackage, className),
                  typeParameters))
              .build();
        }).collect(Collectors.toList()))
        .addMethods(fields.stream().filter(field -> field._optional).map(
          field ->
          MethodSpec.methodBuilder("set" + field.getCoreName())
              .addAnnotation(Override.class)
              .addModifiers(Modifier.PUBLIC)
              .addParameter(TypeName.get(field._type), field.getVariableName())
              .addStatement("_instance." + field.getFieldName() + " = " + field.getVariableName())
              .addStatement("return this")
              .returns(StructUtil.typeQualified(completedBuilderTypeName(classPackage, className), typeParameters))
              .build()
        ).collect(Collectors.toList()))
        .build();
  }

  static TypeSpec getBuilderSetterInterface(String classPackage, String className, StructField field,
      HashMap<StructField, StructField> nextFieldMap, List<? extends TypeParameterElement> typeParameters) {
    return TypeSpec.interfaceBuilder(StructConstants.BUILDER_SETTER_INTERFACE_NAME)
        .addModifiers(Modifier.PUBLIC)
        .addTypeVariables(typeParameters.stream().map(TypeVariableName::get).collect(Collectors.toList()))
        .addMethod(MethodSpec.methodBuilder("set" + field.getCoreName())
            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
            .addParameter(TypeName.get(field._type), field.getVariableName())
            .returns(StructUtil.typeQualified(nextFieldMap.get(field) == null ? completedBuilderTypeName(classPackage, className)
                : builderSetterTypeName(classPackage, className, nextFieldMap.get(field)), typeParameters))
            .addJavadoc(field._javaDocComment == null ? "" : field._javaDocComment)
            .build())
        .build();
  }

  static TypeSpec getBuilderInterface(String classPackage, String className, List<StructField> realFields,
      List<? extends TypeParameterElement> typeParameters) {

    final TypeName typeQualifiedBuilderImplName =
        StructUtil.typeQualified(ClassName.get(classPackage, className, BUILDER_IMPLEMENTATION_CLASS_NAME),
            typeParameters);

    TypeSpec.Builder builderBuilder = TypeSpec.interfaceBuilder(StructConstants.BUILDER_CLASS_NAME)
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC);

    List<StructField> mandatoryFields =
        realFields.stream().filter(field -> !field._optional).collect(Collectors.toList());

    if (mandatoryFields.isEmpty()) {
      final TypeName typeQualifiedStructName =
          StructUtil.typeQualified(ClassName.get(classPackage, className), typeParameters);
      // emulate completed builder
      return builderBuilder
          // build() just returns new instance with default field values
          .addMethod(MethodSpec.methodBuilder("build")
              .addTypeVariables(typeParameters.stream().map(TypeVariableName::get).collect(Collectors.toList()))
              .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
              .returns(StructUtil.typeQualified(ClassName.get(classPackage, className), typeParameters))
              .addStatement("return new $T()", typeQualifiedStructName)
              .build())
          // add methods for each optional field (we know all realFields are optional at this point)
          .addMethods(realFields.stream()
              .map(field -> MethodSpec.methodBuilder("set" + field.getCoreName())
                  .addJavadoc(field._javaDocComment == null ? "" : field._javaDocComment)
                  .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                  .addTypeVariables(typeParameters.stream().map(TypeVariableName::get).collect(Collectors.toList()))
                  .addParameter(TypeName.get(field._type), field.getVariableName())
                  .returns(StructUtil.typeQualified(StructBuilder.completedBuilderTypeName(classPackage, className),
                      typeParameters))
                  .addStatement("return new $T().set$L($L)", typeQualifiedBuilderImplName, field.getCoreName(),
                      field.getVariableName())
                  .build())
              .collect(Collectors.toList())).build();
    } else {
      HashMap<StructField, StructField> nextFieldMap = StructUtil.nextItemMap(mandatoryFields);
      StructField field = mandatoryFields.get(0);

      // emulate builder for first field
      return builderBuilder.addMethod(MethodSpec.methodBuilder("set" + field.getCoreName())
          .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
          .addTypeVariables(typeParameters.stream().map(TypeVariableName::get).collect(Collectors.toList()))
          .addParameter(TypeName.get(field._type), field.getVariableName())
          .returns(StructUtil.typeQualified(
              nextFieldMap.get(field) == null ? completedBuilderTypeName(classPackage, className)
                  : builderSetterTypeName(classPackage, className, nextFieldMap.get(field)), typeParameters))
          .addJavadoc(field._javaDocComment == null ? "" : field._javaDocComment)
          .addStatement("return new $T().set$L($L)", typeQualifiedBuilderImplName, field.getCoreName(),
              field.getVariableName())
          .build()).build();
    }
  }
}
