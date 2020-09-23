package com.linkedin.dagli.nn.interactive.commands;

import com.linkedin.dagli.annotation.struct.Struct;
import com.linkedin.dagli.annotation.struct.TrivialPublicConstructor;
import com.linkedin.dagli.annotation.visitor.VisitedBy;


/**
 * A command that requests that the neural network stop training as soon as possible.
 */
@Struct("Stop")
@TrivialPublicConstructor
@VisitedBy("InteractiveCommandVisitor")
abstract class StopBase implements InteractiveCommand { }
