package dev.horizon.leadseal.protobuf;

import dev.horizon.leadseal.InvalidSchemaException;
import dev.horizon.leadseal.Table;
import dev.horizon.leadseal.attribute.Mutation;
import dev.horizon.leadseal.attribute.Trigger;
import dev.horizon.leadseal.expression.Row;
import dev.horizon.leadseal.expression.SqlExpression;
import dev.horizon.leadseal.type.BooleanType;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

record TriggerOption(TriggerOptions options, String message) {

  Trigger trigger(CelTable expressions, MessageCatalog catalog) {
    if (options.getName().isBlank()) {
      throw new InvalidSchemaException("A trigger of the message " + message + " has no name.");
    }
    List<Mutation> on = options.getOnList().stream().map(this::mutation).toList();
    if (on.isEmpty()) {
      throw new InvalidSchemaException("The trigger " + options.getName() + " of the message " + message + " runs on"
          + " no event. Set on to EVENT_INSERT, EVENT_UPDATE, EVENT_DELETE or more of them.");
    }
    if (options.getActionsList().isEmpty()) {
      throw new InvalidSchemaException("The trigger " + options.getName() + " of the message " + message + " has no"
          + " action. Set actions to insert, update, delete or assert actions.");
    }
    Set<Row> rows = EnumSet.allOf(Row.class);
    on.forEach(mutation -> rows.retainAll(mutation.rows()));
    SqlExpression when = options.getWhen().isBlank() ? Trigger.ALWAYS
        : expressions.parse(options.getWhen(), rows,
            new BooleanType());
    return new Trigger(options.getName(), on, when, options.getActionsList().stream().map(action -> action(action,
        expressions, catalog, rows)).toList());
  }

  private Trigger.Action action(ActionOptions action, CelTable expressions, MessageCatalog catalog, Set<Row> rows) {
    return switch (action.getActionCase()) {
      case INSERT -> insert(action.getInsert(), expressions, catalog, rows);
      case UPDATE -> update(action.getUpdate(), expressions, catalog, rows);
      case DELETE -> delete(action.getDelete(), expressions, catalog, rows);
      case ASSERT -> new Trigger.Assert(expressions.parse(action.getAssert().getExpression(), rows, new BooleanType()),
          action.getAssert().getMessage());
      case ACTION_NOT_SET -> throw new InvalidSchemaException("An action of the trigger " + options.getName() + " of"
          + " the message " + message + " is empty. Set insert, update, delete or assert.");
    };
  }

  private Trigger.Insert insert(InsertAction insert, CelTable expressions, MessageCatalog catalog, Set<Row> rows) {
    TableMessage target = catalog.table(insert.getMessage(), message);
    return new Trigger.Insert(target.name(), values(target, insert.getValuesMap(), expressions, rows));
  }

  private Trigger.Update update(UpdateAction update, CelTable expressions, MessageCatalog catalog, Set<Row> rows) {
    TableMessage target = catalog.table(update.getMessage(), message);
    CelTable destination = expressions.target(target.shape(), target.fields());
    return new Trigger.Update(target.name(), where(update.getWhere(), destination, rows), values(target, update.getSetMap(),
        destination, rows));
  }

  private Trigger.Delete delete(DeleteAction delete, CelTable expressions, MessageCatalog catalog, Set<Row> rows) {
    TableMessage target = catalog.table(delete.getMessage(), message);
    return new Trigger.Delete(target.name(), where(delete.getWhere(), expressions.target(target.shape(), target
        .fields()), rows));
  }

  private SqlExpression where(String where, CelTable destination, Set<Row> rows) {
    if (where.isBlank()) {
      throw new InvalidSchemaException("An update or a delete of the trigger " + options.getName() + " of the message "
          + message + " has no where. Set where to the condition of the rows, such as id == new.order_id.");
    }
    return destination.parse(where, rows, new BooleanType());
  }

  private List<Trigger.Assignment> values(TableMessage target, Map<String, String> values, CelTable expressions,
      Set<Row> rows) {
    Table shape = target.shape();
    values.keySet().forEach(target::column);
    return target.fields().keySet().stream().filter(values::containsKey).map(field -> {
      String column = target.column(field);
      return new Trigger.Assignment(column, expressions.parse(values.get(field), rows, shape.column(column)
          .orElseThrow().type()));
    }).toList();
  }

  private Mutation mutation(Event event) {
    return switch (event) {
      case EVENT_INSERT -> Mutation.INSERT;
      case EVENT_UPDATE -> Mutation.UPDATE;
      case EVENT_DELETE -> Mutation.DELETE;
      case EVENT_UNSPECIFIED, UNRECOGNIZED -> throw new InvalidSchemaException("The trigger " + options.getName()
          + " of the message " + message + " runs on " + event + ". Set on to EVENT_INSERT, EVENT_UPDATE or"
          + " EVENT_DELETE.");
    };
  }
}
