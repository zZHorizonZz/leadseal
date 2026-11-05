package dev.horizon.leadseal.type;

import com.fasterxml.jackson.annotation.JsonValue;
import dev.horizon.leadseal.InvalidSchemaException;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * An enum: a column that holds one of a list of values. The column stores the value names or the value numbers. Each
 * dialect writes the type its own way: PostgreSQL has a native enum type of each enum that stores names, which the
 * columns of the enum share, MariaDB writes {@code ENUM(...)}, and SQL Server and SQLite write text with a check. A
 * column that stores numbers is an integer with a check on every dialect.
 *
 * @param name the name of the enum, such as {@code order_state}, which also names the type of a dialect that has enum
 * types. The enum columns of a schema with this name have the same values.
 * @param values the values of the enum, in their order, each number and each name once
 * @param storage what the column stores of each value
 */
public record EnumType(String name, List<Value> values, Storage storage) implements Type {

  private static final int MAX_LENGTH = 63;

  public EnumType {
    if (name == null || name.isBlank()) {
      throw new InvalidSchemaException("An enum type has no name.");
    }
    if (name.length() > MAX_LENGTH) {
      throw new InvalidSchemaException("The enum name " + name + " is longer than " + MAX_LENGTH + " characters.");
    }
    values = values == null ? List.of() : List.copyOf(values);
    storage = storage == null ? Storage.NAME : storage;
    if (values.isEmpty()) {
      throw new InvalidSchemaException("The enum " + name + " has no values.");
    }
    Set<Integer> numbers = new HashSet<>();
    Set<String> names = new HashSet<>();
    for (Value value : values) {
      if (!numbers.add(value.number()) || !names.add(value.name())) {
        throw new InvalidSchemaException("The enum " + name + " has the value " + value.number() + " " + value.name()
            + " twice.");
      }
      if (value.name().length() > MAX_LENGTH) {
        throw new InvalidSchemaException("The enum value " + name + "." + value.name() + " is longer than "
            + MAX_LENGTH + " characters.");
      }
    }
  }

  /**
   * Gives what the column stores of each value.
   *
   * @return the value names or the value numbers, as text, in their order
   */
  public List<String> literals() {
    return values.stream().map(storage::literal).toList();
  }

  /**
   * Gives the values that keep their number in this enum but have another name than in an earlier version of it.
   *
   * @param origin the earlier version of the enum
   * @return the renames, in the order of this enum
   */
  public List<Rename> renames(EnumType origin) {
    return values.stream().flatMap(value -> origin.values.stream()
        .filter(originValue -> originValue.number() == value.number() && !originValue.name().equals(value.name()))
        .map(originValue -> new Rename(originValue.name(), value.name()))).toList();
  }

  /**
   * Tells if a column of this type holds what a column of another type holds. A column that stores numbers holds the
   * same with other value names, so renaming a value needs no migration.
   *
   * @param other the other type
   * @return true for an equal enum, or for an enum with the same name and value numbers that stores numbers too
   */
  @Override
  public boolean storesLike(Type other) {
    return switch (other) {
      case EnumType type -> storage.select(equals(type), type.storage == storage && name.equals(type.name)
          && numbers().equals(type.numbers()));
      default -> false;
    };
  }

  private List<Integer> numbers() {
    return values.stream().map(Value::number).toList();
  }

  @Override
  public String zero() {
    return literals().getFirst();
  }

  @Override
  public boolean accepts(String value) {
    return literals().contains(value);
  }

  @Override
  public String example() {
    return zero();
  }

  @Override
  public String toString() {
    return "enum " + name;
  }

  /**
   * A value of an enum.
   *
   * @param number the value number
   * @param name the value name
   */
  public record Value(int number, String name) {

    public Value {
      if (name == null || name.isBlank()) {
        throw new InvalidSchemaException("The enum value " + number + " has no name.");
      }
    }
  }

  /**
   * A value that keeps its number but gets another name.
   *
   * @param origin the name before
   * @param target the name after
   */
  public record Rename(String origin, String target) {
  }

  /** What an enum column stores of each value. */
  public enum Storage {
    /** The value name, such as {@code STATE_OPEN}. */
    NAME {
      @Override
      public String literal(Value value) {
        return value.name();
      }

      @Override
      public <T> T select(T name, T number) {
        return name;
      }
    },
    /** The value number, such as {@code 1}. */
    NUMBER {
      @Override
      public String literal(Value value) {
        return String.valueOf(value.number());
      }

      @Override
      public <T> T select(T name, T number) {
        return number;
      }
    };

    /**
     * Gives what the column stores of a value.
     *
     * @param value the value
     * @return the name or the number, as text
     */
    public abstract String literal(Value value);

    /**
     * Gives one of two values, by the storage.
     *
     * @param name the value for a column that stores names
     * @param number the value for a column that stores numbers
     * @param <T> the type of the values
     * @return the value of this storage
     */
    public abstract <T> T select(T name, T number);

    @JsonValue
    @Override
    public String toString() {
      return name().toLowerCase(Locale.ROOT);
    }
  }
}
