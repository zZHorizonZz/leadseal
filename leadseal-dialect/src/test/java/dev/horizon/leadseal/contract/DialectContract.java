package dev.horizon.leadseal.contract;

import org.junit.jupiter.api.Nested;

public abstract class DialectContract {

  protected final TestDatabase fixture;

  protected DialectContract(TestDatabase fixture) {
    this.fixture = fixture;
  }

  @Nested
  class Table extends TableContractTest {

    @Override
    protected TestDatabase database() {
      return fixture;
    }
  }

  @Nested
  class Column extends ColumnContractTest {

    @Override
    protected TestDatabase database() {
      return fixture;
    }
  }

  @Nested
  class Key extends KeyContractTest {

    @Override
    protected TestDatabase database() {
      return fixture;
    }
  }

  @Nested
  class Default extends DefaultContractTest {

    @Override
    protected TestDatabase database() {
      return fixture;
    }
  }

  @Nested
  class Identity extends IdentityContractTest {

    @Override
    protected TestDatabase database() {
      return fixture;
    }
  }

  @Nested
  class Check extends CheckContractTest {

    @Override
    protected TestDatabase database() {
      return fixture;
    }
  }

  @Nested
  class Script extends ScriptContractTest {

    @Override
    protected TestDatabase database() {
      return fixture;
    }
  }

  @Nested
  class ScriptParser extends ScriptParserTest {

    @Override
    protected TestDatabase database() {
      return fixture;
    }
  }

  @Nested
  class Expressions extends SqlExpressionContractTest {

    @Override
    protected TestDatabase database() {
      return fixture;
    }
  }
}
