package dev.horizon.leadseal.protobuf;

import com.google.protobuf.Descriptors.EnumDescriptor;
import dev.horizon.leadseal.SchemaException;

/**
 * A default value of an enum field: a value name for a text column, or a value number for an integer column.
 *
 * @param type the enum type
 * @param value the default value
 */
record ProtoEnumValue(EnumDescriptor type, String value) {

  /**
   * Checks that the enum has the value.
   *
   * @param byName {@code true} if the value is a name, {@code false} if it is a number
   * @param field the full name of the field, for the error message
   * @throws SchemaException if the enum has no such value
   */
  void check(boolean byName, String field) {
    if (!(byName ? type.findValueByName(value) != null : number())) {
      throw new SchemaException("The default '" + value + "' of the field " + field + " is not a value of "
          + type.getFullName() + ". Use a value " + (byName ? "name" : "number") + ".");
    }
  }

  private boolean number() {
    try {
      return type.findValueByNumber(Integer.parseInt(value)) != null;
    } catch (NumberFormatException notANumber) {
      return false;
    }
  }
}
