package dev.horizon.leadseal.command;

import dev.horizon.leadseal.Dialect;
import dev.horizon.leadseal.UnsupportedChangeException;
import dev.horizon.leadseal.type.EnumType;

final class EnumRenameException extends UnsupportedChangeException {

  EnumRenameException(Dialect dialect, EnumType type, EnumType.Rename rename) {
    super("The enum " + type.name() + " renames the value " + rename.origin() + " to " + rename.target() + ", but the"
        + " columns of the " + dialect.name() + " dialect store the value names. Keep the name " + rename.origin()
        + ", or store the value numbers with the integer type option.");
  }
}
