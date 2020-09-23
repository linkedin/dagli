package com.linkedin.dagli.transformer;

import com.linkedin.dagli.distribution.DenseVectorizedDistribution;
import com.linkedin.dagli.distribution.SparseVectorizedDistribution;
import com.linkedin.dagli.evaluation.MultinomialEvaluation;
import com.linkedin.dagli.function.FunctionResult1;
import com.linkedin.dagli.function.FunctionResult2;
import com.linkedin.dagli.function.FunctionResultVariadic;
import com.linkedin.dagli.list.NgramVector;
import com.linkedin.dagli.math.distribution.BinaryDistribution;
import com.linkedin.dagli.math.vector.DenseDoubleArrayVector;
import com.linkedin.dagli.math.vector.DenseFloatArrayVector;
import com.linkedin.dagli.math.vector.SparseFloatArrayVector;
import com.linkedin.dagli.meta.BestModel;
import com.linkedin.dagli.meta.KFoldCrossTrained;
import com.linkedin.dagli.meta.NullFiltered;
import com.linkedin.dagli.meta.PreparedByGroup;
import com.linkedin.dagli.map.DictionaryValue;
import com.linkedin.dagli.object.Index;
import com.linkedin.dagli.object.Max;
import com.linkedin.dagli.object.Multiplicity;
import com.linkedin.dagli.placeholder.Placeholder;
import com.linkedin.dagli.producer.MissingInput;
import com.linkedin.dagli.tester.Tester;
import com.linkedin.dagli.vector.CategoricalFeatureVector;
import com.linkedin.dagli.vector.CompositeSparseVector;
import com.linkedin.dagli.vector.DensifiedVector;
import com.linkedin.dagli.vector.LazyConcatenatedDenseVector;
import com.linkedin.dagli.vector.LazyFilteredVector;
import com.linkedin.dagli.vector.NearestVector;
import com.linkedin.dagli.vector.ScoredVector;
import com.linkedin.dagli.vector.DenseVectorFromNumbers;
import com.linkedin.dagli.vector.TopVectorElementsByMutualInformation;
import com.linkedin.dagli.vector.TopVectorElementsByPMI;
import com.linkedin.dagli.vector.VectorAsDoubleArray;
import com.linkedin.dagli.vector.VectorSum;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;


public class CommonTransformerTests {
  private static Integer dummyMethod(Integer a, Integer b) {
    return 1;
  }

  private static Integer identity(Integer a) {
    return a;
  }

  private static Integer sum(List<? extends Integer> values) {
    return values.stream().mapToInt(v -> v).sum();
  }

