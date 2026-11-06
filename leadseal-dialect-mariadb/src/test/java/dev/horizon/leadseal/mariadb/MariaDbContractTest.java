package dev.horizon.leadseal.mariadb;

import dev.horizon.leadseal.contract.DialectContract;

class MariaDbContractTest extends DialectContract {

  MariaDbContractTest() {
    super(new MariaDbTestDatabase());
  }

}
