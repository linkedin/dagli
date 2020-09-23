package com.linkedin.dagli.vector;

import com.linkedin.dagli.dag.LocalDAGExecutor;
import com.linkedin.dagli.math.vector.DenseFloatArrayVector;
import com.linkedin.dagli.math.vector.Vector;
import com.linkedin.dagli.math.vector.VectorElement;
import com.linkedin.dagli.preparer.PreparerContext;
import com.linkedin.dagli.preparer.PreparerResultMixed;
import com.linkedin.dagli.placeholder.Placeholder;
import com.linkedin.dagli.tester.Tester;
import com.linkedin.dagli.transformer.PreparedTransformer2;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class TopVectorElementsByMITest {
  @Test
  public void testBasic() {
    List<Boolean> labels = Arrays.asList(false, false, false, false, true, true, true);
    List<Vector> vectors = Arrays.asList(DenseFloatArrayVector.wrap(1), DenseFloatArrayVector.wrap(1), DenseFloatArrayVector
            .wrap(1), DenseFloatArrayVector
            .wrap(0, 1),
        DenseFloatArrayVector.wrap(0, 0, 1), DenseFloatArrayVector.wrap(0, 1), DenseFloatArrayVector.wrap(1));

    TopVectorElementsByMutualInformation top =
        new TopVectorElementsByMutualInformation().withMaxElementsToKeep(2).withLabelInput(new Placeholder<Boolean>())
            .withVectorInput(new Placeholder<Vector>());

    Tester.of(top) // basic tests
        .allParallelInputs(labels, vectors)
        .test();

    TopVectorElementsByMutualInformation.Preparer preparer =
        (TopVectorElementsByMutualInformation.Preparer) top.internalAPI()
            .getPreparer(PreparerContext.builder(labels.size()).setExecutor(new LocalDAGExecutor()).build());

    // prepare the preparer with our data
    for (int i = 0; i < labels.size(); i++) {
      preparer.process(labels.get(i), vectors.get(i));
    }

    // check the calculated MI values and ordering
    TreeSet<VectorElement> topElements = preparer.calculateTopVectorElementsByMI();
    Assertions.assertEquals(topElements.size(), 2);
    Assertions.assertEquals(topElements.first().getIndex(), 0);
    Assertions.assertEquals(topElements.first().getValue(), 0.08878194993480415);
    Assertions.assertEquals(topElements.last().getIndex(), 2);
    Assertions.assertEquals(topElements.last().getValue(), 0.13732453187634627);

    // also make sure we can actually prepare this thing
    PreparerResultMixed<? extends PreparedTransformer2<? super Object, ? super Vector, ? extends Vector>, PreparedTransformer2<Object, Vector, Vector>>
        res = top.internalAPI().prepare(new LocalDAGExecutor().withMaxThreads(1), labels, vectors);


    PreparedTransformer2<Object, Vector, Vector> prepared = res.getPreparedTransformerForNewData();
    Assertions.assertNotNull(prepared);
  }
}
