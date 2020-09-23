package com.linkedin.dagli.annotation.processor.struct;

import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeParameterElement;

/**
 * Internal, static utility class that supports @Struct annotation processing,
 * specifically generating methods and classes relating to [StructName].Schema,
 * an inner class that serves to define a RowSchema for the struct that can then
 * be used to, e.g. read the struct from a CSV file.
 */
class StructSchema {
  private StructSchema() { }

  private static final ArrayTypeName STRING_ARRAY_TYPE = ArrayTypeName.of(ClassName.get(String.class));

  private static final String COLUMN_INDEX_SUFFIX = "ColumnIndex";
  private static final String COLUMN_NAME_SUFFIX = "ColumnName";

  private static final Map<TypeName, CodeBlock> KNOWN_FIELD_PARSERS = getKnownFieldParsers();

  private static Map<TypeName, CodeBlock> getKnownFieldParsers() {
    Map<TypeName, CodeBlock> knownParsers = new HashMap<>();
    knownParsers.put(ClassName.get(String.class), CodeBlock.of("s -> s"));
    knownParsers.put(ClassName.get(CharSequence.class), CodeBlock.of("s -> s"));
    knownParsers.put(ClassName.get(Boolean.class), CodeBlock.of("$T::parseBoolean", Boolean.class));
    knownParsers.put(ClassName.get(Byte.class), CodeBlock.of("$T::parseByte", Byte.class));
    knownParsers.put(ClassName.get(Short.class), CodeBlock.of("$T::parseShort", Short.class));
    knownParsers.put(ClassName.get(Integer.class), CodeBlock.of("$T::parseInt", Integer.class));
    knownParsers.put(ClassName.get(Long.class), CodeBlock.of("$T::parseLong", Long.class));
    knownParsers.put(ClassName.get(Character.class), CodeBlock.of("s -> s.charAt(0)"));
    knownParsers.put(ClassName.get(Float.class), CodeBlock.of("$T::parseFloat", Float.class));
    knownParsers.put(ClassName.get(Double.class), CodeBlock.of("$T::parseDouble", Double.class));
    return knownParsers;
  }

  private static ClassName schemaTypeName(String classPackage, String className) {
    return ClassName.get(classPackage, className, StructConstants.SCHEMA_CLASS_NAME);
  }

  private static ClassName schemaBuilderTypeName(String classPackage, String className) {
    return schemaTypeName(classPackage, className).nestedClass(StructConstants.BUILDER_CLASS_NAME);
  }

  private static ClassName schemaBuilderSetterTypeName(String classPackage, String className, StructField field) {
    return StructUtil.helperTypeName(classPackage, className).nestedClass(field.getCoreName()).nestedClass(
        StructConstants.SCHEMA_BUILDER_SETTER_INTERFACE_NAME);
  }

  private static ClassName completedSchemaBuilderTypeName(String classPackage, String className) {
    return StructUtil.helperTypeName(classPackage, className).nestedClass(
        StructConstants.COMPLETED_SCHEMA_BUILDER_INTERFACE_NAME);
  }

