package com.linkedin.dagli.clustering;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.math.vector.Vector;
import com.linkedin.dagli.preparer.AbstractStreamPreparer1;
import com.linkedin.dagli.preparer.PreparerContext;
import com.linkedin.dagli.preparer.PreparerResult;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.transformer.AbstractPreparableTransformer1;
import com.linkedin.dagli.util.invariant.Arguments;
import com.linkedin.dagli.vector.ScoredVector;
import com.linkedin.dagli.vector.VectorAsDoubleArray;
import it.unimi.dsi.fastutil.objects.ObjectBigArrayBigList;
import it.unimi.dsi.fastutil.objects.ObjectBigList;
import java.util.Random;
import java.util.stream.Collectors;
import org.apache.commons.math3.ml.clustering.DoublePoint;
import org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer;
import org.apache.commons.math3.ml.distance.EuclideanDistance;
import org.apache.commons.math3.random.RandomGeneratorFactory;


/**
 * Clusters vectors into k groups via the KMeans++ algorithm and generates the [0, k-1] cluster assignment for each
 * vector.
 */
@ValueEquality
public class KMeansCluster extends AbstractPreparableTransformer1<double[], ScoredVector, NearestDoubleArray, KMeansCluster> {
  private static final long serialVersionUID = 1;

  private long _seed = 0;
  private int _k = 10;
  private int _maxIterations = -1;

  /**
   * Creates a new KMeans clusterer with k = 10 and unlimited iterations.
   */
  public KMeansCluster() {
    super();
  }

  /**
   * Creates a copy of this instance that will obtain {@link Vector} inputs from the specified {@link Producer}.  These
   * vectors will be converted to (dense) arrays and so must not have non-zero elements with negative indices nor
   * should they be too "sparse", since the size of the arrays created will be equal to the highest non-zero element
   * index of any input vector + 1.
   *
   * If you do have "sparse" vectors (or vectors with negative non-zero element indices),
   * {@link com.linkedin.dagli.vector.DensifiedVector} may be used to make them suitable inputs for this transformer.
   *
   * @param vectorInput a producer that will provide {@link Vector} inputs to this transformer
   * @return a copy of this instance that will get its inputs from the specified {@link Producer}
   */
  public KMeansCluster withVectorInput(Producer<? extends Vector> vectorInput) {
    return withInput1(new VectorAsDoubleArray().withInput(vectorInput).withConsistentArrayLength());
  }

  /**
   * Creates a copy of this instance that will obtain its inputs from the specified {@link Producer} of double arrays.
   *
   * @param arrayInput a producer that will provide double[] inputs to this transformer
   * @return a copy of this instance that will get its inputs from the specified {@link Producer}
   */
  public KMeansCluster withArrayInput(Producer<? extends double[]> arrayInput) {
    return withInput1(arrayInput);
  }

  /**
   * Creates a new KMeans clusterer with the specified value of k and unlimited iterations.
   *
   * @param k the number of clusters to be computed
   */
  public KMeansCluster(int k) {
    super();
    _k = k;
  }

  /**
   * Sets the number of clusters that will be computed.  The default number of clusters is 10.
   *
   * @param k the number of clusters
   * @return a copy of this KMeansCluster with the specified k
   */
  public KMeansCluster withK(int k) {
    Arguments.check(k >= 1, "k must be at least 1");
    return clone(c -> c._k = k);
  }

  /**
   * Sets the random seed (by default, 0) used for initialization.  Having fixed seeds ensures that results are
   * consistent from run-to-run.
   *
   * @param seed the random seed to use
   * @return a copy of this KMeansCluster with the specified seed
   */
  public KMeansCluster withSeed(long seed) {
    return clone(c -> c._seed = seed);
  }

  /**
   * Returns a copy of this KMeansCluster transformer with the specified number of maximum iterations that will be used
   * to optimize the clusters.  -1 indicates "unlimited".  The default is unlimited.
   *
   * @param maxIterations the maximum number of iterations that will be performed, or -1 for no limit
   * @return a copy of this KMeansCluster, modified to use the specified maximum number of iterations.
   */
  public KMeansCluster withMaxIterations(int maxIterations) {
    Arguments.check(maxIterations >= -1, "The maximum number of iterations must be either -1 or non-negative");
    return clone(c -> c._maxIterations = maxIterations);
  }

  /**
   * This preparer performs k-means clustering.
   */
  private static class Preparer extends AbstractStreamPreparer1<double[], ScoredVector, NearestDoubleArray> {
    private final KMeansPlusPlusClusterer<DoublePoint> _clusterer;

    //private final ObjectBigList<DoublePoint> _vectors;
    private final ObjectBigList<DoublePoint> _vectors;
    private int maxVectorIndex = -1;

    /**
     * Creates a new Preparer.
     *
     * @param sizeHint the anticipated number of examples that will be seen
     * @param clusterer the clusterer itself (it contains the clustering settings that will be used)
     */
    private Preparer(long sizeHint, KMeansPlusPlusClusterer<DoublePoint> clusterer) {
      _vectors = new ObjectBigArrayBigList<>(sizeHint);
      _clusterer = clusterer;
    }

    @Override
    public PreparerResult<NearestDoubleArray> finish() {
      return new PreparerResult<>(new NearestDoubleArray().withCandidates(_clusterer.cluster(_vectors)
          .stream()
          .map(p -> p.getCenter().getPoint())
          .collect(Collectors.toList())));
    }

    @Override
    public void process(double[] vec) {
      _vectors.add(new DoublePoint(vec));
    }
  }

  @Override
  protected Preparer getPreparer(PreparerContext context) {
    return new Preparer(context.getEstimatedExampleCount(), new KMeansPlusPlusClusterer<DoublePoint>(_k, _maxIterations,
        new EuclideanDistance(), RandomGeneratorFactory.createRandomGenerator(new Random(_seed))));
  }
}
