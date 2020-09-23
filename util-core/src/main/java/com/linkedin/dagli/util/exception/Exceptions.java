package com.linkedin.dagli.util.exception;

import com.concurrentli.UncheckedInterruptedException;
import java.io.IOException;
import java.io.UncheckedIOException;


/**
 * Static class with utility methods pertaining to exceptions.
 */
public abstract class Exceptions {
  private Exceptions() { }

  /**
   * Given a throwable, returns the most pertinent available {@link RuntimeException}:
   * (1) If <code>e</code> is a {@link RuntimeException}, <code>e</code> itself is returned
   * (2) If <code>e</code> is an InterruptedException, re-interrupts the current thread via {@link Thread#interrupt()}
   *     and then returns an exception (although interruption may prevent this method from returning, depending on the
   *     context--see {@link Thread#interrupt()} for details).
   * (3) If <code>e</code> is an {@link IOException}, an {@link UncheckedIOException} wrapper is returned.
   * (4) Otherwise, an exception deriving from {@link RuntimeException} wrapping <code>e</code> is returned.  Currently
   *     this exception is {@link RuntimeException} itself, but this behavior may change in future versions of this
   *     method.
   *
   * This method is useful for, in effect, "converting" checked exceptions into unchecked exceptions, avoiding the need
   * to either explicitly catch them or declare them via "throws" on each method in the call chain.  However, this
   * obviously also contravenes the <em>point</em> of checked exceptions, which is to force callers to address
   * foreseeable exceptions.  Code using this method should either be certain that any caught checked exceptions will
   * never happen in practice or carefully document for clients that these checked exceptions will be thrown as
   * unchecked {@link RuntimeException}s.
   *
   * @param e the original throwable
   * @return a {@link RuntimeException} corresponding to the original throwable.
   */
  public static RuntimeException asRuntimeException(Throwable e) {
    if (e instanceof RuntimeException) {
      return (RuntimeException) e;
    } else if (e instanceof IOException) {
      return new UncheckedIOException((IOException) e);
    } else if (e instanceof InterruptedException) {
      Thread.currentThread().interrupt(); // re-interrupt the current thread
      return new UncheckedInterruptedException((InterruptedException) e); // execution may or may not reach this point
    } else {
      return new RuntimeException(e);
    }
  }
}
