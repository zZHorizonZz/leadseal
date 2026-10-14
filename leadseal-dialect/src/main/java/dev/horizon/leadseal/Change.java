package dev.horizon.leadseal;

import java.util.List;

/**
 * The operations of one migration. It renders each operation in order. Dialects decorate the change to rewrite several
 * operations together, for example to rebuild a table once for all its changes.
 *
 * @param operations the operations, in order
 */
public record Change(List<Operation> operations) implements Node {

  public Change {
    operations = List.copyOf(operations);
  }

  @Override
  public void render(DdlWriter sql) {
    for (Operation operation : operations) {
      sql.render(operation).end();
    }
  }
}
