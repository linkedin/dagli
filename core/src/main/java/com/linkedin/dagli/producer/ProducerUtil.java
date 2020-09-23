package com.linkedin.dagli.producer;

/**
 * Internal itility class that provides methods for {@link Producer}s.
 */
abstract class ProducerUtil {
  private ProducerUtil() { }

  /**
   * Returns true if a producer has a constant result--that is, if we can prove that it will always generate the same
   * value for every example in any given DAG execution (see the {@link Producer#hasConstantResult()} annotation for
   * details).  Knowing that a node produces a constant result allows for optimizations and, in some cases, error
   * checking.
   *
   * @param producer the producer whose constant-result-ness should be checked
   * @return whether or not the producer is constant-result
   */
  static boolean hasConstantResult(Producer<?> producer) {
    if (producer.internalAPI().hasAlwaysConstantResult()) {
      return true;
    } else if (!(producer instanceof ChildProducer)) {
      // non-child producers (nodes without inputs) are only constant-result if they are always constant result
      return false;
    }

    // the producer is constant-result iff all its parents are constant-result
    return ((ChildProducer<?>) producer).internalAPI()
        .getInputList()
        .stream()
        .allMatch(Producer::hasConstantResult);
  }
}
