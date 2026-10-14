module dev.horizon.leadseal.sqlite {
  requires transitive dev.horizon.leadseal;

  exports dev.horizon.leadseal.sqlite;

  provides dev.horizon.leadseal.Dialect with dev.horizon.leadseal.sqlite.SqliteDialect;
}
