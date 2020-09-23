package com.linkedin.dagli.reducer;

import com.linkedin.dagli.util.type.Classes;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;


/**
 * Maps a class to a set of {@link Reducer}s that should be applied to all instances of that class.  This is
 * primarily useful for providing reductions for classes whose code cannot be modified or that need to execute on a
 * general type of {@link com.linkedin.dagli.producer.Producer}, such as all the
 * {@link com.linkedin.dagli.transformer.PreparedTransformer}s in a graph.  Note that, especially in the latter case,
 * this has the risk of executing reducers against a large set of producers which may (greatly) increase the
 * computational cost of reducing a DAG and this should thus be employed judiciously.  E.g. if a reducer only applies
 * to four different types of {@link com.linkedin.dagli.transformer.PreparedTransformer}, specify these types rather
 * than adding a reduction for all {@link com.linkedin.dagli.transformer.PreparedTransformer}s in general.
 */
public class ClassReducerTable {
  private HashMap<Class<?>, HashSet<Reducer<?>>> _reductionMap = new HashMap<>();

  /**
   * Adds a reduction that should be applied for all producer instances of the given classes.
   *
   * @param reduction the reduction to add
   * @param targetClasses the classes (or interfaces) whose instances should be targeted by this reduction (this will
   *                      include instances whose concrete type is a subtype of these classes)
   * @param <T> the type of producer the added reduction targets
   */
  @SafeVarargs
  public final <T> void add(Reducer<T> reduction, Class<? extends T>... targetClasses) {
    for (Class<? extends T> targetClass : targetClasses) {
      add(reduction, targetClass);
    }
  }

  /**
   * Adds a reduction that should be applied for all producer instances of the given class.
   *
   * @param reduction the reduction to add
   * @param targetClass the class (or interface) whose instances should be targeted by this reduction (this will
   *                      include instances whose concrete type is a subtype of this class)
   * @param <T> the type of producer the added reduction targets
   */
  public <T> void add(Reducer<T> reduction, Class<? extends T> targetClass) {
    _reductionMap.computeIfAbsent(targetClass, k -> new HashSet<>()).add(reduction);
  }

  /**
   * Adds all the reductions from another {@link ClassReducerTable} to this one.
   *
   * @param other the instance whose reducers will be added to this one; may be null, in which case this call is a no-op
   */
  @SuppressWarnings("unchecked") // safely casting the result of a clone()
  public void addAll(ClassReducerTable other) {
    if (other == null) {
      return;
    }

    for (Map.Entry<Class<?>, HashSet<Reducer<?>>> entry : other._reductionMap.entrySet()) {
      _reductionMap.compute(entry.getKey(), (key, set) -> {
        if (set == null) {
          return (HashSet<Reducer<?>>) entry.getValue().clone();
        }
        set.addAll(entry.getValue());
        return set;
      });
    }
  }

  /**
   * Gets the set of reducers corresponding the target class.  This includes reducers added for superclasses and
   * interfaces of the specified class.
   *
   * @param targetClass the class of the producer to be reduced
   * @param <T> the target class
   * @return a set containing the applicable reducers; should not be modified
   */
  public <T> Set<? extends Reducer<? super T>> getReducers(Class<T> targetClass) {
    HashSet<Reducer<? super T>> result = new HashSet<>();
    Classes.walkHierarchy(targetClass)
        .map(clazz -> (HashSet) _reductionMap.get(clazz))
        .filter(Objects::nonNull)
        .forEach(result::addAll);

    // each class is guaranteed to be associated with GraphReducers applicable that class
    return result;
  }

  /**
   * Checks to see if this table associates a class with a particular reducer.  This includes reducers added for
   * superclasses and interfaces of the specified class.
   *
   * @param clazz the class of producer whose reducers should be examined
   * @param reducer the reducer whose presence is being queried
   * @param <T> the class of producer whose reducers should be examined
   * @return true if the class is associated with the given reducer
   */
  public <T> boolean hasReducer(Class<T> clazz, Reducer<? super T> reducer) {
    return Classes.walkHierarchy(clazz)
        .map(_reductionMap::get)
        .filter(Objects::nonNull)
        .anyMatch(set -> set.contains(reducer));
  }
}
