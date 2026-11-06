package dev.horizon.leadseal.sqlite;

import dev.horizon.leadseal.contract.DialectContract;

class SqliteContractTest extends DialectContract {

  SqliteContractTest() {
    super(new SqliteTestDatabase());
  }

}
