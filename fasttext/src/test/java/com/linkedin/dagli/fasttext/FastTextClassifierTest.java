package com.linkedin.dagli.fasttext;

import com.linkedin.dagli.dag.DAG;
import com.linkedin.dagli.dag.DAG1x1;
import com.linkedin.dagli.dag.DAG2x1;
import com.linkedin.dagli.dag.SimpleDAGExecutor;
import com.linkedin.dagli.math.distribution.DiscreteDistribution;
import com.linkedin.dagli.math.vector.DenseFloatArrayVector;
import com.linkedin.dagli.fasttext.anonymized.io.BufferedLineReader;
import com.linkedin.dagli.embedding.classification.Embedded;
import com.linkedin.dagli.embedding.classification.PreparedFastTextClassification;
import com.linkedin.dagli.placeholder.Placeholder;
import com.linkedin.dagli.producer.MissingInput;
import com.linkedin.dagli.tester.Tester;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

public class FastTextClassifierTest {
  @Test
  public void test() throws IOException {
    File embeddingsFile = ResourceUtil.createTempFileFromResource("/wiki.en.1000.vec", "ftpretrained", ".vec");
    testPrimary(embeddingsFile);
  }

  private static final List<List<String>> LABELS = Arrays.asList(
      Collections.singletonList("T"),
      Collections.singletonList("T"),
      Collections.singletonList("T"),
      Collections.singletonList("F"),
      Collections.singletonList("F"),
      Collections.singletonList("F")
  );

  private static final List<List<CharSequence>> TOKENS = Arrays.asList(
      Arrays.asList("Bing loves cherries".split(" ")),
      Arrays.asList("Bing enjoys translation".split(" ")),
      Arrays.asList("Bing sits next to me".split(" ")),
      Arrays.asList("Bing owns a fleet of helicopters".split(" ")),
      Arrays.asList("Bing burrows like a gopher".split(" ")),
      Arrays.asList("Bing meanders through the thicket".split(" "))
  );

  public void testPrimary(File embeddingsFile) throws IOException {
    FastTextClassification<String> ftc = new FastTextClassification<String>()
        .withMinTokenCount(1)
        .withBucketCount(200)
        .withEmbeddingLength(300) // match pretrained vectors
        .withPretrainedEmbeddings(embeddingsFile)
        .withThreadCount(1);

    Tester.of(ftc)
        .allParallelInputs(LABELS, TOKENS)
        .preparedTransformerInputLimit(100)
        .test();

    PreparedFastTextClassification<String> model = ftc
        .internalAPI()
        .prepare(new SimpleDAGExecutor(), LABELS, TOKENS)
        .getPreparedTransformerForNewData();

    for (int i = 0; i < TOKENS.size(); i++) {
      DiscreteDistribution<String> dist = model.apply(null, TOKENS.get(i));
      // sanity check for the probabilities
      dist.stream().forEach(lp -> {
        assertTrue(lp.getProbability() >= 0);
        assertTrue(lp.getProbability() <= 1);
      });
      assertEquals(LABELS.get(i).get(0), dist.max().get().getLabel(), "Failed on " + i);
    }
  }

  @Test
  public void testEmbeddings() {
    Placeholder<List<String>> labels = new Placeholder<>();
    Placeholder<List<CharSequence>> texts = new Placeholder<>();

    FastTextClassification<String> ftc = new FastTextClassification<String>()
        .withMinTokenCount(1)
        .withBucketCount(200)
        .withEmbeddingLength(20)
        .withThreadCount(1)
        .withLabelsInput(labels)
        .withTokensInput(texts);

    DAG2x1.Result<List<String>, List<CharSequence>, DenseFloatArrayVector> embeddedTokens =
        DAG.withPlaceholders(labels, texts).withOutput(ftc.asEmbeddedTokens()).prepareAndApply(LABELS, TOKENS);
    Assertions.assertEquals(20, embeddedTokens.toList().get(0).getArray().length);

    DAG2x1.Result<List<String>, List<CharSequence>, DenseFloatArrayVector> embeddedLabel =
        DAG.withPlaceholders(labels, texts).withOutput(ftc.asEmbeddedLabel()).prepareAndApply(LABELS, TOKENS);
    Assertions.assertEquals(20, embeddedLabel.toList().get(0).getArray().length);

    DAG2x1.Result<List<String>, List<CharSequence>, Integer> inVocabCount =
        DAG.withPlaceholders(labels, texts).withOutput(ftc.asTokensInVocabularyCount()).prepareAndApply(LABELS, TOKENS);
    Assertions.assertEquals(3, inVocabCount.toList().get(0).intValue());

    Embedded.Labels<String> embeddedLabels = ftc.asEmbeddedLabels().withLabelsInput(labels);

    DAG2x1.Result<List<String>, List<CharSequence>, List<DenseFloatArrayVector>> elDAG =
        DAG.withPlaceholders(labels, texts).withOutput(embeddedLabels).prepareAndApply(LABELS, TOKENS);

    Placeholder<List<String>> labelsToEmbedPlaceholder = new Placeholder<>();
    Embedded.Labels<String> embeddedLabelsRedux = elDAG.getPreparedDAG()
        .producers(Embedded.Labels.class)
        .findFirst()
        .get()
        .getItem()
        .withLabelsInput(labelsToEmbedPlaceholder);

    DAG1x1.Prepared<List<String>, List<DenseFloatArrayVector>> labelEmbeddings =
        DAG.Prepared.withPlaceholder(labelsToEmbedPlaceholder).withOutput(embeddedLabelsRedux);

    Assertions.assertEquals(20, labelEmbeddings.apply(LABELS.get(0)).get(0).getArray().length);
  }

