package com.linkedin.dagli.nn.layer;

/**
 * Parent interface for {@link LossLayer} and {@link NonTerminalLayer}, used to ensure that these interfaces are
 * mutually exclusive.
 *
 * @param <S> the interface type extending {@link LayerType}
 */
interface LayerType<S extends LayerType> { }
