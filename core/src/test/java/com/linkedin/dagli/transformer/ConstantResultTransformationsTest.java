package com.linkedin.dagli.transformer;

import com.linkedin.dagli.dag.DAG;
import com.linkedin.dagli.placeholder.Placeholder;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests the ConstantResultTransformation classes
 */
public class ConstantResultTransformationsTest {
  @Test
  public void testDynamic() {
    Placeholder<Void> placeholder = new Placeholder<>();
    ConstantResultTransformationDynamic<Integer> trivial = new ConstantResultTransformationDynamic<Integer>()
        .withResult(42)
        .withInputsUnsafe(Collections.singletonList(placeholder));
    Assertions.assertEquals(42, DAG.withPlaceholder(placeholder)
        .withOutput(trivial)
        .prepareAndApply(Arrays.asList(null, null))
        .toList()
        .get(0));
  }

  @Test
  public void testVariadic() {
    Placeholder<Void> placeholder = new Placeholder<>();
    ConstantResultTransformationVariadic<Void, Integer>
        trivial = new ConstantResultTransformationVariadic<Void, Integer>()
        .withResult(42)
        .withInputs(placeholder);
    Assertions.assertEquals(42, DAG.withPlaceholder(placeholder)
        .withOutput(trivial)
        .prepareAndApply(Arrays.asList(null, null))
        .toList()
        .get(0));
  }

  @Test
  public void testArity2() {
    Placeholder<Void> placeholder = new Placeholder<>();
    ConstantResultTransformation2<Void, Void, Integer> trivial = new ConstantResultTransformation2<Void, Void, Integer>()
        .withResult(42)
        .withInput1(placeholder)
        .withInput2(placeholder);
    Assertions.assertEquals(42, DAG.withPlaceholder(placeholder)
        .withOutput(trivial)
        .prepareAndApply(Arrays.asList(null, null))
        .toList()
        .get(0));
  }
}
