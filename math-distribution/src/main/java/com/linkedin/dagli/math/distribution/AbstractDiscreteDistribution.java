package com.linkedin.dagli.math.distribution;

import it.unimi.dsi.fastutil.Size64;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashBigSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;


/**
 * A common ancestor class for {@link DiscreteDistribution}s.  Although {@link DiscreteDistribution} implementations
 * need not necessarily extend this class, it is nonetheless highly recommended.
 *
 * At present this class serves to provide reasonable implementations of {@link Object#hashCode()},
 * {@link Object#equals(Object)}, and {@link Object#toString()}.
 *
 * @param <T> the type of label used in the distribution
 */
public abstract class AbstractDiscreteDistribution<T> implements DiscreteDistribution<T> {
  private static final long serialVersionUID = 1;

  @Override
  public int hashCode() {
    // We treat the distribution as an unordered bag of LabelProbability objects and compute an aggregate hash:
    int hash = 0;
    for (LabelProbability<T> lp : this) {
      hash += lp.hashCode();
    }

    return hash;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof DiscreteDistribution)) {  // note that instanceof is false if obj is null
      return false;
    }
    final DiscreteDistribution<Object> other = (DiscreteDistribution) obj;

    final long mySize = size64();
    if (other.size64() != mySize) {
      return false;
    }

    // decide whether we need to deal with a ridiculously huge set of events or not and choose our hashset accordingly
    Set<T> tiedLabelSet = mySize <= (1 << 30) ? new HashSet<>() : new ObjectOpenHashBigSet<>();

    Iterator<LabelProbability<T>> iterator1 = this.iterator();
    Iterator<LabelProbability<Object>> iterator2 = other.iterator();

    LabelProbability<T> myCachedNext = null; // used when we previously "peeked" at something we need to revisit

    // take advantage of the fact that entries are returned in order of decreasing probability; the tricky part is
    // dealing with instances where multiple events have the same probability can be returned in arbitrary order
    for (long i = 0; i < mySize; i++) {
      LabelProbability<T> myNext = myCachedNext == null ? iterator1.next() : myCachedNext;
      myCachedNext = null;
      LabelProbability<Object> otherNext = iterator2.next();

      if (myNext.getProbability() != otherNext.getProbability()) {
        return false; // two events at the same position in each iterator have different probabilities
      } else if (!Objects.equals(myNext.getLabel(), otherNext.getLabel())) {
        // This is more complicated: two events with the same probability but different labels.  The events of the
        // distributions could be the same, just with different orderings for multiple events sharing the same
        // probability.

        double tiedProbability = myNext.getProbability();

        // Step 1: Read in all entries that have the same label.  Notice that we'll increase i by the number of "extra"
        // things read from our iterator here, except the last extra thing (which is cached for later)
        tiedLabelSet.add(myNext.getLabel());
        for (long j = i + 1; j < mySize; j++) { // make sure we don't go past the end of iterator1
          myNext = iterator1.next();
          if (myNext.getProbability() == tiedProbability) {
            tiedLabelSet.add(myNext.getLabel());

            // CHECKSTYLE:OFF
            i++; // increment the outer loop iterator to account for the extra item we're consuming from iterator1 here
            // CHECKSTYLE:ON
          } else {
            // we're past the entries that were tied; cache the last thing we got via next() so it can be used in the
            // next iteration of the outer loop.
            myCachedNext = myNext;
            break;
          }
        }

        // get the label set size (slightly tricky because we need to get the 64-bit size where available)
        long tiedLabelSetSize = tiedLabelSet instanceof Size64 ? ((Size64) tiedLabelSet).size64() : tiedLabelSet.size();

        // Step 2: check that all the labels we just found at the tiedProbability can be found with the same probability
        // in the other distribution.
        for (long j = 0; j < tiedLabelSetSize; j++) {
          if (j > 0) { // if j == 0 we already have an initial otherNext value to process
            otherNext = iterator2.next();
          }
          if (otherNext.getProbability() != tiedProbability) {
            // if the distributions are equal, the same number of elements should have the tied probability in both
            // distributions
            return false;
          } else if (!tiedLabelSet.contains(otherNext.getLabel())) {
            // the probability is as expected, but this label isn't associated with this probability in the other
            // distribution
            return false;
          }
        }

        tiedLabelSet.clear();
      }
    }

    // no discrepancies found
    return true;
  }

  @Override
  public String toString() {
    return "{" + String.join(", ", stream().map(LabelProbability::toString).toArray(String[]::new)) + "}";
  }
}
