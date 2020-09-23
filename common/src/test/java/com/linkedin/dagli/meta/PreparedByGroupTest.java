package com.linkedin.dagli.meta;

import com.linkedin.dagli.dag.DAG;
import com.linkedin.dagli.dag.DAG2x2;
import com.linkedin.dagli.object.Multiplicity;
import com.linkedin.dagli.placeholder.Placeholder;
import com.linkedin.dagli.tester.Tester;
import com.linkedin.dagli.transformer.PreparedTransformer;
import com.linkedin.dagli.tuple.Tuple2;
import java.util.Arrays;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class PreparedByGroupTest {
  @Test
  void testMultiplicityByGroup() {
    Placeholder<String> group = new Placeholder<>();
    Placeholder<Integer> items = new Placeholder<>();

    Multiplicity multiplicity = new Multiplicity().withInput(items);
    PreparedByGroup<Long> multiplicityByGroup = new PreparedByGroup<Long>().withGroupInput(group)
        .withTransformer(multiplicity)
        .withUnknownGroupPolicy(PreparedByGroup.UnknownGroupPolicy.RETURN_NULL);

    PreparedTransformer<Map<Object, Long>> multiplicityMap = multiplicityByGroup.toResultsByGroup();

    DAG2x2<String, Integer, Long, Map<Object, Long>> dag =
        DAG.withPlaceholders(group, items).withOutputs(multiplicityByGroup, multiplicityMap);

    Tester.of(dag)
        .allParallelInputs(Arrays.asList("A", "A", "A", "A", "A", "B", "B", "B", "B", "B"),
            Arrays.asList(1, 1, 1, 1, 2, 1, 2, 3, 3, 3))
        .preparedTransformerTester(preparedDAG -> {
          Tuple2<Long, Map<Object, Long>> result1 = preparedDAG.apply("A", 1);
          Assertions.assertEquals(result1.get0(), 4); // four 1s in group A
          Assertions.assertEquals(result1.get1().size(), 2); // should be two entries in multiplicity map
          Assertions.assertEquals(result1.get1().get("A"), 4); // four 1s in group A
          Assertions.assertEquals(result1.get1().get("B"), 1); // one 1s in group B

          Tuple2<Long, Map<Object, Long>> result2 = preparedDAG.apply("C", 3);
          Assertions.assertNull(result2.get0()); // group "C" was not present in preparation
          Assertions.assertEquals(result2.get1().size(), 2); // should be two entries in multiplicity map
          Assertions.assertEquals(result2.get1().get("A"), 0); // zero 3s in group A
          Assertions.assertEquals(result2.get1().get("B"), 3); // three 3s in group B
        }).test();
  }
}