  static TypeSpec getSchemaBuilderSetterInterface(String classPackage, String className, StructField field,
      HashMap<StructField, StructField> nextFieldMap, List<? extends TypeParameterElement> typeParameters) {

    TypeSpec.Builder builder = TypeSpec.interfaceBuilder(StructConstants.SCHEMA_BUILDER_SETTER_INTERFACE_NAME)
        .addTypeVariables(typeParameters.stream().map(TypeVariableName::get).collect(Collectors.toList()))
        .addModifiers(Modifier.PUBLIC)
        .addMethod(MethodSpec.methodBuilder(getSchemaBuilderIndexSetterName(field.getCoreName()))
            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
            .addParameter(int.class, field.getVariableName() + COLUMN_INDEX_SUFFIX)
            .addParameter(
                ParameterizedTypeName.get(StructConstants.FUNCTION1_INTERFACE, ClassName.get(String.class),
                    field.getBoxedTypeName()), "parser")
            .returns(StructUtil.typeQualified(
                nextFieldMap.get(field) == null ? StructSchema.completedSchemaBuilderTypeName(classPackage, className)
                    : schemaBuilderSetterTypeName(classPackage, className, nextFieldMap.get(field)), typeParameters))
            .build())
        .addMethod(MethodSpec.methodBuilder(getSchemaBuilderNameSetterName(field.getCoreName()))
            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
            .addParameter(String.class, field.getVariableName() + COLUMN_NAME_SUFFIX)
            .addParameter(
                ParameterizedTypeName.get(StructConstants.FUNCTION1_INTERFACE, ClassName.get(String.class),
                    field.getBoxedTypeName()), "parser")
            .returns(StructUtil.typeQualified(
                nextFieldMap.get(field) == null ? StructSchema.completedSchemaBuilderTypeName(classPackage, className)
                    : schemaBuilderSetterTypeName(classPackage, className, nextFieldMap.get(field)), typeParameters))
            .build())
        .addMethod(MethodSpec.methodBuilder(getSchemaBuilderParserSetterName(field.getCoreName()))
            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
            .addParameter(ParameterizedTypeName.get(ClassName.get(BiFunction.class), STRING_ARRAY_TYPE,
                  STRING_ARRAY_TYPE, field.getBoxedTypeName()), "parser")
            .returns(StructUtil.typeQualified(
                nextFieldMap.get(field) == null ? StructSchema.completedSchemaBuilderTypeName(classPackage, className)
                    : schemaBuilderSetterTypeName(classPackage, className, nextFieldMap.get(field)), typeParameters))
            .build());

    if (KNOWN_FIELD_PARSERS.containsKey(field.getBoxedTypeName())) {
      builder.addMethod(MethodSpec.methodBuilder(getSchemaBuilderIndexSetterName(field.getCoreName()))
          .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
          .addParameter(int.class, field.getVariableName() + COLUMN_INDEX_SUFFIX)
          .returns(StructUtil.typeQualified(
              nextFieldMap.get(field) == null ? StructSchema.completedSchemaBuilderTypeName(classPackage, className)
                  : schemaBuilderSetterTypeName(classPackage, className, nextFieldMap.get(field)), typeParameters))
          .build())
          .addMethod(MethodSpec.methodBuilder(getSchemaBuilderNameSetterName(field.getCoreName()))
              .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
              .addParameter(String.class, field.getVariableName() + COLUMN_NAME_SUFFIX)
              .returns(StructUtil.typeQualified(
                  nextFieldMap.get(field) == null ? StructSchema.completedSchemaBuilderTypeName(classPackage, className)
                      : schemaBuilderSetterTypeName(classPackage, className, nextFieldMap.get(field)), typeParameters))
              .build());
    }

    return builder.build();
  }

  static TypeSpec getCompletedSchemaBuilderType(String classPackage, String className,
      List<StructField> fields, List<? extends TypeParameterElement> typeParameters) {
    return TypeSpec.interfaceBuilder(StructConstants.COMPLETED_SCHEMA_BUILDER_INTERFACE_NAME)
        .addModifiers(Modifier.PUBLIC)
        .addTypeVariables(typeParameters.stream().map(TypeVariableName::get).collect(Collectors.toList()))
        .addMethod(MethodSpec.methodBuilder("build")
            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
            .returns(StructUtil.typeQualified(schemaTypeName(classPackage, className), typeParameters))
            .build())
        .addMethods(fields.stream()
            .filter(field -> field._optional)
            .filter(field -> KNOWN_FIELD_PARSERS.containsKey(field.getBoxedTypeName()))
            .map(field -> MethodSpec.methodBuilder(getSchemaBuilderIndexSetterName(field.getCoreName()))
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(int.class, field.getVariableName() + COLUMN_INDEX_SUFFIX)
                .returns(
                    StructUtil.typeQualified(completedSchemaBuilderTypeName(classPackage, className), typeParameters))
                .build())
            .collect(Collectors.toList()))
        .addMethods(fields.stream()
            .filter(field -> field._optional)
            .map(field -> MethodSpec.methodBuilder(getSchemaBuilderIndexSetterName(field.getCoreName()))
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(int.class, field.getVariableName() + COLUMN_INDEX_SUFFIX)
                .addParameter(
                    ParameterizedTypeName.get(StructConstants.FUNCTION1_INTERFACE, ClassName.get(String.class),
                        field.getBoxedTypeName()), "parser")
                .returns(
                    StructUtil.typeQualified(completedSchemaBuilderTypeName(classPackage, className), typeParameters))
                .build())
            .collect(Collectors.toList()))
        .addMethods(fields.stream()
            .filter(field -> field._optional)
            .filter(field -> KNOWN_FIELD_PARSERS.containsKey(field.getBoxedTypeName()))
            .map(field -> MethodSpec.methodBuilder(getSchemaBuilderNameSetterName(field.getCoreName()))
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(String.class, field.getVariableName() + COLUMN_NAME_SUFFIX)
                .returns(
                    StructUtil.typeQualified(completedSchemaBuilderTypeName(classPackage, className), typeParameters))
                .build())
            .collect(Collectors.toList()))
        .addMethods(fields.stream()
            .filter(field -> field._optional)
            .map(field -> MethodSpec.methodBuilder(getSchemaBuilderNameSetterName(field.getCoreName()))
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(String.class, field.getVariableName() + COLUMN_NAME_SUFFIX)
                .addParameter(
                    ParameterizedTypeName.get(StructConstants.FUNCTION1_INTERFACE, ClassName.get(String.class),
                        field.getBoxedTypeName()), "parser")
                .returns(
                    StructUtil.typeQualified(completedSchemaBuilderTypeName(classPackage, className), typeParameters))
                .build())
            .collect(Collectors.toList()))
        .addMethods(fields.stream()
            .filter(field -> field._optional)
            .map(field -> MethodSpec.methodBuilder(getSchemaBuilderParserSetterName(field.getCoreName()))
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(ParameterizedTypeName.get(ClassName.get(BiFunction.class), STRING_ARRAY_TYPE,
                    STRING_ARRAY_TYPE, field.getBoxedTypeName()), "parser")
                .returns(
                    StructUtil.typeQualified(completedSchemaBuilderTypeName(classPackage, className), typeParameters))
                .build())
            .collect(Collectors.toList()))
        .build();
  }

