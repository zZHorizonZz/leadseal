package dev.horizon.leadseal.protobuf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.DescriptorProtos.SourceCodeInfo;
import com.google.protobuf.Descriptors.DescriptorValidationException;
import com.google.protobuf.Descriptors.FileDescriptor;
import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.InvalidSchemaException;
import dev.horizon.leadseal.Schema;
import dev.horizon.leadseal.Table;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.attribute.Check;
import dev.horizon.leadseal.attribute.Collation;
import dev.horizon.leadseal.attribute.Comment;
import dev.horizon.leadseal.attribute.ForeignKey;
import dev.horizon.leadseal.attribute.Identity;
import dev.horizon.leadseal.attribute.Index;
import dev.horizon.leadseal.attribute.LiteralDefault;
import dev.horizon.leadseal.attribute.Mutation;
import dev.horizon.leadseal.attribute.NotNull;
import dev.horizon.leadseal.attribute.NowDefault;
import dev.horizon.leadseal.attribute.RandomUuidDefault;
import dev.horizon.leadseal.attribute.Rewrite;
import dev.horizon.leadseal.attribute.Trigger;
import dev.horizon.leadseal.attribute.Triggers;
import dev.horizon.leadseal.expression.ColumnChange;
import dev.horizon.leadseal.expression.ColumnReference;
import dev.horizon.leadseal.expression.Comparison;
import dev.horizon.leadseal.expression.ForeignColumn;
import dev.horizon.leadseal.expression.Literal;
import dev.horizon.leadseal.expression.Row;
import dev.horizon.leadseal.protobuf.invalid.action.TriggerActionOuterClass;
import dev.horizon.leadseal.protobuf.invalid.defaults.BothDefaultsOuterClass;
import dev.horizon.leadseal.protobuf.invalid.enumdefault.UnknownEnumDefaultOuterClass;
import dev.horizon.leadseal.protobuf.invalid.identity.TextIdentityOuterClass;
import dev.horizon.leadseal.protobuf.invalid.index.UnknownIndexFieldOuterClass;
import dev.horizon.leadseal.protobuf.invalid.key.RewriteKeyOuterClass;
import dev.horizon.leadseal.protobuf.invalid.nokey.NoPrimaryKeyOuterClass;
import dev.horizon.leadseal.protobuf.invalid.optional.OptionalKeyOuterClass;
import dev.horizon.leadseal.protobuf.invalid.own.TriggerOwnOuterClass;
import dev.horizon.leadseal.protobuf.invalid.reference.UnknownReferenceOuterClass;
import dev.horizon.leadseal.protobuf.invalid.rewrite.RewriteTypeOuterClass;
import dev.horizon.leadseal.protobuf.invalid.type.BadTypeOuterClass;
import dev.horizon.leadseal.protobuf.invalid.where.TriggerWhereOuterClass;
import dev.horizon.leadseal.protobuf.test.PostProto;
import dev.horizon.leadseal.protobuf.test.ShopProto;
import dev.horizon.leadseal.protobuf.test.v2.CatalogProto;
import dev.horizon.leadseal.type.BooleanType;
import dev.horizon.leadseal.type.BytesType;
import dev.horizon.leadseal.type.DateType;
import dev.horizon.leadseal.type.DecimalType;
import dev.horizon.leadseal.type.EnumType;
import dev.horizon.leadseal.type.FloatType;
import dev.horizon.leadseal.type.IntegerType;
import dev.horizon.leadseal.type.JsonType;
import dev.horizon.leadseal.type.TextType;
import dev.horizon.leadseal.type.TimestampType;
import dev.horizon.leadseal.type.UuidType;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ProtoSchemaTest {

  private static final Schema SCHEMA = new ProtoSchema().read(List.of(ShopProto.getDescriptor()));
  private static final IntegerType INT = new IntegerType(IntegerType.Size.INT32);
  private static final IntegerType BIGINT = new IntegerType(IntegerType.Size.INT64);
  private static final NotNull NOT_NULL = NotNull.INSTANCE;
  private static final List<EnumType.Value> METHODS = List.of(new EnumType.Value(0, "METHOD_UNSPECIFIED"),
      new EnumType.Value(1, "METHOD_CARD"), new EnumType.Value(2, "METHOD_CASH"));
  private static final EnumType STATE = new EnumType("order_line_state", List.of(new EnumType.Value(0,
      "STATE_UNSPECIFIED"), new EnumType.Value(1, "STATE_OPEN")), EnumType.Storage.NAME);

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
        arguments(Column.of("9", "thumbnail", BytesType.unlimited(), NOT_NULL)),
        arguments(Column.of("10", "state", STATE, NOT_NULL)),
        arguments(Column.of("11", "created", new TimestampType())),
        arguments(Column.of("12", "discount", BIGINT)),
        arguments(Column.of("13", "attributes", new JsonType())),
        arguments(Column.of("14", "tags", new JsonType())),
        arguments(Column.of("15", "labels", new JsonType())),
        arguments(Column.of("16", "remark", TextType.unlimited())),
        arguments(Column.of("17", "payload", new JsonType(), NOT_NULL)));
  }

  @ParameterizedTest(name = "({0})")
  @MethodSource("columns")
  @DisplayName("Maps a field to a column")
  void mapField(Column column) {
    assertThat(table(TableName.of("order_line")).columnById(column.id())).contains(column);
  }

  @Test
  @DisplayName("Reads only table messages")
  void readTableMessages() {
    assertThat(SCHEMA.names()).containsExactly(TableName.of("customers"), TableName.of("order_line"),
        new TableName("billing", "payment"));
  }

  @Test
  @DisplayName("Identifies tables without the package version")
  void identifyWithoutVersion() {
    Schema catalog = new ProtoSchema().read(List.of(CatalogProto.getDescriptor()));

    assertThat(catalog.namespaces().getFirst().tables()).extracting(Table::id).containsExactly(
        "leadseal.test.Product", "leadseal.test.Product.Variant", "catalog.Price");
    assertThat(SCHEMA.names()).extracting(name -> SCHEMA.table(name).orElseThrow().id()).containsExactly(
        "leadseal.test.Customer", "leadseal.test.OrderLine", "leadseal.test.Payment");
  }

  @Test
  @DisplayName("Reads the table options")
  void readTableOptions() {
    Table customers = table(TableName.of("customers"));

    assertThat(customers.columns()).extracting(Column::name).containsExactly("id", "email", "region");
    assertThat(customers.column("email")).contains(Column.of("2", "email", TextType.of(200), NOT_NULL));
    assertThat(customers.primaryKey().columns()).containsExactly("id");
    assertThat(customers.attributes(Index.class)).containsExactly(
        Index.of("customers", List.of("email"), true),
        Index.of("customers", List.of("region", "email"), false));
  }

  @Test
  @DisplayName("Reads the keys of a field")
  void readFieldKeys() {
    Table lines = table(TableName.of("order_line"));

    assertThat(lines.primaryKey().columns()).containsExactly("order_id", "line");
    assertThat(lines.attributes(Index.class)).containsExactly(Index.of("order_line",
        List.of("customer_id"), false));
    assertThat(lines.attributes(ForeignKey.class)).containsExactly(ForeignKey.of("order_line",
        List.of("customer_id"), TableName.of("customers"), List.of("id"), ForeignKey.Action.CASCADE));
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
        arguments(TextIdentityOuterClass.getDescriptor(), "cannot have the column type 32-bit integer"),
        arguments(RewriteTypeOuterClass.getDescriptor(), "'first'"),
        arguments(RewriteKeyOuterClass.getDescriptor(), "part of the primary key"),
        arguments(TriggerOwnOuterClass.getDescriptor(), "writes into its own table"),
        arguments(TriggerActionOuterClass.getDescriptor(), "has no action."),
        arguments(TriggerWhereOuterClass.getDescriptor(), "has no where."));
  }

  @ParameterizedTest(name = "({0})")
  @MethodSource("invalid")
  @DisplayName("Refuses a message whose options break a rule")
  void reject(FileDescriptor file, String message) {
    assertThatThrownBy(() -> new ProtoSchema().read(List.of(file))).isInstanceOf(InvalidSchemaException.class)
        .hasMessageContaining(message);
  }

  static Stream<Arguments> paymentColumns() {
    return Stream.of(
        arguments(Column.of("1", "id", BIGINT, NOT_NULL, Identity.INSTANCE)),
        arguments(Column.of("2", "amount", new DecimalType(12, 2), NOT_NULL, new LiteralDefault("0"))),
        arguments(Column.of("3", "paid", new TimestampType(), NOT_NULL, new NowDefault())),
        arguments(Column.of("4", "day", new DateType())),
        arguments(Column.of("5", "method", new EnumType("payment_method", METHODS, EnumType.Storage.NUMBER), NOT_NULL,
            new LiteralDefault("1"))),
        arguments(Column.of("6", "reference", TextType.of(40), NOT_NULL, new Collation("postgresql", "C"))),
        arguments(Column.of("9", "token", new UuidType(), NOT_NULL, new RandomUuidDefault())),
        arguments(Column.of("10", "fallback", new EnumType("payment_method", METHODS, EnumType.Storage.NAME),
            NOT_NULL, new LiteralDefault("METHOD_CASH"))),
        arguments(Column.of("11", "rate", new FloatType(FloatType.Size.FLOAT64), NOT_NULL,
            new LiteralDefault("1.5"))),
        arguments(Column.of("12", "signature", BytesType.of(64), NOT_NULL, new LiteralDefault("0aff"))),
        arguments(Column.of("13", "start", new TimestampType(), new LiteralDefault("2026-10-14T07:30:00Z"))),
        arguments(Column.of("14", "active", new BooleanType(), NOT_NULL, new LiteralDefault("true"))));
  }

  @ParameterizedTest(name = "({0})")
  @MethodSource("paymentColumns")
  @DisplayName("Reads the column options")
  void readColumnOptions(Column column) {
    assertThat(table(new TableName("billing", "payment")).columnById(column.id())).contains(column);
  }

  @Test
  @DisplayName("Reads the table options of payments")
  void readPaymentOptions() {
    Table payments = table(new TableName("billing", "payment"));

    assertThat(payments.attributes(Check.class)).containsExactly(Check.of("payment",
        "amount < 1000000"), Check.of("payment", "amount >= 0"));
    assertThat(payments.attributes(Index.class)).containsExactly(new Index("ix_recent_payments",
        List.of("paid", "amount"), false, List.of("paid"), Index.Rows.of("day IS NOT NULL")));
    assertThat(payments.attributes(ForeignKey.class)).containsExactly(ForeignKey.of("payment",
        List.of("order_id", "line"), TableName.of("order_line"), List.of("order_id", "line"),
        ForeignKey.Action.CASCADE).withOnUpdate(ForeignKey.Action.CASCADE));
  }

  @Test
  @DisplayName("Reads comments from the source")
  void readComments() throws DescriptorValidationException {
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

    assertThat(customers.attribute(Comment.class)).contains(new Comment(
        "The customers of the shop.\nEach one has an email."));
    assertThat(customers.column("email").orElseThrow().attribute(Comment.class))
        .contains(new Comment("The address for receipts."));
    assertThat(customers.column("id").orElseThrow().attribute(Comment.class)).isEmpty();
  }

  @Test
  @DisplayName("Names each enum by its path in the package, and gives each number once, by its first name")
  void readEnumTypes() {
    assertThat(SCHEMA.enumTypes()).containsExactly(STATE, new EnumType("payment_method", METHODS,
        EnumType.Storage.NAME));
  }

  @Test
  @DisplayName("Reads the rewrites of a table, by the names of its fields, in the order they run")
  void readRewrites() {
    Triggers triggers = new ProtoSchema().read(List.of(PostProto.getDescriptor())).table(TableName.of("post"))
        .orElseThrow().attribute(Triggers.class).orElseThrow();

    assertThat(triggers.ordered()).extracting(Rewrite::column).containsExactly("url_slug", "revision", "modified",
        "label");
    assertThat(triggers.ordered().getLast().reads()).containsExactly("label", "url_slug");
  }

  @Test
  @DisplayName("Reads the triggers of a table, by the names of the fields of each message")
  void readTriggers() {
    Triggers triggers = new ProtoSchema().read(List.of(PostProto.getDescriptor())).table(TableName.of("post"))
        .orElseThrow().attribute(Triggers.class).orElseThrow();

    assertThat(triggers.triggers()).containsExactly(
        new Trigger("log", List.of(Mutation.UPDATE), new ColumnChange("title"), List.of(new Trigger.Insert(TableName
            .of("post_event"),
            List.of(new Trigger.Assignment("post_id", ColumnReference.of("id")),
                new Trigger.Assignment("title", new ColumnReference("title", Row.OLD)))))),
        new Trigger("active", List.of(Mutation.INSERT), Trigger.ALWAYS, List.of(new Trigger.Assert(new ForeignColumn(
            ColumnReference.of("author_id"), TableName.of("author"), "id", "active"), "The author is not active."))),
        new Trigger("purge", List.of(Mutation.DELETE), Trigger.ALWAYS, List.of(new Trigger.Delete(TableName.of(
            "post_event"),
            new Comparison(Comparison.Operator.EQUALS, ColumnReference.of("post_id"), new ColumnReference(
                "id", Row.OLD))),
            new Trigger.Update(TableName.of("author"), new Comparison(Comparison.Operator.EQUALS,
                ColumnReference.of("id"), new ColumnReference("author_id", Row.OLD)),
                List.of(new Trigger.Assignment(
                    "active", new Literal(new BooleanType(), "false")))))));
  }
}
