package dev.horizon.leadseal.migration;

import java.time.Clock;

/**
 * The options of migration generation.
 *
 * @param clock the clock that names new migrations
 * @param prefix the prefix of the names that migrations give their own temporary objects, such as the temporary table
 * of a SQLite rebuild. An empty prefix gives them names without one.
 * @param unsafe whether a migration applies unsafe changes, which can lose values that the schema still has, such as
 * a column type that holds fewer values
 */
public record MigrationOptions(Clock clock, String prefix, boolean unsafe) {

  /** The prefix that migrations use by default. */
  public static final String DEFAULT_PREFIX = "leadseal_";

  public MigrationOptions {
    clock = clock == null ? Clock.systemUTC() : clock;
    prefix = prefix == null ? "" : prefix;
  }

  /**
   * Returns the default options: the system clock in UTC, the prefix {@code leadseal_}, and no unsafe changes.
   *
   * @return the options
   */
  public static MigrationOptions defaults() {
    return new MigrationOptions(Clock.systemUTC(), DEFAULT_PREFIX, false);
  }

  /**
   * Returns a copy of these options with another clock.
   *
   * @param clock the clock
   * @return the new options
   */
  public MigrationOptions withClock(Clock clock) {
    return new MigrationOptions(clock, prefix, unsafe);
  }

  /**
   * Returns a copy of these options with another prefix.
   *
   * @param prefix the prefix, or {@code null} or an empty text for no prefix
   * @return the new options
   */
  public MigrationOptions withPrefix(String prefix) {
    return new MigrationOptions(clock, prefix, unsafe);
  }

  /**
   * Returns a copy of these options that applies unsafe changes.
   *
   * @return the new options
   */
  public MigrationOptions allowUnsafe() {
    return new MigrationOptions(clock, prefix, true);
  }
}