  private static String getSchemaBuilderIndexSetterName(String fieldName) {
    return "set" + fieldName + COLUMN_INDEX_SUFFIX;
  }

  private static String getSchemaBuilderNameSetterName(String fieldName) {
    return "set" + fieldName + COLUMN_NAME_SUFFIX;
  }

  private static String getSchemaBuilderParserSetterName(String fieldName) {
    return "set" + fieldName + "Parser";
  }

  private static TypeSpec getSchemaBuilderType(String classPackage, String className, List<StructField> fields,
      List<? extends TypeParameterElement> typeParameters) {
    final List<StructField> mandatoryFields = fields.stream().filter(field -> !field._optional).collect(Collectors.toList());
    final HashMap<StructField, StructField> nextItemMap = StructUtil.nextItemMap(mandatoryFields);

    final ClassName structTypeName = ClassName.get(classPackage, className);
    final TypeName typeQualifiedStructName = StructUtil.typeQualified(structTypeName, typeParameters);
    final TypeName typeQualifiedSchemaTypeName = StructUtil.typeQualified(schemaTypeName(classPackage, className), typeParameters);

    return TypeSpec.classBuilder(StructConstants.BUILDER_CLASS_NAME)
        .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
        .addTypeVariables(typeParameters.stream().map(TypeVariableName::get).collect(Collectors.toList()))
        .addField(FieldSpec.builder(typeQualifiedSchemaTypeName, "_instance", Modifier.PRIVATE)
            .initializer("new $T()", typeQualifiedSchemaTypeName)
            .build())
        .addSuperinterface(
            StructUtil.typeQualified(completedSchemaBuilderTypeName(classPackage, className), typeParameters))
        .addSuperinterfaces(mandatoryFields.stream()
            .map(field -> StructUtil.typeQualified(schemaBuilderSetterTypeName(classPackage, className, field), typeParameters))
            .collect(Collectors.toList()))
        .addMethod(MethodSpec.methodBuilder("build")
            .addModifiers(Modifier.PUBLIC)
            .returns(typeQualifiedSchemaTypeName)
            .addStatement("return _instance")
            .build())
        .addMethods(fields.stream().filter(field -> KNOWN_FIELD_PARSERS.containsKey(field.getBoxedTypeName()))
            .map(field -> MethodSpec.methodBuilder(getSchemaBuilderIndexSetterName(field.getCoreName()))
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addParameter(
                    int.class, field.getVariableName() + COLUMN_INDEX_SUFFIX)
                .addStatement("return $L($L, $L)", getSchemaBuilderIndexSetterName(field.getCoreName()),
                    field.getVariableName() + COLUMN_INDEX_SUFFIX, KNOWN_FIELD_PARSERS.get(field.getBoxedTypeName()))
                .returns(StructUtil.typeQualified(
                    nextItemMap.getOrDefault(field, null) != null ? schemaBuilderSetterTypeName(classPackage,
                        className, nextItemMap.get(field)) : completedSchemaBuilderTypeName(classPackage, className),
                    typeParameters))
                .build())
            .collect(Collectors.toList()))
        .addMethods(fields.stream()
            .map(field -> MethodSpec.methodBuilder(getSchemaBuilderIndexSetterName(field.getCoreName()))
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addParameter(
                    int.class, field.getVariableName() + COLUMN_INDEX_SUFFIX)
                .addParameter(ParameterizedTypeName.get(StructConstants.FUNCTION1_INTERFACE, ClassName.get(String.class), field.getBoxedTypeName()),
                    "parser")
                .addStatement("_instance._fields.add($L)", TypeSpec.anonymousClassBuilder("")
                    .addSuperinterface(
                        ParameterizedTypeName.get(StructConstants.ROW_SCHEMA_INDEXED_FIELD_INTERFACE, typeQualifiedStructName))
                    .addMethod(getIsRequiredMethod(field))
                    .addMethod(getReadMethod(typeQualifiedStructName, field))
                    .addMethod(MethodSpec.methodBuilder("getIndex")
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(Override.class)
                        .returns(int.class)
                        .addStatement("return $L", field.getVariableName() + COLUMN_INDEX_SUFFIX)
                        .build())
                    .build())
                .addStatement("return this")
                .returns(StructUtil.typeQualified(
                    nextItemMap.getOrDefault(field, null) != null ? schemaBuilderSetterTypeName(classPackage,
                        className, nextItemMap.get(field)) : completedSchemaBuilderTypeName(classPackage, className),
                    typeParameters))
                .build())
            .collect(Collectors.toList()))
        .addMethods(fields.stream().filter(field -> KNOWN_FIELD_PARSERS.containsKey(field.getBoxedTypeName()))
            .map(field -> MethodSpec.methodBuilder(getSchemaBuilderNameSetterName(field.getCoreName()))
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addParameter(
                    String.class, field.getVariableName() + COLUMN_NAME_SUFFIX)
                .addStatement("return $L($L, $L)", getSchemaBuilderNameSetterName(field.getCoreName()),
                    field.getVariableName() + COLUMN_NAME_SUFFIX, KNOWN_FIELD_PARSERS.get(field.getBoxedTypeName()))
                .returns(StructUtil.typeQualified(
                    nextItemMap.getOrDefault(field, null) != null ? schemaBuilderSetterTypeName(classPackage,
                        className, nextItemMap.get(field)) : completedSchemaBuilderTypeName(classPackage, className),
                    typeParameters))
                .build())
            .collect(Collectors.toList()))
        .addMethods(fields.stream()
            .map(field -> MethodSpec.methodBuilder(getSchemaBuilderNameSetterName(field.getCoreName()))
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addParameter(
                    String.class, field.getVariableName() + COLUMN_NAME_SUFFIX)
                .addParameter(ParameterizedTypeName.get(StructConstants.FUNCTION1_INTERFACE, ClassName.get(String.class), field.getBoxedTypeName()),
                    "parser")
                .addStatement("_instance._fields.add($L)", TypeSpec.anonymousClassBuilder("")
                    .addSuperinterface(
                        ParameterizedTypeName.get(StructConstants.ROW_SCHEMA_NAMED_FIELD_INTERFACE, typeQualifiedStructName))
                    .addMethod(getIsRequiredMethod(field))
                    .addMethod(getReadMethod(typeQualifiedStructName, field))
                    .addMethod(MethodSpec.methodBuilder("getName")
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(Override.class)
                        .returns(String.class)
                        .addStatement("return $L", field.getVariableName() + COLUMN_NAME_SUFFIX)
                        .build())
                    .build())
                .addStatement("return this")
                .returns(StructUtil.typeQualified(
                    nextItemMap.getOrDefault(field, null) != null ? schemaBuilderSetterTypeName(classPackage,
                        className, nextItemMap.get(field)) : completedSchemaBuilderTypeName(classPackage, className),
                    typeParameters))
                .build())
            .collect(Collectors.toList()))
        .addMethods(fields.stream()
            .map(field -> MethodSpec.methodBuilder(getSchemaBuilderParserSetterName(field.getCoreName()))
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addParameter(ParameterizedTypeName.get(ClassName.get(BiFunction.class), STRING_ARRAY_TYPE,
                    STRING_ARRAY_TYPE, field.getBoxedTypeName()), "parser")
                .addStatement("_instance._fields.add($L)", TypeSpec.anonymousClassBuilder("")
                    .addSuperinterface(
                        ParameterizedTypeName.get(StructConstants.ROW_SCHEMA_ALL_FIELDS_INTERFACE, typeQualifiedStructName))
                    .addMethod(getIsRequiredMethod(field))
                    .addMethod(getReadAllMethod(typeQualifiedStructName, field))
                    .build())
                .addStatement("return this")
                .returns(StructUtil.typeQualified(
                    nextItemMap.getOrDefault(field, null) != null ? schemaBuilderSetterTypeName(classPackage,
                        className, nextItemMap.get(field)) : completedSchemaBuilderTypeName(classPackage, className),
                    typeParameters))
                .build())
            .collect(Collectors.toList()))
        .build();
  }

