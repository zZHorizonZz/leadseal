package dev.horizon.leadseal.type;

/** The type of a point in time or a time of day: a timestamp, a date or a time. */
public sealed interface TemporalType extends Type permits TimestampType, DateType, TimeType {
}
