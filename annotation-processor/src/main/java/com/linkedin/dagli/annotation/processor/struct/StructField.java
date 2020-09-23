package com.linkedin.dagli.annotation.processor.struct;

import com.linkedin.dagli.annotation.struct.Optional;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeVariableName;
import com.squareup.javapoet.WildcardTypeName;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.SimpleTypeVisitor8;
import javax.tools.Diagnostic;


/**
 * Internal class used to represent the fields of a @Struct during annotation processing.
 */
class StructField {
  final TypeMirror _type;
  final String _name;
  final boolean _optional;
  final HashSet<String> _typeParameterNames;
  final String _javaDocComment;

  // "Method-backed" fields are read-only and backed by base class methods; this value will be null is this is a real
  // field.
  final String _backingMethodName;

  // Every field has a 0...[number fields - 1] index assigned to it, which should start at the first field (as declared
  // in the source code) of the more distal field-bearing superclass and end with the last field of the @Struct
  // definition class; should be -1 if backingMethodName != null (virtual field)
  final int _index;

  private StructField(ProcessingEnvironment processingEnvironment, TypeMirror type, String name, Element element,
      boolean forceOptional, String backingMethodName, int index) {
    if ((index < 0 && backingMethodName == null) || (index >= 0 && backingMethodName != null)) {
      throw new IllegalArgumentException(
          "Field must have either a valid index or a backing method name, but never both");
    }
    _type = type;
    _name = name;
    _optional = forceOptional || element.getAnnotation(Optional.class) != null;
    _typeParameterNames = new HashSet<>();
    _type.accept(new TypeParameterVisitor(), _typeParameterNames);
    _javaDocComment = processingEnvironment.getElementUtils().getDocComment(element);
    _backingMethodName = backingMethodName;
    _index = index;
  }

  StructField(ProcessingEnvironment processingEnvironment, VariableElement field, boolean forceOptional, int index) {
    this(processingEnvironment, field.asType(), field.getSimpleName().toString(), field, forceOptional, null, index);
  }

  /**
   * Creates a method-backed "virtual" field.
   *
   * @param processingEnvironment the annotation processing environment
   * @param name the name to use for the method-backed field, or "" to use the backing method's name
   * @param method the backing method element
   */
  StructField(ProcessingEnvironment processingEnvironment, String name, ExecutableElement method) {
    this(processingEnvironment, method.getReturnType(), name.isEmpty() ? method.getSimpleName().toString() : name, method,
        false, method.getSimpleName().toString(), -1);

    if (method.getTypeParameters().size() > 0) {
      processingEnvironment.getMessager()
          .printMessage(Diagnostic.Kind.ERROR, "@Transformer methods are not allowed to accept type parameters",
              method);
    }
    if (method.getParameters().size() > 0) {
      processingEnvironment.getMessager()
          .printMessage(Diagnostic.Kind.ERROR, "@Transformer methods are not allowed to accept parameters", method);
    }
    if (method.getThrownTypes().size() > 0) {
      processingEnvironment.getMessager()
          .printMessage(Diagnostic.Kind.ERROR, "@Transformer methods are not allowed to declare checked exceptions",
              method);
    }
  }

  private static class TypeParameterVisitor extends SimpleTypeVisitor8<Void, HashSet<String>> {
    @Override
    public Void visitIntersection(IntersectionType t, HashSet<String> result) {
      for (TypeMirror bound : t.getBounds()) {
        bound.accept(this, result);
      }
      return null;
    }

    @Override
    public Void visitArray(ArrayType t, HashSet<String> result) {
      t.getComponentType().accept(this, result);
      return null;
    }

    @Override
    public Void visitDeclared(DeclaredType t, HashSet<String> result) {
      if (t.getEnclosingType() != null) {
        t.getEnclosingType().accept(this, result);
      }
      for (TypeMirror tp : t.getTypeArguments()) {
        tp.accept(this, result);
      }
      return null;
    }

    @Override
    public Void visitTypeVariable(TypeVariable t, HashSet<String> result) {
      result.add(t.toString());
      return null;
    }

    @Override
    public Void visitWildcard(WildcardType t, HashSet<String> result) {
      if (t.getExtendsBound() != null) {
        t.getExtendsBound().accept(this, result);
      } else if (t.getSuperBound() != null) {
        t.getSuperBound().accept(this, result);
      } // else { noop } because t == unbounded wildcard "?"

      return null;
    }
  }

  public TypeMirror getTypeUpperBound() {
    return _type.getKind() == TypeKind.TYPEVAR ? ((TypeVariable) _type).getUpperBound() : _type;
  }

  public List<TypeParameterElement> getRelevantTypeParameters(List<? extends TypeParameterElement> typeParameters) {
    return typeParameters.stream()
        .filter(tp -> _typeParameterNames.contains(tp.getSimpleName().toString()))
        .collect(Collectors.toList());
  }

  public List<TypeName> getWildcardedTypeParameters(List<? extends TypeParameterElement> typeParameters) {
    return typeParameters.stream()
        .map(tp -> _typeParameterNames.contains(tp.getSimpleName().toString()) ? TypeVariableName.get(tp)
            : WildcardTypeName.subtypeOf(Object.class))
        .collect(Collectors.toList());
  }

  private StringBuilder getTrimmedName() {
    StringBuilder name = new StringBuilder(_name);
    while (!Character.isLetterOrDigit(name.charAt(0))) {
      name.deleteCharAt(0);
    }

    while (!Character.isLetterOrDigit(name.charAt(name.length() - 1))) {
      name.setLength(name.length() - 1);
    }

    return name;
  }

  public String getFieldName() {
    return _name;
  }

  public String getInternalFieldName() {
    return "_" + getVariableName();
  }

  public TypeName getBoxedTypeName() {
    TypeName name = TypeName.get(_type);
    return name.isPrimitive() ? name.box() : name;
  }

  public String getCoreName() {
    StringBuilder name = getTrimmedName();
    name.setCharAt(0, Character.toUpperCase(name.charAt(0)));
    return name.toString();
  }

  public String getVariableName() {
    StringBuilder name = getTrimmedName();
    name.setCharAt(0, Character.toLowerCase(name.charAt(0)));
    return name.toString();
  }

  public boolean isOptional() {
    return _optional;
  }

  /**
   * True iff this is a "virtual" field backed by a method on the base class.
   * @return
   */
  public boolean isBackedByMethod() {
    return _backingMethodName != null;
  }
}
