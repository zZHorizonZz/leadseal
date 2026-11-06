package dev.horizon.leadseal.sqlserver;

import dev.horizon.leadseal.contract.DialectContract;

class SqlServerContractTest extends DialectContract {

  SqlServerContractTest() {
    super(new SqlServerTestDatabase());
  }

}
