package com.linkedin.dagli.annotation.processor.equality;

import com.google.auto.service.AutoService;
import com.linkedin.dagli.annotation.equality.DeepArrayValueEquality;
import com.linkedin.dagli.annotation.processor.AbstractDagliProcessor;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.tools.Diagnostic;


/**
 * Enforces the restrictions on the {@link DeepArrayValueEquality} annotation.
 */
@SupportedAnnotationTypes("com.linkedin.dagli.annotation.equality.DeepArrayValueEquality")
//@SupportedSourceVersion(SourceVersion.RELEASE_9)
@AutoService(Processor.class)
public class DeepArrayValueEqualityProcessor extends AbstractDagliProcessor {
  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    List<TypeElement> arrayCompatibleTypes =
        Arrays.asList(processingEnv.getElementUtils().getTypeElement(Object.class.getName()),
            processingEnv.getElementUtils().getTypeElement(Serializable.class.getName()),
            processingEnv.getElementUtils().getTypeElement(Cloneable.class.getName()));

    for (Element element : roundEnv.getElementsAnnotatedWith(DeepArrayValueEquality.class)) {
      VariableElement field = (VariableElement) element;
      if (!canContainArray(arrayCompatibleTypes, field.asType())) {
        processingEnv.getMessager()
            .printMessage(Diagnostic.Kind.ERROR, field.getSimpleName()
                + " is annotated as @DeepArrayValueEquality, but is typed such that it cannot contain an array", field);
      }
    }

    return true;
  }

  private static boolean canContainArray(List<TypeElement> arrayCompatibleTypes, TypeMirror type) {
    switch (type.getKind()) {
      case ARRAY:
        return true;
      case DECLARED:
        return arrayCompatibleTypes.contains(((DeclaredType) type).asElement());
      case TYPEVAR:
        return canContainArray(arrayCompatibleTypes, ((TypeVariable) type).getUpperBound());
      default:
        return false;
    }
  }
}