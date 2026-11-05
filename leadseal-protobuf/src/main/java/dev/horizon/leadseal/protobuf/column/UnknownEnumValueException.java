package dev.horizon.leadseal.protobuf.column;

import com.google.protobuf.Descriptors.EnumDescriptor;
import dev.horizon.leadseal.InvalidSchemaException;

final class UnknownEnumValueException extends InvalidSchemaException {

  UnknownEnumValueException(String field, EnumDescriptor values, String value, String form) {
    super("The default '" + value + "' of the field " + field + " is not a value of " + values.getFullName()
        + ". Use a value " + form + ".");
  }
}
