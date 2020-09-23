package com.linkedin.dagli.calibration;

import com.linkedin.dagli.util.invariant.Arguments;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.function.IntBinaryOperator;
import java.util.stream.IntStream;
import java.util.stream.Stream;


/**
 * Class for training an {@link MonotonicPiecewiseLinearFunction}.
 *
 * Isotonic regression fits a monotonic function to the original data points.
 *
 * This code is ported from the SPARK scala implementation of isotonic regression.
 * See: https://github.com/apache/spark/blob/master/docs/mllib-isotonic-regression.md
 *
 * @author dgolland
 */
class IsotonicRegressor {

  private IsotonicRegressor() {
    // Utility classes should not have a public or default constructor.
  }

  /**
   * Fit an isotonic regression model to the given list of (x,y) data points and return the corresponding
   * {@link MonotonicPiecewiseLinearFunction}.
   *
   * @param x x-coordinate of the data
   * @param y y-coordinate of the data
   * @param increasing boolean flag indicating whether the function is monotonically increasing (true) or
   *                   decreasing (false)
   * @return {@link MonotonicPiecewiseLinearFunction} fit to the data
   * @throws IllegalArgumentException if the input arrays are empty or of different lengths
   */
  static MonotonicPiecewiseLinearFunction train(double[] x, double[] y, boolean increasing) {
    Arguments.check(x.length > 0, "Empty input");
    Arguments.check(x.length == y.length, "Input vectors must be the same length");

    double[][] input = new double[x.length][3];
    for (int i = 0; i < x.length; i++) {
      input[i][0] = y[i];
      input[i][1] = x[i];
      input[i][2] = 1;
    }
    return train(input, increasing);
  }

  /**
   * Train an {@link MonotonicPiecewiseLinearFunction} from the provided input.
   * The input consists of an n-by-3 array, where each row corresponds to a weighted data point: {y, x, weight}.
   *
   * @param input
   * @param increasing boolean indicating whether to fit a monotonically increasing function (otherwise decreasing)
   * @return a fitted {@link MonotonicPiecewiseLinearFunction}
   */
  static MonotonicPiecewiseLinearFunction train(double[][] input, boolean increasing) {
    Arguments.check(input.length > 0, "Empty input");
    Arguments.check(Stream.of(input).allMatch(row -> row.length == 3),
        "Invalid input. Row does not contain 3 columns.");

    final double[][] preprocessedInput;

    if (increasing) {
      preprocessedInput = input;
    } else {
      preprocessedInput = Arrays.stream(input).map(x -> new double[]{-x[0], x[1], x[2]}).toArray(double[][]::new);
    }

    Arrays.sort(preprocessedInput,
        Comparator.comparing((double[] arr) -> arr[1]).thenComparing((double[] arr) -> arr[0]));

    double[][] pooled = poolAdjacentViolators(preprocessedInput);

    double[] predictions = new double[pooled.length];
    double[] boundaries = new double[pooled.length];

    if (increasing) {
      for (int i = 0; i < pooled.length; i++) {
        predictions[i] = pooled[i][0];
        boundaries[i] = pooled[i][1];
      }
    } else {
      for (int i = 0; i < pooled.length; i++) {
        predictions[i] = -pooled[i][0];
        boundaries[i] = pooled[i][1];
      }
    }

    return new MonotonicPiecewiseLinearFunction(boundaries, predictions, increasing);
  }

