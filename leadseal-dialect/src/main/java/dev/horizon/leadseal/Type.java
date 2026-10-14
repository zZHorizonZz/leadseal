package dev.horizon.leadseal;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * The type of a column. Each dialect maps a type to a database type.
 *
 * <p>
 * Each type has a canonical text form for its values, which defaults use. Dialects turn these values into SQL literals.
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
    @JsonSubTypes.Type(value = JsonType.class, name = "json")
})
public sealed interface Type permits BooleanType, IntegerType, FloatType, DecimalType, TextType, BytesType, UuidType,
    TimestampType, DateType, TimeType, JsonType {

  /**
   * Returns the zero value of this type. Migrations fill existing rows with it when they add a column that does not
   * accept {@code NULL} and has no default.
   *
   * @return the value in its canonical text form, for example {@code 0} or {@code false}
   */
  String zero();

  /**
   * Returns whether a text is a value of this type in its canonical form.
   *
   * @param value the text
   * @return {@code true} if the text is a value of this type
   */
  boolean accepts(String value);

  /**
   * Returns an example of the canonical form, for error messages.
   *
   * @return the example, for example {@code 42}
   */
  String example();
}
