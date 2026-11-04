package dev.horizon.leadseal.sqlite;

import dev.horizon.leadseal.contract.DialectContract;
import dev.horizon.leadseal.contract.IdentityChangeContractTest;
import dev.horizon.leadseal.contract.PartialIndexContractTest;
import dev.horizon.leadseal.contract.TestDatabase;
import org.junit.jupiter.api.Nested;

class SqliteContractTest extends DialectContract {

  SqliteContractTest() {
    super(new SqliteTestDatabase());
  }

  @Nested
  class PartialIndexes extends PartialIndexContractTest {

    @Override
    protected TestDatabase database() {
      return fixture;
    }
  }

  @Nested
  class IdentityChanges extends IdentityChangeContractTest {

    @Override
    protected TestDatabase database() {
      return fixture;
    }
  }
}
