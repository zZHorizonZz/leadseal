module dev.horizon.leadseal.postgres {
  requires transitive dev.horizon.leadseal;

  exports dev.horizon.leadseal.postgres;

  provides dev.horizon.leadseal.Dialect with dev.horizon.leadseal.postgres.PostgresDialect;
}
