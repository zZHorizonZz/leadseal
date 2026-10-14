package dev.horizon.leadseal.impl;

import dev.horizon.leadseal.Operation;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Generates the name of a migration from its operations, for example {@code add_orders_note}. */
public final class MigrationName {

  private static final int MAX_LENGTH = 60;

  private MigrationName() {
  }

  /**
   * Returns the name of a migration.
   *
   * @param operations the operations of the migration
   * @return the name, with lowercase letters, digits and underscores
   */
  public static String of(List<Operation> operations) {
    Set<String> parts = new LinkedHashSet<>();
    for (Operation operation : operations) {
      if (main(operation)) {
        parts.add(describe(operation));
      }
    }
    if (parts.isEmpty()) {
      operations.forEach(operation -> parts.add(describe(operation)));
    }
    List<String> names = new ArrayList<>(parts);
    String name = switch (names.size()) {
      case 0 -> "change";
      case 1 -> names.getFirst();
      case 2 -> names.get(0) + "_and_" + names.get(1);
      default -> names.getFirst() + "_and_" + (names.size() - 1) + "_more";
    };
    String clean = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_").replaceAll("^_|_$", "");
    return clean.length() <= MAX_LENGTH ? clean : clean.substring(0, MAX_LENGTH).replaceAll("_$", "");
  }

  private static boolean main(Operation operation) {
    return !(operation instanceof Operation.AddIndex || operation instanceof Operation.DropIndex
        || operation instanceof Operation.AddForeignKey || operation instanceof Operation.DropForeignKey
        || operation instanceof Operation.AddCheck || operation instanceof Operation.DropCheck
        || operation instanceof Operation.CommentTable);
  }

  private static String describe(Operation operation) {
    return switch (operation) {
      case Operation.CreateTable create -> "create_" + create.table();
      case Operation.DropTable drop -> "drop_" + drop.table();
      case Operation.RenameTable rename -> "rename_" + rename.from() + "_to_" + rename.table();
      case Operation.AddColumn add -> "add_" + add.table() + "_" + add.column().name();
      case Operation.DropColumn drop -> "drop_" + drop.table() + "_" + drop.column();
      case Operation.RenameColumn rename -> "rename_" + rename.table() + "_" + rename.from() + "_to_" + rename.to();
      case Operation.AlterColumn alter -> "alter_" + alter.table() + "_" + alter.to().name();
      case Operation.AlterPrimaryKey alter -> "alter_" + alter.table() + "_primary_key";
      case Operation.AddIndex add -> "add_" + add.index().name();
      case Operation.DropIndex drop -> "drop_" + drop.index().name();
      case Operation.AddForeignKey add -> "add_" + add.key().name();
      case Operation.DropForeignKey drop -> "drop_" + drop.key().name();
      case Operation.MoveTable move -> "move_" + move.table() + "_to_" + (move.to() == null ? "default" : move.to());
      case Operation.AddCheck add -> "add_" + add.check().name();
      case Operation.DropCheck drop -> "drop_" + drop.check().name();
      case Operation.CommentTable comment -> "comment_" + comment.table();
    };
  }
}
