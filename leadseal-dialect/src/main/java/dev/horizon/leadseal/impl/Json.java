package dev.horizon.leadseal.impl;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import dev.horizon.leadseal.Default;
import dev.horizon.leadseal.Operation;
import dev.horizon.leadseal.SchemaException;
import java.util.List;

/** Writes and reads the JSON files of a migration history. */
public final class Json {

  private static final ObjectMapper MAPPER = new ObjectMapper()
      .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)
      .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
      .addMixIn(Operation.class, OperationType.class)
      .addMixIn(Default.class, DefaultValue.class);

  static {
    MAPPER.registerSubtypes(
        new NamedType(Operation.CreateTable.class, "create_table"),
        new NamedType(Operation.DropTable.class, "drop_table"),
        new NamedType(Operation.RenameTable.class, "rename_table"),
        new NamedType(Operation.AddColumn.class, "add_column"),
        new NamedType(Operation.DropColumn.class, "drop_column"),
        new NamedType(Operation.RenameColumn.class, "rename_column"),
        new NamedType(Operation.AlterColumn.class, "alter_column"),
        new NamedType(Operation.AlterPrimaryKey.class, "alter_primary_key"),
        new NamedType(Operation.AddIndex.class, "add_index"),
        new NamedType(Operation.DropIndex.class, "drop_index"),
        new NamedType(Operation.AddForeignKey.class, "add_foreign_key"),
        new NamedType(Operation.DropForeignKey.class, "drop_foreign_key"),
        new NamedType(Operation.MoveTable.class, "move_table"),
        new NamedType(Operation.AddCheck.class, "add_check"),
        new NamedType(Operation.DropCheck.class, "drop_check"),
        new NamedType(Operation.CommentTable.class, "comment_table"));
  }

  private Json() {
  }

  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "op")
  private interface OperationType {
  }

  private interface DefaultValue {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    String value();
  }

  /**
   * Writes a value as JSON.
   *
   * @param value the value
   * @param pretty {@code true} for indented output with a trailing newline
   * @return the JSON text
   */
  public static String write(Object value, boolean pretty) {
    try {
      return pretty ? MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(value) + "\n"
          : MAPPER.writeValueAsString(value);
    } catch (JsonProcessingException failed) {
      throw new SchemaException("Cannot write " + value.getClass().getSimpleName() + " as JSON.", failed);
    }
  }

  /**
   * Writes operations as indented JSON, each with its {@code op} type.
   *
   * @param operations the operations
   * @return the JSON text, with a trailing newline
   */
  public static String write(List<Operation> operations) {
    try {
      return MAPPER.writerFor(new TypeReference<List<Operation>>() {
      }).withDefaultPrettyPrinter().writeValueAsString(operations) + "\n";
    } catch (JsonProcessingException failed) {
      throw new SchemaException("Cannot write operations as JSON.", failed);
    }
  }

  /**
   * Returns whether a text is a JSON document.
   *
   * @param text the text
   * @return {@code true} if the text parses as JSON
   */
  public static boolean valid(String text) {
    try {
      MAPPER.readTree(text);
      return true;
    } catch (JsonProcessingException invalid) {
      return false;
    }
  }

  /**
   * Reads a value from JSON.
   *
   * @param text the JSON text
   * @param type the type of the value
   * @param <T> the type of the value
   * @return the value
   */
  public static <T> T read(String text, TypeReference<T> type) {
    try {
      return MAPPER.readValue(text, type);
    } catch (JsonProcessingException failed) {
      throw new SchemaException("Cannot read JSON: " + failed.getOriginalMessage(), failed);
    }
  }
}
