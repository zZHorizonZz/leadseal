package dev.horizon.leadseal.protoc;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest;
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorResponse;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LeadsealGeneratorTest {

  @TempDir
  Path migrations;

  @Test
  void reportsAnUnknownDialectAsAnError() {
    CodeGeneratorRequest request = CodeGeneratorRequest.newBuilder()
        .setParameter("migrations=" + migrations + ",dialect=oracle").build();

    CodeGeneratorResponse response = LeadsealGenerator.processRequest(request);

    assertThat(response.getError()).contains("No installed dialect is named 'oracle'");
    assertThat(response.getFileList()).isEmpty();
  }
}
