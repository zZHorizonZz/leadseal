package dev.horizon.leadseal.migration;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.horizon.leadseal.BrokenHistoryException;
import dev.horizon.leadseal.Schema;
import java.nio.file.Path;

record Snapshot(@JsonProperty("schemaVersion") String format, @JsonProperty("storage") Schema storage,
    @JsonProperty("storageHash") SchemaHash hash) {

  private static final String FORMAT = "1";

  Snapshot(Schema storage, SchemaHash hash) {
    this(FORMAT, storage, hash);
  }

  Schema schema(Path file) {
    if (!FORMAT.equals(format)) {
      throw new BrokenHistoryException("The snapshot " + file + " has the format version " + format + ", but this version"
          + " of leadseal reads version " + FORMAT + ".");
    }
    return storage;
  }
}