  /**
   * Run the pool adjacent violators algorithm to fit the isotonic regression model.
   *
   * Code heavily inspired by the spark implementation. The code is translated to java, but the comments and structure
   * are preserved.
   * See: https://github.com/apache/spark/blob/master/mllib/src/main/scala/org/apache/spark/ml/regression/IsotonicRegression.scala
   *
   * @param input Input data of tuples (label, feature, weight). Weights must be non-negative.
   * @return Result tuples (label, feature, weight) where labels were updated
   *         to form a monotone sequence as per isotonic regression definition.
   */
  private static double[][] poolAdjacentViolators(double[][] input) {

    // clean input
    double[][] cleanInput = Stream.of(input).filter(x -> {
      Arguments.check(x[2] >= 0.0, "Negative weight at point " + Arrays.toString(x));
      return x[2] > 0.0;
    }).toArray(double[][]::new);

    // Keeps track of the start and end indices of the blocks. if [i, j] is a valid block from
    // cleanInput(i) to cleanInput(j) (inclusive), then blockBounds(i) = j and blockBounds(j) = i
    // Initially, each data point is its own block.
    int[] blockBounds = IntStream.range(0, cleanInput.length).toArray();

    // Keep track of the sum of weights and sum of weight * y for each block. weights(start)
    // gives the values for the block. Entries that are not at the start of a block
    // are meaningless.
    double[][] weights = new double[cleanInput.length][2];
    for (int i = 0; i < cleanInput.length; i++) {
      weights[i][0] = cleanInput[i][2];
      weights[i][1] = cleanInput[i][2] * cleanInput[i][0];
    }

    // Merge two adjacent blocks, updating blockBounds and weights to reflect the merge
    // Return the start index of the merged block
    IntBinaryOperator merge = (block1, block2) -> {

      if (nextBlock(blockBounds, block1) != block2) {
        throw new IllegalStateException(String.format("Attempting to merge non-consecutive blocks %s and %s", block1,
            blockEnd(blockBounds, block2)));
      }

      blockBounds[block1] = blockEnd(blockBounds, block2);
      blockBounds[blockEnd(blockBounds, block2)] = block1;
      weights[block1][0] = weights[block1][0] + weights[block2][0];
      weights[block1][1] = weights[block1][1] + weights[block2][1];
      return block1;
    };

    // Implement Algorithm PAV from [3].
    // Merge on >= instead of > because it eliminates adjacent blocks with the same average, and we
    // want to compress our output as much as possible. Both give correct results.
    for (int i = 0; nextBlock(blockBounds, i) < cleanInput.length; ) {
      if (average(weights, i) >= average(weights, nextBlock(blockBounds, i))) {
        merge.applyAsInt(i, nextBlock(blockBounds, i));
        while ((i > 0) && (average(weights, prevBlock(blockBounds, i)) >= average(weights, i))) {
          i = merge.applyAsInt(prevBlock(blockBounds, i), i);
        }
      } else {
        i = nextBlock(blockBounds, i);
      }
    }

    // construct the output by walking through the blocks in order
    ArrayList<double[]> output = new ArrayList<>();
    for (int i = 0; i < cleanInput.length; ) {
      // If block size is > 1, a point at the start and end of the block,
      // each receiving half the weight. Otherwise, a single point with
      // all the weight.
      if (cleanInput[blockEnd(blockBounds, i)][1] > cleanInput[i][1]) {
        output.add(new double[]{average(weights, i), cleanInput[i][1], weights[i][0] / 2});
        output.add(new double[]{average(weights, i), cleanInput[blockEnd(blockBounds, i)][1], weights[i][0] / 2});
      } else {
        output.add(new double[]{average(weights, i), cleanInput[i][1], weights[i][0]});
      }
      i = nextBlock(blockBounds, i);
    }

    return output.toArray(new double[output.size()][]);
  }

  /**
   * Return the end of the block.
   *
   * Convenience method to make the code more legible.
   */
  private static int blockEnd(int[] blockBounds, int start) {
    return blockBounds[start];
  }

  /**
   * Return the start of the block.
   *
   * Convenience method to make the code more legible.
   */
  private static int blockStart(int[] blockBounds, int end) {
    return blockBounds[end];
  }

  /**
   * @return The next block starts at the index after the end of this block
   */
  private static int nextBlock(int[] blockBounds, int start) {
    return blockEnd(blockBounds, start) + 1;
  }

  /**
   * @return The previous block ends at the index before the start of this block we then use {@link #blockStart} to find
   *        the start
   */
  private static int prevBlock(int[] blockBounds, int start) {
    return blockStart(blockBounds, start - 1);
  }

  /**
   * @return The average value of a block
   */
  private static double average(double[][] weights, int start) {
    return weights[start][1] / weights[start][0];
  }
}
