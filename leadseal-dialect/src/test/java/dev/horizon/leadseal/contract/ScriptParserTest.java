package dev.horizon.leadseal.contract;

import static org.assertj.core.api.Assertions.assertThat;

import dev.horizon.leadseal.ScriptWriter;
import java.util.ArrayList;
import java.util.List;
import org.flywaydb.core.internal.resource.StringResource;
import org.flywaydb.core.internal.sqlscript.SqlStatementIterator;
import org.junit.jupiter.api.Test;

public abstract class ScriptParserTest {

  protected abstract TestDatabase database();

  @Test
  void splitsTheTextIntoTheStatementsOfTheScript() {
    ScriptWriter script = ScriptContractTest.script(database());

    assertThat(parse(script.text())).containsExactlyElementsOf(script.statements());
  }

  private List<String> parse(String text) {
    List<String> statements = new ArrayList<>();
    try (SqlStatementIterator iterator = database().flywayParser().parse(new StringResource(text))) {
      iterator.forEachRemaining(statement -> statements.add(trim(statement.getSql())));
    }
    return statements;
  }

  private static String trim(String statement) {
    String sql = statement.strip();
    return sql.endsWith(";") ? sql.substring(0, sql.length() - 1).strip() : sql;
  }
}
