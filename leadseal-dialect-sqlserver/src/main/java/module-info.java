module dev.horizon.leadseal.sqlserver {
  requires transitive dev.horizon.leadseal;

  exports dev.horizon.leadseal.sqlserver;

  provides dev.horizon.leadseal.Dialect with dev.horizon.leadseal.sqlserver.SqlServerDialect;
}
