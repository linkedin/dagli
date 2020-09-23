package com.linkedin.dagli.dag;

import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.reducer.Reducer;
import com.linkedin.dagli.transformer.PreparableTransformer;
import com.linkedin.dagli.transformer.PreparedTransformer;
import com.linkedin.dagli.transformer.Tupled10;
import com.linkedin.dagli.transformer.Tupled2;
import com.linkedin.dagli.transformer.Tupled3;
import com.linkedin.dagli.transformer.Tupled4;
import com.linkedin.dagli.transformer.Tupled5;
import com.linkedin.dagli.transformer.Tupled6;
import com.linkedin.dagli.transformer.Tupled7;
import com.linkedin.dagli.transformer.Tupled8;
import com.linkedin.dagli.transformer.Tupled9;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.stream.Collectors;


/**
 * Reducer for DAGXxY instances that are embedded within other DAGs, replacing the embedded DAG instance with its
 * subgraph.  This is a valuable optimization because it both eliminates the friction of the DAGXxY wrapper and allows
 * for further reductions between the embedded subgraph and the wider containing DAG; even though it is also somewhat
 * expensive, it is considered {@link Reducer.Level#ESSENTIAL}.
 */
class DAGTransformerReducer implements Reducer<DAGTransformer<?, ?>> {
  static final DAGTransformerReducer INSTANCE = new DAGTransformerReducer(); // just need one

  private DAGTransformerReducer() { }

  @Override
  public Level getLevel() {
    return Level.ESSENTIAL;
  }

  private static Producer<?> tupled(List<Producer<?>> outputs) {
    switch (outputs.size()) {
      case 1:
        return outputs.get(0); // "naked" output, without tupling
      case 2:
        return new Tupled2<>(outputs.get(0), outputs.get(1));
      case 3:
        return new Tupled3<>(outputs.get(0), outputs.get(1), outputs.get(2));
      case 4:
        return new Tupled4<>(outputs.get(0), outputs.get(1), outputs.get(2), outputs.get(3));
      case 5:
        return new Tupled5<>(outputs.get(0), outputs.get(1), outputs.get(2), outputs.get(3), outputs.get(4));
      case 6:
        return new Tupled6<>(outputs.get(0), outputs.get(1), outputs.get(2), outputs.get(3), outputs.get(4),
            outputs.get(5));
      case 7:
        return new Tupled7<>(outputs.get(0), outputs.get(1), outputs.get(2), outputs.get(3), outputs.get(4),
            outputs.get(5), outputs.get(6));
      case 8:
        return new Tupled8<>(outputs.get(0), outputs.get(1), outputs.get(2), outputs.get(3), outputs.get(4),
            outputs.get(5), outputs.get(6), outputs.get(7));
      case 9:
        return new Tupled9<>(outputs.get(0), outputs.get(1), outputs.get(2), outputs.get(3), outputs.get(4),
            outputs.get(5), outputs.get(6), outputs.get(7), outputs.get(8));
      case 10:
        return new Tupled10<>(outputs.get(0), outputs.get(1), outputs.get(2), outputs.get(3), outputs.get(4),
            outputs.get(5), outputs.get(6), outputs.get(7), outputs.get(8), outputs.get(9));
      default:
        throw new IllegalArgumentException("Unexpected DAG result arity: " + outputs.size());
    }
  }

  @Override
  @SuppressWarnings("unchecked") // correctness ensured by semantics of DAGs
  public void reduce(DAGTransformer<?, ?> target, Context context) {
    if (context.isViewed(target)) {
      // if this is a preparable DAG that has an attached view, we can't reduce it
      return;
    }

    DAGStructure<?> dag = target.internalAPI().getDAGStructure();

    // replace all the outputs with new versions whose ancestors are rooted at the subdag's parents in the working graph
    List<? extends Producer<?>> parents = context.getParents(target);

    IdentityHashMap<Producer<?>, Producer<?>> remapping = new IdentityHashMap<>(parents.size());
    for (int i = 0; i < dag._placeholders.size(); i++) {
      remapping.put(dag._placeholders.get(i), parents.get(i));
    }

    List<Producer<?>> newOutputs = dag._outputs.stream()
        .map(output -> DAGUtil.replaceInputs(output, remapping))
        .collect(Collectors.toList());

    Producer<?> replacement = tupled(newOutputs);

    // replace the subdag in the working graph with a tuple of its outputs
    if (target instanceof PreparableTransformer) {
      context.replaceUnviewed((PreparableTransformer) target, replacement);
    } else {
      context.replace((PreparedTransformer) target, replacement);
    }
  }
}