  @Test
  public void testEmbeddingsConvenienceTransformers() {
    Placeholder<List<String>> labels = new Placeholder<>();
    Placeholder<List<CharSequence>> texts = new Placeholder<>();

    FastTextClassification<String> ftc = new FastTextClassification<String>()
        .withMinTokenCount(1)
        .withBucketCount(200)
        .withEmbeddingLength(20)
        .withThreadCount(1)
        .withLabelsInput(labels)
        .withTokensInput(texts);

    Assertions.assertEquals(20, DAG.withPlaceholders(labels, texts)
        .withOutput(ftc.asEmbeddedLabels().withLabelsInput(labels))
        .prepareAndApply(LABELS, TOKENS)
        .toList()
        .get(0).get(0).getArray().length);

    Assertions.assertEquals(20, DAG.withPlaceholders(labels, texts)
        .withOutput(ftc.asEmbeddedLabels())
        .prepareAndApply(LABELS, TOKENS)
        .toList()
        .get(0).get(0).getArray().length);

    Assertions.assertEquals(20, DAG.withPlaceholders(labels, texts)
        .withOutput(ftc.asEmbeddedTokens())
        .prepareAndApply(LABELS, TOKENS)
        .toList()
        .get(0).getArray().length);

    Assertions.assertEquals(2, DAG.withPlaceholders(labels, texts)
        .withOutput(ftc)
        .prepareAndApply(LABELS, TOKENS)
        .toList()
        .get(0).size64());
  }

  @Test
  public void testSplitting() {
    Assertions.assertArrayEquals(BufferedLineReader.splitLine("Hello!"), new String[] {"Hello!"});
    Assertions.assertArrayEquals(BufferedLineReader.splitLine("Hello !"), new String[] {"Hello", "!"});
    Assertions.assertArrayEquals(BufferedLineReader.splitLine(" Hello  ! "), new String[] {"", "Hello", "", "!", ""});
  }

  private static ArrayList<String> getNgramSentence(String[][] ngrams, int ngramCount, Random r) {
    ArrayList<String> res = new ArrayList<>(ngramCount * 3);
    for (int i = 0; i < ngramCount; i++) {
      res.addAll(Arrays.asList(ngrams[r.nextInt(ngrams.length)]));
    }
    return res;
  }

  @ParameterizedTest
  @EnumSource(FastTextDataSerializationMode.class)
  public void testClassificationAcrossSerializationModes(FastTextDataSerializationMode mode) {
    testClassification(mode, FastTextLoss.NEGATIVE_SAMPLING, 0.9);
  }

  @ParameterizedTest
  @EnumSource(FastTextLoss.class)
  public void testClassificationAcrossLossModes(FastTextLoss lossType) {
    // Heirarchical softmax won't do well in our test; apply lower min accuracy (note: random would be 1/3).
    testClassification(FastTextDataSerializationMode.NORMAL, lossType,
        lossType == FastTextLoss.HEIRARCHICAL_SOFTMAX ? 0.5 : 0.9);
  }

