package com.linkedin.dagli.avro;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.transformer.AbstractPreparedTransformer1WithInput;
import com.linkedin.dagli.util.invariant.Arguments;
import java.util.Objects;
import org.apache.avro.generic.GenericRecord;

/**
 * Gets the value of a named field from Avro objects, including both GenericRecords and classes codegen'ed from
 * Avro schemas.
 *
 * Note that, if you have the codegen'ed class, we recommend creating a @Struct for that class instead, as the
 * generated @Struct will have field-getter transformers (as inner classes with names matching the field names) that
 * are slightly more efficient and, more importantly, completely statically typed.
 *
 * Please see {@code /documentation/avro.md} for details, but basically, given a codegen'ed class "User", we can create
 * our @Struct definition class as:
 * <code>
 * &commat;Struct("UserStruct")
 * class UserStructBase extends User { }
 * </code>
 *
 * This will cause a @Struct class named UserStruct to be created.  If the User schema has a field called "name", the
 * transformer to extract this field will just be called "UserStruct.Name".
 *
 * @param <T> the type of object stored in the field
 */
@ValueEquality
public class AvroField<T> extends AbstractPreparedTransformer1WithInput<GenericRecord, T, AvroField<T>> {
  private static final long serialVersionUID = 1;

  private Class<T> _fieldType = null;
  private String _fieldName = null;
  private int _fieldIndex = -1;

  /**
   * Sets the type (class) of the Avro field.  For primitive types (e.g. long) specify the boxed type (e.g. Long).
   *
   * Please note that a "string" in Avro is, by default, a Utf8 object, not a Java String.  We suggest specifying
   * CharSequence as the type for Avro String fields (this interface is implemented by Utf8).
   *
   * You must specify a field type prior to calling apply().
   *
   * @param fieldType the type (or a supertype) of the field
   * @return a new instance with the specified field type
   */
  @SuppressWarnings("unchecked")
  public <U> AvroField<U> withFieldType(Class<U> fieldType) {
    return (AvroField) clone(c -> c._fieldType = (Class) fieldType);
  }

  /**
   * Sets the name of the field whose value will be returned.  This will replace a field index, if one has previously
   * been specified.
   *
   * @param fieldName the name of the field to be retrieved
   * @return a new instance that will retrieve the named field
   */
  public AvroField<T> withFieldName(String fieldName) {
    return clone(c -> {
      c._fieldName = fieldName;
      c._fieldIndex = -1;
    });
  }

  /**
   * Sets the index of the field whose value will be returned (this is faster than looking up via a field name and
   * suitable if you're certain all the Avro objects this transformer will process will have the same schema).
   *
   * This will replace a field name, if one has previously been set.
   *
   * @param fieldIndex the index of the field to be retrieved
   * @return a new instance that will retrieved the specified field
   */
  public AvroField<T> withFieldIndex(int fieldIndex) {
    return clone(c -> {
      c._fieldName = null;
      c._fieldIndex = fieldIndex;
    });
  }

  @Override
  public T apply(GenericRecord record) {
    if (_fieldName != null) {
      org.apache.avro.Schema.Field field = record.getSchema().getField(_fieldName);
      if (field == null) {
        throw new IllegalArgumentException("The provided Avro object does not have a field named '" + _fieldName + "'");
      }
      return _fieldType.cast(record.get(field.pos()));
    } else {
      return _fieldType.cast(record.get(_fieldIndex));
    }
  }

  @Override
  public void validate() {
    super.validate();
    Objects.requireNonNull(_fieldType, "The type of the field has not been specified");
    Arguments.check(_fieldIndex >= 0 || _fieldName != null, "A field name or index must be provided for an AvroField");
  }
}
