package com.linkedin.dagli.liblinear;

import com.linkedin.dagli.dag.LocalDAGExecutor;
import com.linkedin.dagli.math.vector.DenseFloatArrayVector;
import com.linkedin.dagli.math.vector.DenseVector;
import com.linkedin.dagli.math.vector.Vector;
import com.linkedin.dagli.producer.MissingInput;
import com.linkedin.dagli.tester.Tester;
import com.linkedin.dagli.transformer.PreparedTransformer3;
import com.linkedin.dagli.vector.LazyFilteredVector;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


public class TopVectorElementsByLiblinearMTWeightTest {
  @Test
  public void testBasic() {
    TopVectorElementsByLiblinearWeight top = new TopVectorElementsByLiblinearWeight().withTopK(2)
        .internalAPI()
        .withInputs(MissingInput.get(), MissingInput.get(), MissingInput.get());

    Tester.of(top).input(1.0, true, DenseFloatArrayVector.wrap(3)).test();

    List<Boolean> labels = Arrays.asList(true, false, true, false);
    List<DenseFloatArrayVector> vectors =
        Arrays.asList(
            DenseFloatArrayVector.wrap(1.0f, 0.0f, 1.0f, 0.0f), DenseFloatArrayVector.wrap(1.0f, 1.0f, 0.0f, 0.0f),
            DenseFloatArrayVector.wrap(0.0f, 0.0f, 1.0f, 1.0f), DenseFloatArrayVector.wrap(0.0f, 1.0f, 0.0f, 0.0f));

    PreparedTransformer3<Number, Object, DenseVector, Vector> prepared = top.internalAPI()
        .prepare(new LocalDAGExecutor().withMaxThreads(1), Collections.nCopies(labels.size(), 1), labels, vectors)
        .getPreparedTransformerForNewData();

    LongOpenHashSet set = new LongOpenHashSet();
    set.add(1);
    set.add(2);
    LazyFilteredVector filter = new LazyFilteredVector().withIndicesToKeep(set);

    for (int i = 0; i < vectors.size(); i++) {
      assertEquals(prepared.apply(1, false, vectors.get(i)), filter.apply(vectors.get(i)));
    }
  }
}
