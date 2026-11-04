module dev.horizon.leadseal.protobuf {
  requires transitive dev.horizon.leadseal;
  requires transitive com.google.protobuf;
  requires dev.horizon.aquery.cel;

  exports dev.horizon.leadseal.protobuf;
}
