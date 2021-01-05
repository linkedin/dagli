package com.linkedin.dagli.assorted;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.dag.DAG;
import com.linkedin.dagli.dag.DAG3x1;
import com.linkedin.dagli.placeholder.Placeholder;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.transformer.AbstractPreparedTransformer1WithInput;
import com.linkedin.dagli.util.exception.Exceptions;
import com.linkedin.dagli.util.invariant.Arguments;
import com.linkedin.dagli.view.PreparedTransformerView;
import com.linkedin.dagli.xgboost.XGBoostClassification;
import java.util.ArrayList;
import java.util.Map;
import java.util.Random;
import ml.dmlc.xgboost4j.java.Booster;
import ml.dmlc.xgboost4j.java.XGBoostError;


/**
 * In this example we'll look at how <strong>transformer views</strong> are typically used: either directly or via
 * a convenience method.  Transformer views allow us to see some property of a transformer (quite
 * possibly the prepared transformer itself!) that will be prepared/trained when preparing the DAG.  In this case, we'll
 * use them to access an underlying XGBoost {@link ml.dmlc.xgboost4j.java.Booster} model that is trained by an
 * {@link XGBoostClassification} transformer.
 */
public class SimpleTransformerViewExample {
  private SimpleTransformerViewExample() { }

  /**
   * Creates the DAG that will be prepared (trained) to solve some arbitrary problem of predicting a boolean label given
   * integer and String categorical features and whose (constant) result will be a feature importance map.
   *
   * @return the preparable DAG
   */
  public static DAG3x1<Integer, String, Boolean, Map<String, Double>> createDAG() {
    // Define the placeholders for our two boolean inputs:
    Placeholder<Integer> input1 = new Placeholder<>();
    Placeholder<String> input2 = new Placeholder<>();
    // And our boolean label:
    Placeholder<Boolean> label = new Placeholder<>();

    // Now configure our XGBoost classifier (which, with default settings, will be far more expressive than we actually
    // need for this problem!):
    XGBoostClassification<Boolean> classification = new XGBoostClassification<Boolean>()
        .withLabelInput(label)
        .withFeaturesInput().fromCategoricalValues(input1, input2); // "true" and "false" are the "categories" here

    // Let's say we don't care about the trained model--we just want the feature importances from the Booster object
    // that underlies the trained XGBoostClassification model.  We have two ways to get this object; both will yield
    // the same result (we'll use the first, booster1, later, but the choice doesn't doesn't matter).

    // Option 1: The convenience method.  Oftentimes, Dagli has a convenience method that will (covertly) create a view
    // for you:
    Producer<Booster> booster1 = classification.asBooster();

    // Option 2: Create the view ourselves.  We could create a new implementation extending AbstractTransformerView that
    // does exactly what we need, but for one-off applications it's much easier to just use the PreparedTransformerView
    // to get the prepared transformer itself, then using a FunctionResult1 transformer to pull out whatever value we
    // want.
    // First, get the prepared XGBoostClassification.Prepared instance using a PreparedTransformerView:
    Producer<XGBoostClassification.Prepared<Boolean>> preparedClassifier =
        new PreparedTransformerView<>(classification);
    // Then, take advantage of the XGBoostClassification.Prepared::getBooster method to pull out the booster instance;
    // we use a new transformer class rather than a FunctionResult1:
    Producer<Booster> booster2 = new BoosterFromXGBoostClassification().withInput(preparedClassifier);
    // (the reason for not using FunctionResult1 is technical: on our compiler,
    // XGBoostClassification.Prepared::getBooster is not safely serializable because it's a method that
    // XGBoostClassification.Prepared inherits from an inaccessible class, which causes our compiler to create a "shim"
    // lambda function which is not safely-serializable; the good news is that, in corner cases when a method reference
    // *isn't* safely-serializable and thus can't be safely used with a FunctionResult transformer, you'll see an
    // exception to this effect thrown during DAG creation and can, as we have, just create a small transformer as we
    // have done here.)

    // booster1 and booster2 are both constants and will both have the same result.
    Arguments.check(booster1.hasConstantResult());
    Arguments.check(booster2.hasConstantResult());

    // At this point, we could just make the booster itself the result of our DAG and then do whatever we wanted with it
    // in our client code.  However, it's also simple enough to pull out the feature map we're after within the DAG.
    // In this toy problem, it doesn't really make any difference, but in general it's more efficient to do as much work
    // in the DAG as possible (both because of parallelization and potential graph optimizations).
    // Unfortunately, it's not *quite* as simple as wrapping Booster::getScore with a FunctionResult transformer,
    // because it throws a checked exception.  Instead, we'll just use our own, custom transformer class.
    FeatureImportanceMap featureImportanceMap = new FeatureImportanceMap().withInput(booster1);
    // (we could also use booster2 here--they will both yield the same Booster instance)

    return DAG.withPlaceholders(input1, input2, label).withOutput(featureImportanceMap);
  }

  @ValueEquality
  static class BoosterFromXGBoostClassification extends
      AbstractPreparedTransformer1WithInput<XGBoostClassification.Prepared<?>, Booster, BoosterFromXGBoostClassification> {
    private static final long serialVersionUID = 1;

    @Override
    public Booster apply(XGBoostClassification.Prepared<?> prepared) {
      return prepared.getBooster();
    }
  }

  @ValueEquality
  static class FeatureImportanceMap
      extends AbstractPreparedTransformer1WithInput<Booster, Map<String, Double>, FeatureImportanceMap> {
    private static final long serialVersionUID = 1;

    @Override
    public Map<String, Double> apply(Booster booster) {
      try {
        // We pass an empty array of feature names so XGBoost will use its default names ("f_")
        return booster.getScore(new String[0], Booster.FeatureImportanceType.GAIN);
      } catch (XGBoostError e) {
        throw Exceptions.asRuntimeException(e);
      }
    }
  }

  public static void main(String[] args) {
    // Our data doesn't really matter--we'll just use some arbitrary values to prepare the DAG:
    ArrayList<Integer> feature1 = new ArrayList<>();
    ArrayList<String> feature2 = new ArrayList<>();
    ArrayList<Boolean> label = new ArrayList<>();
    Random random = new Random(1);
    for (int i = 0; i < 10000; i++) {
      int v = random.nextInt(1000);
      feature1.add(v % 11);
      feature2.add(String.valueOf(v % 13));
      label.add(v % 2 == 0);
    }

    DAG3x1.Result<Integer, String, Boolean, Map<String, Double>> result =
        createDAG().prepareAndApply(feature1, feature2, label);

    // The only output of our DAG is the feature importance map, which is a constant value; our result object contains
    // five results, but they're all actually the same value:
    Arguments.check(result.stream().distinct().count() == 1);

    // Because the feature importance map is a constant value, we can actually read it directly from the prepared DAG,
    // too; this has the advantage of being slightly cleaner and making it clear that a constant value is expected:
    Map<String, Double> featureImportanceMap = result.getPreparedDAG().getConstantResult();

    // Finally, let's print out the feature importance map and see what it looks like:
    System.out.println(featureImportanceMap);
    // e.g. "{f10=4.893529415, f21=1.96387255, f20=4.27127647, f14=4.134990573333334, f13=0.853662074, ...}"
    // Because we used categorical features in our DAG, which map each feature value to an arbitrary, unique index, we
    // can't readily connect the numbered features in this map to our original feature values; if we wanted these
    // feature importances to be useful we'd have to be more deterministic and deliberate about how our feature values
    // were mapped to indices.
  }
}