  private static MethodSpec getIsRequiredMethod(StructField field) {
    return MethodSpec.methodBuilder("isRequired")
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(Override.class)
        .returns(boolean.class)
        .addStatement("return $L", !field.isOptional())
        .build();
  }

  private static MethodSpec getReadAllMethod(TypeName typeQualifiedStructName, StructField field) {
    return MethodSpec.methodBuilder("read")
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(Override.class)
        .addParameter(typeQualifiedStructName, "accumulator")
        .addParameter(String[].class, "fieldNames")
        .addParameter(String[].class, "fieldText")
        .addStatement("accumulator.$L = parser.apply(fieldNames, fieldText)", field.getFieldName())
        .build();
  }

  private static MethodSpec getReadMethod(TypeName typeQualifiedStructName, StructField field) {
    return MethodSpec.methodBuilder("read")
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(Override.class)
        .addParameter(typeQualifiedStructName, "accumulator")
        .addParameter(String.class, "fieldText")
        .addStatement("accumulator.$L = parser.apply(fieldText)", field.getFieldName())
        .build();
  }

  static TypeSpec getRowSchemaType(String classPackage, String className, List<StructField> fields, long version,
      List<? extends TypeParameterElement> typeParameters) {
    final ClassName structTypeName = ClassName.get(classPackage, className);
    final TypeName typeQualifiedStructName = StructUtil.typeQualified(structTypeName, typeParameters);
    final ParameterizedTypeName fieldTypeName = ParameterizedTypeName.get(StructConstants.ROW_SCHEMA_FIELD_SCHEMA_INTERFACE, typeQualifiedStructName);
    final ParameterizedTypeName fieldListTypeName = ParameterizedTypeName.get(ClassName.get(ArrayList.class), fieldTypeName);

    return TypeSpec.classBuilder("Schema")
        .addSuperinterface(
            ParameterizedTypeName.get(StructConstants.ROW_SCHEMA_INTERFACE, typeQualifiedStructName, typeQualifiedStructName))
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
        .addTypeVariables(typeParameters.stream().map(TypeVariableName::get).collect(Collectors.toList()))
        .addField(FieldSpec.builder(fieldListTypeName, "_fields", Modifier.PRIVATE, Modifier.FINAL)
            .initializer("new $T($L)", fieldListTypeName, fields.size())
            .build())
        .addType(getSchemaBuilderType(classPackage, className, fields, typeParameters))
        .addMethod(MethodSpec.methodBuilder("builder")
            .addTypeVariables(typeParameters.stream().map(TypeVariableName::get).collect(Collectors.toList()))
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .returns(StructUtil.typeQualified(fields.stream()
                .filter(field -> !field._optional)
                .map(field -> schemaBuilderSetterTypeName(classPackage, className, field))
                .findFirst()
                .orElse(StructSchema.completedSchemaBuilderTypeName(classPackage, className)), typeParameters))
            .addStatement("return new Builder$L()", typeParameters.isEmpty() ? "" : "<>")
            .build())
        .addMethod(MethodSpec.methodBuilder("createAccumulator")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override.class)
            .returns(typeQualifiedStructName)
            .addStatement("return new $T()", typeQualifiedStructName)
            .build())
        .addMethod(MethodSpec.methodBuilder("finish")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override.class)
            .returns(typeQualifiedStructName)
            .addParameter(typeQualifiedStructName, "accumulator")
            .addStatement("return accumulator")
            .build())
        .addMethod(MethodSpec.methodBuilder("getFields")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override.class)
            .returns(ParameterizedTypeName.get(ClassName.get(Collection.class), fieldTypeName))
            .addStatement("return _fields")
            .build())
        .build();

  }
}
