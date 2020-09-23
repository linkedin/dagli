package com.linkedin.dagli.liblinear;

import com.linkedin.dagli.dag.LocalDAGExecutor;
import com.linkedin.dagli.math.vector.DenseFloatArrayVector;
import com.linkedin.dagli.math.vector.Vector;
import com.linkedin.dagli.producer.MissingInput;
import com.linkedin.dagli.tester.Tester;
import com.linkedin.dagli.transformer.PreparedTransformer2;
import com.linkedin.dagli.vector.LazyFilteredVector;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


public class TopVectorElementsByLiblinearMTWeightTest {
  @Test
  public void testBasic() {
    TopVectorElementsByLiblinearWeight top = new TopVectorElementsByLiblinearWeight().withTopK(2)
        .internalAPI()
        .withInputs(MissingInput.get(), MissingInput.get());

    Tester.of(top).input(true, DenseFloatArrayVector.wrap(3)).test();

    List<Boolean> labels = Arrays.asList(true, false, true, false);
    List<DenseFloatArrayVector> vectors =
        Arrays.asList(
            DenseFloatArrayVector.wrap(1.0f, 0.0f, 1.0f, 0.0f), DenseFloatArrayVector.wrap(1.0f, 1.0f, 0.0f, 0.0f),
            DenseFloatArrayVector.wrap(0.0f, 0.0f, 1.0f, 1.0f), DenseFloatArrayVector.wrap(0.0f, 1.0f, 0.0f, 0.0f));

    PreparedTransformer2<Object, Vector, Vector> prepared = top.internalAPI()
        .prepare(new LocalDAGExecutor().withMaxThreads(1), labels, vectors)
        .getPreparedTransformerForNewData();

    LongOpenHashSet set = new LongOpenHashSet();
    set.add(1);
    set.add(2);
    LazyFilteredVector filter = new LazyFilteredVector().withIndicesToKeep(set);

    for (int i = 0; i < vectors.size(); i++) {
      assertEquals(prepared.apply(false, vectors.get(i)), filter.apply(vectors.get(i)));
    }
  }
}
