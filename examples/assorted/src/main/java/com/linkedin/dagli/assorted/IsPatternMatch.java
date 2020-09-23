package com.linkedin.dagli.assorted;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.transformer.AbstractPreparedStatefulTransformer2;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;


/**
 * Transformer that demonstrates a "stateful" transformer that is capable of minibatching and/or caching data across
 * examples.
 *
 * Accepts a string value and a string regex pattern as inputs and outputs whether the string value matches the pattern.
 */
@ValueEquality // value equality is default, but explicitly specifying it here precludes a compile-time warning
public class IsPatternMatch extends
    AbstractPreparedStatefulTransformer2<String, String, Boolean, ConcurrentHashMap<String, Pattern>, IsPatternMatch> {
  private static final long serialVersionUID = 1;

  /**
   * Although not required, overriding this method allows us to handle the "minibatch" case where we improve efficiency
   * by transforming multiple values at once.
   *
   * Here, we potentially reduce the number of calls to ConcurrentHashMap, although in reality the benefits of doing
   * so are likely outweighed by the cost of creating our local map.  In other cases--such as inference in neural
   * networks--minibatching is far more essential to efficiency.
   */
  @Override
  protected void applyAll(ConcurrentHashMap<String, Pattern> executionCache, List<? extends String> values,
      List<? extends String> patterns, List<? super Boolean> results) {
    // get (or compile) the needed patterns, using a local HashMap as a buffer to reduce the more expensive operations
    // on the ConcurrentHashMap executionCache
    HashMap<String, Pattern> patternMap = new HashMap<>(patterns.size());
    patterns.stream()
        .distinct()
        .forEach(
            pattern -> patternMap.computeIfAbsent(pattern, p -> executionCache.computeIfAbsent(p, Pattern::compile)));

    // get each compiled pattern, test it against the corresponding value, and put the result in the results list
    for (int i = 0; i < values.size(); i++) {
      results.add(patternMap.get(patterns.get(i)).asPredicate().test(values.get(i)));
    }
  }

  @Override
  protected int getPreferredMinibatchSize() {
    return 128; // this is the minimum number of examples we'd "prefer" to see at once, though we may get any quantity
  }

  @Override
  protected ConcurrentHashMap<String, Pattern> createExecutionCache(long exampleCountGuess) {
    return super.createExecutionCache(exampleCountGuess);
  }

  // we *must* override this method
  @Override
  protected Boolean apply(ConcurrentHashMap<String, Pattern> executionCache, String value, String pattern) {
    return executionCache.computeIfAbsent(pattern, Pattern::compile).asPredicate().test(value);
  }
}
