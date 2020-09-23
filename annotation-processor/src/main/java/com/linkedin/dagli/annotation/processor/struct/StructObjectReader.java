package com.linkedin.dagli.annotation.processor.struct;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import com.squareup.javapoet.WildcardTypeName;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeParameterElement;


/**
 * Internal, static utility class that supports @Struct annotation processing,
 * specifically generating methods and classes related to implementing Reader.
 */
class StructObjectReader {
  private StructObjectReader() { }

  private static ClassName iterableTypeName(String classPackage, String className) {
    return ClassName.get(classPackage, className, "Reader");
  }

  private static ClassName iteratorTypeName(String classPackage, String className) {
    return iterableTypeName(classPackage, className).nestedClass("Iterator");
  }

  private static ClassName iterableBuilderTypeName(String classPackage, String className) {
    return iterableTypeName(classPackage, className).nestedClass(StructConstants.BUILDER_CLASS_NAME);
  }

  private static ClassName iterableBuilderSetterTypeName(String classPackage, String className, StructField field) {
    return StructUtil.helperTypeName(classPackage, className).nestedClass(field.getCoreName()).nestedClass(
        StructConstants.ITERABLE_BUILDER_SETTER_INTERFACE_NAME);
  }

  static TypeSpec getReaderBuilderSetterInterface(String classPackage, String className, StructField field,
      HashMap<StructField, StructField> nextFieldMap, List<? extends TypeParameterElement> typeParameters) {
    return TypeSpec.interfaceBuilder(StructConstants.ITERABLE_BUILDER_SETTER_INTERFACE_NAME)
        .addTypeVariables(typeParameters.stream().map(TypeVariableName::get).collect(Collectors.toList()))
        .addModifiers(Modifier.PUBLIC)
        .addMethod(MethodSpec.methodBuilder("set" + field.getCoreName())
            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
            .addParameter(
                ParameterizedTypeName.get(StructConstants.BATCH_ITERABLE, WildcardTypeName.subtypeOf(field.getBoxedTypeName())),
                field.getVariableName())
            .returns(StructUtil.typeQualified(
                nextFieldMap.get(field) == null ? completedReaderBuilderTypeName(classPackage, className)
                    : iterableBuilderSetterTypeName(classPackage, className, nextFieldMap.get(field)), typeParameters))
            .build())
        .build();
  }

  static TypeSpec getCompletedReaderBuilderType(String classPackage, String className, List<StructField> fields,
      List<? extends TypeParameterElement> typeParameters) {
    return TypeSpec.interfaceBuilder(StructConstants.COMPLETED_ITERABLE_BUILDER_INTERFACE_NAME)
        .addModifiers(Modifier.PUBLIC)
        .addTypeVariables(typeParameters.stream().map(TypeVariableName::get).collect(Collectors.toList()))
        .addMethod(MethodSpec.methodBuilder("build")
            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
            .returns(StructUtil.typeQualified(iterableTypeName(classPackage, className), typeParameters))
            .build())
        .addMethods(fields.stream()
            .filter(field -> field._optional)
            .map(field -> MethodSpec.methodBuilder("set" + field.getCoreName())
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(
                    ParameterizedTypeName.get(StructConstants.BATCH_ITERABLE, WildcardTypeName.subtypeOf(field.getBoxedTypeName())),
                    field.getVariableName())
                .returns(StructUtil.typeQualified(completedReaderBuilderTypeName(classPackage, className), typeParameters))
                .build())
            .collect(Collectors.toList()))
        .build();
  }

