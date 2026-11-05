package dev.horizon.leadseal.type;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import dev.horizon.leadseal.Dialect;
import dev.horizon.leadseal.InvalidSchemaException;
import dev.horizon.leadseal.SchemaException;

/**
 * The type of a column. Each dialect maps a type to a database type.
 *
 * <p>
 * Each type has a canonical text form for its values, which defaults use. Dialects turn these values into SQL literals.
 * {@link NumberType} and {@link TemporalType} group the types of numbers and of points in time.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind")
@JsonSubTypes({
    @JsonSubTypes.Type(value = BooleanType.class, name = "boolean"),
    @JsonSubTypes.Type(value = IntegerType.class, name = "integer"),
    @JsonSubTypes.Type(value = FloatType.class, name = "float"),
    @JsonSubTypes.Type(value = DecimalType.class, name = "decimal"),
    @JsonSubTypes.Type(value = TextType.class, name = "text"),
    @JsonSubTypes.Type(value = BytesType.class, name = "bytes"),
    @JsonSubTypes.Type(value = UuidType.class, name = "uuid"),
    @JsonSubTypes.Type(value = TimestampType.class, name = "timestamp"),
    @JsonSubTypes.Type(value = DateType.class, name = "date"),
    @JsonSubTypes.Type(value = TimeType.class, name = "time"),
    @JsonSubTypes.Type(value = JsonType.class, name = "json"),
    @JsonSubTypes.Type(value = EnumType.class, name = "enum")
})
public sealed interface Type permits BooleanType, NumberType, TextType, BytesType, UuidType,
    TemporalType, JsonType, EnumType {

  /**
   * Gives the zero value of this type. Migrations fill existing rows with it when they add a column that does not
   * accept {@code NULL} and has no default.
   *
   * @return the value in its canonical text form, for example {@code 0} or {@code false}
   */
  String zero();

  /**
   * Tells if a text is a value of this type in its canonical form.
   *
   * @param value the text
   * @return true if the text is a value of this type
   */
  boolean accepts(String value);

  /**
   * Gives an example of the canonical form, for error messages.
   *
   * @return the example, for example {@code 42}
   */
  String example();

  /**
   * Tells if the values of this type sort by a collation.
   *
   * @return true for text. The default is false.
   */
  default boolean collates() {
    return false;
  }

  /**
   * Tells if the database can count the values of this type for an identity column.
   *
   * @return true for an integer. The default is false.
   */
  default boolean counts() {
    return false;
  }

  /**
   * Returns the SQL expression of the current value of this type, the default of a column that defaults to the current
   * time.
   *
   * @param dialect the dialect of the expression
   * @return the expression
   * @throws SchemaException if the values of this type are not dates or timestamps
   */
  default String current(Dialect dialect) {
    throw new InvalidSchemaException("A " + this + " column cannot default to the current time.");
  }

  /**
   * Tells if the values of this type are points in time, which the current time is a default of.
   *
   * @return true for a timestamp and a date. The default is false.
   */
  default boolean dates() {
    return false;
  }

  /**
   * Tells if the values of this type are UUIDs, which a random UUID is a default of.
   *
   * @return true for a UUID. The default is false.
   */
  default boolean identifies() {
    return false;
  }

  /**
   * Tells if a column of this type holds every value that a column of an earlier type holds, so that a change from the
   * earlier type loses no data.
   *
   * @param origin the earlier type
   * @return true if every value of the earlier type fits. The default is true only for an equal type.
   */
  default boolean isAssignableFrom(Type origin) {
    return equals(origin);
  }
}
