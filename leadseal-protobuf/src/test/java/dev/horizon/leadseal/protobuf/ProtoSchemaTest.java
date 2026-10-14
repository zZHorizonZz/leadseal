package dev.horizon.leadseal.protobuf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.DescriptorProtos.SourceCodeInfo;
import com.google.protobuf.Descriptors.DescriptorValidationException;
import com.google.protobuf.Descriptors.FileDescriptor;
import dev.horizon.leadseal.Check;
import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.ColumnType;
import dev.horizon.leadseal.Default;
import dev.horizon.leadseal.ForeignKey;
import dev.horizon.leadseal.Index;
import dev.horizon.leadseal.Schema;
import dev.horizon.leadseal.SchemaException;
import dev.horizon.leadseal.Table;
import dev.horizon.leadseal.protobuf.invalid.defaults.BothDefaultsOuterClass;
import dev.horizon.leadseal.protobuf.invalid.enumdefault.UnknownEnumDefaultOuterClass;
import dev.horizon.leadseal.protobuf.invalid.identity.TextIdentityOuterClass;
import dev.horizon.leadseal.protobuf.invalid.index.UnknownIndexFieldOuterClass;
import dev.horizon.leadseal.protobuf.invalid.nokey.NoPrimaryKeyOuterClass;
import dev.horizon.leadseal.protobuf.invalid.optional.OptionalKeyOuterClass;
import dev.horizon.leadseal.protobuf.invalid.reference.UnknownReferenceOuterClass;
import dev.horizon.leadseal.protobuf.invalid.type.BadTypeOuterClass;
import dev.horizon.leadseal.protobuf.test.ShopProto;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ProtoSchemaTest {

  private static final Schema SCHEMA = new ProtoSchema().read(List.of(ShopProto.getDescriptor()));

  private static Table table(String name) {
    return SCHEMA.table(name).orElseThrow();
  }

  static Stream<Arguments> columns() {
    return Stream.of(
        arguments(new Column("1", "order_id", ColumnType.UUID, null, false)),
        arguments(new Column("2", "line", ColumnType.INT, null, false)),
        arguments(new Column("3", "customer_id", ColumnType.BIGINT, null, false)),
        arguments(new Column("4", "gift", ColumnType.BOOLEAN, null, false)),
        arguments(new Column("5", "quantity", ColumnType.BIGINT, null, false)),
        arguments(new Column("6", "total", ColumnType.BIGINT, null, false)),
        arguments(new Column("7", "weight", ColumnType.REAL, null, false)),
        arguments(new Column("8", "price", ColumnType.DOUBLE, null, false)),
        arguments(new Column("9", "thumbnail", ColumnType.BYTES, null, false)),
        arguments(new Column("10", "state", ColumnType.TEXT, null, false)),
        arguments(new Column("11", "created", ColumnType.TIMESTAMP, null, true)),
        arguments(new Column("12", "discount", ColumnType.BIGINT, null, true)),
        arguments(new Column("13", "attributes", ColumnType.JSON, null, true)),
        arguments(new Column("14", "tags", ColumnType.JSON, null, true)),
        arguments(new Column("15", "labels", ColumnType.JSON, null, true)),
        arguments(new Column("16", "remark", ColumnType.TEXT, null, true)),
        arguments(new Column("17", "payload", ColumnType.JSON, null, false)));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("columns")
  void mapsAFieldToAColumn(Column column) {
    assertThat(table("order_line").columnById(column.id())).contains(column);
  }

  @Test
  void readsOnlyTableMessages() {
    assertThat(SCHEMA.tables()).extracting(Table::id).containsExactly("leadseal.test.Customer",
        "leadseal.test.OrderLine", "leadseal.test.Payment");
  }

  @Test
  void readsTheTableOptions() {
    Table customers = table("customers");

    assertThat(customers.columns()).extracting(Column::name).containsExactly("id", "email", "region");
    assertThat(customers.column("email")).contains(new Column("2", "email", ColumnType.TEXT, 200, false));
    assertThat(customers.primaryKey().columns()).containsExactly("id");
    assertThat(customers.indexes()).containsExactly(Index.of("customers", List.of("email"), true),
        Index.of("customers", List.of("region", "email"), false));
  }

  @Test
  void readsTheKeysOfAField() {
    Table lines = table("order_line");

    assertThat(lines.primaryKey().columns()).containsExactly("order_id", "line");
    assertThat(lines.indexes()).containsExactly(Index.of("order_line", List.of("customer_id"), false));
    assertThat(lines.foreignKeys()).containsExactly(ForeignKey.of("order_line", List.of("customer_id"),
        "customers", List.of("id"), ForeignKey.Action.CASCADE));
  }

  static Stream<Arguments> invalid() {
    return Stream.of(
        arguments(NoPrimaryKeyOuterClass.getDescriptor(), "has no primary key"),
        arguments(BadTypeOuterClass.getDescriptor(), "cannot have the column type UUID"),
        arguments(UnknownReferenceOuterClass.getDescriptor(), "which is not a table message"),
        arguments(OptionalKeyOuterClass.getDescriptor(), "must not be nullable"),
        arguments(UnknownIndexFieldOuterClass.getDescriptor(), "has no field 'missing'"),
        arguments(BothDefaultsOuterClass.getDescriptor(), "both a default_value and a generated default"),
        arguments(UnknownEnumDefaultOuterClass.getDescriptor(), "is not a value of"),
        arguments(TextIdentityOuterClass.getDescriptor(), "cannot have the column type INT"));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("invalid")
  void rejects(FileDescriptor file, String message) {
    assertThatThrownBy(() -> new ProtoSchema().read(List.of(file))).isInstanceOf(SchemaException.class)
        .hasMessageContaining(message);
  }

  static Stream<Arguments> paymentColumns() {
    return Stream.of(
        arguments(new Column("1", "id", ColumnType.BIGINT, null, false).withIdentity(true)),
        arguments(new Column("2", "amount", ColumnType.DECIMAL, null, 12, 2, false, Default.value("0"), false, null,
            null)),
        arguments(new Column("3", "paid", ColumnType.TIMESTAMP, null, false).withDefault(Default.now())),
        arguments(new Column("4", "day", ColumnType.DATE, null, true)),
        arguments(new Column("5", "method", ColumnType.INT, null, false).withDefault(Default.value("1"))),
        arguments(new Column("6", "reference", ColumnType.TEXT, 40, false).withCollations(Map.of("postgresql",
            "C"))),
        arguments(new Column("9", "token", ColumnType.UUID, null, false).withDefault(Default.randomUuid())),
        arguments(new Column("10", "fallback", ColumnType.TEXT, null, false).withDefault(Default.value(
            "METHOD_CASH"))),
        arguments(new Column("11", "rate", ColumnType.DOUBLE, null, false).withDefault(Default.value("1.5"))),
        arguments(new Column("12", "signature", ColumnType.BYTES, 64, false).withDefault(Default.value("0aff"))),
        arguments(new Column("13", "start", ColumnType.TIMESTAMP, null, true).withDefault(Default.value(
            "2026-10-14T07:30:00Z"))),
        arguments(new Column("14", "active", ColumnType.BOOLEAN, null, false).withDefault(Default.value("true"))));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("paymentColumns")
  void readsTheColumnOptions(Column column) {
    assertThat(table("payment").columnById(column.id())).contains(column);
  }

  @Test
  void readsTheTableOptionsOfPayments() {
    Table payments = table("payment");

    assertThat(payments.schema()).isEqualTo("billing");
    assertThat(payments.checks()).containsExactly(Check.of("payment", "amount < 1000000"), Check.of("payment",
        "amount >= 0"));
    assertThat(payments.indexes()).containsExactly(new Index("ix_recent_payments", List.of("paid", "amount"), false,
        List.of("paid"), "day IS NOT NULL"));
    assertThat(payments.foreignKeys()).containsExactly(ForeignKey.of("payment", List.of("order_id", "line"),
        "order_line", List.of("order_id", "line"), ForeignKey.Action.CASCADE).withOnUpdate(ForeignKey.Action.CASCADE));
  }

  @Test
  void readsCommentsFromTheSource() throws DescriptorValidationException {
    FileDescriptor original = ShopProto.getDescriptor();
    int customer = original.findMessageTypeByName("Customer").getIndex();
    FileDescriptorProto proto = original.toProto().toBuilder().setSourceCodeInfo(SourceCodeInfo.newBuilder()
        .addLocation(SourceCodeInfo.Location.newBuilder().addAllPath(List.of(4, customer))
            .setLeadingComments(" The customers of the shop.\n Each one has an email.\n"))
        .addLocation(SourceCodeInfo.Location.newBuilder().addAllPath(List.of(4, customer, 2, 1))
            .setLeadingComments(" The address for receipts.\n")))
        .build();
    FileDescriptor file = FileDescriptor.buildFrom(proto, original.getDependencies().toArray(FileDescriptor[]::new));

    Table customers = new ProtoSchema().read(List.of(file)).table("customers").orElseThrow();

    assertThat(customers.comment()).isEqualTo("The customers of the shop.\nEach one has an email.");
    assertThat(customers.column("email").orElseThrow().comment()).isEqualTo("The address for receipts.");
    assertThat(customers.column("id").orElseThrow().comment()).isNull();
  }
}
