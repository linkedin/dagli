package com.linkedin.dagli.nn.layer;

/**
 * Marker interface for non-terminal layers.  Only non-terminal layers are allowed to have children in the neural
 * network.
 */
public interface NonTerminalLayer extends LayerType<NonTerminalLayer> { }
