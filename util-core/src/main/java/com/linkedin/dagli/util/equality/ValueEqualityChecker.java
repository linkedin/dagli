package com.linkedin.dagli.util.equality;

import com.linkedin.dagli.annotation.equality.DeepArrayValueEquality;
import com.linkedin.dagli.annotation.equality.IgnoredByValueEquality;
import com.linkedin.dagli.util.exception.Exceptions;
import com.linkedin.dagli.util.invariant.Arguments;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;

/**
 * Provides methods for determining whether an object is equal to another, and calculating its hashCode, on the basis
 * of its field values as accessed via reflection.  Specifically, two instances have value equality if and only if:
 * (1) The instances have the same type
 * (2) All non-static fields, including inherited fields but excluding fields annotated with
 *    {@link IgnoredByValueEquality}, are equal as determined by {@link Objects#equals(Object, Object)}
 *
 * {@link ValueEqualityChecker} caches the {@link java.lang.reflect.Field}s for the class whose objects are to be
 * compared, but even with this optimization reflection will invariably be substantially slower than "native" access.
 *
 * Finally, please note that Dagli will use {@link java.lang.reflect.Field#setAccessible(boolean)} to access non-public
 * fields when checking equality.  If you are running in a context with a {@link SecurityManager}, and that security
 * manager forbids this access, a runtime exception will be thrown.  See
 * {@link java.lang.reflect.Field#setAccessible(boolean)} for more details.
 */
public class ValueEqualityChecker<T> {
  private final Class<T> _class;
  private final Field[] _fields;
  private final boolean[] _fieldComparedAsDeepArray;
  private final boolean _hasDeepArrayFields;

  /**
   * Creates a new value equality checker for the provided class.
   *
   * @param classToCompare the class whose instances will be compared for value equality
   */
  public ValueEqualityChecker(Class<T> classToCompare) {
    Arguments.check(!classToCompare.isPrimitive() && !classToCompare.isArray() && !classToCompare.isInterface(),
      "ValueEqualityChecker cannot compare (boxed) primitives, arrays or interfaces");

    _class = classToCompare;

    ArrayList<Field> fields = new ArrayList<>();
    Class<?> ancestor = _class;
    while (!ancestor.equals(Object.class)) {
      if (!ancestor.isAnnotationPresent(IgnoredByValueEquality.class)) {
        // we sort fields by name to guarantee a consistent ordering; this matters when we later calculate the hashCode
        // and want to be consistent between different instances of ValueEqualityChecker created for this same class
        Field[] ancestorFields = ancestor.getDeclaredFields();
        Arrays.sort(ancestorFields, Comparator.comparing(Field::getName));

        for (Field field : ancestorFields) {
          if (!field.isAnnotationPresent(IgnoredByValueEquality.class) && !Modifier.isStatic(field.getModifiers())) {
            fields.add(field);
          }
        }
      }
      ancestor = ancestor.getSuperclass();
    }

    _fields = fields.toArray(new Field[0]);
    Field.setAccessible(_fields, true); // make all the things accessible

    boolean hasDeepArrayFields = false;
    _fieldComparedAsDeepArray = new boolean[_fields.length];
    for (int i = 0; i < _fields.length; i++) {
      _fieldComparedAsDeepArray[i] = _fields[i].isAnnotationPresent(DeepArrayValueEquality.class);
      hasDeepArrayFields |= _fieldComparedAsDeepArray[i];
    }
    _hasDeepArrayFields = hasDeepArrayFields;
  }

  /**
   * Computes a hashCode for an instance of type <code>T</code> that is consistent with value equality.  The instance
   * must have exactly this type and not, e.g. a subclass thereof.
   *
   * @param instance the instance whose hashCode should be calculated
   * @return a hashCode for the provided instance
   */
  public int hashCode(T instance) {
    if (instance == null) {
      return 0;
    }

    Arguments.check(instance.getClass().equals(_class),
        "The provided instance must be exactly of the type " + _class + " and not, e.g. a subclass");

    // Reuse a single Object[] wrapper to avoid duplicative allocations; we use the wrapper because Objects doesn't
    // have a "deepHashCode(...)" method that accepts objects, and Arrays.deepHashCode(...) only accepts Object[].
    Object[] deepArrayWrapper = _hasDeepArrayFields ? new Object[1] : null;

    try {
      int hashCode = _class.hashCode();
      for (int i = 0; i < _fields.length; i++) {
        final int fieldHashCode;
        if (_fieldComparedAsDeepArray[i]) {
          deepArrayWrapper[0] = _fields[i].get(instance);
          fieldHashCode = Arrays.deepHashCode(deepArrayWrapper);
        } else {
          fieldHashCode = Objects.hashCode(_fields[i].get(instance));
        }

        hashCode = hashCode * 31 + fieldHashCode;
      }
      return hashCode;
    } catch (IllegalAccessException e) {
      throw Exceptions.asRuntimeException(e);
    }
  }

  /**
   * Compares two instances of type <code>T</code> for value equality.  Both instances must have exactly this type (and
   * not, e.g. a subclass thereof).
   *
   * @param instance1 the first instance to compare
   * @param instance2 the second instance to compare
   * @return whether the instances are considered value-equals
   */
  public boolean equals(T instance1, T instance2) {
    if (instance1 == instance2) {
      return true;
    } else if (instance1 == null) {
      return false;
    }

    Arguments.check(instance1.getClass().equals(_class) && instance2.getClass().equals(_class),
        "The objects to be compared must both be instances of type " + _class);

    try {
      for (int i = 0; i < _fields.length; i++) {
        if (_fieldComparedAsDeepArray[i]) {
          if (!Objects.deepEquals(_fields[i].get(instance1), _fields[i].get(instance2))) {
            return false;
          }
        } else {
          if (!Objects.equals(_fields[i].get(instance1), _fields[i].get(instance2))) {
            return false;
          }
        }
      }
      return true;
    } catch (IllegalAccessException e) {
      throw Exceptions.asRuntimeException(e);
    }
  }
}
