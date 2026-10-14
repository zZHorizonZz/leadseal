package dev.horizon.leadseal.postgres;

import dev.horizon.leadseal.contract.ColumnContractTest;
import dev.horizon.leadseal.contract.KeyContractTest;
import dev.horizon.leadseal.contract.TableContractTest;
import dev.horizon.leadseal.contract.TestDatabase;
import org.junit.jupiter.api.Nested;

class PostgresContractTest {

  private static final TestDatabase DATABASE = new PostgresTestDatabase();

  @Nested
  class Table extends TableContractTest {

    @Override
    protected TestDatabase database() {
      return DATABASE;
    }
  }

  @Nested
  class Column extends ColumnContractTest {

    @Override
    protected TestDatabase database() {
      return DATABASE;
    }
  }

  @Nested
  class Key extends KeyContractTest {

    @Override
    protected TestDatabase database() {
      return DATABASE;
    }
  }
}
