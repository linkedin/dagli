package com.linkedin.dagli.annotation.processor.struct;

import com.google.auto.service.AutoService;
import com.linkedin.dagli.annotation.processor.AbstractDagliProcessor;
import com.linkedin.dagli.annotation.processor.ProcessorConstants;
import com.linkedin.dagli.annotation.processor.ProcessorUtil;
import com.linkedin.dagli.annotation.struct.Accessibility;
import com.linkedin.dagli.annotation.struct.HasStructField;
import com.linkedin.dagli.annotation.struct.OptionalField;
import com.linkedin.dagli.annotation.struct.Struct;
import com.linkedin.dagli.annotation.struct.TrivialPublicConstructor;
import com.linkedin.dagli.annotation.struct.VirtualField;
import com.linkedin.dagli.annotation.visitor.VisitedBy;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import com.squareup.javapoet.WildcardTypeName;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;


/**
 * Processes @Struct-annotated classes, auto-generating a new "struct" class based on the fields of the annotated class.
 * See {@link Struct} for details.
 */
@SupportedAnnotationTypes({"com.linkedin.dagli.annotation.struct.Struct"})
//@SupportedSourceVersion(SourceVersion.RELEASE_9)
@AutoService(Processor.class)
public class StructProcessor extends AbstractDagliProcessor {
  // Maps the names of generated @Structs (only those being generated during compilation, not pre-existing) to their
  // corresponding @Struct definition classes
  private HashMap<String, TypeElement> _generatedStructDefinitionClasses = new HashMap<>();

  @Override
  public boolean process(Set<? extends TypeElement> annotations,
      RoundEnvironment roundEnv) {
    try {
      return processImpl(annotations, roundEnv);
    } catch (Exception e) {
      ProcessorUtil.printProcessorError("The @Struct processor", processingEnv, null, e);
      throw e;
    }
  }