  private static TypeSpec getReaderBuilderType(String classPackage, String className, List<StructField> fields,
      List<? extends TypeParameterElement> typeParameters) {
    List<StructField> mandatoryFields = fields.stream().filter(field -> !field._optional).collect(Collectors.toList());
    HashMap<StructField, StructField> nextItemMap = StructUtil.nextItemMap(mandatoryFields);
    TypeName typeQualifiedReaderTypeName = StructUtil.typeQualified(iterableTypeName(classPackage, className), typeParameters);

    return TypeSpec.classBuilder(StructConstants.BUILDER_CLASS_NAME)
        .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
        .addTypeVariables(typeParameters.stream().map(TypeVariableName::get).collect(Collectors.toList()))
        .addField(FieldSpec.builder(typeQualifiedReaderTypeName, "_instance", Modifier.PRIVATE)
            .initializer("new $T()", typeQualifiedReaderTypeName)
            .build())
        .addSuperinterface(
            StructUtil.typeQualified(completedReaderBuilderTypeName(classPackage, className), typeParameters))
        .addSuperinterfaces(mandatoryFields.stream()
            .map(field -> StructUtil.typeQualified(iterableBuilderSetterTypeName(classPackage, className, field), typeParameters))
            .collect(Collectors.toList()))
        .addMethod(MethodSpec.methodBuilder("build")
            .addModifiers(Modifier.PUBLIC)
            .returns(typeQualifiedReaderTypeName)
            .addStatement("return _instance")
            .build())
        .addMethods(fields.stream()
            .map(field -> MethodSpec.methodBuilder("set" + field.getCoreName())
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addParameter(
                    ParameterizedTypeName.get(StructConstants.BATCH_ITERABLE, WildcardTypeName.subtypeOf(field.getBoxedTypeName())),
                    field.getVariableName())
                .addStatement("_instance." + field.getInternalFieldName() + " = " + field.getVariableName())
                .addStatement("return this")
                .returns(StructUtil.typeQualified(
                    nextItemMap.getOrDefault(field, null) != null ? iterableBuilderSetterTypeName(classPackage,
                        className, nextItemMap.get(field)) : completedReaderBuilderTypeName(classPackage, className),
                    typeParameters))
                .build())
            .collect(Collectors.toList()))
        .build();
  }

  private static TypeSpec getIteratorType(String classPackage, String className, List<StructField> fields,
      List<? extends TypeParameterElement> typeParameters) {
    final ClassName structTypeName = ClassName.get(classPackage, className);
    final TypeName typeQualifiedStructTypeName = StructUtil.typeQualified(structTypeName, typeParameters);

    return TypeSpec.classBuilder("Iterator")
        .addSuperinterface(ParameterizedTypeName.get(StructConstants.BATCH_ITERATOR, typeQualifiedStructTypeName))
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
        .addTypeVariables(typeParameters.stream().map(TypeVariableName::get).collect(Collectors.toList()))
        .addMethod(getIteratorConstructorMethod(classPackage, className, fields, typeParameters))
        .addFields(fields.stream()
            .map(field -> FieldSpec.builder(
                ParameterizedTypeName.get(StructConstants.BATCH_ITERATOR, WildcardTypeName.subtypeOf(field.getBoxedTypeName())),
                "_" + field.getVariableName(), Modifier.PRIVATE).build())
            .collect(Collectors.toList()))
        .addMethod(getIteratorHasNextMethod(classPackage, className, fields))
        .addMethod(getIteratorNextMethod(classPackage, className, fields, typeParameters))
        .addMethod(getReaderCloseMethod(classPackage, className, fields))
//        .addMethod(getIteratorAvailableMethod(classPackage, className, fields))
        .build();
  }

//  private static MethodSpec getIteratorAvailableMethod(String classPackage, String className, List<StructField> fields) {
//    MethodSpec.Builder builder = MethodSpec.methodBuilder("available")
//        .addModifiers(Modifier.PUBLIC)
//        .addAnnotation(Override.class)
//        .returns(long.class)
//        .addStatement("long res = Long.MAX_VALUE");
//
//    for (StructField field : fields) {
//      if (field._optional) {
//        builder.beginControlFlow("if (_$L != null)", field.getVariableName());
//      }
//
//      builder.addStatement("res = Math.min(res, _$L.available())", field.getVariableName());
//
//      if (field._optional) {
//        builder.endControlFlow();
//      }
//    }
//
//    builder.addStatement("return res");
//    return builder.build();
//  }

