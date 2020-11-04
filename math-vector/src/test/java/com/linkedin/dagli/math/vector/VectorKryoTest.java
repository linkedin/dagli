package com.linkedin.dagli.math.vector;

import com.esotericsoftware.kryo.kryo5.Kryo;
import com.esotericsoftware.kryo.kryo5.Serializer;
import com.esotericsoftware.kryo.kryo5.io.Input;
import com.esotericsoftware.kryo.kryo5.io.Output;
import com.esotericsoftware.kryo.kryo5.serializers.JavaSerializer;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.FloatBuffer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;


/**
 * Tests various Vector types to make sure they're Kryo-serializable
 */
public class VectorKryoTest {
  private static final Kryo KRYO = new Kryo();
  static {
    KRYO.setRegistrationRequired(false);
    KRYO.setReferences(true);
  }

  @BeforeAll
  public static void registerFloatBufferSerializer() {
    // FloatBufferVector is not normally serializable because it contains the non-Kryo-serializable FloatBuffer.
    // We can register a custom serializer to overcome this.
    KRYO.register(DenseFloatBufferVector.class, new Serializer() {
      @Override
      public void write(Kryo kryo, Output output, Object object) {
        // get the array of double values, then down-cast it to floats
        double[] doubleValues = ((DenseFloatBufferVector) object).toDoubleArray();
        float[] floatValues = new float[doubleValues.length];
        for (int i = 0; i < doubleValues.length; i++) {
          floatValues[i] = (float) doubleValues[i];
        }
        kryo.writeObject(output, floatValues);
      }

      @Override
      public Object read(Kryo kryo, Input input, Class type) {
        float[] array = kryo.readObject(input, float[].class);
        return new DenseFloatBufferVector(FloatBuffer.wrap(array), 0, array.length);
      }
    });

    KRYO.register(DenseDoubleBufferVector.class, new JavaSerializer());
  }

  @Test
  public void test() {
    float[] values = new float[] { 1, 2, 3, 4 };
    VectorTest.ALL_VECTOR_GENERATORS.forEach(generator -> testSerialization(generator.apply(values)));
  }

  private static void testSerialization(Vector vector) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    Output output = new Output(baos);
    KRYO.writeClassAndObject(output, vector);
    output.close();

    Input input = new Input(new ByteArrayInputStream(baos.toByteArray()));
    Vector read = (Vector) KRYO.readClassAndObject(input);
    Assertions.assertEquals(read, vector);
  }
}
