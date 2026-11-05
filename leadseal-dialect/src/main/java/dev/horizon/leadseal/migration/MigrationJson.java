package dev.horizon.leadseal.migration;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import dev.horizon.leadseal.BrokenHistoryException;
import dev.horizon.leadseal.Digest;
import dev.horizon.leadseal.Schema;
import java.io.IOException;
import java.nio.file.Path;

final class MigrationJson {

  private static final int HASH_LENGTH = 16;

  private final ObjectMapper mapper = new ObjectMapper()
      .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
      .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
      .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);

  String write(Object value) {
    try {
      return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(value) + "\n";
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Cannot write " + value.getClass().getSimpleName() + " as JSON.", exception);
    }
  }

  <T> T read(Path file, Class<T> type) {
    try {
      return mapper.readValue(file.toFile(), type);
    } catch (IOException exception) {
      throw new BrokenHistoryException("Cannot read " + file + ": " + exception.getMessage(), exception);
    }
  }

  SchemaHash hash(Schema schema) {
    try {
      return new SchemaHash.Value(new Digest(mapper.writeValueAsString(schema)).hex(HASH_LENGTH));
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Cannot write the schema as JSON.", exception);
    }
  }
}
