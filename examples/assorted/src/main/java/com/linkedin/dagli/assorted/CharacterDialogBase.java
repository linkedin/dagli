package com.linkedin.dagli.assorted;

import com.linkedin.dagli.annotation.struct.Optional;
import com.linkedin.dagli.annotation.struct.Struct;


/**
 * Defines a simple @Struct that will represent our example data.  Dagli will generate a class named "CharacterDialog"
 * with numerous useful methods and nested types.
 *
 * Please see {@code dagli/documentation/structs.md} for details.
 */
@Struct("CharacterDialog")
abstract class CharacterDialogBase {
  /**
   * The text we'll try to classify with the corresponding character.
   */
  String _dialog;

  /**
   * The "label" we're ultimately trying to predict.  Optional because, at inference time, it won't be set.
   */
  @Optional
  String _character = null;
}
