package com.linkedin.dagli.util.closeable;

import com.linkedin.dagli.util.exception.Exceptions;


/**
 * Static utility methods for interacting with {@link AutoCloseable} instances.
 */
public abstract class Closeables {
  private Closeables() { }

  /**
   * Closes an instance if it is AutoCloseable, otherwise this method call has no effect.  Any exceptions thrown by the
   * {@link AutoCloseable#close()} method are caught and rethrown as runtime exceptions consistent with the
   * exception-wrapping done by {@link Exceptions#asRuntimeException(Throwable)}.
   *
   * This method is a convenient way to close instances that may or may not be {@link AutoCloseable}.
   *
   * @param obj an instance to close if it is AutoCloseable.  If null or not AutoCloseable, the call will be a no-op.
   */
  public static void tryClose(Object obj) {
    if (obj instanceof AutoCloseable) {
      try {
        ((AutoCloseable) obj).close();
      } catch (Exception e) {
        throw Exceptions.asRuntimeException(e);
      }
    }
  }
}
