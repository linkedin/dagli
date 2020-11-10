package com.linkedin.dagli.vector;

import com.linkedin.dagli.generator.Constant;
import com.linkedin.dagli.math.vector.DenseFloatArrayVector;
import com.linkedin.dagli.math.vector.DenseVector;
import com.linkedin.dagli.math.vector.Vector;
import com.linkedin.dagli.placeholder.Placeholder;
import com.linkedin.dagli.tester.Tester;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/**
 *
 */
public class DensifiedVectorTest {
  @Test
  public void test() {
    Tester.of(new DensifiedVector())
        .input(DenseFloatArrayVector.wrap(1))
        .output(DenseFloatArrayVector.wrap(1))
        .test();

    Long2IntOpenHashMap densificationMap = new Long2IntOpenHashMap();
    densificationMap.defaultReturnValue(-1);
    Tester.of(new DensifiedVector.Prepared(densificationMap))
        .input(DenseFloatArrayVector.wrap())
        .output(DenseFloatArrayVector.wrap())
        .test();

    Constant<DenseVector> emptyVectorConstant = new Constant<>(Vector.empty());
    Tester.of(new DensifiedVector().withName("child").withInputs(
        emptyVectorConstant,
        new DensifiedVector().withInputs(emptyVectorConstant),
        new DensifiedVector().withName("parent").withInputs(new Placeholder<>(), emptyVectorConstant)))
        .keepOriginalParents()
        .input(DenseFloatArrayVector.wrap(1, 0, 1, 0, 1))
        .input(DenseFloatArrayVector.wrap(1, 1))
        .input(Vector.empty())
        .output(DenseFloatArrayVector.wrap(1, 1, 1))
        .outputTest(vec -> vec.size64() == 2 && vec.capacity() <= 4)
        .output(DenseFloatArrayVector.wrap())
        .reductionTest(stream -> stream.filter(stack -> stack.peek() instanceof DensifiedVector)
            .peek(stack -> Assertions.assertEquals(stack.peek().getName(), "child"))
            .count() == 1)
        .test();
  }
}
