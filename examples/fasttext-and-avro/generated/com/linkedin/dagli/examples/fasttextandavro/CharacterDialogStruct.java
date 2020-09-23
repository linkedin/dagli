package com.linkedin.dagli.examples.fasttextandavro;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.annotation.struct.HasStructField;
import com.linkedin.dagli.data.schema.RowSchema;
import com.linkedin.dagli.generator.AbstractGenerator;
import com.linkedin.dagli.objectio.ObjectIterator;
import com.linkedin.dagli.objectio.ObjectReader;
import com.linkedin.dagli.producer.MissingInput;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.reducer.InverseClassReducer;
import com.linkedin.dagli.reducer.Reducer;
import com.linkedin.dagli.struct.Struct;
import com.linkedin.dagli.transformer.AbstractPreparedTransformer1;
import com.linkedin.dagli.transformer.AbstractPreparedTransformerDynamic;
import com.linkedin.dagli.transformer.Transformer;
import com.linkedin.dagli.util.cloneable.AbstractCloneable;
import com.linkedin.dagli.util.function.Function1;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.CharSequence;
import java.lang.CloneNotSupportedException;
import java.lang.Cloneable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

@HasStructField(
    optional = true,
    name = "character",
    type = CharSequence.class
)
@HasStructField(
    optional = false,
    name = "dialog",
    type = CharSequence.class
)
public class CharacterDialogStruct extends CharacterDialogStructBase implements Cloneable, Struct {
  public static final org.apache.avro.Schema SCHEMA$ = getCorrectedAvroSchema$();

  private static final long serialVersionUID = 1L;

  public CharacterDialogStruct() {
    super();
  }

  @Override
  public String toString() {
    return "CharacterDialogStruct(Character = " + character + ", " + "Dialog = " + dialog + ")";
  }

  @Override
  public boolean equals(Object other) {
    if (other == null || other.getClass() != this.getClass()) {
      return false;
    }
    CharacterDialogStruct o = (CharacterDialogStruct) other;
    return java.util.Objects.equals(character, o.character) && java.util.Objects.equals(dialog, o.dialog);
  }

  @Override
  public int hashCode() {
    return Objects.hash(character, dialog);
  }

  /**
   * The name of the character saying something  */
  public CharSequence getCharacter() {
    return character;
  }

  /**
   * What the character says  */
  public CharSequence getDialog() {
    return dialog;
  }

  public static CharacterDialogStruct fromMap(Map<? extends CharSequence, Object> map) {
    com.linkedin.dagli.examples.fasttextandavro.CharacterDialogStruct res = new com.linkedin.dagli.examples.fasttextandavro.CharacterDialogStruct();
    if (!map.containsKey("dialog") && !map.containsKey("Dialog")) { throw new NoSuchElementException("dialog"); };
    res.character = (CharSequence) map.getOrDefault("character", map.getOrDefault("Character", res.character));
    res.dialog = (CharSequence) map.getOrDefault("dialog", map.getOrDefault("Dialog", res.dialog));
    return res;
  }

  @Override
  public Map<String, Object> toMap() {
    HashMap<String, Object> map = new HashMap<String, Object>(2);
    map.put("character", character);
    map.put("dialog", dialog);
    return map;
  }

