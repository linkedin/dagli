package com.linkedin.dagli.vector;

import com.linkedin.dagli.dag.LocalDAGExecutor;
import com.linkedin.dagli.math.vector.DenseFloatArrayVector;
import com.linkedin.dagli.preparer.PreparerResultMixed;
import com.linkedin.dagli.tester.Tester;
import com.linkedin.dagli.transformer.PreparedTransformer2;
import com.linkedin.dagli.math.vector.Vector;
import com.linkedin.dagli.placeholder.Placeholder;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


public class TopVectorElementsByPMITest {
  @Test
  public void testBasic() {
    List<Boolean> labels = Arrays.asList(false, false, false, false, true, true, true);
    List<Vector> vectors = Arrays.asList(DenseFloatArrayVector.wrap(new float[]{1}), DenseFloatArrayVector.wrap(new float[]{1}),
        DenseFloatArrayVector.wrap(new float[]{1}), DenseFloatArrayVector.wrap(new float[]{0, 1}), DenseFloatArrayVector
            .wrap(new float[]{0, 0, 1}),
        DenseFloatArrayVector.wrap(new float[]{0, 1}), DenseFloatArrayVector.wrap(new float[]{1}));

    TopVectorElementsByPMI top = new TopVectorElementsByPMI()
        .withMaxElementsToKeep(2)
        .withMinOccurrenceCount(1)
        .withLabelInput(new Placeholder<Boolean>())
        .withVectorInput(new Placeholder<Vector>());

    Tester.of(top) // basic tests
        .allParallelInputs(labels, vectors)
        .test();

    PreparerResultMixed<? extends PreparedTransformer2<? super Boolean, ? super Vector, ? extends Vector>, PreparedTransformer2<Boolean, Vector, Vector>>
        res = top.internalAPI().prepare(new LocalDAGExecutor().withMaxThreads(1), labels, vectors);

    PreparedTransformer2<Boolean, Vector, Vector> prepared = res.getPreparedTransformerForNewData();

    assertEquals(prepared.apply(false, vectors.get(0)).size64(), 0);
    assertEquals(prepared.apply(false, vectors.get(4)).size64(), 1);
    assertEquals(prepared.apply(false, vectors.get(5)).size64(), 1);
  }
}
