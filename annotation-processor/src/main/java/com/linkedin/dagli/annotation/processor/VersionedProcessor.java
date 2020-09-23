package com.linkedin.dagli.annotation.processor;

import com.google.auto.service.AutoService;
import com.linkedin.dagli.annotation.Versioned;
import java.io.Serializable;
import java.util.Objects;
import java.util.Set;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;

@SupportedAnnotationTypes("com.linkedin.dagli.annotation.Versioned")
//@SupportedSourceVersion(SourceVersion.RELEASE_9)
@AutoService(Processor.class)
public class VersionedProcessor extends AbstractDagliProcessor {

  private void checkRoot(final Element root, final TypeMirror serializableType) {
    Objects.requireNonNull(root);
    Objects.requireNonNull(serializableType);

    TypeElement typeElement = (TypeElement) root;

    if (root.getKind() != ElementKind.CLASS) {
      processingEnv.getMessager()
          .printMessage(Diagnostic.Kind.ERROR,
              "@Versioned annotation has been applied to non-class type " + root.getSimpleName()
                  + "  This attribute is only applicable to declared classes.", root);

    } else if (!processingEnv.getTypeUtils().isSubtype(typeElement.asType(), serializableType)) {
      processingEnv.getMessager()
          .printMessage(Diagnostic.Kind.ERROR,
              "@Versioned annotation has been applied to non-serializable type " + typeElement.getSimpleName()
                  + " or one of its superclasses.  You should either remove the @Versioned annotation if this "
                  + "type is not meant to be serializable, or implement the java.io.Serializable interface.",
              typeElement);
    } else if (typeElement.getEnclosedElements()
        .stream()
        .noneMatch(e -> e instanceof VariableElement && ((VariableElement) e).asType().getKind() == TypeKind.LONG
            && ((VariableElement) e).getSimpleName().contentEquals("serialVersionUID"))) {
      boolean strict = typeElement.getAnnotation(Versioned.class).strict();
      processingEnv.getMessager()
          .printMessage(strict ? Diagnostic.Kind.ERROR : Diagnostic.Kind.WARNING,
              "The @Versioned attribute has been applied to " + typeElement.getSimpleName().toString()
                  + " or one of its superclasses, but it does not have a serialVersionUID field.  This field is "
                  + "necessary for consistent serialization.  Please define it on your class, e.g. "
                  + "'private static final long serialVersionUID = 1;'", typeElement);
    }
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations,
      RoundEnvironment roundEnv) {
    TypeMirror serializableType =
        processingEnv.getElementUtils().getTypeElement(Serializable.class.getCanonicalName()).asType();

    for (Element root : roundEnv.getElementsAnnotatedWith(Versioned.class)) {
      try {
        checkRoot(root, serializableType);
      } catch (Exception e) {
        ProcessorUtil.printProcessorError("The @Versioned processor", processingEnv, root, e);
        throw e;
      }
    }

    return true;
  }
}