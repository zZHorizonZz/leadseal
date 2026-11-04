package dev.horizon.leadseal.sqlserver;

import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.Table;
import dev.horizon.leadseal.attribute.ConstraintName;
import java.util.List;

record SqlServerDefaultName(Table table, Column column) {

  String value() {
    return new ConstraintName("df", table.name(), List.of(column.name())).value();
  }
}
