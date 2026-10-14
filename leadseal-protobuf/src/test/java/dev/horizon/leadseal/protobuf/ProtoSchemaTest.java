package dev.horizon.leadseal.protobuf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.DescriptorProtos.SourceCodeInfo;
import com.google.protobuf.Descriptors.DescriptorValidationException;
import com.google.protobuf.Descriptors.FileDescriptor;
import dev.horizon.leadseal.BooleanType;
import dev.horizon.leadseal.BytesType;
import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.DateType;
import dev.horizon.leadseal.DecimalType;
import dev.horizon.leadseal.FloatType;
import dev.horizon.leadseal.IntegerType;
import dev.horizon.leadseal.JsonType;
import dev.horizon.leadseal.Schema;
import dev.horizon.leadseal.SchemaException;
import dev.horizon.leadseal.Table;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.TextType;
import dev.horizon.leadseal.TimestampType;
import dev.horizon.leadseal.UuidType;
import dev.horizon.leadseal.attribute.CheckAttribute;
import dev.horizon.leadseal.attribute.CollationAttribute;
import dev.horizon.leadseal.attribute.CommentAttribute;
import dev.horizon.leadseal.attribute.ForeignKeyAttribute;
import dev.horizon.leadseal.attribute.IdentityAttribute;
import dev.horizon.leadseal.attribute.IndexAttribute;
import dev.horizon.leadseal.attribute.LiteralDefaultAttribute;
import dev.horizon.leadseal.attribute.NotNullAttribute;
import dev.horizon.leadseal.attribute.NowDefaultAttribute;
import dev.horizon.leadseal.attribute.RandomUuidDefaultAttribute;
import dev.horizon.leadseal.protobuf.invalid.defaults.BothDefaultsOuterClass;
import dev.horizon.leadseal.protobuf.invalid.enumdefault.UnknownEnumDefaultOuterClass;
import dev.horizon.leadseal.protobuf.invalid.identity.TextIdentityOuterClass;
import dev.horizon.leadseal.protobuf.invalid.index.UnknownIndexFieldOuterClass;
import dev.horizon.leadseal.protobuf.invalid.nokey.NoPrimaryKeyOuterClass;
import dev.horizon.leadseal.protobuf.invalid.optional.OptionalKeyOuterClass;
import dev.horizon.leadseal.protobuf.invalid.reference.UnknownReferenceOuterClass;
import dev.horizon.leadseal.protobuf.invalid.type.BadTypeOuterClass;
import dev.horizon.leadseal.protobuf.test.ShopProto;
import dev.horizon.leadseal.protobuf.test.v2.CatalogProto;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ProtoSchemaTest {

  private static final Schema SCHEMA = new ProtoSchema().read(List.of(ShopProto.getDescriptor()));
  private static final IntegerType INT = new IntegerType(IntegerType.Size.INT32);
  private static final IntegerType BIGINT = new IntegerType(IntegerType.Size.INT64);
  private static final NotNullAttribute NOT_NULL = NotNullAttribute.INSTANCE;

  private static Table table(TableName name) {
    return SCHEMA.table(name).orElseThrow();
  }

  static Stream<Arguments> columns() {
    return Stream.of(
        arguments(Column.of("1", "order_id", new UuidType(), NOT_NULL)),
        arguments(Column.of("2", "line", INT, NOT_NULL)),
        arguments(Column.of("3", "customer_id", BIGINT, NOT_NULL)),
        arguments(Column.of("4", "gift", new BooleanType(), NOT_NULL)),
        arguments(Column.of("5", "quantity", BIGINT, NOT_NULL)),
        arguments(Column.of("6", "total", BIGINT, NOT_NULL)),
        arguments(Column.of("7", "weight", new FloatType(FloatType.Size.FLOAT32), NOT_NULL)),
        arguments(Column.of("8", "price", new FloatType(FloatType.Size.FLOAT64), NOT_NULL)),
        arguments(Column.of("9", "thumbnail", new BytesType(null), NOT_NULL)),
        arguments(Column.of("10", "state", new TextType(null), NOT_NULL)),
        arguments(Column.of("11", "created", new TimestampType())),
        arguments(Column.of("12", "discount", BIGINT)),
        arguments(Column.of("13", "attributes", new JsonType())),
        arguments(Column.of("14", "tags", new JsonType())),
        arguments(Column.of("15", "labels", new JsonType())),
        arguments(Column.of("16", "remark", new TextType(null))),
        arguments(Column.of("17", "payload", new JsonType(), NOT_NULL)));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("columns")
  void mapsAFieldToAColumn(Column column) {
    assertThat(table(TableName.of("order_line")).columnById(column.id())).contains(column);
  }

  @Test
  void readsOnlyTableMessages() {
    assertThat(SCHEMA.names()).containsExactly(TableName.of("customers"), TableName.of("order_line"),
        new TableName("billing", "payment"));
  }

  @Test
  void identifiesTablesWithoutThePackageVersion() {
    Schema catalog = new ProtoSchema().read(List.of(CatalogProto.getDescriptor()));

    assertThat(catalog.namespaces().getFirst().tables()).extracting(Table::id).containsExactly(
        "leadseal.test.Product", "leadseal.test.Product.Variant", "catalog.Price");
    assertThat(SCHEMA.names()).extracting(name -> SCHEMA.table(name).orElseThrow().id()).containsExactly(
        "leadseal.test.Customer", "leadseal.test.OrderLine", "leadseal.test.Payment");
  }

  @Test
  void readsTheTableOptions() {
    Table customers = table(TableName.of("customers"));

    assertThat(customers.columns()).extracting(Column::name).containsExactly("id", "email", "region");
    assertThat(customers.column("email")).contains(Column.of("2", "email", new TextType(200), NOT_NULL));
    assertThat(customers.primaryKey().columns()).containsExactly("id");
    assertThat(customers.attributes(IndexAttribute.class)).containsExactly(
        IndexAttribute.of("customers", List.of("email"), true),
        IndexAttribute.of("customers", List.of("region", "email"), false));
  }

  @Test
  void readsTheKeysOfAField() {
    Table lines = table(TableName.of("order_line"));

    assertThat(lines.primaryKey().columns()).containsExactly("order_id", "line");
    assertThat(lines.attributes(IndexAttribute.class)).containsExactly(IndexAttribute.of("order_line",
        List.of("customer_id"), false));
    assertThat(lines.attributes(ForeignKeyAttribute.class)).containsExactly(ForeignKeyAttribute.of("order_line",
        List.of("customer_id"), TableName.of("customers"), List.of("id"), ForeignKeyAttribute.Action.CASCADE));
  }

  static Stream<Arguments> invalid() {
    return Stream.of(
        arguments(NoPrimaryKeyOuterClass.getDescriptor(), "has no primary key"),
        arguments(BadTypeOuterClass.getDescriptor(), "cannot have the column type UUID"),
        arguments(UnknownReferenceOuterClass.getDescriptor(), "which is not a table message"),
        arguments(OptionalKeyOuterClass.getDescriptor(), "must not accept NULL"),
        arguments(UnknownIndexFieldOuterClass.getDescriptor(), "has no field 'missing'"),
        arguments(BothDefaultsOuterClass.getDescriptor(), "both a default_value and a generated default"),
        arguments(UnknownEnumDefaultOuterClass.getDescriptor(), "is not a value of"),
        arguments(TextIdentityOuterClass.getDescriptor(), "cannot have the column type 32-bit integer"));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("invalid")
  void rejects(FileDescriptor file, String message) {
    assertThatThrownBy(() -> new ProtoSchema().read(List.of(file))).isInstanceOf(SchemaException.class)
        .hasMessageContaining(message);
  }

  static Stream<Arguments> paymentColumns() {
    return Stream.of(
        arguments(Column.of("1", "id", BIGINT, NOT_NULL, IdentityAttribute.INSTANCE)),
        arguments(Column.of("2", "amount", new DecimalType(12, 2), NOT_NULL, new LiteralDefaultAttribute("0"))),
        arguments(Column.of("3", "paid", new TimestampType(), NOT_NULL, new NowDefaultAttribute())),
        arguments(Column.of("4", "day", new DateType())),
        arguments(Column.of("5", "method", INT, NOT_NULL, new LiteralDefaultAttribute("1"))),
        arguments(Column.of("6", "reference", new TextType(40), NOT_NULL, new CollationAttribute("postgresql", "C"))),
        arguments(Column.of("9", "token", new UuidType(), NOT_NULL, new RandomUuidDefaultAttribute())),
        arguments(Column.of("10", "fallback", new TextType(null), NOT_NULL, new LiteralDefaultAttribute(
            "METHOD_CASH"))),
        arguments(Column.of("11", "rate", new FloatType(FloatType.Size.FLOAT64), NOT_NULL,
            new LiteralDefaultAttribute("1.5"))),
        arguments(Column.of("12", "signature", new BytesType(64), NOT_NULL, new LiteralDefaultAttribute("0aff"))),
        arguments(Column.of("13", "start", new TimestampType(), new LiteralDefaultAttribute("2026-10-14T07:30:00Z"))),
        arguments(Column.of("14", "active", new BooleanType(), NOT_NULL, new LiteralDefaultAttribute("true"))));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("paymentColumns")
  void readsTheColumnOptions(Column column) {
    assertThat(table(new TableName("billing", "payment")).columnById(column.id())).contains(column);
  }

  @Test
  void readsTheTableOptionsOfPayments() {
    Table payments = table(new TableName("billing", "payment"));

    assertThat(payments.attributes(CheckAttribute.class)).containsExactly(CheckAttribute.of("payment",
        "amount < 1000000"), CheckAttribute.of("payment", "amount >= 0"));
    assertThat(payments.attributes(IndexAttribute.class)).containsExactly(new IndexAttribute("ix_recent_payments",
        List.of("paid", "amount"), false, List.of("paid"), "day IS NOT NULL"));
    assertThat(payments.attributes(ForeignKeyAttribute.class)).containsExactly(ForeignKeyAttribute.of("payment",
        List.of("order_id", "line"), TableName.of("order_line"), List.of("order_id", "line"),
        ForeignKeyAttribute.Action.CASCADE).withOnUpdate(ForeignKeyAttribute.Action.CASCADE));
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

    Table customers = new ProtoSchema().read(List.of(file)).table(TableName.of("customers")).orElseThrow();

    assertThat(customers.attribute(CommentAttribute.class)).contains(new CommentAttribute(
        "The customers of the shop.\nEach one has an email."));
    assertThat(customers.column("email").orElseThrow().attribute(CommentAttribute.class))
        .contains(new CommentAttribute("The address for receipts."));
    assertThat(customers.column("id").orElseThrow().attribute(CommentAttribute.class)).isEmpty();
  }
}
