package com.linkedin.dagli.annotation.processor.producer;

import com.google.auto.service.AutoService;
import com.linkedin.dagli.annotation.equality.HandleEquality;
import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.annotation.processor.AbstractDagliProcessor;
import com.linkedin.dagli.annotation.processor.ProcessorConstants;
import com.linkedin.dagli.annotation.producer.internal.IsAbstractProducer;
import java.util.Set;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;

/**
 * Checks for the correctness of classes derived from AbstractProducer (which have the inherited
 * {@link IsAbstractProducer} annotation), including the use of the equality semantics annotations.
 */
@SupportedAnnotationTypes({"com.linkedin.dagli.annotation.producer.internal.IsAbstractProducer",
    "com.linkedin.dagli.annotation.equality.ValueEquality", "com.linkedin.dagli.annotation.equality.HandleEquality"})
//@SupportedSourceVersion(SourceVersion.RELEASE_9)
@AutoService(Processor.class)
public class IsAbstractProducerProcessor extends AbstractDagliProcessor {
  @Override
  public boolean process(Set<? extends TypeElement> annotations,
      RoundEnvironment roundEnv) {
    for (Element element : roundEnv.getElementsAnnotatedWith(ValueEquality.class)) {
      checkDerivesFromAbstractProducer((TypeElement) element, "is annotated with @ValueEquality");
      checkNoCustomEqualsImplementation((TypeElement) element, "is annotated with @ValueEquality");
    }

    for (Element element : roundEnv.getElementsAnnotatedWith(HandleEquality.class)) {
      checkDerivesFromAbstractProducer((TypeElement) element, "is annotated with @HandleEquality");
      checkNoCustomEqualsImplementation((TypeElement) element, "is annotated with @HandleEquality");
    }

    for (Element element : roundEnv.getElementsAnnotatedWith(IsAbstractProducer.class)) {
      TypeElement type = (TypeElement) element;

      checkDerivesFromAbstractProducer(type, "is annotated with @IsAbstractProducer");

      boolean hasValueEquality = type.getAnnotation(ValueEquality.class) != null;
      boolean hasHandleEquality = type.getAnnotation(HandleEquality.class) != null;

      if (!type.getModifiers().contains(Modifier.ABSTRACT) && !hasValueEquality && !hasHandleEquality
          && getAncestorWithCustomEqualsImplementation(type) == null) {
        processingEnv.getMessager()
            .printMessage(Diagnostic.Kind.NOTE, type.getQualifiedName() + " does not have a @ValueEquality or "
                + "@HandleEquality annotation, and does not override computeEqualsUnsafe() and computeHashCode(); "
                + "Dagli will assume @ValueEquality semantics.", type);
      }

      if (hasHandleEquality && hasValueEquality) {
        processingEnv.getMessager()
            .printMessage(Diagnostic.Kind.ERROR, type.getQualifiedName() + " is annotated with both "
                + "@ValueEquality and @HandleEquality; these annotations are mutually exclusive.", type);
      }
    }

    return true; // we're the only processor for this annotation
  }

  void checkDerivesFromAbstractProducer(TypeElement type, String whyItShouldDeriveFromAbstractProducer) {
    TypeElement ancestor = type;
    while (!ancestor.getQualifiedName().contentEquals(ProducerConstants.ABSTRACT_PRODUCER_CLASS_NAME)) {
      TypeMirror parentTypeMirror = ancestor.getSuperclass();

      // if we hit exhaust our ancestors before finding AbstractProducer, we fail the check
      if (parentTypeMirror.getKind() == TypeKind.NONE) {
        processingEnv.getMessager()
            .printMessage(Diagnostic.Kind.ERROR, type.getQualifiedName()
                + " " + whyItShouldDeriveFromAbstractProducer + " but does not derive from AbstractProducer", type);
        break;
      }

      ancestor = (TypeElement) ((DeclaredType) parentTypeMirror).asElement();
    }
  }

  private void checkNoCustomEqualsImplementation(TypeElement type,
      String whyThereShouldBeNoCustomEqualsImplementation) {
    TypeElement typeElementWithCustomImplementation = getAncestorWithCustomEqualsImplementation(type);

    if (typeElementWithCustomImplementation != null) {
      processingEnv.getMessager()
          .printMessage(Diagnostic.Kind.ERROR,
              type.getQualifiedName() + " " + whyThereShouldBeNoCustomEqualsImplementation + ", but " + (
                  type.equals(typeElementWithCustomImplementation) ? "it"
                      : ("an ancestor, " + typeElementWithCustomImplementation.getSimpleName())) + " defines "
                  + ProcessorConstants.PRODUCER_EQUALS_METHOD_NAME + " or "
                  + ProcessorConstants.PRODUCER_HASH_CODE_METHOD_NAME, type);
    }
  }

  /**
   * Gets the {@link TypeElement} of the ancestor (which may be the passed type) that has an implementation of
   * computeEqualsUnsafe() or computeHashCode().  If no ancestor (other than AbstractProducer) has such an
   * implementation, null is returned.
   *
   * @param type the type to check
   * @return null if no custom implementation exists, otherwise the {@link TypeElement} of the most-derived ancestor
   *         with such an implementation
   */
  private TypeElement getAncestorWithCustomEqualsImplementation(TypeElement type) {
    TypeElement ancestor = type;
    while (!ancestor.getQualifiedName().contentEquals(ProducerConstants.ABSTRACT_PRODUCER_CLASS_NAME)) {
      if (hasEqualsOrHashCodeOverrides(ancestor)) {
        return ancestor;
      }

      ancestor = (TypeElement) ((DeclaredType) ancestor.getSuperclass()).asElement();
    }

    return null; // loop terminated because we hit AbstractProducer
  }


  private boolean hasEqualsOrHashCodeOverrides(TypeElement type) {
    return (type.getEnclosedElements()
        .stream()
        .filter(member -> member instanceof ExecutableElement)
        .anyMatch(member ->  ProducerConstants.EQUALITY_METHODS.contains(member.getSimpleName().toString())));
  }
}