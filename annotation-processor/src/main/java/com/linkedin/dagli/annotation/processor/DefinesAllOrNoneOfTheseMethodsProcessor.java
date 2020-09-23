package com.linkedin.dagli.annotation.processor;

import com.google.auto.service.AutoService;
import com.linkedin.dagli.annotation.DefinesAllOrNoneOfTheseMethods;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;


/**
 * Enforces the {@link com.linkedin.dagli.annotation.DefinesAllOrNoneOfTheseMethods} annotation.
 */
@SupportedAnnotationTypes("com.linkedin.dagli.annotation.DefinesAllOrNoneOfTheseMethods")
//@SupportedSourceVersion(SourceVersion.RELEASE_9)
@AutoService(Processor.class)
public class DefinesAllOrNoneOfTheseMethodsProcessor extends AbstractDagliProcessor {
  @Override
  public boolean process(Set<? extends TypeElement> annotations,
      RoundEnvironment roundEnv) {
    for (Element element : roundEnv.getElementsAnnotatedWith(DefinesAllOrNoneOfTheseMethods.class)) {
      TypeElement type = (TypeElement) element;
      String[] allOrNoneMethodNames = type.getAnnotation(DefinesAllOrNoneOfTheseMethods.class).value();
      ArrayList<String> allOrNoneMethodNameList = new ArrayList<>(Arrays.asList(allOrNoneMethodNames));

      allOrNoneMethodNameList.removeAll(type.getEnclosedElements()
          .stream()
          .filter(member -> member instanceof ExecutableElement)
          .map(member -> ((ExecutableElement) member).getSimpleName().toString())
          .collect(Collectors.toList()));

      if (!allOrNoneMethodNameList.isEmpty() && allOrNoneMethodNameList.size() < allOrNoneMethodNames.length) {
        processingEnv.getMessager()
            .printMessage(Diagnostic.Kind.ERROR,
                type.getQualifiedName() + " must define either none of these methods or all of them: " + String.join(
                    ", ", allOrNoneMethodNames) + " (missing " + String.join(", ", allOrNoneMethodNameList) + ")",
                type);
      }
    }

    return true;
  }
}