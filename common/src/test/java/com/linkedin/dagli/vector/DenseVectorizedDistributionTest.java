package com.linkedin.dagli.vector;

import com.linkedin.dagli.distribution.DenseVectorFromDistribution;
import com.linkedin.dagli.math.distribution.ArrayDiscreteDistribution;
import com.linkedin.dagli.math.distribution.LabelProbability;
import com.linkedin.dagli.math.vector.DenseFloatArrayVector;
import com.linkedin.dagli.placeholder.Placeholder;
import com.linkedin.dagli.tester.Tester;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

public class DenseVectorizedDistributionTest {
  @Test
  public void testBasic() {
    ArrayDiscreteDistribution<String> dd1 = new ArrayDiscreteDistribution<>(
        Arrays.asList(new LabelProbability<String>("One", 0.1), new LabelProbability<String>("Two", 0.2),
            new LabelProbability<String>("Three", 0.3)));
    ArrayDiscreteDistribution<String> dd2 = new ArrayDiscreteDistribution<>(
        Arrays.asList(new LabelProbability<String>("Four", 0.4), new LabelProbability<String>("Two", 0.2),
            new LabelProbability<String>("Five", 0.5)));

    Placeholder<ArrayDiscreteDistribution<String>> placeholder = new Placeholder<>("Distribution");
    DenseVectorFromDistribution.Prepared<String> densifier =
        new DenseVectorFromDistribution.Prepared<>(Arrays.asList("One", "Two", "Three", "Four", "Five")).withInput(
            placeholder);

    Tester.of(densifier)
        .input(dd1)
        .output(DenseFloatArrayVector.wrap(0.1f, 0.2f, 0.3f))
        .input(dd2)
        .output(DenseFloatArrayVector.wrap(0.0f, 0.2f, 0.0f, 0.4f, 0.5f))
        .test();
  }
}