  private static MethodSpec getIteratorConstructorMethod(String classPackage, String className,
      List<StructField> fields, List<? extends TypeParameterElement> typeParameters) {
    MethodSpec.Builder builder = MethodSpec.constructorBuilder()
        .addModifiers(Modifier.PUBLIC)
        .addParameter(StructUtil.typeQualified(iterableTypeName(classPackage, className), typeParameters), "owner");

    for (StructField field : fields) {
      if (field._optional) {
        builder.addStatement("_$L = owner._$L == null ? null : owner._$L.iterator()", field.getVariableName(),
            field.getVariableName(), field.getVariableName());
      } else {
        builder.addStatement("_$L = owner._$L.iterator()", field.getVariableName(), field.getVariableName());
      }
    }

    return builder.build();
  }

  private static MethodSpec getIteratorNextMethod(String classPackage, String className, List<StructField> fields,
      List<? extends TypeParameterElement> typeParameters) {
    ClassName structTypeName = ClassName.get(classPackage, className);
    TypeName typeQualifiedStructTypeName = StructUtil.typeQualified(structTypeName, typeParameters);

    MethodSpec.Builder builder = MethodSpec.methodBuilder("next")
        .addAnnotation(Override.class)
        .returns(typeQualifiedStructTypeName)
        .addModifiers(Modifier.PUBLIC)
        .addStatement("$T res = new $T()", typeQualifiedStructTypeName, typeQualifiedStructTypeName);

    for (StructField field : fields) {
      if (field._optional) {
        builder.beginControlFlow("if (_$L != null)", field.getVariableName());
      }
      builder.addStatement("res.$L = _$L.next()", field.getFieldName(), field.getVariableName());

      if (field._optional) {
        builder.endControlFlow();
      }
    }
    builder.addStatement("return res");

    return builder.build();
  }

  private static MethodSpec getIteratorHasNextMethod(String classPackage, String className, List<StructField> fields) {
    Optional<StructField> mandatoryReader = fields.stream().filter(field -> !field._optional).findFirst();

    MethodSpec.Builder builder = MethodSpec.methodBuilder("hasNext")
        .addAnnotation(Override.class)
        .returns(boolean.class)
        .addModifiers(Modifier.PUBLIC);

    if (mandatoryReader.isPresent()) {
      builder.addStatement("return _$L.hasNext()", mandatoryReader.get().getVariableName());
    } else {
      for (StructField field : fields) {
        builder.beginControlFlow("if (_$L != null)", field.getVariableName());
        builder.addStatement("return _$L.hasNext()", field.getVariableName());
        builder.endControlFlow();
      }
      builder.addStatement("return true");
    }

    return builder.build();
  }

