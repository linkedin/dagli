package com.linkedin.dagli.nn.layer;

/**
 * Marker interface for layers that calculates the loss (or part of the loss) for the network.  These layers are used
 * during both training (to compute the loss to be minimized) and, optionally, at inference (to extract a corresponding
 * inferred "result" that can be conveniently consumed by other nodes in the DAG of which the neural network model is
 * part; for example, an inferred classification can then be transformed into a feature that is ultimately fed into a
 * pipelined downstream XGBoost model).
 */
public interface LossLayer extends LayerType<LossLayer> { }
