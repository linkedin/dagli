package com.linkedin.dagli.util.stdio;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;


/**
 * {@link StandardInputListener} provides a way for any number of listeners to asynchronously listen for lines of input
 * from stdin.  Listeners are registered with {@link #register(Consumer)}, which then returns a {@link Token} that may
 * be {@link Token#close()}'d to unregister the listener.  Input is not consumed when no listeners are waiting.
 *
 * This class should not be used in parallel with any competing readers of stdin in the same program.
 *
 * @deprecated because it can be difficult to know a priori whether your code might run in a context where stdin behaves
 *             differently than "normal" or other dependencies might be impacted by an asynchronous thread blocking on
 *             stdin, we currently recommend against using this class in production code.
 */
@Deprecated
public abstract class StandardInputListener {
  private StandardInputListener() { }

  private static final HashMap<Token, Consumer<String>> LISTENER_TABLE = new HashMap<>();
  private static Thread _listenerThread = null;

  /**
   * A "token" whose {@link #close()} method will unregister the associated listener.
   */
  public static class Token implements AutoCloseable {
    @Override
    public void close() {
      unregister(this);
    }

    @Override
    protected void finalize() throws Throwable {
      close();
      super.finalize();
    }
  }

  private static void unregister(Token token) {
    synchronized (LISTENER_TABLE) {
      LISTENER_TABLE.remove(token);
      if (LISTENER_TABLE.isEmpty() && _listenerThread != null) {
        _listenerThread.interrupt();
        _listenerThread = null;
      }
    }
  }

  /**
   * Registers a listener to stdin and returns a {@link Token} that may be subsequently used to unregister the listener.
   *
   * This method is thread-safe and may be called concurrently by multiple threads.
   *
   * @param callback a callback method that should be called each time  new line of input is read
   * @return a {@link Token} that may be subsequently used to unregister the listener
   */
  public static Token register(Consumer<String> callback) {
    synchronized (LISTENER_TABLE) {
      Token token = new Token();
      LISTENER_TABLE.put(token, callback);

      if (_listenerThread == null) {
        _listenerThread = new Thread(StandardInputListener::run, "StandardInputListener");
        _listenerThread.setDaemon(true);
        _listenerThread.start();
      }

      return token;
    }
  }

  private static void run() {
    BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

    try {
      while (true) {
        String line = reader.readLine();
        final List<Consumer<String>> listeners;
        synchronized (LISTENER_TABLE) {
          listeners = new ArrayList<>(LISTENER_TABLE.values());
        }

        listeners.forEach(listener -> listener.accept(line));
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
