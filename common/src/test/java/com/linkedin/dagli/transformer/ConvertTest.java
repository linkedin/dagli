package com.linkedin.dagli.transformer;

import com.linkedin.dagli.object.Convert;
import com.linkedin.dagli.producer.MissingInput;
import com.linkedin.dagli.tester.Tester;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.params.provider.Arguments.*;


public class ConvertTest {
  @Test
  public void test() {
    Tester.of(Convert.Number.toFloat(MissingInput.get()))
        .input(4)
        .output(4f)
        .input(null)
        .output(null)
        .test();

    Tester.of(Convert.String.toFloat(MissingInput.get()))
        .input("4")
        .output(4f)
        .input(null)
        .output(null)
        .test();
  }

  @ParameterizedTest
  @MethodSource
  public void testConversions(PreparedTransformer1<Object, Object> transformer, Object input, Object output) {
    Tester.of(transformer)
        .input(input)
        .output(output)
        .test();
  }

  // this method provides the parameters for the testConversions parameterized test, above
  static Stream<Arguments> testConversions() {
    return Stream.of(
        arguments(Convert.String.toFloat(MissingInput.get()), "4.5", 4.5f),
        arguments(Convert.String.toBoolean(MissingInput.get()), "true", true),
        arguments(Convert.String.toByte(MissingInput.get()), "42", (byte) 42),
        arguments(Convert.String.toDouble(MissingInput.get()), "4.5", 4.5d),
        arguments(Convert.String.toInteger(MissingInput.get()), "42", 42),
        arguments(Convert.String.toLong(MissingInput.get()), "42", 42L),
        arguments(Convert.String.toShort(MissingInput.get()), "42", (short) 42),
        arguments(Convert.Number.toFloat(MissingInput.get()), 4.5d, 4.5f),
        arguments(Convert.Number.toByte(MissingInput.get()), 42d, (byte) 42),
        arguments(Convert.Number.toDouble(MissingInput.get()), 4.5f, 4.5d),
        arguments(Convert.Number.toInteger(MissingInput.get()), 42d, 42),
        arguments(Convert.Number.toLong(MissingInput.get()), 42f, 42L),
        arguments(Convert.Number.toShort(MissingInput.get()), 42L, (short) 42)
    );
  }
}
