package com.linkedin.dagli.annotation.processor.visitor;

import com.google.auto.service.AutoService;
import com.linkedin.dagli.annotation.processor.AbstractDagliProcessor;
import com.linkedin.dagli.annotation.processor.ProcessorUtil;
import com.linkedin.dagli.annotation.struct.Struct;
import com.linkedin.dagli.annotation.visitor.VisitedBy;
import com.linkedin.dagli.annotation.visitor.Visitor;
import com.linkedin.dagli.annotation.visitor.Visitors;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;


/**
 * Processes classes annotated with {@link VisitedBy}, auto-generating new
 * visitor interfaces with visit(...) methods corresponding to these classes.
 * See {@link VisitedBy} for details.
 */
@SupportedAnnotationTypes({"com.linkedin.dagli.annotation.visitor.VisitedBy",
    "com.linkedin.dagli.annotation.visitor.Visitor", "com.linkedin.dagli.annotation.visitor.Visitors"})
//@SupportedSourceVersion(SourceVersion.RELEASE_9)
@AutoService(Processor.class)
public class VisitedByProcessor extends AbstractDagliProcessor {
  // map from fully-qualified visitor names to Visitor annotations that configure them
  private HashMap<String, Visitor> _visitorConfigs = new HashMap<>();

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    // first, look for visitor configurations on packages
    processVisitorAnnotatedPackages(roundEnv.getElementsAnnotatedWith(Visitor.class));
    processVisitorAnnotatedPackages(roundEnv.getElementsAnnotatedWith(Visitors.class));

    // next, sort the annotated visitee classes by their to-be-created visitor interface types
    HashMap<String, ArrayList<TypeElement>> visitees = new HashMap<>();
    for (Element element : roundEnv.getElementsAnnotatedWith(VisitedBy.class)) {
      TypeElement type = (TypeElement) element;
      visitees.computeIfAbsent(ProcessorUtil.getFullyQualifiedClassName(type, getRawPackageAndClassName(type)),
          val -> new ArrayList<>()).add(type);
    }

    visitees.values().forEach(this::createVisitor);

    return true;
  }

  private void processVisitorAnnotatedPackages(Collection<? extends Element> elements) {
    for (Element element : elements) {
      PackageElement pkg = (PackageElement) element;
      for (Visitor visitorAnnotation : pkg.getAnnotationsByType(Visitor.class)) {
        _visitorConfigs.put(
            ProcessorUtil.getFullyQualifiedClassName(pkg.getQualifiedName().toString(), visitorAnnotation.name()),
            visitorAnnotation);
      }
    }
  }

  private void createVisitor(ArrayList<TypeElement> visitees) {
    String rawClassAndPackageName = getRawPackageAndClassName(visitees.get(0));
    String classPackage = ProcessorUtil.getPackageName(visitees.get(0), rawClassAndPackageName);
    String className = ProcessorUtil.getClassName(rawClassAndPackageName);

    try {
      javax.tools.JavaFileObject file = processingEnv.getFiler()
        .createSourceFile(ProcessorUtil.getFullyQualifiedClassName(classPackage, className), visitees.toArray(new Element[0]));
      java.io.Writer writer = file.openWriter();
      writer.write(createJavaSource(visitees, processingEnv, classPackage, className));
      writer.close();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private String createJavaSource(ArrayList<TypeElement> visitees, ProcessingEnvironment processingEnv,
      String classPackage, String className) {

    // find an available type variable name for the return type of the visitors (in case "R" is already in use)
    Set<String> usedTypeNames = visitees.stream()
        .flatMap(visitee -> visitee.getTypeParameters().stream())
        .map(typeParam -> typeParam.getSimpleName().toString())
        .collect(Collectors.toSet());

    String resultTypeName = "R";
    while (usedTypeNames.contains(resultTypeName)) {
      resultTypeName += "R"; // RRRRRR....
    }

    Visitor visitorAnnotation = _visitorConfigs.get(ProcessorUtil.getFullyQualifiedClassName(classPackage, className));

    // define the visitor builder
    TypeSpec.Builder visitorBuilder = TypeSpec.interfaceBuilder(className)
        .addTypeVariable(TypeVariableName.get(resultTypeName))
        .addJavadoc("An interface for a visitor that may be used to implement the Visitor Pattern.  It was "
            + "automatically generated via the @VisitedBy annotation.\n\n"
            + "@param <" + resultTypeName + "> the type of the result returned by this visitor's methods\n");

    if (visitorAnnotation == null || visitorAnnotation.isPublic()) {
      visitorBuilder.addModifiers(Modifier.PUBLIC);
    }

    // iterate over all the visitee types to create visit(...) methods
    for (TypeElement visitee : visitees) {
      VisitedBy visitedBy = visitee.getAnnotation(VisitedBy.class);

      MethodSpec.Builder methodSpec = MethodSpec.methodBuilder("visit")
          .addModifiers(Modifier.PUBLIC) // interface method
          .addTypeVariables(
              visitee.getTypeParameters().stream().map(TypeVariableName::get).collect(Collectors.toList()))
          .addParameter(getVisiteeType(visitee), "visited")
          .returns(TypeVariableName.get(resultTypeName))
          .addJavadoc("Processes a {@link $L} instance.\n\n"
              + "@param visited the visited instance\n"
              + "@return the result of processing this instance\n", getVisiteeType(visitee));

      if (visitedBy.throwIfUnimplemented()) {
        methodSpec.addModifiers(Modifier.DEFAULT);
        methodSpec.addStatement("throw new $T()", UnsupportedOperationException.class);
      } else {
        methodSpec.addModifiers(Modifier.ABSTRACT);
      }

      visitorBuilder.addMethod(methodSpec.build());
    }

    return JavaFile.builder(classPackage, visitorBuilder.build()).build().toString();
  }

  private static TypeName getVisiteeType(TypeElement visitee) {
    Struct structAnnotation = visitee.getAnnotation(Struct.class);
    if (structAnnotation == null) {
      // this isn't a @Struct--the visitee is just the annotated type
      return TypeName.get(visitee.asType());
    } else {
      // this *is* a @Struct--the visitee is the class generated from @Struct definition which has been annotated by
      // @VisitedBy
      String rawClassName = structAnnotation.value();
      return ClassName.get(ProcessorUtil.getPackageName(visitee, rawClassName),
          ProcessorUtil.getClassName(rawClassName));
    }
  }

  private static String getRawPackageAndClassName(TypeElement structElement) {
    return structElement.getAnnotation(VisitedBy.class).value();
  }
}