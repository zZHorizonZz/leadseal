package dev.horizon.leadseal.protobuf.column;

import dev.horizon.leadseal.InvalidSchemaException;

final class DefaultConflictException extends InvalidSchemaException {

  DefaultConflictException(FieldKind kind) {
    super("The field " + kind.field() + " has both a default_value and a generated default. Set one of them.");
  }
}
