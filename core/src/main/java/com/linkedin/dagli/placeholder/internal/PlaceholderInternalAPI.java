package com.linkedin.dagli.placeholder.internal;

import com.linkedin.dagli.producer.internal.RootProducerInternalAPI;
import com.linkedin.dagli.placeholder.Placeholder;
import java.util.ArrayList;
import java.util.List;


/**
 * Base interface for internal APIs of {@link Placeholder}s.
 * @param <R> the type of value supplied by the {@link Placeholder}
 * @param <S> the type of the {@link Placeholder}
 */
public interface PlaceholderInternalAPI<R, S extends Placeholder<R>> extends RootProducerInternalAPI<R, S> {
  /**
   * Returns a list of new placeholders.
   *
   * @param count the number of placeholders to creat
   * @return the list of new placeholders
   */
  static List<Placeholder<?>> createPlaceholderList(int count) {
    ArrayList<Placeholder<?>> placeholders = new ArrayList<>(count);
    for (int i = 1; i <= count; i++) {
      placeholders.add(new Placeholder<>("Placeholder #" + i));
    }
    return placeholders;
  }
}
