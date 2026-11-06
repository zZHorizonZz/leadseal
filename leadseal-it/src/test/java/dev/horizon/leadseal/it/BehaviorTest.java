package dev.horizon.leadseal.it;

import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

class BehaviorTest {

  @TestFactory
  @DisplayName("Runs each behavior file of src/test/cases on each database")
  Stream<DynamicContainer> runBehaviors() throws Exception {
    Cases cases = new Cases(Path.of(System.getProperty("leadseal.cases")), Path.of(System.getProperty(
        "leadseal.descriptors")));
    return cases.behaviors().stream().map(behavior -> DynamicContainer.dynamicContainer(behavior.suite() + "/"
        + behavior.name(),
        Stream.of(Database.values()).filter(behavior::runs).map(database -> DynamicTest.dynamicTest(database.dialect(),
            () -> behavior.run(database, cases)))));
  }
}
