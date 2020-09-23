package com.linkedin.dagli.data.schema;

import java.util.Collection;
import java.util.Collections;


/**
 * Provides convenient, pre-defined row schemas.
 */
public class RowSchemas {
  private RowSchemas() { }

  private abstract static class SingletonSchema<T> implements RowSchema<T, Object[]> {
    @Override
    public Object[] createAccumulator() {
      return new Object[1];
    }

    protected abstract FieldSchema<Object[]> getSingletonField();

    @Override
    public Collection<? extends FieldSchema<Object[]>> getFields() {
      return Collections.singleton(getSingletonField());
    }

    @Override
    public T finish(Object[] accumulator) {
      return (T) accumulator[0];
    }
  }

  private static abstract class SingletonField<T> implements RowSchema.Field<Object[]> {
    private final boolean _isRequired;

    public SingletonField(boolean isRequired) {
      _isRequired = isRequired;
    }

    @Override
    public boolean isRequired() {
      return _isRequired;
    }

    protected abstract T parseText(String text);

    @Override
    public void read(Object[] accumulator, String fieldText) {
      accumulator[0] = parseText(fieldText);
    }
  }

  private static abstract class SingletonIndexedField<T>
      extends SingletonField<T>
      implements RowSchema.Field.Indexed<Object[]> {

    private final int _index;

    public SingletonIndexedField(int index, boolean isRequired) {
      super(isRequired);
      _index = index;
    }

    @Override
    public int getIndex() {
      return _index;
    }
  }

  private static abstract class SingletonNamedField<T>
      extends SingletonField<T>
      implements RowSchema.Field.Named<Object[]> {

    private final String _name;

    public SingletonNamedField(String name, boolean isRequired) {
      super(isRequired);
      _name = name;
    }

    @Override
    public String getName() {
      return _name;
    }
  }

  /**
   * Gets a schema that reads all String values from a row.
   *
   * @return a {@link RowSchema}
   */
  public static RowSchema<String[], ?> forStrings() {
    return new SingletonSchema<String[]>() {
      @Override
      protected RowSchema.FieldSchema<Object[]> getSingletonField() {
        return new AllFields<Object[]>() {
          @Override
          public boolean isRequired() {
            return false;
          }

          @Override
          public void read(Object[] accumulator, String[] fieldNames, String[] fieldText) {
            accumulator[0] = fieldText.clone();
          }
        };
      }
    };
  }

  /**
   * Gets a schema that reads a String value from a single field.
   *
   * @param fieldIndex the index of the field
   * @param required if true, an exception will be thrown if the field is missing; otherwise, a null value will be
   *                 read
   * @return a {@link RowSchema}
   */
  public static RowSchema<String, ?> forString(int fieldIndex, boolean required) {
    return new SingletonSchema<String>() {
      @Override
      protected RowSchema.FieldSchema<Object[]> getSingletonField() {
        return new SingletonIndexedField<String>(fieldIndex, required) {
          @Override
          protected String parseText(String text) {
            return text;
          }
        };
      }
    };
  }

  /**
   * Gets a schema that reads an Integer value from a single field.
   *
   * @param fieldIndex the index of the field
   * @param required if true, an exception will be thrown if the field is missing; otherwise, a null value will be
   *                 read
   * @return a {@link RowSchema}
   */
  public static RowSchema<Integer, ?> forInteger(int fieldIndex, boolean required) {
    return new SingletonSchema<Integer>() {
      @Override
      protected RowSchema.FieldSchema<Object[]> getSingletonField() {
        return new SingletonIndexedField<Integer>(fieldIndex, required) {
          @Override
          protected Integer parseText(String text) {
            return Integer.parseInt(text);
          }
        };
      }
    };
  }

  /**
   * Gets a schema that reads a Long value from a single field.
   *
   * @param fieldIndex the index of the field
   * @param required if true, an exception will be thrown if the field is missing; otherwise, a null value will be
   *                 read
   * @return a {@link RowSchema}
   */
  public static RowSchema<Long, ?> forLong(int fieldIndex, boolean required) {
    return new SingletonSchema<Long>() {
      @Override
      protected RowSchema.FieldSchema<Object[]> getSingletonField() {
        return new SingletonIndexedField<Long>(fieldIndex, required) {
          @Override
          protected Long parseText(String text) {
            return Long.parseLong(text);
          }
        };
      }
    };
  }

  /**
   * Gets a schema that reads a Float value from a single field.
   *
   * @param fieldIndex the index of the field
   * @param required if true, an exception will be thrown if the field is missing; otherwise, a null value will be
   *                 read
   * @return a {@link RowSchema}
   */
  public static RowSchema<Float, ?> forFloat(int fieldIndex, boolean required) {
    return new SingletonSchema<Float>() {
      @Override
      protected RowSchema.FieldSchema<Object[]> getSingletonField() {
        return new SingletonIndexedField<Float>(fieldIndex, required) {
          @Override
          protected Float parseText(String text) {
            return Float.parseFloat(text);
          }
        };
      }
    };
  }