  /**
   * The name of the character saying something  */
  public CharacterDialogStruct withCharacter(CharSequence character) {
    try {
      CharacterDialogStruct res = (CharacterDialogStruct) this.clone();
      res.character = character;
      return res;
    } catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * What the character says  */
  public CharacterDialogStruct withDialog(CharSequence dialog) {
    try {
      CharacterDialogStruct res = (CharacterDialogStruct) this.clone();
      res.dialog = dialog;
      return res;
    } catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
  }

  private static org.apache.avro.Schema getCorrectedAvroSchema$() {
    org.apache.avro.Schema originalSchema = CharacterDialogStructBase.SCHEMA$;
    List<org.apache.avro.Schema.Field> fields = originalSchema.getFields().stream().map(oldField ->  {
      org.apache.avro.Schema.Field newField = new org.apache.avro.Schema.Field(oldField.name(), oldField.schema(), oldField.doc(), oldField.defaultVal(), oldField.order());
      oldField.aliases().forEach(newField::addAlias);
      return newField;
    } ).collect(Collectors.toList());
    return org.apache.avro.Schema.createRecord("CharacterDialogStruct", originalSchema.getDoc(), "com.linkedin.dagli.examples.fasttextandavro", originalSchema.isError(), fields);
  }

  public interface Builder {
    /**
     * What the character says  */
    static Helper.CompletedBuilder setDialog(CharSequence dialog) {
      return new BuilderImpl().setDialog(dialog);
    }
  }

  public static class Placeholder extends com.linkedin.dagli.placeholder.Placeholder<CharacterDialogStruct> {
    private static final long serialVersionUID = 1L;

    private transient Character _character = null;

    private transient Dialog _dialog = null;

    public Character asCharacter() {
      if (_character == null) {
        _character = new Character().withInput(this);
      }
      return _character;
    }

    public Dialog asDialog() {
      if (_dialog == null) {
        _dialog = new Dialog().withInput(this);
      }
      return _dialog;
    }
  }

  private static class BuilderImpl implements Helper.CompletedBuilder, Helper.Dialog.Builder {
    private CharacterDialogStruct _instance = new CharacterDialogStruct();

    public CharacterDialogStruct build() {
      return _instance;
    }

    @Override
    public Helper.CompletedBuilder setDialog(CharSequence dialog) {
      _instance.dialog = dialog;
      return this;
    }

    @Override
    public Helper.CompletedBuilder setCharacter(CharSequence character) {
      _instance.character = character;
      return this;
    }
  }

  public static class Character extends AbstractPreparedTransformer1<CharacterDialogStruct, CharSequence, Character> {
    private static final long serialVersionUID = 1L;

    public Character withInput(Producer<? extends CharacterDialogStruct> input) {
      return withInput1(input);
    }

    @Override
    public int computeHashCode() {
      return Transformer.hashCodeOfInputs(this);
    }

    @Override
    public boolean computeEqualsUnsafe(Character other) {
      return Transformer.sameInputs(this, other);
    }

    @Override
    protected Collection<? extends Reducer<? super Character>> getGraphReducers() {
      return Collections.singleton(new InverseClassReducer(0, Assembled.class));
    }

    @Override
    public CharSequence apply(CharacterDialogStruct struct) {
      return struct.character;
    }
  }

  public static class Dialog extends AbstractPreparedTransformer1<CharacterDialogStruct, CharSequence, Dialog> {
    private static final long serialVersionUID = 1L;

    public Dialog withInput(Producer<? extends CharacterDialogStruct> input) {
      return withInput1(input);
    }

    @Override
    public int computeHashCode() {
      return Transformer.hashCodeOfInputs(this);
    }

    @Override
    public boolean computeEqualsUnsafe(Dialog other) {
      return Transformer.sameInputs(this, other);
    }

    @Override
    protected Collection<? extends Reducer<? super Dialog>> getGraphReducers() {
      return Collections.singleton(new InverseClassReducer(1, Assembled.class));
    }

    @Override
    public CharSequence apply(CharacterDialogStruct struct) {
      return struct.dialog;
    }
  }

  public static class Helper {
    public interface CompletedBuilder {
      CharacterDialogStruct build();

      /**
       * The name of the character saying something  */
      CompletedBuilder setCharacter(CharSequence character);
    }

    public interface CompletedAssembledBuilder {
      Assembled build();

      CompletedAssembledBuilder setCharacter(Producer<? extends CharSequence> character);
    }

    public interface CompletedReaderBuilder {
      Reader build();

      CompletedReaderBuilder setCharacter(ObjectReader<? extends CharSequence> character);
    }

    public interface CompletedSchemaBuilder {
      Schema build();

      CompletedSchemaBuilder setCharacterColumnIndex(int characterColumnIndex);

      CompletedSchemaBuilder setCharacterColumnIndex(int characterColumnIndex,
          Function1<String, CharSequence> parser);

      CompletedSchemaBuilder setCharacterColumnName(String characterColumnName);

      CompletedSchemaBuilder setCharacterColumnName(String characterColumnName,
          Function1<String, CharSequence> parser);

      CompletedSchemaBuilder setCharacterParser(BiFunction<String[], String[], CharSequence> parser);
    }

    public static class Character {
      public interface Builder {
        /**
         * The name of the character saying something  */
        CompletedBuilder setCharacter(CharSequence character);
      }

      public interface AssembledBuilder {
        CompletedAssembledBuilder setCharacter(Producer<? extends CharSequence> character);
      }

      public interface ReaderBuilder {
        CompletedReaderBuilder setCharacter(ObjectReader<? extends CharSequence> character);
      }

      public interface SchemaBuilder {
        CompletedSchemaBuilder setCharacterColumnIndex(int characterColumnIndex,
            Function1<String, CharSequence> parser);

        CompletedSchemaBuilder setCharacterColumnName(String characterColumnName,
            Function1<String, CharSequence> parser);

        CompletedSchemaBuilder setCharacterParser(BiFunction<String[], String[], CharSequence> parser);

        CompletedSchemaBuilder setCharacterColumnIndex(int characterColumnIndex);

        CompletedSchemaBuilder setCharacterColumnName(String characterColumnName);
      }
    }

    public static class Dialog {
      public interface Builder {
        /**
         * What the character says  */
        CompletedBuilder setDialog(CharSequence dialog);
      }

      public interface AssembledBuilder {
        CompletedAssembledBuilder setDialog(Producer<? extends CharSequence> dialog);
      }

      public interface ReaderBuilder {
        CompletedReaderBuilder setDialog(ObjectReader<? extends CharSequence> dialog);
      }

      public interface SchemaBuilder {
        CompletedSchemaBuilder setDialogColumnIndex(int dialogColumnIndex,
            Function1<String, CharSequence> parser);

        CompletedSchemaBuilder setDialogColumnName(String dialogColumnName,
            Function1<String, CharSequence> parser);

        CompletedSchemaBuilder setDialogParser(BiFunction<String[], String[], CharSequence> parser);

        CompletedSchemaBuilder setDialogColumnIndex(int dialogColumnIndex);

        CompletedSchemaBuilder setDialogColumnName(String dialogColumnName);
      }
    }
  }

  public static class Assembled extends AbstractPreparedTransformerDynamic<CharacterDialogStruct, Assembled> {
    private static final long serialVersionUID = 1L;

    private Producer<? extends CharSequence> _character = DefaultGenerator.get();

    private Producer<? extends CharSequence> _dialog = MissingInput.get();

    private Assembled() {
    }

    public static Helper.Dialog.AssembledBuilder builder() {
      return new Builder();
    }

    public Assembled withCharacter(Producer<? extends CharSequence> characterInput) {
      return clone(c -> c._character = characterInput);
    }

    public Assembled withDialog(Producer<? extends CharSequence> dialogInput) {
      return clone(c -> c._dialog = dialogInput);
    }

    @Override
    protected CharacterDialogStruct apply(List<?> values) {
      CharacterDialogStruct res = new CharacterDialogStruct();
      if (!(_character instanceof DefaultGenerator)) {
        res.character = (CharSequence) values.get(0);
      }
      res.dialog = (CharSequence) values.get(1);
      return res;
    }

    @Override
    protected Assembled withInputsUnsafe(List<? extends Producer<?>> inputs) {
      return clone(c ->  {
        c._character = (Producer<? extends CharSequence>) inputs.get(0);
        c._dialog = (Producer<? extends CharSequence>) inputs.get(1);
      } );
    }

    @Override
    protected int computeHashCode() {
      return Transformer.hashCodeOfInputs(this);
    }

    @Override
    protected boolean computeEqualsUnsafe(Assembled other) {
      return Transformer.sameInputs(this, other);
    }

    @Override
    protected List<Producer<?>> getInputList() {
      return Arrays.asList(new Producer<?>[] {_character, _dialog});
    }

    @ValueEquality
    private static final class DefaultGenerator<R> extends AbstractGenerator<R, DefaultGenerator<R>> {
      private static final long serialVersionUID = 0L;

      private static DefaultGenerator _singleton = new DefaultGenerator();

      DefaultGenerator() {
        super(0xe327508a79df43b0L, 0xfb0af9e419745536L);
      }

      public static <R> DefaultGenerator<R> get() {
        return _singleton;
      }

      @Override
      public R generate(long index) {
        return null;
      }

      private Object readResolve() throws ObjectStreamException {
        return _singleton;
      }
    }

    private static class Builder implements Helper.CompletedAssembledBuilder, Helper.Dialog.AssembledBuilder {
      private Assembled _instance = new Assembled();

      public Assembled build() {
        return _instance;
      }

      @Override
      public Helper.CompletedAssembledBuilder setCharacter(Producer<? extends CharSequence> character) {
        _instance._character = character;
        return this;
      }

      @Override
      public Helper.CompletedAssembledBuilder setDialog(Producer<? extends CharSequence> dialog) {
        _instance._dialog = dialog;
        return this;
      }
    }
  }

  public static class Reader extends AbstractCloneable<Reader> implements ObjectReader<CharacterDialogStruct>, Serializable {
    private static final long serialVersionUID = 1L;

    private ObjectReader<? extends CharSequence> _character = null;

    private ObjectReader<? extends CharSequence> _dialog = null;

    private Reader() {
    }

    public ObjectReader<? extends CharSequence> getCharacterReader() {
      return _character;
    }

    public ObjectReader<? extends CharSequence> getDialogReader() {
      return _dialog;
    }

    public static Helper.Dialog.ReaderBuilder builder() {
      return new Builder();
    }

    public Reader withCharacter(ObjectReader<? extends CharSequence> characterInput) {
      return clone(c -> c._character = characterInput);
    }

    public Reader withDialog(ObjectReader<? extends CharSequence> dialogInput) {
      return clone(c -> c._dialog = dialogInput);
    }

    @Override
    public long size64() {
      return _dialog.size64();
    }

    @Override
    public Iterator iterator() {
      return new Iterator(this);
    }

    @Override
    public void close() {
      if (_character != null) {
        _character.close();
      }
      _dialog.close();
    }

    private static class Builder implements Helper.CompletedReaderBuilder, Helper.Dialog.ReaderBuilder {
      private Reader _instance = new Reader();

      public Reader build() {
        return _instance;
      }

      @Override
      public Helper.CompletedReaderBuilder setCharacter(ObjectReader<? extends CharSequence> character) {
        _instance._character = character;
        return this;
      }

      @Override
      public Helper.CompletedReaderBuilder setDialog(ObjectReader<? extends CharSequence> dialog) {
        _instance._dialog = dialog;
        return this;
      }
    }

    public static class Iterator implements ObjectIterator<CharacterDialogStruct> {
      private ObjectIterator<? extends CharSequence> _character;

      private ObjectIterator<? extends CharSequence> _dialog;

      public Iterator(Reader owner) {
        _character = owner._character == null ? null : owner._character.iterator();
        _dialog = owner._dialog.iterator();
      }

      @Override
      public boolean hasNext() {
        return _dialog.hasNext();
      }

      @Override
      public CharacterDialogStruct next() {
        CharacterDialogStruct res = new CharacterDialogStruct();
        if (_character != null) {
          res.character = _character.next();
        }
        res.dialog = _dialog.next();
        return res;
      }

      @Override
      public void close() {
        if (_character != null) {
          _character.close();
        }
        _dialog.close();
      }
    }
  }

  public static class Schema implements RowSchema<CharacterDialogStruct, CharacterDialogStruct> {
    private final ArrayList<RowSchema.FieldSchema<CharacterDialogStruct>> _fields = new ArrayList<RowSchema.FieldSchema<CharacterDialogStruct>>(2);

    public static Helper.Dialog.SchemaBuilder builder() {
      return new Builder();
    }

    @Override
    public CharacterDialogStruct createAccumulator() {
      return new CharacterDialogStruct();
    }

    @Override
    public CharacterDialogStruct finish(CharacterDialogStruct accumulator) {
      return accumulator;
    }

    @Override
    public Collection<RowSchema.FieldSchema<CharacterDialogStruct>> getFields() {
      return _fields;
    }

    private static class Builder implements Helper.CompletedSchemaBuilder, Helper.Dialog.SchemaBuilder {
      private Schema _instance = new Schema();

      public Schema build() {
        return _instance;
      }

      @Override
      public Helper.CompletedSchemaBuilder setCharacterColumnIndex(int characterColumnIndex) {
        return setCharacterColumnIndex(characterColumnIndex, s -> s);
      }

      @Override
      public Helper.CompletedSchemaBuilder setDialogColumnIndex(int dialogColumnIndex) {
        return setDialogColumnIndex(dialogColumnIndex, s -> s);
      }

      @Override
      public Helper.CompletedSchemaBuilder setCharacterColumnIndex(int characterColumnIndex,
          Function1<String, CharSequence> parser) {
        _instance._fields.add(new RowSchema.Field.Indexed<CharacterDialogStruct>() {
          @Override
          public boolean isRequired() {
            return false;
          }

          @Override
          public void read(CharacterDialogStruct accumulator, String fieldText) {
            accumulator.character = parser.apply(fieldText);
          }

          @Override
          public int getIndex() {
            return characterColumnIndex;
          }
        });
        return this;
      }

      @Override
      public Helper.CompletedSchemaBuilder setDialogColumnIndex(int dialogColumnIndex,
          Function1<String, CharSequence> parser) {
        _instance._fields.add(new RowSchema.Field.Indexed<CharacterDialogStruct>() {
          @Override
          public boolean isRequired() {
            return true;
          }

          @Override
          public void read(CharacterDialogStruct accumulator, String fieldText) {
            accumulator.dialog = parser.apply(fieldText);
          }

          @Override
          public int getIndex() {
            return dialogColumnIndex;
          }
        });
        return this;
      }

      @Override
      public Helper.CompletedSchemaBuilder setCharacterColumnName(String characterColumnName) {
        return setCharacterColumnName(characterColumnName, s -> s);
      }

      @Override
      public Helper.CompletedSchemaBuilder setDialogColumnName(String dialogColumnName) {
        return setDialogColumnName(dialogColumnName, s -> s);
      }

      @Override
      public Helper.CompletedSchemaBuilder setCharacterColumnName(String characterColumnName,
          Function1<String, CharSequence> parser) {
        _instance._fields.add(new RowSchema.Field.Named<CharacterDialogStruct>() {
          @Override
          public boolean isRequired() {
            return false;
          }

          @Override
          public void read(CharacterDialogStruct accumulator, String fieldText) {
            accumulator.character = parser.apply(fieldText);
          }

          @Override
          public String getName() {
            return characterColumnName;
          }
        });
        return this;
      }

      @Override
      public Helper.CompletedSchemaBuilder setDialogColumnName(String dialogColumnName,
          Function1<String, CharSequence> parser) {
        _instance._fields.add(new RowSchema.Field.Named<CharacterDialogStruct>() {
          @Override
          public boolean isRequired() {
            return true;
          }

          @Override
          public void read(CharacterDialogStruct accumulator, String fieldText) {
            accumulator.dialog = parser.apply(fieldText);
          }

          @Override
          public String getName() {
            return dialogColumnName;
          }
        });
        return this;
      }

      @Override
      public Helper.CompletedSchemaBuilder setCharacterParser(BiFunction<String[], String[], CharSequence> parser) {
        _instance._fields.add(new RowSchema.AllFields<CharacterDialogStruct>() {
          @Override
          public boolean isRequired() {
            return false;
          }

          @Override
          public void read(CharacterDialogStruct accumulator, String[] fieldNames,
              String[] fieldText) {
            accumulator.character = parser.apply(fieldNames, fieldText);
          }
        });
        return this;
      }

      @Override
      public Helper.CompletedSchemaBuilder setDialogParser(BiFunction<String[], String[], CharSequence> parser) {
        _instance._fields.add(new RowSchema.AllFields<CharacterDialogStruct>() {
          @Override
          public boolean isRequired() {
            return true;
          }

          @Override
          public void read(CharacterDialogStruct accumulator, String[] fieldNames,
              String[] fieldText) {
            accumulator.dialog = parser.apply(fieldNames, fieldText);
          }
        });
        return this;
      }
    }
  }
}
