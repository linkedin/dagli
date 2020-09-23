package com.linkedin.dagli.assorted;

import com.linkedin.dagli.tester.Tester;
import org.junit.jupiter.api.Test;


/**
 * This is an example of how to use Dagli's {@link com.linkedin.dagli.tester.Tester} to test the
 * {@link NormalizedDouble} preparable transformer.
 */
public class NormalizedDoubleTest {
  @Test
  public void test() {
    // Note that it's not (usually) necessary to configure a transformer's input producers prior to testing, as Dagli
    // will automatically configure placeholder inputs for the transformer.  keepOriginalParents() may be used to
    // disable this when specific producer inputs are required (sometimes true when testing advanced functionality,
    // especially reductions [optimizations that rewrite the DAG containing the transformer]).

    // First test: what happens if we prepare with no inputs?  We know what the default prepared transformer should
    // look like:
    Tester.of(new NormalizedDouble())
        .preparedTransformerExpected(new NormalizedDouble.Prepared().withMin(0).withMax(1));

    // Second test: what if there's a single distinct input value?  Outputs for any input should be 0.
    Tester.of(new NormalizedDouble())
        .input(5.0)
        .input(5.0)
        .preparedTransformerTester(prepared -> Tester.of(prepared)
            .input(-3.0)
            .output(0.0)
            .input(5.0)
            .output(0.0)
            .input(9.0)
            .output(0.0)
            .test())
        .test();

    // Third test: dealing with both positive and negative infinities
    Tester.of(new NormalizedDouble())
        .input(5.0)
        .input(Double.NEGATIVE_INFINITY)
        .input(Double.POSITIVE_INFINITY)
        .output(Double.NaN)
        .output(Double.NaN)
        .output(Double.NaN)
        .test();

    // Fourth test: dealing with positive infinity only
    Tester.of(new NormalizedDouble())
        .input(5.0)
        .input(-5.0)
        .input(Double.POSITIVE_INFINITY)
        .output(0.0)
        .output(0.0)
        .output(Double.NaN)
        .test();

    // Fifth test: finite inputs
    Tester.of(new NormalizedDouble())
        .input(5.0)
        .input(1.0)
        .input(-5.0)
        .output(1.0)
        .output(0.6)
        .output(0.0)
        .preparedTransformerTester(
            prepared -> Tester.of(prepared).input(-8.0).output(0.0).input(8.0).output(1.0).test())
        .test();
  }
}