  @Test
  public void basicTests() {
    FunctionResult2<Integer, Integer, Integer> biFunctionResult =
        new FunctionResult2<Integer, Integer, Integer>().withFunction(CommonTransformerTests::dummyMethod);

    Tester.of(biFunctionResult).input(0, 0).output(1).test();

    Tester.of(new DenseVectorizedDistribution.Prepared<>(Collections.singletonList(true)))
        .input(new BinaryDistribution(0.7))
        .output(DenseFloatArrayVector.wrap(0.7f))
        .test();

    Tester.of(new DensifiedVector())
        .input(DenseFloatArrayVector.wrap(1))
        .output(DenseFloatArrayVector.wrap(1))
        .test();

    Tester.of(new CategoricalFeatureVector())
        .input(Arrays.asList(1, 3, 3, 7))
        .outputTest(vec -> vec.size64() == 4 && vec.norm(1) == 4)
        .test();

    Tester.of(new LazyFilteredVector().withIndicesToKeep(new LongOpenHashSet(3)))
        .input(DenseFloatArrayVector.wrap(1, 2, 3))
        .output(DenseFloatArrayVector.wrap())
        .test();

    Tester.of(new FunctionResult1<>(CommonTransformerTests::identity))
        .input(5)
        .output(5)
        .test();

    Tester.of(new KFoldCrossTrained<>(new TriviallyPreparable<>(biFunctionResult)).withSplitCount(5).withSeed(0))
        .input(2, 3, "grp")
        .skipNonTrivialEqualityCheck() // TriviallyPreparable doesn't implement robust equality
        .test();

    Tester.of(new BestModel<>().withCandidate(
        new TriviallyPreparable<>(biFunctionResult.withInputs(new Placeholder<>(), new Placeholder<>())))
        .withEvaluator(new MultinomialEvaluation().withActualLabelInput(new Placeholder<>())::withPredictedLabelInput)
        .withSplitCount(5)
        .withSeed(0))
        .skipNonTrivialEqualityCheck() // TriviallyPreparable doesn't implement robust equality
        .input(2, 3, 4, 5, 6)
        .test();

    Long2IntOpenHashMap densificationMap = new Long2IntOpenHashMap();
    densificationMap.defaultReturnValue(-1);
    Tester.of(new DensifiedVector.Prepared(densificationMap))
        .input(DenseFloatArrayVector.wrap())
        .output(DenseFloatArrayVector.wrap())
        .test();

    Tester.of(
        new PreparedByGroup.Prepared<>(Collections.singletonMap(1, new VectorSum().withInputs(MissingInput.get())),
            PreparedByGroup.UnknownGroupPolicy.USE_ANY))
        .output(DenseFloatArrayVector.wrap())
        .input(0, DenseFloatArrayVector.wrap())
        .test();

    Tester.of(new CompositeSparseVector()).input(DenseFloatArrayVector.wrap(0, 1, 2)).test();

    Tester.of(new SparseVectorizedDistribution()).input(new BinaryDistribution(0.5)).test();

    Tester.of(new PreparedByGroup<>().withTransformer(new TriviallyPreparable<>(new VectorSum())))
        .input(0, DenseFloatArrayVector.wrap(3))
        .output(DenseFloatArrayVector.wrap(3))
        .skipNonTrivialEqualityCheck()
        .test();

    Tester.of(new TopVectorElementsByMutualInformation().withMaxElementsToKeep(20))
        .input(true, DenseFloatArrayVector.wrap(4))
        .test();

    Tester.of(new TopVectorElementsByPMI().withMaxElementsToKeep(20).withMinOccurrenceCount(5))
        .input(true, DenseFloatArrayVector.wrap(3))
        .test();

    Tester.of(new FunctionResultVariadic<Integer, Integer>(CommonTransformerTests::sum))
        .input(2, 4)
        .output(6)
        .test();

    Tester.of(new VectorSum())
        .output(DenseFloatArrayVector.wrap(2, 3))
        .input(DenseFloatArrayVector.wrap(1, 2), DenseFloatArrayVector.wrap(1, 1))
        .test();

    Tester.of(new Index<>()
        .withMaxUniqueObjects(2)
        .withInput(MissingInput.get()))
        .output(0)
        .input("A")
        .test();

    Tester.of(new MappedIterable.Prepared<>(new FunctionResult1<>(CommonTransformerTests::identity)::withInput))
        .output(Collections.singletonList(1))
        .input(Collections.singletonList(1))
        .test();

    Tester.of(new NgramVector()).input(Collections.singletonList("A")).test();

    Tester.of(new DenseVectorFromNumbers())
        .output(DenseFloatArrayVector.wrap(1.0f, 2.0f))
        .input(Arrays.asList(1.0, 2.0))
        .test();

    Tester.of(new NullFiltered.Prepared<>(new FunctionResult1<Integer, Integer>().withFunction(CommonTransformerTests::identity)))
        .input(3)
        .output(3)
        .test();

    Tester.of(new NearestVector().withCandidates(Collections.singletonList(DenseFloatArrayVector.wrap(1.0f))))
        .output(ScoredVector.Builder.setIndex(0).setScore(1).setVector(DenseFloatArrayVector.wrap(1.0f)).build())
        .input(DenseFloatArrayVector.wrap(1, 1))
        .test();

    Tester.of(new Multiplicity())
        .output(1L)
        .input("a")
        .test();

    Tester.of(new DictionaryValue<>().withDictionary(Collections.singletonMap("b", 2)))
        .output(2)
        .input("b")
        .test();

    Tester.of(new Max<Double>())
        .input(null)
        .input(null)
        .output(null)
        .output(null)
        .test();

    Tester.of(new Max<Integer>())
        .input(null)
        .input(1)
        .input(2)
        .input(1)
        .input(1)
        .input(null)
        .output(2)
        .test();

    Tester.of(new VectorAsDoubleArray().withIgnoredOutOfBoundsIndices().withConsistentArrayLength())
        .input(new SparseFloatArrayVector(new long[] { -1, 1, 2}, new float[] { -1, 1, 2}), 1L)
        .outputTest(arr -> Arrays.equals(arr, new double[] {0, 1}))
        .test();

    Tester.of(new LazyConcatenatedDenseVector().withInputs(new Placeholder<>(), new Placeholder<>(), new Placeholder<>()))
        .input(1L, 1L, null, new DenseDoubleArrayVector(1, 2, 3), new DenseFloatArrayVector(4, 5, 6),
            new DenseFloatArrayVector(7, 8, 9))
        .outputTest(res -> res.equals(new DenseFloatArrayVector(1, 2, 4, 5)))
        .test();
  }
}
