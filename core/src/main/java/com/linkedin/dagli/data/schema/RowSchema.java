package com.linkedin.dagli.data.schema;

import java.util.Collection;


/**
 * Defines a schema for reading objects from "rows" where each field of the object corresponds to an integer-
 * indexed column.
 *
 * @param <T> the type of object being read
 * @param <A> a mutable "accumulator" object that will be used to construct the read object; this may be the same as T
 *            and can generally be hidden as an implementation detail
 */
public interface RowSchema<T, A> {
  interface FieldSchema<A> {
    /**
     * If true, reading an object will fail with an exception when the associated field(s) are not available.
     * For multifields, any missing associated field will cause an exception.
     *
     * Note that explicitly represented nulls in the row do not count as "missing" and will be read as null values.
     * Missing values occur only when the row has either too few fields (for an ordinal field index) or lacks a field
     * with a specified name (for a named field).
     *
     * @return whether the field(s) must be present
     */
    boolean isRequired();
  }

  /**
   * Reads all fields of a row together.
   *
   * Because "all" fields are always available, the value of isRequired() is immaterial.
   *
   * @param <A> the type of the accumulator object that will be read into
   */
  interface AllFields<A> extends FieldSchema<A> {
    /**
     * Parses the provided text for the fields, the result of which should be placed in the provided accumulator.
     *
     * @param accumulator the accumulator in which to add the parsed data
     * @param fieldNames the names of the fields; this entire array will be null if the fields do not have names.  This
     *                   array may be of a different size than fieldText if there were more or fewer headers than fields
     *                   in this row.  Elements may be null if the column name was null or if the field name was ignored
     *                   for some reason (for example, because the field name was a duplicate and the reader does not
     *                   support duplicates).  During a given pass over a table, fieldNames is guaranteed to be the
     *                   exact same object--this can be useful as you can use a {@link java.util.WeakHashMap} to "cache"
     *                   derived information (such as a field to index hashtable) without creating a potential memory
     *                   leak.  Do not modify this array.
     * @param fieldText the text of the fields.  Do not modify this array.
     */
    void read(A accumulator, String[] fieldNames, String[] fieldText);
  }

  /**
   * Multiple fields/columns in a row that are to be read together.
   *
   * @param <A> the type of the accumulator object that will be read into
   */
  interface MultiField<A> extends FieldSchema<A> {
    /**
     * Parses the provided text for the fields, the result of which should be placed in the provided accumulator.
     *
     * @param accumulator the accumulator in which to add the parsed data
     * @param fieldText the text of the fields.  Do not modify this array.
     */
    void read(A accumulator, String[] fieldText);

    /**
     * Fields associated with ordinal 0-based field numbers.
     *
     * @param <A> the type of the accumulator object that will be read into
     */
    interface Indexed<A> extends MultiField<A> {
      /**
       * Gets the 0-based ordinal positions in the row of the fields.
       * The order of these positions determines the order the fields are read.
       *
       * @return the field indices
       */
      int[] getIndices();
    }

    /**
     * Fields associated with String field names.  Names are case-sensitive.
     *
     * @param <A> the type of the accumulator object that will be read into
     */
    interface Named<A> extends MultiField<A> {
      /**
       * Gets the case-sensitive String names of the fields.
       *
       * @return the field names
       */
      String[] getNames();
    }
  }

  /**
   * A specific field/column in the row.
   *
   * @param <A> the type of the accumulator object that will be read into
   */
  interface Field<A> extends FieldSchema<A> {
    /**
     * Parses the provided text for the field, the result of which should be placed in the provided accumulator.
     *
     * @param accumulator the accumulator in which to add the parsed data
     * @param fieldText the text of the field
     */
    void read(A accumulator, String fieldText);

    /**
     * A field associated with an ordinal 0-based field number.
     *
     * @param <A> the type of the accumulator object that will be read into
     */
    interface Indexed<A> extends Field<A> {
      /**
       * Gets the 0-based ordinal position in the row of the field.
       *
       * @return the field index
       */
      int getIndex();
    }

    /**
     * A field associated with a String field name.  Names are case-sensitive.
     *
     * @param <A> the type of the accumulator object that will be read into
     */
    interface Named<A> extends Field<A> {
      /**
       * Gets the case-sensitive String name of the field.
       *
       * @return the field's name
       */
      String getName();
    }
  }

  /**
   * Creates a new instance of the accumulator object used to read a row
   *
   * @return a new accumulator object
   */
  A createAccumulator();

  /**
   * Gets a collection of all the fields in this {@link RowSchema}.
   * These fields will be processed in their iteration order in the collection.
   *
   * @return a collection of fields for this RowSchema
   */
  Collection<? extends FieldSchema<A>> getFields();

  /**
   * Transforms the accumulator instance into the final, desired result.
   * If the accumulator and result type are the same this will typically be a trivial identity function.
   *
   * @param accumulator the accumulator instance used to read the row
   * @return the final result of reading the row
   */
  T finish(A accumulator);
}