  private void processStructElement(TypeElement element) {
    try {
      String classPackage = ProcessorUtil.getPackageName(element, getRawPackageAndClassName(element));
      String className = ProcessorUtil.getClassName(getRawPackageAndClassName(element));

      Set<String> extraOptionals = getClassOptionalFields(element);

      javax.tools.JavaFileObject file = processingEnv.getFiler()
          .createSourceFile(ProcessorUtil.getFullyQualifiedClassName(classPackage, className), element);
      java.io.Writer writer = file.openWriter();
      writer.write(createJavaSource(element, classPackage, className, extraOptionals));
      writer.close();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private Set<String> getClassOptionalFields(TypeElement element) {
    OptionalField[] annotations = element.getAnnotationsByType(OptionalField.class);
    return Arrays.stream(annotations).map(OptionalField::value).collect(Collectors.toSet());
  }

  /**
   * Collect information about each @Struct being generated before code generation commences.
   *
   * @param element the @Struct defintion class element
   */
  private void registerStructDefinitionClass(TypeElement element) {
    String classPackage = ProcessorUtil.getPackageName(element, getRawPackageAndClassName(element));
    String className = ProcessorUtil.getClassName(getRawPackageAndClassName(element));
    _generatedStructDefinitionClasses.put(classPackage + "." + className, element);
  }

  private boolean processImpl(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    List<TypeElement> structDefinitionClasses = roundEnv.getElementsAnnotatedWith(Struct.class)
        .stream()
        .map(elem -> (TypeElement) elem)
        .collect(Collectors.toList());

    structDefinitionClasses.forEach(this::registerStructDefinitionClass);
    structDefinitionClasses.forEach(this::processStructElement);

    return true;
  }

  public static FieldSpec getSerialVersionUIDField(long version) {
    return FieldSpec.builder(long.class, "serialVersionUID")
        .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
        .initializer(Long.toString(version) + "L")
        .build();
  }

  // add special-purpose members, e.g. set up the SCHEMA$ field for a rather picky Avro
  private static void addSpecialMembers(TypeSpec.Builder builder, TypeElement structElement,
      ProcessingEnvironment processingEnv, String structPackage, String structName) {

    // for @VisitedBy @Structs, we need to add an accept(...) method
    VisitedBy visitedByAnnotation = structElement.getAnnotation(VisitedBy.class);
    if (visitedByAnnotation != null) {
      String visitorPackageName = ProcessorUtil.getPackageName(structElement, visitedByAnnotation.value());
      String visitorClassName = ProcessorUtil.getClassName(visitedByAnnotation.value());

      // Unfortunately, it's not possible to vet that the accept(...) method we find has an argument whose type matches
      // our visitor interface; this is because the visitor class may not yet exist and so the argument can appear
      // untyped in this case.  Although we have the option of checking the parameter type when it is available, this
      // creates an undesirable situation where the generated class may vary depending on the order in which the
      // annotation processors run.
      Function<TypeElement, Optional<ExecutableElement>> acceptFinder = (TypeElement type) -> type.getEnclosedElements()
          .stream()
          .filter(elem -> elem.getKind() == ElementKind.METHOD)
          .map(elem -> (ExecutableElement) elem)
          .filter(executable -> executable.getSimpleName().toString().equals("accept"))
          .filter(executable -> !executable.getModifiers().contains(Modifier.PRIVATE) && !executable.getModifiers()
              .contains(Modifier.STATIC) && !executable.getModifiers().contains(Modifier.FINAL))
          .filter(executable -> executable.getParameters().size() == 1)
          .findFirst();

      TypeElement acceptingAncestor =
          ProcessorUtil.findAncestorType(structElement, type -> acceptFinder.apply(type).isPresent());

      boolean override = false;
      Collection<Modifier> modifiers = Collections.emptySet(); // default to package-private
      if (acceptingAncestor != null) {
        ExecutableElement acceptMethod = acceptFinder.apply(acceptingAncestor).get();
        modifiers = ProcessorUtil.getVisibilityModifiers(acceptMethod.getModifiers());
        override = true; // yes, we're creating an override
      }

      MethodSpec.Builder acceptMethod = MethodSpec.methodBuilder("accept")
          .addModifiers(modifiers)
          .addTypeVariable(TypeVariableName.get("R"))
          .returns(TypeVariableName.get("R"))
          .addParameter(
              ParameterizedTypeName.get(ClassName.get(visitorPackageName, visitorClassName), TypeVariableName.get("R")),
              "visitor")
          .addStatement("return visitor.visit(this)");

      if (override) {
        acceptMethod.addAnnotation(Override.class);
      }

      builder.addMethod(acceptMethod.build());
    }

    // For Avro files, we need to re-declare the SCHEMA$ field because Avro tries to read this with
    // getDeclaredField(...) rather than getField(...)
    TypeElement specificRecordTypeElement =
        processingEnv.getElementUtils().getTypeElement("org.apache.avro.specific.SpecificRecord");

    if (specificRecordTypeElement != null && processingEnv.getTypeUtils()
        .isAssignable(structElement.asType(), specificRecordTypeElement.asType())) {
      for (Element member : processingEnv.getElementUtils().getAllMembers(structElement)) {
        if (member.getKind() == ElementKind.FIELD) {
          VariableElement field = (VariableElement) member;
          if (field.getModifiers().contains(Modifier.STATIC) && field.getSimpleName().contentEquals("SCHEMA$")) {
            final String avroPackage = "org.apache.avro";
            final ClassName schemaClassName = ClassName.get(avroPackage, "Schema");
            final ClassName fieldClassName = ClassName.get(avroPackage, "Schema", "Field");

            builder.addMethod(MethodSpec.methodBuilder("getCorrectedAvroSchema$")
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .returns(schemaClassName)
                .addStatement("$T originalSchema = $T.$L", schemaClassName, TypeName.get(structElement.asType()),
                    "SCHEMA$")
                .beginControlFlow("$T<$T> fields = originalSchema.getFields().stream().map(oldField -> ", List.class,
                    fieldClassName)
                .addStatement("$T newField = new $T(oldField.name(), oldField.schema(), oldField.doc(), oldField.defaultVal(), oldField.order())",
                  fieldClassName, fieldClassName)
                .addStatement("oldField.aliases().forEach(newField::addAlias)")
                .addStatement("return newField")
                .endControlFlow(").collect($T.toList())", Collectors.class)
                .addStatement("return $T.createRecord($S, originalSchema.getDoc(), $S, originalSchema.isError(), fields)",
                    schemaClassName, structName, structPackage)
                .build())
                .addField(FieldSpec.builder(TypeName.get(field.asType()), "SCHEMA$")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                    .initializer("$L()", "getCorrectedAvroSchema$")
                    .build());
          }
        }
      }
    }
  }

  private TypeElement getSuperclassTypeElement(TypeElement typeElement) {
    TypeMirror superclass = typeElement.getSuperclass();
    if (superclass.getKind() == TypeKind.NONE) {
      return null;
    }

    TypeElement structElement = _generatedStructDefinitionClasses.get(superclass.toString());
    if (structElement == null && superclass.toString().indexOf('.') < 0) {
      // if the superclass was not found amongst our set of @Struct definitions and there's not a package qualifier
      // already, try prepending our package name and trying again:
      structElement = _generatedStructDefinitionClasses.get(
          processingEnv.getElementUtils().getPackageOf(typeElement).getQualifiedName().toString() + "."
              + superclass.toString());
    }

    return structElement != null ? structElement : (TypeElement) processingEnv.getTypeUtils().asElement(superclass);
  }

  /**
   * Unlike ProcessingEnvironment::getElementUtils()::getAllMembers(...), returns all the members of a class and its
   * superclass(es) in a well-defined order, starting with the first member (as defined in the source code) in the root
   * class (the most distal superclass) and ending with the last member of the provided {@code typeElement} target
   *
   * @param typeElement the type whose members should be returned (will not include superinterfaces)
   * @return a list of members in well-defined order
   */
  private List<Element> getAllMembers(TypeElement typeElement) {
    TypeElement superclass = getSuperclassTypeElement(typeElement);
    List<Element> result = superclass == null ? new ArrayList<>() : getAllMembers(superclass);

    result.addAll(typeElement.getEnclosedElements());

    return result;
  }

  private List<StructField> getFields(TypeElement structElement, Set<String> forcedOptionals) {
    ArrayList<StructField> result = new ArrayList<>();

    for (Element member : getAllMembers(structElement)) {
      if (member instanceof VariableElement) {
        VariableElement field = (VariableElement) member;
        if (field.getModifiers().contains(Modifier.STATIC) || field.getModifiers().contains(Modifier.PRIVATE)
            || field.getModifiers().contains(Modifier.FINAL)) {
          // skip final, static and private members
          continue;
        }

        // the ordering in the list will match the fields' assigned indices
        result.add(new StructField(processingEnv, field,
            forcedOptionals.contains(field.getSimpleName().toString().toLowerCase()), result.size()));
      }
    }

    if (result.isEmpty()) {
      processingEnv.getMessager()
          .printMessage(Diagnostic.Kind.NOTE,
              "Creating a Struct from the class "
                  + structElement.getSimpleName().toString() + " but this class does not have any non-static, "
                  + "non-private, non-final fields.", structElement);
    }
    return result;
  }

  /**
   * Gets all the method-backed "fields" for this @Struct.  These are methods that have been annotated with
   * the @VirtualField attribute.
   *
   * @param structElement the @Struct definition class
   * @param processingEnv the annotation processing environment
   * @return a list of method-backed "fields" for the @Struct
   */
  private static List<StructField> getMethodBackedFields(TypeElement structElement,
      ProcessingEnvironment processingEnv) {
    ArrayList<StructField> result = new ArrayList<>();

    for (Element member : processingEnv.getElementUtils().getAllMembers(structElement)) {
      if (member instanceof ExecutableElement) {
        ExecutableElement method = (ExecutableElement) member;
        VirtualField transformationAnnotation = method.getAnnotation(VirtualField.class);
        if (transformationAnnotation != null) {
          result.add(new StructField(processingEnv, transformationAnnotation.value(), method));
        }
      }
    }

    return result;
  }

  private static String getRawPackageAndClassName(TypeElement structElement) {
    return structElement.getAnnotation(Struct.class).value();
  }

  // returns a list so we can choose to return a method or to skip it
  private static List<MethodSpec> getReducerMethod(String classPackage, String className, StructField field,
      List<? extends TypeParameterElement> typeParameters) {

    if (field.isBackedByMethod()) {
      // the value depends on the struct in some unknown way; can't reduce!
      return Collections.emptyList();
    }

    // first, figure out the return type of the method (this is extremely verbose, unfortunately)
    ClassName transformerType = ClassName.get(classPackage, className, field.getCoreName());
    List<TypeParameterElement> typeParams = field.getRelevantTypeParameters(typeParameters);
    TypeName parameterizedTransformerType = typeParams.isEmpty() ? transformerType
        : ParameterizedTypeName.get(transformerType,
            typeParams.stream().map(TypeVariableName::get).toArray(TypeName[]::new));

    TypeName reducerCollection = ParameterizedTypeName.get(ClassName.get(Collection.class), WildcardTypeName.subtypeOf(
        ParameterizedTypeName.get(StructConstants.REDUCER,
            WildcardTypeName.supertypeOf(parameterizedTransformerType))));

    // now build the method
    return Collections.singletonList(MethodSpec.methodBuilder("getGraphReducers")
        .addModifiers(Modifier.PROTECTED)
        .addAnnotation(Override.class)
        .returns(reducerCollection)
        .addStatement("return $T.singleton(new $T($L, $T.class))", Collections.class,
            StructConstants.INVERSE_CLASS_REDUCER, field._index, ClassName.get(classPackage, className, "Assembled"))
        .build());
  }

  private static TypeName typeQualifiedFieldTransformerName(String classPackage, String className, StructField field,
      List<? extends TypeParameterElement> typeParameters) {
    return StructUtil.typeQualified(ClassName.get(classPackage, className, field.getCoreName()),
        field.getRelevantTypeParameters(typeParameters));
  }

  private static TypeName typeQualifiedStructNameWithWildcards(String classPackage, String className, StructField field,
      List<? extends TypeParameterElement> typeParameters) {
    return StructUtil.typeNameQualified(ClassName.get(classPackage, className),
        field.getWildcardedTypeParameters(typeParameters));
  }

  private static List<TypeSpec> getFieldTypes(String classPackage, String className, List<StructField> fields,
      long version, List<? extends TypeParameterElement> typeParameters) {

    return fields.stream()
        .map(field -> TypeSpec.classBuilder(field.getCoreName())
            .superclass(ParameterizedTypeName.get(StructConstants.ABSTRACT_PREPARED_TRANSFORMER_1,
                typeQualifiedStructNameWithWildcards(classPackage, className, field, typeParameters),
                field.getBoxedTypeName(),
                typeQualifiedFieldTransformerName(classPackage, className, field,
                    field.getRelevantTypeParameters(typeParameters))))
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addTypeVariables(field.getRelevantTypeParameters(typeParameters)
                .stream()
                .map(TypeVariableName::get)
                .collect(Collectors.toList()))
            .addField(getSerialVersionUIDField(version))
            .addMethod(MethodSpec.methodBuilder("withInput")
                .addModifiers(Modifier.PUBLIC)
                .returns(typeQualifiedFieldTransformerName(classPackage, className, field,
                    field.getRelevantTypeParameters(typeParameters)))
                .addParameter(ParameterizedTypeName.get(StructConstants.PRODUCER_INTERFACE, WildcardTypeName.subtypeOf(
                    typeQualifiedStructNameWithWildcards(classPackage, className, field, typeParameters))), "input")
                .addStatement("return withInput1(input)")
                .build())
            .addMethod(MethodSpec.methodBuilder(ProcessorConstants.PRODUCER_HASH_CODE_METHOD_NAME)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(TypeName.INT)
                .addStatement("return $T.hashCodeOfInputs(this)", StructConstants.TRANSFORMER_CLASS)
                .build())
            .addMethod(MethodSpec.methodBuilder(ProcessorConstants.PRODUCER_EQUALS_METHOD_NAME)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addParameter(ClassName.get(classPackage, className, field.getCoreName()), "other")
                .returns(TypeName.BOOLEAN)
                .addStatement("return $T.sameInputs(this, other)", StructConstants.TRANSFORMER_CLASS)
                .build())
            .addMethods(getReducerMethod(classPackage, className, field, typeParameters))
            .addMethod(MethodSpec.methodBuilder("apply")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(field.getBoxedTypeName())
                .addParameter(StructUtil.typeNameQualified(ClassName.get(classPackage, className),
                    field.getWildcardedTypeParameters(typeParameters)), "struct")
                .addStatement("return struct."
                    + (field.isBackedByMethod() ? field._backingMethodName + "()" : field.getFieldName()))
                .build())
            .build())
        .collect(Collectors.toList());
  }

  public static TypeSpec getHelperType(String classPackage, String className, List<StructField> fields,
      List<? extends TypeParameterElement> typeParameters) {
    List<StructField> mandatoryFields = fields.stream().filter(field -> !field._optional).collect(Collectors.toList());
    HashMap<StructField, StructField> nextFieldMap = StructUtil.nextItemMap(mandatoryFields);

    return TypeSpec.classBuilder(StructConstants.HELPER_CLASS_NAME)
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
        .addType(getCompletedFieldType(classPackage, className, fields, typeParameters))
        .addType(StructAssembled.getCompletedAssembledBuilderType(classPackage, className, fields, typeParameters))
        .addType(StructObjectReader.getCompletedReaderBuilderType(classPackage, className, fields, typeParameters))
        .addType(StructSchema.getCompletedSchemaBuilderType(classPackage, className, fields, typeParameters))
        .addTypes(fields.stream()
            .map(field -> TypeSpec.classBuilder(field.getCoreName())
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addType(
                    StructBuilder.getBuilderSetterInterface(classPackage, className, field, nextFieldMap, typeParameters))
                .addType(StructAssembled.getAssembledBuilderSetterInterface(classPackage, className, field, nextFieldMap, typeParameters))
                .addType(
                    StructObjectReader.getReaderBuilderSetterInterface(classPackage, className, field, nextFieldMap, typeParameters))
                .addType(StructSchema.getSchemaBuilderSetterInterface(classPackage, className, field, nextFieldMap, typeParameters))
                .build())
            .collect(Collectors.toList()))
        .build();
  }

  public static TypeSpec getCompletedFieldType(String classPackage, String className, List<StructField> fields,
      List<? extends TypeParameterElement> typeParameters) {
    return TypeSpec.interfaceBuilder(StructConstants.COMPLETED_BUILDER_INTERFACE_NAME)
        .addModifiers(Modifier.PUBLIC)
        .addTypeVariables(typeParameters.stream().map(TypeVariableName::get).collect(Collectors.toList()))
        .addMethod(MethodSpec.methodBuilder("build")
            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
            .returns(StructUtil.typeQualified(ClassName.get(classPackage, className), typeParameters))
            .build())
        .addMethods(fields.stream()
            .filter(field -> field._optional)
            .map(field -> MethodSpec.methodBuilder("set" + field.getCoreName())
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addJavadoc(field._javaDocComment == null ? "" : field._javaDocComment)
                .addParameter(TypeName.get(field._type), field.getVariableName())
                .returns(StructUtil.typeQualified(StructBuilder.completedBuilderTypeName(classPackage, className), typeParameters))
                .build())
            .collect(Collectors.toList()))
        .build();
  }

  private static TypeSpec getPlaceholderType(String classPackage, String className, List<StructField> fields, long version,
      List<? extends TypeParameterElement> typeParameters) {
    return TypeSpec.classBuilder("Placeholder")
        .addTypeVariables(typeParameters.stream().map(TypeVariableName::get).collect(Collectors.toList()))
        .superclass(ParameterizedTypeName.get(StructConstants.DAGLI_PLACEHOLDER,
            StructUtil.typeQualified(ClassName.get(classPackage, className), typeParameters)))
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
        .addField(getSerialVersionUIDField(version))
        .addFields(fields.stream()
            .map(field -> FieldSpec.builder(StructUtil.typeQualified(ClassName.get(classPackage, className, field.getCoreName()),
                field.getRelevantTypeParameters(typeParameters)),
                field.getInternalFieldName(), Modifier.PRIVATE, Modifier.TRANSIENT).initializer("null").build())
            .collect(Collectors.toList()))
        .addMethods(fields.stream()
            .map(field -> {
              List<TypeParameterElement> relevantParameters = field.getRelevantTypeParameters(typeParameters);
              TypeName typeQualifiedFieldClassName =
                  StructUtil.typeQualified(ClassName.get(classPackage, className, field.getCoreName()), relevantParameters);

              return MethodSpec.methodBuilder("as" + field.getCoreName())
                  .addModifiers(Modifier.PUBLIC)
                  .returns(typeQualifiedFieldClassName)
                  .beginControlFlow("if ($L == null)", field.getInternalFieldName())
                  .addStatement("$L = new $T().withInput(this)", field.getInternalFieldName(),
                      typeQualifiedFieldClassName)
                  .endControlFlow()
                  .addStatement("return $L", field.getInternalFieldName())
                  .build();
            })
            .collect(Collectors.toList()))
        .build();
  }

  private static List<MethodSpec> getGetterMethods(List<StructField> fields, ProcessingEnvironment processingEnv) {
    ArrayList<MethodSpec> methods = new ArrayList<>(fields.size());
    for (StructField field : fields) {
      String coreName = field.getCoreName();
      methods.add(MethodSpec.methodBuilder("get" + coreName)
          .addModifiers(Modifier.PUBLIC)
          .returns(TypeName.get(field._type))
          .addStatement("return $L", field.getFieldName())
          .addJavadoc(field._javaDocComment == null ? "" : field._javaDocComment)
          .build());
    }

    return methods;
  }

  private static MethodSpec getToStringMethod(String classPackage, String className, List<StructField> fields) {
    ClassName myType = ClassName.get(classPackage, className);

    return MethodSpec.methodBuilder("toString")
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(Override.class)
        .returns(String.class)
        .addStatement("return \"" + className + "(" + (fields.isEmpty() ? ")\"" : fields.stream()
            .map(field -> field.getCoreName() + " = \" + " + field.getFieldName())
            .collect(Collectors.joining(" + \", \" + \"")) + " + \")\""))
        .build();

  }

  private static MethodSpec getEqualsMethod(String classPackage, String className, List<StructField> fields) {
    ClassName myType = ClassName.get(classPackage, className);

    MethodSpec.Builder equalsBuilder = MethodSpec.methodBuilder("equals")
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(Override.class)
        .addParameter(Object.class, "other")
        .returns(boolean.class);

    if (fields.isEmpty()) {
      equalsBuilder.addStatement("return other != null && other.getClass() == this.getClass()");
    } else {
      equalsBuilder.beginControlFlow("if (other == null || other.getClass() != this.getClass())")
          .addStatement("return false")
          .endControlFlow()
          .addStatement("$T o = ($T) other", myType, myType)
          .addStatement("return " + String.join(" && ", fields.stream()
              .map(field -> TypeName.get(field._type).isPrimitive() ? field.getFieldName() + " == o."
                  + field.getFieldName()
                  : "java.util.Objects.equals(" + field.getFieldName() + ", o." + field.getFieldName() + ")")
              .toArray(String[]::new)));
    }

    return equalsBuilder.build();
  }

  private static MethodSpec getHashCodeMethod(String classPackage, String className, List<StructField> fields) {
    List<String> sortedFields = fields.stream()
        .sorted(Comparator.comparing(StructField::getVariableName))
        .map(field -> field.getFieldName())
        .collect(Collectors.toList());

    return MethodSpec.methodBuilder("hashCode")
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(Override.class)
        .returns(int.class)
        .addStatement("return $T.hash(" + String.join(", ", sortedFields) + ")", Objects.class)
        .build();
  }

  private static MethodSpec getFromMapMethod(String classPackage, String className, List<StructField> fields) {
    ClassName structName = ClassName.get(classPackage, className);
    MethodSpec.Builder methodSpec = MethodSpec.methodBuilder("fromMap")
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
        .addParameter(ParameterizedTypeName.get(ClassName.get(Map.class), WildcardTypeName.subtypeOf(CharSequence.class), ClassName.OBJECT), "map")
        .returns(structName)
        .addStatement("$L res = new $L()", structName, structName);

    fields.stream()
        .filter(field -> !field.isOptional())
        .forEach(field -> methodSpec.addStatement("if (!map.containsKey($S) && !map.containsKey($S)) { throw new $T($S); }",
            field.getVariableName(), field.getCoreName(), NoSuchElementException.class, field.getVariableName()));

    fields.forEach(field -> methodSpec.addStatement("res.$L = ($T) map.getOrDefault($S, map.getOrDefault($S, res.$L))",
        field.getFieldName(), StructUtil.rawGenericType(TypeName.get(field.getTypeUpperBound())),
        field.getVariableName(), field.getCoreName(), field.getFieldName()));

    methodSpec.addStatement("return res");
    return methodSpec.build();
  }

  private static MethodSpec getToMapMethod(List<StructField> fields) {
    ParameterizedTypeName mapType = ParameterizedTypeName.get(HashMap.class, String.class, Object.class);

    MethodSpec.Builder methodSpec = MethodSpec.methodBuilder("toMap")
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(Override.class)
        .returns(ParameterizedTypeName.get(Map.class, String.class, Object.class))
        .addStatement("$T map = new $T($L)", mapType, mapType, fields.size());

    fields.stream()
        .forEach(field -> methodSpec.addStatement("map.put($S, $L)", field.getVariableName(), field.getFieldName()));

    methodSpec.addStatement("return map");
    return methodSpec.build();
  }

  private static MethodSpec methodSpecFromExecutableElement(ProcessingEnvironment env, boolean instanceMethod, ExecutableElement exec) {
    Set<Modifier> filteredModifiers = exec.getModifiers().stream().filter(
        m -> m == Modifier.PUBLIC || m == Modifier.PROTECTED || m == Modifier.SYNCHRONIZED).collect(Collectors.toSet());

    List<VariableElement> params = new ArrayList<>(exec.getParameters());
    List<String> paramNames =
        params.stream().map(VariableElement::getSimpleName).map(CharSequence::toString).collect(Collectors.toList());

    if (!instanceMethod) {
      filteredModifiers.add(Modifier.STATIC);
    } else {
      params.remove(0);
      paramNames.set(0, "this");
    }

    /**
     * Check for ERROR types
     */
    if (exec.getReturnType().getKind() == TypeKind.ERROR && exec.getReturnType().toString().startsWith("<")) {
      throw new IllegalArgumentException("The return type for the method " + exec.getSimpleName().toString()
          + " is of kind 'ERROR', which means Java couldn't resolve the type.  This might not be a problem, but the "
          + "annotation processor was also unable to get the type's name.  A common cause is the use of a "
          + "generic @Struct as the type; try using the struct's type without type parameters.");
    }
    for (VariableElement param : params) {
      if (param.asType().getKind() == TypeKind.ERROR && param.asType().toString().startsWith("<")) {
        throw new IllegalArgumentException("The type of parameter " + param.getSimpleName() + " passed to the method "
            + exec.getSimpleName().toString()
            + " is of kind 'ERROR', which means Java couldn't resolve the type.  This might not be a problem, but the "
            + "annotation processor was also unable to get the type's name.  A common cause is the use of a "
            + "generic @Struct as the type; try using the struct's type without type parameters.");
      }
    }

    return MethodSpec.methodBuilder(exec.getSimpleName().toString())
        .addParameters(params.stream().map(ParameterSpec::get).collect(Collectors.toList()))
        .returns(TypeName.get(exec.getReturnType()))
        .addModifiers(filteredModifiers)
        .varargs(exec.isVarArgs())
        .addExceptions(exec.getThrownTypes().stream().map(TypeName::get).collect(Collectors.toList()))
        .addTypeVariables(exec.getTypeParameters().stream().map(TypeVariableName::get).collect(Collectors.toList()))
        .addStatement((exec.getReturnType().getKind() == TypeKind.VOID ? "" : "return ") + "$T.$L($L)",
            StructUtil.rawGenericType(TypeName.get(exec.getEnclosingElement().asType())), exec.getSimpleName(),
            String.join(", ", paramNames))
        .build();
  }

  private static List<MethodSpec> getWithMethods(String classPackage, String className, List<StructField> fields,
      List<? extends TypeParameterElement> typeParameters) {
    TypeName tqStructName = StructUtil.typeQualified(ClassName.get(classPackage, className), typeParameters);

    return fields.stream()
        .map(field -> MethodSpec.methodBuilder("with" + field.getCoreName())
            .addModifiers(Modifier.PUBLIC)
            .addParameter(TypeName.get(field._type), field.getVariableName())
            .returns(tqStructName)
            .addJavadoc(field._javaDocComment == null ? "" : field._javaDocComment)
            .beginControlFlow("try")
            .addStatement("$T res = ($T) this.clone()", tqStructName, tqStructName)
            .addStatement("res.$L = $L", field.getFieldName(), field.getVariableName())
            .addStatement("return res")
            .nextControlFlow("catch ($T e)", CloneNotSupportedException.class)
            .addStatement("throw new RuntimeException(e)")
            .endControlFlow()
            .build())
        .collect(Collectors.toList());
  }

  private static boolean shouldCreateTrivialPublicConstructor(TypeElement structElement,
      ProcessingEnvironment processingEnv, List<StructField> fields) {
    // Externalizable *requires* a no-arg public constructor
    TypeElement externalizable = processingEnv.getElementUtils().getTypeElement("java.io.Externalizable");
    if (externalizable != null && processingEnv.getTypeUtils()
        .isAssignable(structElement.asType(), externalizable.asType())) {
      return true;
    }

    if (structElement.getAnnotation(TrivialPublicConstructor.class) != null) {
      if (!fields.stream().allMatch(field -> field.isOptional())) {
        processingEnv.getMessager()
            .printMessage(Diagnostic.Kind.ERROR,
                "The @Struct definition is annotated with @TrivialPublicConstructor but has non-optional fields.  "
                    + "Generating a trivial public constructor requires that the @Structs have only optional fields.",
                structElement);
      }
      return true;
    }

    return false;
  }

  private String createJavaSource(TypeElement structElement, String classPackage, String className,
      Set<String> extraOptionals) {
    List<StructField> realFields = getFields(structElement, extraOptionals);
    List<StructField> methodBackedFields = getMethodBackedFields(structElement, processingEnv);

    List<StructField> allFields = new ArrayList<>(realFields.size() + methodBackedFields.size());
    allFields.addAll(realFields);
    allFields.addAll(methodBackedFields);

    List<? extends TypeParameterElement> typeParameters = structElement.getTypeParameters();

    boolean isSerializable = isSerializable(structElement, processingEnv);
    long version = isSerializable ? getSerializationUID(structElement, processingEnv) : 0;

//    MethodSpec builderMethod = MethodSpec.methodBuilder("builder")
//        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
//        .addTypeVariables(typeParameters.stream().map(TypeVariableName::get).collect(Collectors.toList()))
//        .returns(StructUtil.typeQualified(realFields.stream()
//            .filter(field -> !field._optional)
//            .map(field -> StructBuilder.builderSetterTypeName(classPackage, className, field))
//            .findFirst()
//            .orElse(StructBuilder.completedBuilderTypeName(classPackage, className)), typeParameters))
//        .addStatement("return new Builder$L()", typeParameters.isEmpty() ? "" : "<>")
//        .build();

    Modifier[] accessModifiers;
    Accessibility.Level accessibilityLevel = getAccessibility(structElement);
    switch (accessibilityLevel) {
      case PUBLIC:
        accessModifiers = new Modifier[] { Modifier.PUBLIC };
        break;
      case PACKAGE_PRIVATE:
        accessModifiers = new Modifier[0];
        break;
      default:
        throw new IllegalArgumentException("Unknown accessibility level: " + accessibilityLevel);
    }

    TypeSpec.Builder structTypeSpecBuilder = TypeSpec.classBuilder(className)
        .addModifiers(accessModifiers)
        .addTypeVariables(typeParameters.stream().map(TypeVariableName::get).collect(Collectors.toList()))
        //.superclass(TypeName.get(structElement.asType()))
        //.addSuperinterface(Cloneable.class)
        .addSuperinterface(Cloneable.class)
        .addSuperinterface(com.linkedin.dagli.struct.Struct.class)
        .addAnnotations(realFields.stream()
            .map(field -> AnnotationSpec.builder(HasStructField.class)
                .addMember("optional", "$L", field.isOptional())
                .addMember("name", "$S", field.getVariableName())
                .addMember("type", "$T.class", StructUtil.rawGenericType(TypeName.get(field.getTypeUpperBound())))
                .build())
            .collect(Collectors.toList()))
        .addType(StructBuilder.getBuilderInterface(classPackage, className, realFields, typeParameters))
        .addType(getPlaceholderType(classPackage, className, allFields, version, typeParameters))
        .addType(StructBuilder.getBuilderType(classPackage, className, realFields, typeParameters))
        .addTypes(getFieldTypes(classPackage, className, allFields, version, typeParameters))
        .addType(getHelperType(classPackage, className, realFields, typeParameters))
        .addType(StructAssembled.getAssembledType(classPackage, className, realFields, version, typeParameters))
        .addType(StructObjectReader.getReaderType(classPackage, className, realFields, version, typeParameters))
        .addType(StructSchema.getRowSchemaType(classPackage, className, realFields, version, typeParameters))
        .addMethod(getToStringMethod(classPackage, className, realFields))
        .addMethod(getEqualsMethod(classPackage, className, realFields))
        .addMethod(getHashCodeMethod(classPackage, className, realFields))
        .addMethods(getGetterMethods(realFields, processingEnv))
        .addMethod(getFromMapMethod(classPackage, className, realFields))
        .addMethod(getToMapMethod(realFields))
        //.addMethod(builderMethod)
        .addMethods(getWithMethods(classPackage, className, realFields, typeParameters));

    addSpecialMembers(structTypeSpecBuilder, structElement, processingEnv, classPackage, className);

    // if serializable, make sure we set the serialization version field
    if (isSerializable) {
      structTypeSpecBuilder.addField(getSerialVersionUIDField(version));
    }

    // creating the constructor as "protected" will allow other @Structs to extend this one
    structTypeSpecBuilder.addMethod(MethodSpec.constructorBuilder()
        .addModifiers(shouldCreateTrivialPublicConstructor(structElement, processingEnv, realFields) ? Modifier.PUBLIC
            : Modifier.PROTECTED)
        .addStatement("super()")
        .build());

    structTypeSpecBuilder.superclass(TypeName.get(structElement.asType()));

    return JavaFile.builder(classPackage, structTypeSpecBuilder.build()).build().toString();
  }

  private static Accessibility.Level getAccessibility(TypeElement typeElement) {
    Accessibility[] accessAnnotations = typeElement.getAnnotationsByType(Accessibility.class);
    if (accessAnnotations.length == 0) {
      return Accessibility.Level.PUBLIC;
    } else {
      return accessAnnotations[0].value();
    }
  }

  private static boolean isSerializable(TypeElement typeElement, ProcessingEnvironment processingEnv) {
    TypeMirror serializable = processingEnv.getElementUtils().getTypeElement("java.io.Serializable").asType();
    return processingEnv.getTypeUtils().isAssignable(typeElement.asType(), serializable);
  }

  private static long getSerializationUID(TypeElement typeElement, ProcessingEnvironment processingEnv) {
    Optional<? extends Element> serializationIDMatch = typeElement.getEnclosedElements()
        .stream()
        .filter(e -> e instanceof VariableElement && ((VariableElement) e).asType().getKind() == TypeKind.LONG
            && ((VariableElement) e).getSimpleName().contentEquals("serialVersionUID")).findAny();

    // make sure the struct def has a serialVersionUID defined
    if (!serializationIDMatch.isPresent()) {
      processingEnv.getMessager()
          .printMessage(Diagnostic.Kind.WARNING,
              "The @Struct attribute has been applied to Serializable class "
                  + typeElement.getSimpleName().toString() + " but this class does not have a serialVersionUID field."
                  + "  A version ID helps prevent logic bugs when the struct definition changes."
                  + "  Please define it in this class by defining the field"
                  + " 'private static final long serialVersionUID = 1;'",
              typeElement);
      return 0;
    }

    VariableElement serializationIDElement = (VariableElement) serializationIDMatch.get();
    if (serializationIDElement.getConstantValue() == null) {
      processingEnv.getMessager()
          .printMessage(
              Diagnostic.Kind.ERROR,
              "The @Struct attribute has been applied to "
                  + typeElement.getSimpleName().toString()
                  + " but this class does not assign a constant value to the serialVersionUID field.  This field is "
                  + "necessary for consistent serialization.  Please define a constant value to this field, e.g., "
                  + "'private static final long serialVersionUID = 1;'", typeElement);
    }

    return (long) serializationIDElement.getConstantValue();
  }
}