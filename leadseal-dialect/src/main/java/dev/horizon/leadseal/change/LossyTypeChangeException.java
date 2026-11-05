package dev.horizon.leadseal.change;

import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.UnsupportedChangeException;
import dev.horizon.leadseal.type.Type;

final class LossyTypeChangeException extends UnsupportedChangeException {

  LossyTypeChangeException(TableName table, String column, Type origin, Type target) {
    super("The migration changes the column " + table + "." + column + " from " + origin + " to " + target + ", which"
        + " can lose data and is unsafe. Keep a type that holds every value of " + origin + ", or set allow_unsafe=true"
        + " to apply the change.");
  }
}
