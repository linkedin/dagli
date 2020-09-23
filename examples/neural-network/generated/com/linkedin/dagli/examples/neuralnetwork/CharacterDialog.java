package com.linkedin.dagli.examples.neuralnetwork;

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

@HasStructField(
    optional = false,
    name = "dialog",
    type = String.class
)
@HasStructField(
    optional = true,
    name = "character",
    type = String.class
)
public class CharacterDialog extends CharacterDialogBase implements Cloneable, Struct {
  protected CharacterDialog() {
    super();
  }

  @Override
  public String toString() {
    return "CharacterDialog(Dialog = " + _dialog + ", " + "Character = " + _character + ")";
  }

  @Override
  public boolean equals(Object other) {
    if (other == null || other.getClass() != this.getClass()) {
      return false;
    }
    CharacterDialog o = (CharacterDialog) other;
    return java.util.Objects.equals(_dialog, o._dialog) && java.util.Objects.equals(_character, o._character);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_character, _dialog);
  }

  /**
   *  The text we'll try to classify with the corresponding character.
   */
  public String getDialog() {
    return _dialog;
  }

  /**
   *  The "label" we're ultimately trying to predict.  Optional because, at inference time, it won't be set.
   */
  public String getCharacter() {
    return _character;
  }

  public static CharacterDialog fromMap(Map<? extends CharSequence, Object> map) {
    com.linkedin.dagli.examples.neuralnetwork.CharacterDialog res = new com.linkedin.dagli.examples.neuralnetwork.CharacterDialog();
    if (!map.containsKey("dialog") && !map.containsKey("Dialog")) { throw new NoSuchElementException("dialog"); };
    res._dialog = (String) map.getOrDefault("dialog", map.getOrDefault("Dialog", res._dialog));
    res._character = (String) map.getOrDefault("character", map.getOrDefault("Character", res._character));
    return res;
  }

  @Override
  public Map<String, Object> toMap() {
    HashMap<String, Object> map = new HashMap<String, Object>(2);
    map.put("dialog", _dialog);
    map.put("character", _character);
    return map;
  }

  /**
   *  The text we'll try to classify with the corresponding character.
   */
  public CharacterDialog withDialog(String dialog) {
    try {
      CharacterDialog res = (CharacterDialog) this.clone();
      res._dialog = dialog;
      return res;
    } catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   *  The "label" we're ultimately trying to predict.  Optional because, at inference time, it won't be set.
   */
  public CharacterDialog withCharacter(String character) {
    try {
      CharacterDialog res = (CharacterDialog) this.clone();
      res._character = character;
      return res;
    } catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
  }

  public interface Builder {
    /**
     *  The text we'll try to classify with the corresponding character.
     */
    static Helper.CompletedBuilder setDialog(String dialog) {
      return new BuilderImpl().setDialog(dialog);
    }
  }

  public static class Placeholder extends com.linkedin.dagli.placeholder.Placeholder<CharacterDialog> {
    private static final long serialVersionUID = 0L;

    private transient Dialog _dialog = null;

    private transient Character _character = null;

    public Dialog asDialog() {
      if (_dialog == null) {
        _dialog = new Dialog().withInput(this);
      }
      return _dialog;
    }

    public Character asCharacter() {
      if (_character == null) {
        _character = new Character().withInput(this);
      }
      return _character;
    }
  }

  private static class BuilderImpl implements Helper.CompletedBuilder, Helper.Dialog.Builder {
    private CharacterDialog _instance = new CharacterDialog();

    public CharacterDialog build() {
      return _instance;
    }

    @Override
    public Helper.CompletedBuilder setDialog(String dialog) {
      _instance._dialog = dialog;
      return this;
    }

    @Override
    public Helper.CompletedBuilder setCharacter(String character) {
      _instance._character = character;
      return this;
    }
  }

  public static class Dialog extends AbstractPreparedTransformer1<CharacterDialog, String, Dialog> {
    private static final long serialVersionUID = 0L;

    public Dialog withInput(Producer<? extends CharacterDialog> input) {
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
      return Collections.singleton(new InverseClassReducer(0, Assembled.class));
    }

    @Override
    public String apply(CharacterDialog struct) {
      return struct._dialog;
    }
  }

  public static class Character extends AbstractPreparedTransformer1<CharacterDialog, String, Character> {
    private static final long serialVersionUID = 0L;

    public Character withInput(Producer<? extends CharacterDialog> input) {
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
      return Collections.singleton(new InverseClassReducer(1, Assembled.class));
    }

    @Override
    public String apply(CharacterDialog struct) {
      return struct._character;
    }
  }

  public static class Helper {
    public interface CompletedBuilder {
      CharacterDialog build();

      /**
       *  The "label" we're ultimately trying to predict.  Optional because, at inference time, it won't be set.
       */
      CompletedBuilder setCharacter(String character);
    }

    public interface CompletedAssembledBuilder {
      Assembled build();

      CompletedAssembledBuilder setCharacter(Producer<? extends String> character);
    }

    public interface CompletedReaderBuilder {
      Reader build();

      CompletedReaderBuilder setCharacter(ObjectReader<? extends String> character);
    }

    public interface CompletedSchemaBuilder {
      Schema build();

      CompletedSchemaBuilder setCharacterColumnIndex(int characterColumnIndex);

      CompletedSchemaBuilder setCharacterColumnIndex(int characterColumnIndex,
          Function1<String, String> parser);

      CompletedSchemaBuilder setCharacterColumnName(String characterColumnName);

      CompletedSchemaBuilder setCharacterColumnName(String characterColumnName,
          Function1<String, String> parser);

      CompletedSchemaBuilder setCharacterParser(BiFunction<String[], String[], String> parser);
    }

    public static class Dialog {
      public interface Builder {
        /**
         *  The text we'll try to classify with the corresponding character.
         */
        CompletedBuilder setDialog(String dialog);
      }

      public interface AssembledBuilder {
        CompletedAssembledBuilder setDialog(Producer<? extends String> dialog);
      }

      public interface ReaderBuilder {
        CompletedReaderBuilder setDialog(ObjectReader<? extends String> dialog);
      }

      public interface SchemaBuilder {
        CompletedSchemaBuilder setDialogColumnIndex(int dialogColumnIndex,
            Function1<String, String> parser);

        CompletedSchemaBuilder setDialogColumnName(String dialogColumnName,
            Function1<String, String> parser);

        CompletedSchemaBuilder setDialogParser(BiFunction<String[], String[], String> parser);

        CompletedSchemaBuilder setDialogColumnIndex(int dialogColumnIndex);

        CompletedSchemaBuilder setDialogColumnName(String dialogColumnName);
      }
    }

    public static class Character {
      public interface Builder {
        /**
         *  The "label" we're ultimately trying to predict.  Optional because, at inference time, it won't be set.
         */
        CompletedBuilder setCharacter(String character);
      }

      public interface AssembledBuilder {
        CompletedAssembledBuilder setCharacter(Producer<? extends String> character);
      }

      public interface ReaderBuilder {
        CompletedReaderBuilder setCharacter(ObjectReader<? extends String> character);
      }

      public interface SchemaBuilder {
        CompletedSchemaBuilder setCharacterColumnIndex(int characterColumnIndex,
            Function1<String, String> parser);

        CompletedSchemaBuilder setCharacterColumnName(String characterColumnName,
            Function1<String, String> parser);

        CompletedSchemaBuilder setCharacterParser(BiFunction<String[], String[], String> parser);

        CompletedSchemaBuilder setCharacterColumnIndex(int characterColumnIndex);

        CompletedSchemaBuilder setCharacterColumnName(String characterColumnName);
      }
    }
  }

  public static class Assembled extends AbstractPreparedTransformerDynamic<CharacterDialog, Assembled> {
    private static final long serialVersionUID = 0L;

    private Producer<? extends String> _dialog = MissingInput.get();

    private Producer<? extends String> _character = DefaultGenerator.get();

    private Assembled() {
    }

    public static Helper.Dialog.AssembledBuilder builder() {
      return new Builder();
    }

    public Assembled withDialog(Producer<? extends String> dialogInput) {
      return clone(c -> c._dialog = dialogInput);
    }

    public Assembled withCharacter(Producer<? extends String> characterInput) {
      return clone(c -> c._character = characterInput);
    }

    @Override
    protected CharacterDialog apply(List<?> values) {
      CharacterDialog res = new CharacterDialog();
      res._dialog = (String) values.get(0);
      if (!(_character instanceof DefaultGenerator)) {
        res._character = (String) values.get(1);
      }
      return res;
    }

    @Override
    protected Assembled withInputsUnsafe(List<? extends Producer<?>> inputs) {
      return clone(c ->  {
        c._dialog = (Producer<? extends String>) inputs.get(0);
        c._character = (Producer<? extends String>) inputs.get(1);
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
      return Arrays.asList(new Producer<?>[] {_dialog, _character});
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
      public Helper.CompletedAssembledBuilder setDialog(Producer<? extends String> dialog) {
        _instance._dialog = dialog;
        return this;
      }

      @Override
      public Helper.CompletedAssembledBuilder setCharacter(Producer<? extends String> character) {
        _instance._character = character;
        return this;
      }
    }
  }

  public static class Reader extends AbstractCloneable<Reader> implements ObjectReader<CharacterDialog>, Serializable {
    private static final long serialVersionUID = 0L;

    private ObjectReader<? extends String> _dialog = null;

    private ObjectReader<? extends String> _character = null;

    private Reader() {
    }

    public ObjectReader<? extends String> getDialogReader() {
      return _dialog;
    }

    public ObjectReader<? extends String> getCharacterReader() {
      return _character;
    }

    public static Helper.Dialog.ReaderBuilder builder() {
      return new Builder();
    }

    public Reader withDialog(ObjectReader<? extends String> dialogInput) {
      return clone(c -> c._dialog = dialogInput);
    }

    public Reader withCharacter(ObjectReader<? extends String> characterInput) {
      return clone(c -> c._character = characterInput);
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
      _dialog.close();
      if (_character != null) {
        _character.close();
      }
    }

    private static class Builder implements Helper.CompletedReaderBuilder, Helper.Dialog.ReaderBuilder {
      private Reader _instance = new Reader();

      public Reader build() {
        return _instance;
      }

      @Override
      public Helper.CompletedReaderBuilder setDialog(ObjectReader<? extends String> dialog) {
        _instance._dialog = dialog;
        return this;
      }

      @Override
      public Helper.CompletedReaderBuilder setCharacter(ObjectReader<? extends String> character) {
        _instance._character = character;
        return this;
      }
    }

    public static class Iterator implements ObjectIterator<CharacterDialog> {
      private ObjectIterator<? extends String> _dialog;

      private ObjectIterator<? extends String> _character;

      public Iterator(Reader owner) {
        _dialog = owner._dialog.iterator();
        _character = owner._character == null ? null : owner._character.iterator();
      }

      @Override
      public boolean hasNext() {
        return _dialog.hasNext();
      }

      @Override
      public CharacterDialog next() {
        CharacterDialog res = new CharacterDialog();
        res._dialog = _dialog.next();
        if (_character != null) {
          res._character = _character.next();
        }
        return res;
      }

      @Override
      public void close() {
        _dialog.close();
        if (_character != null) {
          _character.close();
        }
      }
    }
  }

  public static class Schema implements RowSchema<CharacterDialog, CharacterDialog> {
    private final ArrayList<RowSchema.FieldSchema<CharacterDialog>> _fields = new ArrayList<RowSchema.FieldSchema<CharacterDialog>>(2);

    public static Helper.Dialog.SchemaBuilder builder() {
      return new Builder();
    }

    @Override
    public CharacterDialog createAccumulator() {
      return new CharacterDialog();
    }

    @Override
    public CharacterDialog finish(CharacterDialog accumulator) {
      return accumulator;
    }

    @Override
    public Collection<RowSchema.FieldSchema<CharacterDialog>> getFields() {
      return _fields;
    }

    private static class Builder implements Helper.CompletedSchemaBuilder, Helper.Dialog.SchemaBuilder {
      private Schema _instance = new Schema();

      public Schema build() {
        return _instance;
      }

      @Override
      public Helper.CompletedSchemaBuilder setDialogColumnIndex(int dialogColumnIndex) {
        return setDialogColumnIndex(dialogColumnIndex, s -> s);
      }

      @Override
      public Helper.CompletedSchemaBuilder setCharacterColumnIndex(int characterColumnIndex) {
        return setCharacterColumnIndex(characterColumnIndex, s -> s);
      }

      @Override
      public Helper.CompletedSchemaBuilder setDialogColumnIndex(int dialogColumnIndex,
          Function1<String, String> parser) {
        _instance._fields.add(new RowSchema.Field.Indexed<CharacterDialog>() {
          @Override
          public boolean isRequired() {
            return true;
          }

          @Override
          public void read(CharacterDialog accumulator, String fieldText) {
            accumulator._dialog = parser.apply(fieldText);
          }

          @Override
          public int getIndex() {
            return dialogColumnIndex;
          }
        });
        return this;
      }

      @Override
      public Helper.CompletedSchemaBuilder setCharacterColumnIndex(int characterColumnIndex,
          Function1<String, String> parser) {
        _instance._fields.add(new RowSchema.Field.Indexed<CharacterDialog>() {
          @Override
          public boolean isRequired() {
            return false;
          }

          @Override
          public void read(CharacterDialog accumulator, String fieldText) {
            accumulator._character = parser.apply(fieldText);
          }

          @Override
          public int getIndex() {
            return characterColumnIndex;
          }
        });
        return this;
      }

      @Override
      public Helper.CompletedSchemaBuilder setDialogColumnName(String dialogColumnName) {
        return setDialogColumnName(dialogColumnName, s -> s);
      }

      @Override
      public Helper.CompletedSchemaBuilder setCharacterColumnName(String characterColumnName) {
        return setCharacterColumnName(characterColumnName, s -> s);
      }

      @Override
      public Helper.CompletedSchemaBuilder setDialogColumnName(String dialogColumnName,
          Function1<String, String> parser) {
        _instance._fields.add(new RowSchema.Field.Named<CharacterDialog>() {
          @Override
          public boolean isRequired() {
            return true;
          }

          @Override
          public void read(CharacterDialog accumulator, String fieldText) {
            accumulator._dialog = parser.apply(fieldText);
          }

          @Override
          public String getName() {
            return dialogColumnName;
          }
        });
        return this;
      }

      @Override
      public Helper.CompletedSchemaBuilder setCharacterColumnName(String characterColumnName,
          Function1<String, String> parser) {
        _instance._fields.add(new RowSchema.Field.Named<CharacterDialog>() {
          @Override
          public boolean isRequired() {
            return false;
          }

          @Override
          public void read(CharacterDialog accumulator, String fieldText) {
            accumulator._character = parser.apply(fieldText);
          }

          @Override
          public String getName() {
            return characterColumnName;
          }
        });
        return this;
      }

      @Override
      public Helper.CompletedSchemaBuilder setDialogParser(BiFunction<String[], String[], String> parser) {
        _instance._fields.add(new RowSchema.AllFields<CharacterDialog>() {
          @Override
          public boolean isRequired() {
            return true;
          }

          @Override
          public void read(CharacterDialog accumulator, String[] fieldNames, String[] fieldText) {
            accumulator._dialog = parser.apply(fieldNames, fieldText);
          }
        });
        return this;
      }

      @Override
      public Helper.CompletedSchemaBuilder setCharacterParser(BiFunction<String[], String[], String> parser) {
        _instance._fields.add(new RowSchema.AllFields<CharacterDialog>() {
          @Override
          public boolean isRequired() {
            return false;
          }

          @Override
          public void read(CharacterDialog accumulator, String[] fieldNames, String[] fieldText) {
            accumulator._character = parser.apply(fieldNames, fieldText);
          }
        });
        return this;
      }
    }
  }
}
