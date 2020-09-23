package com.linkedin.dagli.annotation.processor;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;


/**
 * Shared static methods for Dagli annotation processing.
 */
public abstract class ProcessorUtil {
  private ProcessorUtil() { }

  private static final List<Modifier> VISIBILITY_MODIFIERS =
      Arrays.asList(Modifier.PUBLIC, Modifier.PRIVATE, Modifier.PROTECTED);

  /**
   * Checks if the specified {@link TypeElement} is the same one specified by the given package and class name.
   *
   * @param type the type to check
   * @param packageName the package name
   * @param className the unqualified class name
   * @return true if they are the same, false if they are not
   */
  public static boolean isSameType(TypeMirror type, String packageName, String className) {
    if (type.getKind() != TypeKind.DECLARED) {
      return false;
    }

    Element element = ((DeclaredType) type).asElement();

    if (element instanceof TypeElement) {
      return isSameType((TypeElement) element, packageName, className);
    } else {
      return false; // unclear if this branch will ever be reached
    }
  }

  /**
   * Checks if the specified {@link TypeElement} is the same one specified by the given package and class name.
   *
   * @param type the type to check
   * @param packageName the package name
   * @param className the unqualified class name
   * @return true if they are the same, false if they are not
   */
  public static boolean isSameType(TypeElement type, String packageName, String className) {
    return type.getSimpleName().toString().equals(className) && getPackageFromElement(type).equals(packageName);
  }

  public static String getPackageFromElement(Element element) {
    while (element.getKind() != ElementKind.PACKAGE) {
      element = element.getEnclosingElement();
    }

    return ((PackageElement) element).getQualifiedName().toString();
  }

  /**
   * Given a class name that may or may not be fully qualified, gets the package name if available, and otherwise
   * returns the package of the provided element.  This method assumes the name refers to a top-level type.
   *
   * @param element the element whose package will serve as the default package
   * @param packageAndClassName a possibly fully-qualified class name, or just a class name
   * @return the package name
   */
  public static String getPackageName(TypeElement element, String packageAndClassName) {
    final int lastDot = packageAndClassName.lastIndexOf('.');

    if (lastDot < 0) {
      return getPackageFromElement(element);
    } else {
      return packageAndClassName.substring(0, lastDot);
    }
  }

  /**
   * Given a class name that may or may not be fully qualified, gets the class name.  This method assumes the name
   * refers to a top-level type.
   *
   * @param packageAndClassName the class name that may or may not be fully qualified
   * @return the class name (without package qualification)
   */
  public static String getClassName(String packageAndClassName) {
    final int lastDot = packageAndClassName.lastIndexOf('.');

    if (lastDot < 0) {
      return packageAndClassName;
    } else {
      return packageAndClassName.substring(lastDot + 1);
    }
  }

  /**
   * Given an (unqualified) class name and a package name, returns the fully-qualified class name.
   *
   * @param packageName the package name
   * @param className the unqualified class name
   * @return a fully-qualified class name
   */
  public static String getFullyQualifiedClassName(String packageName, String className) {
    return packageName + (packageName.isEmpty() ? "" : ".") + className;
  }

  /**
   * Gets a fully-qualified class name from a possibly-qualified class name, using the provided element's package
   * if the possibly-qualified class name does not include one.
   *
   * @param element an element whose package will be used as the default
   * @param possiblyQualifiedClassName a possibly-qualified class name
   * @return a fully-qualified class name
   */
  public static String getFullyQualifiedClassName(TypeElement element, String possiblyQualifiedClassName) {
    return getFullyQualifiedClassName(getPackageName(element, possiblyQualifiedClassName),
        getClassName(possiblyQualifiedClassName));
  }

  /**
   * Searches the ancestors of the provided derived type (including superclasses and interfaces), returning the first
   * type found that satisfies the given predicate.  If no matching type is found, null is returned.
   *
   * @param derived the starting point of the search: the type whose ancestors will be checked
   * @param predicate a predicate used to check each ancestor; the first ancestor found for which the predicate returns
   *                  true will be returned
   * @return the first ancestor type found satisfying the given predicate, or null if no ancestor is satisfactory
   */
  public static TypeElement findAncestorType(TypeElement derived, Predicate<TypeElement> predicate) {
    if (predicate.test(derived)) {
      return derived;
    }
    javax.lang.model.type.TypeMirror superclass = derived.getSuperclass();
    if (superclass.getKind() == TypeKind.DECLARED) {
      TypeElement result = findAncestorType((TypeElement) ((DeclaredType) superclass).asElement(), predicate);
      if (result != null) {
        return result;
      }
    }

    for (javax.lang.model.type.TypeMirror iface : derived.getInterfaces()) {
      TypeElement result = findAncestorType((TypeElement) ((DeclaredType) iface).asElement(), predicate);
      if (result != null) {
        return result;
      }
    }

    return null;
  }

  public static List<Modifier> getVisibilityModifiers(Collection<Modifier> modifiers) {
    return modifiers.stream().filter(VISIBILITY_MODIFIERS::contains).collect(Collectors.toList());
  }

  /**
   * Generates a compiler error corresponding to a caught exception in the processor logic
   * @param processorName the name of the processor
   * @param processingEnv the processing environment
   * @param element the element being processed when the error occurred
   * @param e the exception
   */
  public static void printProcessorError(String processorName, ProcessingEnvironment processingEnv, Element element,
      Exception e) {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    e.printStackTrace(pw);

    if (element == null) {
      processingEnv.getMessager()
          .printMessage(Diagnostic.Kind.ERROR,
              processorName + " encountered an exception: " + e.getMessage()
                  + " with stack trace: " + sw.toString());
    } else {
      processingEnv.getMessager()
          .printMessage(Diagnostic.Kind.ERROR,
              processorName + " encountered an exception while processing " + element + ": " + e.getMessage()
                  + " with stack trace: " + sw.toString(), element);
    }
  }
}
