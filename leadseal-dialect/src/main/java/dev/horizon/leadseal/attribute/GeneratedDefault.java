package dev.horizon.leadseal.attribute;

/** A default that the database computes for each new row, such as the current time. */
public sealed interface GeneratedDefault extends Default permits NowDefault, RandomUuidDefault {
}
