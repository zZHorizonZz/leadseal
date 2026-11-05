package dev.horizon.leadseal.sqlserver;

import dev.horizon.leadseal.contract.CommentContractTest;
import dev.horizon.leadseal.contract.DialectContract;
import dev.horizon.leadseal.contract.EnumRenameRefusalContractTest;
import dev.horizon.leadseal.contract.PartialIndexContractTest;
import dev.horizon.leadseal.contract.SchemaContractTest;
import dev.horizon.leadseal.contract.TestDatabase;
import org.junit.jupiter.api.Nested;

class SqlServerContractTest extends DialectContract {

  SqlServerContractTest() {
    super(new SqlServerTestDatabase());
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
  class PartialIndexes extends PartialIndexContractTest {

    @Override
    protected TestDatabase database() {
      return fixture;
    }
  }

  @Nested
  class Enums extends EnumRenameRefusalContractTest {

    @Override
    protected TestDatabase database() {
      return fixture;
    }
  }
}
