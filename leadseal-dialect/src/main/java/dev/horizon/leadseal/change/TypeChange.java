package dev.horizon.leadseal.change;

/**
 * A change of an enum type, which the columns of several tables can share. A dialect with enum types, such as
 * PostgreSQL, creates, changes and drops the type. Other dialects write each enum column with its own type.
 */
public sealed interface TypeChange extends Change permits CreateEnumType, AlterEnumType, DropEnumType {
}
