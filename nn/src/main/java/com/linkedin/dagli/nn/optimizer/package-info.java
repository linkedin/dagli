/**
 * Optimizers determine how the parameters are iteratively updated to minimize the loss (error) of the neural network
 * during training.
 */
@Visitor(name = "OptimizerVisitor", isPublic = true)
package com.linkedin.dagli.nn.optimizer;

import com.linkedin.dagli.annotation.visitor.Visitor;