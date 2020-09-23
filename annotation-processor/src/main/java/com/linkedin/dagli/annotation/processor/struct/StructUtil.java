package com.linkedin.dagli.annotation.processor.struct;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeVariableName;
import java.util.HashMap;
import java.util.List;
import javax.lang.model.element.TypeParameterElement;

/**
 * Static utility class that supports @Struct annotation processing,
 * specifically generic/common utility methods that defy more specific categorization.
 */
class StructUtil {
  private StructUtil() { }

  public static ClassName helperTypeName(String classPackage, String className) {
    return ClassName.get(classPackage, className, StructConstants.HELPER_CLASS_NAME);
  }

  public static TypeName typeQualified(ClassName className, List<? extends TypeParameterElement> typeParameters) {
    if (typeParameters.isEmpty()) {
      return className;
    }

    return ParameterizedTypeName.get(className,
        typeParameters.stream().map(TypeVariableName::get).toArray(TypeName[]::new));
  }

  public static TypeName typeNameQualified(ClassName className, List<TypeName> typeParameters) {
    if (typeParameters.isEmpty()) {
      return className;
    }

    return ParameterizedTypeName.get(className, typeParameters.toArray(new TypeName[typeParameters.size()]));
  }

  public static <T> HashMap<T, T> nextItemMap(List<T> items) {
    HashMap<T, T> result = new HashMap<>(items.size());

    if (!items.isEmpty()) {
      for (int i = 0; i < items.size() - 1; i++) {
        result.put(items.get(i), items.get(i + 1));
      }
      result.put(items.get(items.size() - 1), null);
    }

    return result;
  }

  public static TypeName rawGenericType(TypeName typeName) {
    if (typeName instanceof ParameterizedTypeName) {
      return ((ParameterizedTypeName) typeName).rawType;
    } else {
      return typeName;
    }
  }
}
