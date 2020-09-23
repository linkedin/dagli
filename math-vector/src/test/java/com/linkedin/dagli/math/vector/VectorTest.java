package com.linkedin.dagli.math.vector;

import com.linkedin.dagli.util.array.ArraysEx;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Spliterators;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.LongStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class VectorTest {
  /**
   * Generators for mutable vectors.  Generators construct a vector from an array of float element values.
   */
  static final List<Function<float[], MutableVector>> MUTABLE_VECTOR_GENERATORS = Arrays.asList(
      DenseFloatArrayVector::wrap,
      floats -> new DenseDoubleArrayVector(ArraysEx.toDoubles(floats)),
      floats -> new DenseFloatBufferVector(FloatBuffer.wrap(floats.clone()), 0, floats.length),
      floats -> new DenseDoubleBufferVector(DoubleBuffer.wrap(ArraysEx.toDoubles(floats)), 0, floats.length),
      floats -> {
        FloatBuffer buff = ByteBuffer.allocateDirect(4 * (floats.length + 2)).asFloatBuffer();
        for (int i = 0; i < floats.length; i++) {
          buff.put(i + 1, floats[i]);
        }
        return new DenseFloatBufferVector(buff, 1, floats.length);
      },
      floats -> {
        SparseFloatMapVector vec = new SparseFloatMapVector(floats.length);
        for (int i = 0; i < floats.length; i++) {
          vec.put(i, floats[i]);
        }
        return vec;
      },
      floats -> {
        SparseDoubleMapVector vec = new SparseDoubleMapVector(floats.length);
        for (int i = 0; i < floats.length; i++) {
          vec.put(i, floats[i]);
        }
        return vec;
      }
  );
  /**
   * Generators for immutable vectors.
   */
  static final List<Function<float[], Vector>> IMMUTABLE_VECTOR_GENERATORS = Arrays.asList(
      floats -> SparseFloatArrayVector.wrap(LongStream.range(0, floats.length).toArray(), floats.clone()),
      floats -> SparseDoubleArrayVector.wrap(LongStream.range(0, floats.length).toArray(), ArraysEx.toDoubles(floats)),
      floats -> new LazyConcatenatedDenseVector(new DenseFloatArrayVector(floats)),
      floats -> new LazyConcatenatedDenseVector(EmptyVector.INSTANCE, EmptyVector.INSTANCE, new DenseFloatArrayVector(floats), EmptyVector.INSTANCE),
      floats -> new LazyConcatenatedDenseVector(new DenseFloatArrayVector(floats), EmptyVector.INSTANCE),
      floats -> new LazyConcatenatedDenseVector(EmptyVector.INSTANCE, new DenseFloatArrayVector(floats)),
      floats -> new LazyConcatenatedDenseVector(DenseFloatArrayVector.wrap(Arrays.copyOf(floats, floats.length / 2)),
          DenseFloatArrayVector.wrap(Arrays.copyOfRange(floats, floats.length / 2, floats.length))),
      floats -> {
        float[] duplicatedFloats = new float[floats.length * 2];
        System.arraycopy(floats, 0, duplicatedFloats, 0, floats.length);
        System.arraycopy(floats, 0, duplicatedFloats, floats.length, floats.length);
        DenseFloatArrayVector denseVector = DenseFloatArrayVector.wrap(floats);
        return new LazyConcatenatedDenseVector(new DenseVector[]{DenseFloatArrayVector.wrap(duplicatedFloats),
            denseVector.maxNonZeroElementIndex().isPresent() ? new DenseFloatArrayVector(
                floats[(int) denseVector.maxNonZeroElementIndex().getAsLong()]) : EmptyVector.INSTANCE},
            new long[]{denseVector.maxNonZeroElementIndex().orElse(0), 1});
      }
  );

  /**
   * Generators for "concrete" vectors--vectors that actually store elements rather than generate them on the fly from
   * another data source (e.g. another vector)
   */
  static final List<Function<float[], ? extends Vector>> CONCRETE_VECTOR_GENERATORS =
      concat(MUTABLE_VECTOR_GENERATORS, IMMUTABLE_VECTOR_GENERATORS);

  /**
   * Generator for vector wrappers--vectors that perform lazy operations on other, wrapped vectors.  The wrapped
   * vectors are the set of concrete vectors as defined above.
   */
  static final List<Function<float[], Vector>> WRAPPER_VECTOR_GENERATORS =
      getWrapperVectorGenerators(CONCRETE_VECTOR_GENERATORS);

  /**
   * All vector generators, including "concrete" vectors and their wrappers.  Does not include certain "special" types
   * of vectors that have limited representational power, such as the empty vector or vectors with fixed element values.
   */
  static final List<Function<float[], ? extends Vector>> ALL_VECTOR_GENERATORS =
      concat(CONCRETE_VECTOR_GENERATORS, WRAPPER_VECTOR_GENERATORS);

  private static List<Function<float[], Vector>> getWrapperVectorGenerators(
      List<Function<float[], ? extends Vector>> wrappedGenerators) {
    List<Function<float[], Vector>> wrapperGenerators = new ArrayList<>();
    for (Function<float[], ? extends Vector> generator : wrappedGenerators) {
      wrapperGenerators.add(floats -> generator.apply(floats).lazyAdd(DenseFloatArrayVector.wrap()));
      wrapperGenerators.add(floats -> generator.apply(floats).lazySubtract(DenseFloatArrayVector.wrap()));
      wrapperGenerators.add(floats -> generator.apply(floats).lazyNegation().lazyNegation());
      wrapperGenerators.add(floats -> {
        float[] ones = new float[floats.length];
        Arrays.fill(ones, 1);
        return generator.apply(floats).lazyMultiply(DenseFloatArrayVector.wrap(ones));
      });
    }

    return wrapperGenerators;
  }

  @SafeVarargs
  private static <T> List<T> concat(List<? extends T>... lists) {
    int size = 0;
    for (List<? extends T> list : lists) {
      size += list.size();
    }

    ArrayList<T> result = new ArrayList<T>(size);

    for (List<? extends T> list : lists) {
      result.addAll(list);
    }

    return result;
  }

  @Test
  public void runTests() {
    for (Function<float[], ? extends Vector> generator : ALL_VECTOR_GENERATORS) {
      testBasics(generator);
      testToDoubleArray(generator);
      testNormalVectorSerialization(generator);
      toStringSanityCheck(generator);

      for (Function<float[], ? extends Vector> otherGenerator : ALL_VECTOR_GENERATORS) {
        testMath(generator, otherGenerator);
      }
    }

    for (Function<float[], MutableVector> generator : MUTABLE_VECTOR_GENERATORS) {
      testTransformInPlace(generator);
      testMultiplicativeScaling(generator);
      testIncrease(generator);
    }
  }

  @Test
  public void testEmptyVector() {
    ArrayList<Function<float[], ? extends Vector>> emptyGenerators = new ArrayList<>();
    emptyGenerators.add(values -> Vector.empty());
    emptyGenerators.addAll(getWrapperVectorGenerators(emptyGenerators));

    emptyGenerators.forEach(VectorTest::testEmptyVector);
  }

  private static void testEmptyVector(Function<float[], ? extends Vector> generator) {
    testSerialization(generator); // empty vector intializer
    generator.apply(new float[0]).toString(); // make sure this doesn't throw
  }

  @Test
  public void testIndexVector() {
    ArrayList<Function<float[], ? extends Vector>> indexVectorGenerators = new ArrayList<>();
    indexVectorGenerators.add(values -> SparseIndexArrayVector.wrap(
        DenseFloatArrayVector.wrap(values).stream().mapToLong(VectorElement::getIndex).toArray(), 1));
    indexVectorGenerators.addAll(getWrapperVectorGenerators(indexVectorGenerators));

    indexVectorGenerators.forEach(VectorTest::testIndexVector);
  }

  private static void testIndexVector(Function<float[], ? extends Vector> generator) {
    generator.apply(new float[0]).toString(); // make sure this doesn't throw
    generator.apply(new float[]{1}).toString(); // make sure this doesn't throw
    generator.apply(new float[]{1, 0, 1}).toString(); // make sure this doesn't throw

    testSerialization(generator); // empty vector initializer
    testSerialization(generator, 1);
    testSerialization(generator, 1, 0, 1);
  }

  private static void testNormalVectorSerialization(Function<float[], ? extends Vector> generator) {
    testSerialization(generator); // empty vector
    testSerialization(generator, 1);
    testSerialization(generator, 1, 2, 3);
    testSerialization(generator, 1, 0, 2);
    testSerialization(generator, 0);
  }

  private static byte[] objectToBytes(Object o) {
    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ObjectOutputStream oos = new ObjectOutputStream(baos);
      oos.writeObject(o);
      oos.close();
      return baos.toByteArray();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static Object bytesToObject(byte[] bytes) {
    try {
      ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
      ObjectInputStream ois = new ObjectInputStream(bais);
      return ois.readObject();
    } catch (ClassNotFoundException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static void testSerialization(Function<float[], ? extends Vector> generator, float... values) {
    byte[] bytes = objectToBytes(generator.apply(values));
    Object vector = bytesToObject(bytes);
    Assertions.assertEquals(DenseFloatArrayVector.wrap(values), vector);
  }

  private static void toStringSanityCheck(Function<float[], ? extends Vector> generator) {
    generator.apply(new float[] { }).toString();
    generator.apply(new float[] { 1 }).toString();
    generator.apply(new float[] { 1, 0, 2 }).toString();
  }

  private void testMath(Function<float[], ? extends Vector> vg1, Function<float[], ? extends Vector> vg2) {
    Vector v1 = vg1.apply(new float[] {0, 2, 0, 4, -3});
    Vector v2 = vg2.apply(new float[] {-2, 0, 7, 2, -3, 8, 16});

    assertEquals(v1.lazyMultiply(v2), DenseFloatArrayVector.wrap(0, 0, 0, 8, 9, 0, 0));
    assertEquals(v1.lazyAdd(v2), DenseFloatArrayVector.wrap(-2, 2, 7, 6, -6, 8, 16));
    assertEquals(v1.lazySubtract(v2), DenseFloatArrayVector.wrap(2, 2, -7, 2, 0, -8, -16));
    assertEquals(v1.dotProduct(v2), 17, 0.0001);
    assertEquals(v1.lazyMultiply(2), DenseFloatArrayVector.wrap(0, 4, 0, 8, -6));
    assertEquals(v1.lazyDivide(2), DenseFloatArrayVector.wrap(0, 1, 0, 2, -1.5f));
  }

  /**
   * Tests the the elements iterated by a vector match those expected.
   *
   * @param vec the vector to test
   * @param indexValuePairs pairs of values (an index and the accompanying value); for example, index1, value1, index2
   *                        value2, index3, value3...
   */
  private static void testIteratorAndCopier(Vector vec, float... indexValuePairs) {
    testIterator(vec::iterator, indexValuePairs);
    testBasicIterator(vec.stream().iterator(), indexValuePairs);
    testBasicIterator(Spliterators.iterator(vec.spliterator()), indexValuePairs);

    SparseDoubleMapVector copy = new SparseDoubleMapVector(indexValuePairs.length / 2);
    float[] reversedPairs = new float[indexValuePairs.length];
    for (int i = 0; i < indexValuePairs.length; i += 2) {
      long index = (long) indexValuePairs[indexValuePairs.length - i - 2];
      float value = indexValuePairs[indexValuePairs.length - i - 1];
      reversedPairs[i] = index;
      reversedPairs[i + 1] = value;

      copy.put(index, value);
    }
    testIterator(vec::reverseIterator, reversedPairs);

    // check copy-to-array methods
    assertArrayEquals(copy.toFloatArray(), vec.toFloatArray());
    assertArrayEquals(copy.toDoubleArray(), vec.toDoubleArray());
  }

  // tests the standard next()/hasNext() iterator functionality
  private static void testBasicIterator(Iterator<VectorElement> iterator, float... indexValuePairs) {
    for (int i = 0; i < indexValuePairs.length; i += 2) {
      VectorElement expected = new VectorElement((long) indexValuePairs[i], indexValuePairs[i + 1]);
      assertEquals(expected, iterator.next());
    }
    assertFalse(iterator.hasNext());
  }

  // tests comprehensive VectorElementIterator functionality
  private static void testIterator(Supplier<VectorElementIterator> iteratorSupplier, float... indexValuePairs) {
    VectorElementIterator iter1 = iteratorSupplier.get();
    VectorElementIterator iter2 = iteratorSupplier.get();
    testBasicIterator(iteratorSupplier.get(), indexValuePairs);

    for (int i = 0; i < indexValuePairs.length; i += 2) {
      VectorElement expected = new VectorElement((long) indexValuePairs[i], indexValuePairs[i + 1]);

      assertEquals(expected, iter1.mapNext(VectorElement::new));
      iter2.next((idx, val) -> assertEquals(expected, new VectorElement(idx, val)));
    }

    assertFalse(iter1.hasNext());
    assertFalse(iter2.hasNext());

    for (int i = 1; i < 5; i++) {
      int[] count = new int[1];
      int limit = i;
      iteratorSupplier.get().forEachRemainingUntilFalse((idx, val) -> ++count[0] < limit);
      assertEquals(Math.min(indexValuePairs.length / 2, limit), count[0]);
    }
  }

  private void testBasics(Function<float[], ? extends Vector> vectorGenerator) {
    Vector v = vectorGenerator.apply(new float[] { 0.0f, 1.0f, 2.0f });
    assertEquals(v.get(1), 1.0f, 0.0001);
    assertEquals(v.get(0), 0.0, 0.0001);
    assertEquals(v.get(2), 2.0, 0.0001);
    assertEquals(v.size64(), 2);

    testIteratorAndCopier(v, 1, 1, 2, 2);

    VectorElementIterator iter = v.unorderedIterator();
    int nextIndex = iter.mapNext((index, value) -> {
      if (index == 1) {
        assertEquals(1.0, value, 0.0001);
        return 2;
      } else if (index == 2) {
        assertEquals(2.0, value, 0.0001);
        return 1;
      } else {
        throw new AssertionError();
      }
    });
    assertTrue(iter.hasNext());
    iter.next((index, value) -> {
      assertEquals(index, nextIndex);
      assertEquals(value, nextIndex, 0.0001);
    });

    assertFalse(iter.hasNext());

    assertEquals(2, v.maxNonZeroElementIndex().getAsLong());

    Vector v2 = vectorGenerator.apply(new float[] { 1.0f, 2.0f, 0.0f });
    testIteratorAndCopier(v2, 0, 1, 1, 2);

    Vector v3 = vectorGenerator.apply(new float[] {0.0f, 1.0f, 2.0f, 0.0f });
    testIteratorAndCopier(v3, 1, 1, 2, 2);

    Vector clipped = vectorGenerator.apply(new float[] {-1.0f, -0.1f, 0.0f, 1.0f, 2.0f, 0.0f }).lazyClip(-0.5, 1);
    assertEquals(DenseFloatArrayVector.wrap(-0.5f, -0.1f, 0, 1.0f, 1), clipped);
  }

  @Test
  public void testSparseVectors() {
    SparseFloatMapVector shv = new SparseFloatMapVector(10);
    shv.put(-100, 1.0f);
    shv.put(-100, 0.0f);
    shv.put(-1, 1);
    shv.put(0, 1);
    shv.put(1, 1);
    shv.put(100, 0);

    assertEquals(shv.size64(), 3);

    SparseFloatArrayVector ssv = new SparseFloatArrayVector(shv);

    assertEquals(ssv.get(-100), 0, 0.0001);
    assertEquals(ssv.get(-1), 1, 0.0001);
    assertEquals(ssv.get(0), 1, 0.0001);
    assertEquals(ssv.get(1), 1, 0.0001);
    assertEquals(ssv.get(100), 0, 0.0001);

    assertEquals(ssv.size64(), 3);

    assertEquals(ssv, shv);
    assertEquals(ssv.hashCode(), shv.hashCode());

    // iteration testing
    VectorElementIterator shvIter = shv.unorderedIterator();
    int count = 0;
    while (shvIter.hasNext()) {
      count++;
      shvIter.next((index, value) -> {
        assertEquals(shv.get(index), value, 0.0001);
      });
    }
    assertEquals(count, 3);

    VectorElementIterator ssvIter = ssv.unorderedIterator();
    count = 0;
    while (ssvIter.hasNext()) {
      count++;
      ssvIter.next((index, value) -> {
        assertEquals(ssv.get(index), value, 0.0001);
      });
    }
    assertEquals(count, 3);
  }

  private void testToDoubleArray(Function<float[], ? extends Vector> generator) {
    float[] data = new float[101];
    data[0] = 1;
    data[100] = 1;

    Vector vec = generator.apply(data);

    double[] arr = vec.toDoubleArray();
    assertEquals(arr.length, 101);
    assertEquals(arr[0], 1.0, 0.01);
    assertEquals(arr[100], 1.0, 0.01);
    assertEquals(arr[50], 0.0, 0.01);

    vec = generator.apply(new float[0]);
    // an empty array is not actually a strict requirement of the spec, but it's good practice:
    assertEquals(vec.toDoubleArray().length, 0);
  }

  @Test
  public void testAddition() {
    DenseFloatArrayVector dv = DenseFloatArrayVector.wrap(1, 2);
    SparseFloatMapVector shv = new SparseFloatMapVector();
    shv.put(1, 1);
    shv.addInPlace(dv);
    assertEquals(shv.get(0), 1, 0.000001);
    assertEquals(shv.get(1), 3, 0.000001);

    dv.addInPlace(dv);
    assertEquals(dv.get(0), 2, 0.000001);
    assertEquals(dv.get(1), 4, 0.000001);
  }

  @Test
  public void testVectorElementFilteredIterator() {
    DenseFloatArrayVector dv = DenseFloatArrayVector.wrap(new float[] {0, 1, 2, 3, 4, 5});
    VectorElementFilteredIterator iter =
        new VectorElementFilteredIterator(dv.iterator(), (index, value) -> index % 2 == 0);

    iter.next((index, value) -> {
      assertEquals(index, 2);
      assertEquals(value, 2, 0.001);
    });
    iter.next((index, value) -> {
      assertEquals(index, 4);
      assertEquals(value, 4, 0.001);
    });
  }

  @Test
  public void testNorms() {
    assertEquals(1, DenseFloatArrayVector.wrap(0, 10, 0).norm(0), 0.01);
    assertEquals(15, DenseFloatArrayVector.wrap(0, 10, -5).norm(1), 0.01);
    assertEquals(Math.sqrt(8), DenseFloatArrayVector.wrap(0, 2, -2).norm(2), 0.01);
    assertEquals(Math.pow(16, 1.0 / 3.0), DenseFloatArrayVector.wrap(0, 2, -2).norm(3), 0.01);
    assertEquals(10, DenseFloatArrayVector.wrap(5, 0, -10).norm(Double.POSITIVE_INFINITY), 0.01);
  }

  private void testIncrease(Function<float[], ? extends MutableVector> generator) {
    MutableVector dv = generator.apply(new float[]{1, 2, 3});
    assertEquals(1, dv.increase(0, 100));
    assertEquals(101, dv.get(0), 0.0001);
    assertEquals(2, dv.increase(1, -2));
    assertEquals(0, dv.get(1), 0.0001);
    assertEquals(101, dv.increase(0, -101));
    assertEquals(1, dv.size64());
  }

  private void testMultiplicativeScaling(Function<float[], ? extends MutableVector> generator) {
    MutableVector dv = generator.apply(new float[] {1, 2});
    dv.multiplyInPlace(0.5);

    assertEquals(dv.get(0), 0.5, 0.000001);
    assertEquals(dv.get(1), 1, 0.000001);

    SparseFloatMapVector shv = new SparseFloatMapVector();
    shv.put(0, 1);
    shv.put(1, 2);
    shv.multiplyInPlace(0.5);

    assertEquals(shv, dv);
  }

  private void testScalarDivision(Function<float[], ? extends MutableVector> generator) {
    MutableVector dv = generator.apply(new float[] {1, 2});
    dv.divideInPlace(2);

    assertEquals(dv.get(0), 0.5, 0.000001);
    assertEquals(dv.get(1), 1, 0.000001);

    SparseFloatMapVector shv = new SparseFloatMapVector();
    shv.put(0, 1);
    shv.put(1, 2);
    shv.divideInPlace(2);

    assertEquals(shv, dv);
  }

  private void testTransformInPlace(Function<float[], ? extends MutableVector> generator) {
    MutableVector vec = generator.apply(new float[]{1, 2, 3});

    // delete middle element
    vec.transformInPlace((index, value) -> index == 1 ? 0 : value);

    assertEquals(vec, DenseFloatArrayVector.wrap(1, 0, 3));
    assertEquals(vec.size64(), 2);

    // delete all elements
    vec.transformInPlace((index, value) -> 0);
    assertEquals(vec.size64(), 0);
  }

  @Test
  public void testSparseSortedVector() {
    SparseFloatArrayVector vec = SparseFloatArrayVector.wrap(
        new long[] {3, 2, 3, 2, 1, 1, 0, -4, 6, 6, 4},
        new float[]{0, 0, 3, 0, 1, 1, 0, -4, 0, 6, 4});

    assertEquals(vec.size64(), 5);
    assertEquals(vec.get(3), 3.0, 0);
    assertEquals(vec.get(2), 0.0, 0);
    assertEquals(vec.get(-4), -4.0, 0);
    assertEquals(vec.get(0), 0.0, 0);
    assertEquals(vec.get(6), 6.0, 0);
  }

  @Test
  public void testDenseVector() {
    DenseFloatArrayVector v1 = DenseFloatArrayVector.wrap(1, -2, 3, 0);
    DenseFloatArrayVector v2 = DenseFloatArrayVector.wrap(0, 2, 3, 4, 0, 6, 0);
    SparseFloatArrayVector v3 = SparseFloatArrayVector.wrap(new long[]{0, 1}, new float[]{-2, 2});

    assertEquals(v1.dotProduct(v2), 5, 0.00001);
    assertEquals(v1.dotProduct(v3), -6, 0.00001);
  }

  @Test
  public void testDoubleVector() {
    SparseDoubleMapVector v = new SparseDoubleMapVector();
    Random r = new Random(0);
    double[] values = new double[1000000];
    for (int i = 0; i < values.length; i++) {
      values[i] = r.nextDouble();
      v.put(i, values[i]);
    }

    for (int i = 0; i < values.length; i++) {
      assertEquals(values[i], v.get(i), 0.000001);
    }
  }

  @Test
  public void vectorToStringTest() {
    Assertions.assertEquals("[1: 1.0, 2: 2.0, 3: 3.0]", DenseFloatArrayVector.wrap(0, 1, 2, 3).toString());
  }

  @Test
  public void vectorElementTransformedValueIteratorTest() {
    DenseFloatArrayVector v = DenseFloatArrayVector.wrap(0, 1, 2, 3);
    VectorElementTransformedValueIterator transformedIterator =
        new VectorElementTransformedValueIterator(v.iterator(), (index, value) -> 2 * value);

    transformedIterator.next((index, value) -> Assertions.assertEquals(2, value));
    Assertions.assertEquals(4.0, transformedIterator.mapNext((index, value) -> value));
    Assertions.assertTrue(transformedIterator.hasNext());
    transformedIterator.forEachRemaining((index, value) -> Assertions.assertEquals(value, 2 * index));
    Assertions.assertFalse(transformedIterator.hasNext());
  }
}
