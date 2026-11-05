package dev.horizon.leadseal.protobuf;

import dev.horizon.leadseal.InvalidSchemaException;

final class UnsupportedExpressionException extends InvalidSchemaException {

  UnsupportedExpressionException(CelTranslation translation, String reason) {
    super("The expression '" + translation.text() + "' is not valid: " + reason + ".");
  }
}
