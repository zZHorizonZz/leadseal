package dev.horizon.leadseal.protobuf;

import dev.horizon.leadseal.InvalidSchemaException;
import dev.horizon.leadseal.attribute.ForeignKey;

record ActionOption(Action action, String origin) {

  ForeignKey.Action value() {
    return switch (action) {
      case ACTION_UNSPECIFIED, ACTION_NO_ACTION -> ForeignKey.Action.NO_ACTION;
      case ACTION_RESTRICT -> ForeignKey.Action.RESTRICT;
      case ACTION_CASCADE -> ForeignKey.Action.CASCADE;
      case ACTION_SET_NULL -> ForeignKey.Action.SET_NULL;
      case UNRECOGNIZED -> throw new InvalidSchemaException("The foreign key of " + origin + " has an action that this"
          + " version of leadseal does not know. Use NO_ACTION, RESTRICT, CASCADE or SET_NULL.");
    };
  }
}
