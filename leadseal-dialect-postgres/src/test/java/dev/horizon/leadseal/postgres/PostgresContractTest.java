package dev.horizon.leadseal.postgres;

import dev.horizon.leadseal.contract.DialectContract;

class PostgresContractTest extends DialectContract {

  PostgresContractTest() {
    super(new PostgresTestDatabase());
  }

}