  public void testClassification(FastTextDataSerializationMode dataSerializationMode, FastTextLoss lossType, double minCorrectProportion) {
    final int ngramCount = 100;
    final int trainingExamples = 100000;
    final int evaluationExamples = 10000;
    final int minEvaluationExamplesCorrect = (int) (evaluationExamples * minCorrectProportion);

    String[][] ngramsA = new String[][] {
        new String[] {"a", "b", "c"},
        new String[] {"a", "b"},
        new String[] {"a", "c"},
        new String[] {"b", "c"},
        new String[] {"a"},
        new String[] {"b"},
        new String[] {"c"},
    };
    String[][] ngramsB = new String[][] {
        new String[] {"c", "b", "a"},
        new String[] {"b", "a"},
        new String[] {"c", "a"},
        new String[] {"c", "b"},
        new String[] {"a"},
        new String[] {"b"},
        new String[] {"c"},
    };

    String[][] ngramsC = new String[][] {
        new String[] {"a", "a", "a"},
        new String[] {"b", "b", "b"},
        new String[] {"c", "c", "c"},
        new String[] {"b", "b"},
        new String[] {"a", "a"},
        new String[] {"c", "c"},
        new String[] {"a"},
        new String[] {"b"},
        new String[] {"c"},
    };

    String[][][] allNgrams = new String[][][] { ngramsA, ngramsB, ngramsC };

    ArrayList<ArrayList<String>> tokens = new ArrayList<>();
    ArrayList<List<Integer>> labels = new ArrayList<>();

    Random r = new Random(0);

    for (int i = 0; i < trainingExamples; i++) {
      int label = r.nextInt(3);
      tokens.add(getNgramSentence(allNgrams[label], ngramCount, r));
      labels.add(Collections.singletonList(label));
    }

    FastTextClassification<Integer> ftr = new FastTextClassification<Integer>().withMaxWordNgramLength(3)
        .withDataSerializationMode(dataSerializationMode)
        .withLossType(lossType)
        .withSynchronizedTrainingStart(true)
        .withBucketCount(100); // (greatly) speeds up serialization and deserialization when the tester runs below

    Tester.of(ftr).allParallelInputs(labels, tokens).preparedTransformerInputLimit(100).test();

    PreparedFastTextClassification<Integer> prepared =
        ftr.internalAPI().prepare(new SimpleDAGExecutor(), labels, tokens).getPreparedTransformerForNewData();

    int evaluationExamplesCorrect = 0;
    for (int i = 0; i < evaluationExamples; i++) {
      int label = r.nextInt(3);
      DiscreteDistribution<Integer> cRes = prepared.apply(null, getNgramSentence(allNgrams[label], ngramCount, r));
      if (cRes.max().get().getLabel() == label) {
        evaluationExamplesCorrect++;
      }
    }
    assertTrue(evaluationExamplesCorrect >= minEvaluationExamplesCorrect);
    System.out.println("Accuracy on artificial task: " + ((double) evaluationExamplesCorrect / evaluationExamples));
  }

  @Test
  public void getterSetterTest() {
    Placeholder<String> somePlaceholder = new Placeholder<>();

    // these are just arbitrary values for testing--don't read anything into them
    FastTextClassification<String> ftc = new FastTextClassification<String>()
        .withLabelInput(somePlaceholder)
        .withBucketCount(42)
        .withEmbeddingLength(4)
        .withEpochCount(1337)
        .withLearningRate(0.42)
        .withLearningRateUpdateRate(100)
        .withMaxPredictionCount(19)
        .withMaxWordNgramLength(27)
        .withMinLabelCount(3)
        .withMinTokenCount(2)
        .withMultilabel(true)
        .withSampledNegativeCount(11)
        .withThreadCount(14)
        .withVerbosity(2);

    Assertions.assertNotEquals(MissingInput.get(), ftc.getLabelsInput());
    Assertions.assertEquals(42, ftc.getBucketCount());
    Assertions.assertEquals(4, ftc.getEmbeddingLength());
    Assertions.assertEquals(1337, ftc.getEpochCount());
    Assertions.assertEquals(0.42, ftc.getLearningRate());
    Assertions.assertEquals(100, ftc.getLearningRateUpdateRate());
    Assertions.assertEquals(19, ftc.getMaxPredictionCount());
    Assertions.assertEquals(27, ftc.getMaxWordNgramLength());
    Assertions.assertEquals(3, ftc.getMinLabelCount());
    Assertions.assertEquals(2, ftc.getMinTokenCount());
    Assertions.assertTrue(ftc.isMultilabel());
    Assertions.assertEquals(11, ftc.getSampledNegativeCount());
    Assertions.assertEquals(14, ftc.getThreadCount());
    Assertions.assertEquals(2, ftc.getVerbosity());
  }
}
