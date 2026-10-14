package dev.horizon.leadseal;

import java.time.Clock;

/**
 * The options of migration generation.
 *
 * @param clock the clock that names new migrations
 * @param prefix the prefix of the names that migrations give their own temporary objects, such as the temporary table of
 * a SQLite rebuild. An empty prefix gives them names without one.
 */
public record MigrationOptions(Clock clock, String prefix) {

  /** The prefix that migrations use by default. */
  public static final String DEFAULT_PREFIX = "leadseal_";

  public MigrationOptions {
    clock = clock == null ? Clock.systemUTC() : clock;
    prefix = prefix == null ? "" : prefix;
  }

  /**
   * Returns the default options: the system clock in UTC and the prefix {@code leadseal_}.
   *
   * @return the options
   */
  public static MigrationOptions defaults() {
    return new MigrationOptions(Clock.systemUTC(), DEFAULT_PREFIX);
  }

  /**
   * Returns a copy of these options with another clock.
   *
   * @param clock the clock
   * @return the new options
   */
  public MigrationOptions withClock(Clock clock) {
    return new MigrationOptions(clock, prefix);
  }

  /**
   * Returns a copy of these options with another prefix.
   *
   * @param prefix the prefix, or {@code null} or an empty text for no prefix
   * @return the new options
   */
  public MigrationOptions withPrefix(String prefix) {
    return new MigrationOptions(clock, prefix);
  }
}
