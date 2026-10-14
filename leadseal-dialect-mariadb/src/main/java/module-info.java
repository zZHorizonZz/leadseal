module dev.horizon.leadseal.mariadb {
  requires transitive dev.horizon.leadseal;

  exports dev.horizon.leadseal.mariadb;

  provides dev.horizon.leadseal.Dialect with dev.horizon.leadseal.mariadb.MariaDbDialect;
}
