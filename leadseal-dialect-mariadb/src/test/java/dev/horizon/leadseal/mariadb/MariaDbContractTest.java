package dev.horizon.leadseal.mariadb;

import dev.horizon.leadseal.contract.CommentContractTest;
import dev.horizon.leadseal.contract.DialectContract;
import dev.horizon.leadseal.contract.IdentityChangeContractTest;
import dev.horizon.leadseal.contract.IdentityRemovalContractTest;
import dev.horizon.leadseal.contract.SchemaContractTest;
import dev.horizon.leadseal.contract.TestDatabase;
import org.junit.jupiter.api.Nested;

class MariaDbContractTest extends DialectContract {

  MariaDbContractTest() {
    super(new MariaDbTestDatabase());
  }

  @Nested
  class Schemas extends SchemaContractTest {

    @Override
    protected TestDatabase database() {
      return fixture;
    }
  }

  @Nested
  class Comments extends CommentContractTest {

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

  @Nested
  class IdentityRemoval extends IdentityRemovalContractTest {

    @Override
    protected TestDatabase database() {
      return fixture;
    }
  }
}
