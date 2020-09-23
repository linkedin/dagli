package com.linkedin.dagli;

import com.linkedin.dagli.generator.Constant;
import com.linkedin.dagli.objectio.ConstantReader;
import com.linkedin.dagli.annotation.struct.HasStructField;
import com.linkedin.dagli.annotation.struct.Optional;
import com.linkedin.dagli.annotation.struct.Struct;
import com.linkedin.dagli.producer.Producer;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class StructAnnotationTest {
  @Struct("MyStruct")
  public static class StructDef implements Serializable {
    private static final long serialVersionUID = 1;

    /**
     * Comments can be added to fields.
     *
     * These will be copied to their accessors on the @Struct.
     */
    String _name;

    @Optional
    String _title = "Dr.";

    int _age;
    String _favoriteBand;

    @Optional
    String _favoriteColor = "red";
  }

  @Struct("MyStruct2")
  public static class StructDef2 {
    protected String _name;

    protected int _age;
    protected String _favoriteBand;
  }

  @Struct("MyStruct3")
  public static class StructDef3 {
    @Optional
    protected String _title = "Dr.";

    @Optional
    protected String _favoriteColor = "red";
  }

  @Struct("GenericStruct")
  public static class GenericStructDef<R, S, T extends CharSequence> implements Serializable {
    private static final long serialVersionUID = 1;

    Collection<R> _field0;
    S _field1;
    T _field2;

    @Optional
    List<T> _field3 = null;

    public static <R, S, T extends CharSequence> boolean doSomething(GenericStruct struct, boolean good, Collection<T> c) {
      return !good;
    }

    public boolean doSomething(boolean good) {
      return _field0.isEmpty();
    }

    public static boolean doSomethingSimple(boolean input) {
      return !input;
    }
  }

  @Struct("MyStructWithoutUnderscoredFieldNames")
  public static class StructWithoutUnderscoredFieldNamesDef implements Serializable {
    private static final long serialVersionUID = 1;

    /**
     * Comments can be added to fields.
     *
     * These will be copied to their accessors on the @Struct.
     */
    String name;

    @Optional
    String title = "Dr.";

    int age;
    String favoriteBand;

    @Optional
    String favoriteColor = "red";
  }

  @Struct("GenericStructWithoutUnderscoredFieldNames")
  public static class GenericStructWithoutUnderscoredFieldNamesDef<R, S, T extends CharSequence>
      implements Serializable {
    private static final long serialVersionUID = 1;

    Collection<R> field0;
    S field1;
    T field2;

    @Optional
    List<T> field3 = null;

    public static <R, S, T extends CharSequence> boolean doSomething(GenericStructWithoutUnderscoredFieldNames struct,
        boolean good, Collection<T> c) {
      return !good;
    }

    public boolean doSomething(boolean good) {
      return field0.isEmpty();
    }

    public static boolean doSomethingSimple(boolean input) {
      return !input;
    }
  }


  @Test
  public void testWithMethods() {
    GenericStruct<String, String, String> gs = GenericStruct.Builder
        .<String, String, String>setField0(new ArrayList<>())
        .setField1("abc")
        .setField2("zyx")
        .build();

    GenericStruct<String, String, String> gs123 = gs.withField1("123").withField3(new ArrayList<>());
    Assertions.assertNotSame(gs, gs123);
    Assertions.assertEquals(gs123.getField1(), "123");
    Assertions.assertEquals(gs123.getField2(), "zyx");
    Assertions.assertNotNull(gs123.getField3());
  }

  @Test
  public void testStructMethods() {
    GenericStruct<String, String, String> gs = GenericStruct.Builder
        .<String, String, String>setField0(new ArrayList<>())
        .setField1("abc")
        .setField2("zyx")
        .build();
    Assertions.assertTrue(GenericStruct.doSomethingSimple(false));
    Assertions.assertTrue(GenericStruct.doSomething(gs, false, null));
    Assertions.assertTrue(gs.doSomething(true));
  }

  @Test
  public void testHashCode() {
    Assertions.assertEquals(-641117220,
        MyStruct.Builder.setName("Jeff").setAge(100).setFavoriteBand("Aardvarks").build().hashCode());
  }

  @Test
  public void testFieldAnnotations() {
    HasStructField[] annos = GenericStruct.class.getAnnotationsByType(HasStructField.class);
    Assertions.assertEquals(annos[0].type(), Collection.class);
    Assertions.assertEquals(annos[1].type(), Object.class);
    Assertions.assertEquals(annos[2].type(), CharSequence.class);
    Assertions.assertEquals(annos[3].type(), List.class);
    Assertions.assertEquals(annos[0].name(), "field0");
    Assertions.assertEquals(annos[1].name(), "field1");
    Assertions.assertEquals(annos[2].name(), "field2");
    Assertions.assertEquals(annos[3].name(), "field3");
    Assertions.assertEquals(annos[0].optional(), false);
    Assertions.assertEquals(annos[1].optional(), false);
    Assertions.assertEquals(annos[2].optional(), false);
    Assertions.assertEquals(annos[3].optional(), true);
  }

  @Test
  public void testFromMap() {
    HashMap<String, Object> genericValues = new HashMap<>();
    genericValues.put("Field0", new ArrayList<>());
    genericValues.put("field1", 4);
    genericValues.put("field2", "blah");
    GenericStruct gs = GenericStruct.fromMap(genericValues);
    Assertions.assertTrue(gs.getField0() instanceof ArrayList);
    Assertions.assertEquals(gs.getField1(), 4);
    Assertions.assertEquals(gs.getField2(), "blah");
    Assertions.assertNull(gs.getField3());

    HashMap<String, Object> stringValues = new HashMap<>();
    stringValues.put("Title", "Ms.");
    MyStruct3 ms = MyStruct3.fromMap(stringValues);
    Assertions.assertEquals(ms.getTitle(), "Ms.");
    Assertions.assertEquals(ms.getFavoriteColor(), "red");
  }

  @Test
  public void testToMap() {
    MyStruct built = MyStruct.Builder
        .setName("Jeff")
        .setAge(100)
        .setFavoriteBand("The Arthropods")
        .setFavoriteColor("blue")
        .build();
    Assertions.assertEquals(built, MyStruct.fromMap(built.toMap()));

    GenericStruct<Integer, Boolean, String> gBuilt = GenericStruct.Builder
        .<Integer, Boolean, String>setField0(new ArrayList<>())
        .setField1(true)
        .setField2("plasma")
        .setField3(Arrays.asList("a", "b", "c"))
        .build();
    Assertions.assertEquals(gBuilt, GenericStruct.fromMap(gBuilt.toMap()));
  }

  @Test
  public void testBuilder() {
    MyStruct built = MyStruct.Builder
        .setName("Jeff")
        .setAge(100)
        .setFavoriteBand("The Arthropods")
        .setFavoriteColor("blue")
        .build();

    Assertions.assertEquals(built.getName(), "Jeff");
    Assertions.assertEquals(built.getAge(), 100);
    Assertions.assertEquals(built.getFavoriteColor(), "blue");
    Assertions.assertEquals(built.getTitle(), "Dr.");
  }

  @Test
  public void testPlaceholder() {
    MyStruct.Placeholder placeholder = new MyStruct.Placeholder();
    Producer<Integer> age = placeholder.asAge();
  }

  @Test
  public void testAssembled() {
    MyStruct.Assembled assembled = MyStruct.Assembled.builder().setName(Constant.nullValue())
        .setAge(Constant.nullValue())
        .setFavoriteBand(Constant.nullValue())
        .build();

    MyStruct built = assembled.internalAPI().applyUnsafe(null, new Object[]{"Jeff", null, 100, "IBM", null});
    Assertions.assertEquals(built.getName(), "Jeff");
    Assertions.assertEquals(built.getAge(), 100);
    Assertions.assertEquals(built.getTitle(), "Dr.");
  }

  @Test
  public void testExtractor() {
    MyStruct built = MyStruct.Builder
        .setName("Jeff")
        .setAge(100)
        .setFavoriteBand("The Arthropods")
        .build();
    MyStruct.Age age = new MyStruct.Age();
    Assertions.assertEquals((long) age.apply(built), 100);
  }

  @Test
  public void testIterable() {
    ConstantReader<String> name = new ConstantReader<>("Jeff", 5);
    ConstantReader<Integer> age = new ConstantReader<>(100, 5);
    ConstantReader<String> band = new ConstantReader<>("IBM", 5);
    ConstantReader<String> color = new ConstantReader<>("blue", 5);

    MyStruct.Reader iterable =
        MyStruct.Reader.builder().setName(name).setAge(age).setFavoriteBand(band).setFavoriteColor(color).build();

    iterable.stream().forEach(struct -> {
      Assertions.assertEquals(struct.getName(), "Jeff");
      Assertions.assertEquals(struct.getAge(), 100);
      Assertions.assertEquals(struct.getFavoriteColor(), "blue");
      Assertions.assertEquals(struct.getTitle(), "Dr.");
    });

    Assertions.assertEquals(iterable.size64(), 5);
  }
}
