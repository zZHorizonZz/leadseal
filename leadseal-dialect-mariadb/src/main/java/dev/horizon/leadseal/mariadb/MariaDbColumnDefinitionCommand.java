package dev.horizon.leadseal.mariadb;

import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.SchemaException;
import dev.horizon.leadseal.ScriptWriter;
import dev.horizon.leadseal.Table;
import dev.horizon.leadseal.attribute.CommentAttribute;
import dev.horizon.leadseal.attribute.DefaultAttribute;
import dev.horizon.leadseal.attribute.IdentityAttribute;
import dev.horizon.leadseal.command.ColumnDefinitionCommand;
import java.util.regex.Pattern;

/**
 * A MariaDB column definition: the name, the type, the collation, {@code NULL} or {@code NOT NULL}, the default,
 * {@code AUTO_INCREMENT} and the comment. MariaDB writes collation names without quotes.
 */
class MariaDbColumnDefinitionCommand extends ColumnDefinitionCommand {

  private static final Pattern COLLATION = Pattern.compile("[A-Za-z0-9_]+");

  MariaDbColumnDefinitionCommand(Table definition, Column column) {
    super(definition, column);
  }

  @Override
  public void render(ScriptWriter sql) {
    Column column = column();
    sql.name(column.name()).append(" " + sql.dialect().type(definition(), column));
    column.collation(sql.dialect().name()).ifPresent(collation -> sql.append(" COLLATE " + checked(collation)));
    sql.append(column.nullable() ? " NULL" : " NOT NULL");
    column.attribute(DefaultAttribute.class)
        .ifPresent(value -> sql.append(" DEFAULT " + sql.dialect().defaultValue(column, value)));
    if (column.attribute(IdentityAttribute.class).isPresent()) {
      sql.append(" AUTO_INCREMENT");
    }
    column.attribute(CommentAttribute.class)
        .ifPresent(comment -> sql.append(" COMMENT " + sql.dialect().string(comment.text())));
  }

  private static String checked(String collation) {
    if (!COLLATION.matcher(collation).matches()) {
      throw new SchemaException("The MariaDB collation '" + collation + "' is not a collation name.");
    }
    return collation;
  }
}
