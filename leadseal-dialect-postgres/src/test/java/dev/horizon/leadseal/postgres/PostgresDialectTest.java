package dev.horizon.leadseal.postgres;

import static org.assertj.core.api.Assertions.assertThat;

import dev.horizon.leadseal.Dialect;
import dev.horizon.leadseal.contract.DialectExpressionTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PostgresDialectTest {

  @ParameterizedTest(name = "[{0}]")
  @CsvSource(delimiter = '|', quoteCharacter = '~', textBlock = """
      BEGIN RETURN NEW; END;               | $leadseal$
      SELECT '$leadseal$', '$leadseal1$'   | $leadseal2$
      """)
  void quotesABodyWithATagThatTheBodyDoesNotContain(String body, String tag) {
    assertThat(new PostgresDialect().dollarQuote(body)).isEqualTo(tag + "\n" + body + "\n" + tag);
  }

  @Nested
  class Expressions extends DialectExpressionTest {

    @Override
    protected Dialect dialect() {
      return new PostgresDialect();
    }

    @Override
    protected String sql() {
      return """
          PAID_CONDITION                 | NEW."paid"
          NOT_PAID_CONDITION             | (NOT NEW."paid")
          POSITIVE_CONDITION             | (NEW."quantity" > 2)
          POSITIVE_VALUE                 | (NEW."quantity" > 2)
          POSITIVE_EQUALS_PAID_CONDITION | ((NEW."quantity" > 2) = NEW."paid")
          TRUE_CONDITION                 | true
          CONCAT_VALUE                   | (NEW."name" || '!')
          INTEGER_DIVIDE_VALUE           | (NEW."quantity" / 2)
          LENGTH_VALUE                   | CHAR_LENGTH(NEW."name")
          CHANGE_CONDITION               | (OLD."name" IS DISTINCT FROM NEW."name")
          NOW_VALUE                      | CURRENT_TIMESTAMP
          UUID_VALUE                     | gen_random_uuid()
          """;
    }
  }
}
