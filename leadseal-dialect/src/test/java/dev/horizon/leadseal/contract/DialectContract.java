package dev.horizon.leadseal.contract;

import org.junit.jupiter.api.Nested;

public abstract class DialectContract {

  protected final TestDatabase fixture;

  protected DialectContract(TestDatabase fixture) {
    this.fixture = fixture;
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

}