  /**
   * Gets a schema that reads a Double value from a single field.
   *
   * @param fieldIndex the index of the field
   * @param required if true, an exception will be thrown if the field is missing; otherwise, a null value will be
   *                 read
   * @return a {@link RowSchema}
   */
  public static RowSchema<Double, ?> forDouble(int fieldIndex, boolean required) {
    return new SingletonSchema<Double>() {
      @Override
      protected RowSchema.FieldSchema<Object[]> getSingletonField() {
        return new SingletonIndexedField<Double>(fieldIndex, required) {
          @Override
          protected Double parseText(String text) {
            return Double.parseDouble(text);
          }
        };
      }
    };
  }

  /**
   * Gets a schema that reads a Boolean value from a single field.
   *
   * @param fieldIndex the index of the field
   * @param required if true, an exception will be thrown if the field is missing; otherwise, a null value will be
   *                 read
   * @return a {@link RowSchema}
   */
  public static RowSchema<Boolean, ?> forBoolean(int fieldIndex, boolean required) {
    return new SingletonSchema<Boolean>() {
      @Override
      protected RowSchema.FieldSchema<Object[]> getSingletonField() {
        return new SingletonIndexedField<Boolean>(fieldIndex, required) {
          @Override
          protected Boolean parseText(String text) {
            return Boolean.parseBoolean(text);
          }
        };
      }
    };
  }

  /**
   * Gets a schema that reads a String value from a single field.
   *
   * @param fieldName the index of the field
   * @param required if true, an exception will be thrown if the field is missing; otherwise, a null value will be
   *                 read
   * @return a {@link RowSchema}
   */
  public static RowSchema<String, ?> forString(String fieldName, boolean required) {
    return new SingletonSchema<String>() {
      @Override
      protected RowSchema.FieldSchema<Object[]> getSingletonField() {
        return new SingletonNamedField<String>(fieldName, required) {
          @Override
          protected String parseText(String text) {
            return text;
          }
        };
      }
    };
  }

  /**
   * Gets a schema that reads an Integer value from a single field.
   *
   * @param fieldName the index of the field
   * @param required if true, an exception will be thrown if the field is missing; otherwise, a null value will be
   *                 read
   * @return a {@link RowSchema}
   */
  public static RowSchema<Integer, ?> forInteger(String fieldName, boolean required) {
    return new SingletonSchema<Integer>() {
      @Override
      protected RowSchema.FieldSchema<Object[]> getSingletonField() {
        return new SingletonNamedField<Integer>(fieldName, required) {
          @Override
          protected Integer parseText(String text) {
            return Integer.parseInt(text);
          }
        };
      }
    };
  }

  /**
   * Gets a schema that reads a Long value from a single field.
   *
   * @param fieldName the index of the field
   * @param required if true, an exception will be thrown if the field is missing; otherwise, a null value will be
   *                 read
   * @return a {@link RowSchema}
   */
  public static RowSchema<Long, ?> forLong(String fieldName, boolean required) {
    return new SingletonSchema<Long>() {
      @Override
      protected RowSchema.FieldSchema<Object[]> getSingletonField() {
        return new SingletonNamedField<Long>(fieldName, required) {
          @Override
          protected Long parseText(String text) {
            return Long.parseLong(text);
          }
        };
      }
    };
  }

  /**
   * Gets a schema that reads a Float value from a single field.
   *
   * @param fieldName the index of the field
   * @param required if true, an exception will be thrown if the field is missing; otherwise, a null value will be
   *                 read
   * @return a {@link RowSchema}
   */
  public static RowSchema<Float, ?> forFloat(String fieldName, boolean required) {
    return new SingletonSchema<Float>() {
      @Override
      protected RowSchema.FieldSchema<Object[]> getSingletonField() {
        return new SingletonNamedField<Float>(fieldName, required) {
          @Override
          protected Float parseText(String text) {
            return Float.parseFloat(text);
          }
        };
      }
    };
  }

  /**
   * Gets a schema that reads a Double value from a single field.
   *
   * @param fieldName the index of the field
   * @param required if true, an exception will be thrown if the field is missing; otherwise, a null value will be
   *                 read
   * @return a {@link RowSchema}
   */
  public static RowSchema<Double, ?> forDouble(String fieldName, boolean required) {
    return new SingletonSchema<Double>() {
      @Override
      protected RowSchema.FieldSchema<Object[]> getSingletonField() {
        return new SingletonNamedField<Double>(fieldName, required) {
          @Override
          protected Double parseText(String text) {
            return Double.parseDouble(text);
          }
        };
      }
    };
  }

  /**
   * Gets a schema that reads a Boolean value from a single field.
   *
   * @param fieldName the index of the field
   * @param required if true, an exception will be thrown if the field is missing; otherwise, a null value will be
   *                 read
   * @return a {@link RowSchema}
   */
  public static RowSchema<Boolean, ?> forBoolean(String fieldName, boolean required) {
    return new SingletonSchema<Boolean>() {
      @Override
      protected RowSchema.FieldSchema<Object[]> getSingletonField() {
        return new SingletonNamedField<Boolean>(fieldName, required) {
          @Override
          protected Boolean parseText(String text) {
            return Boolean.parseBoolean(text);
          }
        };
      }
    };
  }
}
