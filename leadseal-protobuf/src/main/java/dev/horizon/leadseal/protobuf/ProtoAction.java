package dev.horizon.leadseal.protobuf;

import dev.horizon.leadseal.attribute.ForeignKeyAttribute;

/**
 * A foreign key action of the protobuf options.
 *
 * @param action the option value
 */
record ProtoAction(Action action) {

  /**
   * Returns the action.
   *
   * @return the action, {@code NO_ACTION} if the option is not set
   */
  ForeignKeyAttribute.Action value() {
    return switch (action) {
      case ACTION_UNSPECIFIED, UNRECOGNIZED, ACTION_NO_ACTION -> ForeignKeyAttribute.Action.NO_ACTION;
      case ACTION_RESTRICT -> ForeignKeyAttribute.Action.RESTRICT;
      case ACTION_CASCADE -> ForeignKeyAttribute.Action.CASCADE;
      case ACTION_SET_NULL -> ForeignKeyAttribute.Action.SET_NULL;
    };
  }
}
