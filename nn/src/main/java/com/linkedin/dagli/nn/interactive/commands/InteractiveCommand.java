package com.linkedin.dagli.nn.interactive.commands;

/**
 * Common interface for interactive commands to the neural network preparer.
 */
public interface InteractiveCommand {
  <R> R accept(InteractiveCommandVisitor<R> visitor);
}
