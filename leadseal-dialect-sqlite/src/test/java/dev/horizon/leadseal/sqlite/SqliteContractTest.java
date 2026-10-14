package dev.horizon.leadseal.sqlite;

import dev.horizon.leadseal.contract.CheckContractTest;
import dev.horizon.leadseal.contract.ColumnContractTest;
import dev.horizon.leadseal.contract.CommentContractTest;
import dev.horizon.leadseal.contract.DefaultContractTest;
import dev.horizon.leadseal.contract.IdentityContractTest;
import dev.horizon.leadseal.contract.KeyContractTest;
import dev.horizon.leadseal.contract.SchemaContractTest;
import dev.horizon.leadseal.contract.TableContractTest;
import dev.horizon.leadseal.contract.TestDatabase;
import org.junit.jupiter.api.Nested;

class SqliteContractTest {

  private static final TestDatabase DATABASE = new SqliteTestDatabase();

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

  @Nested
  class Default extends DefaultContractTest {

    @Override
    protected TestDatabase database() {
      return DATABASE;
    }
  }

  @Nested
  class Identity extends IdentityContractTest {

    @Override
    protected TestDatabase database() {
      return DATABASE;
    }
  }

  @Nested
  class Check extends CheckContractTest {

    @Override
    protected TestDatabase database() {
      return DATABASE;
    }
  }

  @Nested
  class Comment extends CommentContractTest {

    @Override
    protected TestDatabase database() {
      return DATABASE;
    }
  }

  @Nested
  class Schema extends SchemaContractTest {

    @Override
    protected TestDatabase database() {
      return DATABASE;
    }
  }
}