  static TypeSpec getReaderType(String classPackage, String className, List<StructField> fields, long version,
      List<? extends TypeParameterElement> typeParameters) {
    final ClassName structTypeName = ClassName.get(classPackage, className);
    final TypeName typeQualifiedStructName = StructUtil.typeQualified(structTypeName, typeParameters);

    return TypeSpec.classBuilder("Reader")
        .superclass(ParameterizedTypeName.get(StructConstants.ABSTRACT_CLONEABLE,
            StructUtil.typeQualified(iterableTypeName(classPackage, className), typeParameters)))
        .addSuperinterface(ParameterizedTypeName.get(StructConstants.BATCH_ITERABLE, typeQualifiedStructName))
        .addSuperinterface(Serializable.class)
        .addTypeVariables(typeParameters.stream().map(TypeVariableName::get).collect(Collectors.toList()))
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
        .addType(getReaderBuilderType(classPackage, className, fields, typeParameters))
        .addType(getIteratorType(classPackage, className, fields, typeParameters))
        .addField(StructProcessor.getSerialVersionUIDField(version))
        .addFields(fields.stream()
            .map(field -> FieldSpec.builder(
                ParameterizedTypeName.get(StructConstants.BATCH_ITERABLE, WildcardTypeName.subtypeOf(field.getBoxedTypeName())),
                field.getInternalFieldName(), Modifier.PRIVATE).initializer("null").build())
            .collect(Collectors.toList()))
        .addMethods(fields.stream().map(field -> MethodSpec.methodBuilder("get" + field.getCoreName() + "Reader")
            .addModifiers(Modifier.PUBLIC)
            .returns(ParameterizedTypeName.get(StructConstants.BATCH_ITERABLE, WildcardTypeName.subtypeOf(field.getBoxedTypeName())))
            .addStatement("return $L", field.getInternalFieldName())
            .build()).collect(Collectors.toList()))
        .addMethod(MethodSpec.constructorBuilder()
            .addModifiers(Modifier.PRIVATE)
            .build())
        .addMethod(MethodSpec.methodBuilder("builder")
            .addTypeVariables(typeParameters.stream().map(TypeVariableName::get).collect(Collectors.toList()))
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .returns(StructUtil.typeQualified(fields.stream()
                .filter(field -> !field._optional)
                .map(field -> iterableBuilderSetterTypeName(classPackage, className, field))
                .findFirst()
                .orElse(completedReaderBuilderTypeName(classPackage, className)), typeParameters))
            .addStatement("return new Builder$L()", typeParameters.isEmpty() ? "" : "<>")
            .build())
        .addMethods(fields.stream().map(field -> {
          String inputParam = field.getVariableName() + "Input";

          return MethodSpec.methodBuilder("with" + field.getCoreName())
              .addModifiers(Modifier.PUBLIC)
              .addParameter(
                  ParameterizedTypeName.get(StructConstants.BATCH_ITERABLE, WildcardTypeName.subtypeOf(field.getBoxedTypeName())),
                  inputParam)
              .returns(iterableTypeName(classPackage, className))
              .addStatement("return clone(c -> c._$L = $L)", field.getVariableName(), inputParam)
              .build();
        }).collect(Collectors.toList()))
        .addMethod(getReaderSize64Method(classPackage, className, fields))
        .addMethod(MethodSpec.methodBuilder("iterator")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override.class)
            .returns(StructUtil.typeQualified(iteratorTypeName(classPackage, className), typeParameters))
            .addStatement("return new $T(this)", StructUtil.typeQualified(iteratorTypeName(classPackage, className), typeParameters))
            .build())
        .addMethod(getReaderCloseMethod(classPackage, className, fields))
        .build();
  }

  private static MethodSpec getReaderCloseMethod(String classPackage, String className, List<StructField> fields) {
    MethodSpec.Builder builder = MethodSpec.methodBuilder("close")
        .addAnnotation(Override.class)
        .returns(TypeName.VOID)
        .addModifiers(Modifier.PUBLIC);

    for (StructField field : fields) {
      if (field._optional) {
        builder.beginControlFlow("if (_$L != null)", field.getVariableName());
        builder.addStatement("_$L.close()", field.getVariableName());
        builder.endControlFlow();
      } else {
        builder.addStatement("_$L.close()", field.getVariableName());
      }
    }

    return builder.build();
  }

  private static MethodSpec getReaderSize64Method(String classPackage, String className, List<StructField> fields) {
    Optional<StructField> mandatoryReader = fields.stream().filter(field -> !field._optional).findFirst();

    MethodSpec.Builder builder = MethodSpec.methodBuilder("size64")
        .addAnnotation(Override.class)
        .returns(long.class)
        .addModifiers(Modifier.PUBLIC);

    if (mandatoryReader.isPresent()) {
      builder.addStatement("return _$L.size64()", mandatoryReader.get().getVariableName());
    } else {
      for (StructField field : fields) {
        builder.beginControlFlow("if (_$L != null)", field.getVariableName());
        builder.addStatement("return _$L.size64()", field.getVariableName());
        builder.endControlFlow();
      }
      builder.addStatement("return $LL", Long.MAX_VALUE);
    }

    return builder.build();
  }

  static ClassName completedReaderBuilderTypeName(String classPackage, String className) {
    return StructUtil.helperTypeName(classPackage, className).nestedClass(
        StructConstants.COMPLETED_ITERABLE_BUILDER_INTERFACE_NAME);
  }
}
